package ru.kuzdikenov.ragupdater.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.kuzdikenov.ragupdater.domain.SyncJobStatus
import ru.kuzdikenov.ragupdater.domain.SyncJobType
import ru.kuzdikenov.ragupdater.integration.GitChangeType
import ru.kuzdikenov.ragupdater.integration.GitFileChange
import ru.kuzdikenov.ragupdater.integration.GitProviderClient
import ru.kuzdikenov.ragupdater.integration.RagDeleteDocumentRequest
import ru.kuzdikenov.ragupdater.integration.RagPlatformClient
import ru.kuzdikenov.ragupdater.integration.RagUpsertDocumentRequest
import ru.kuzdikenov.ragupdater.persistence.RagDocumentEntity
import ru.kuzdikenov.ragupdater.persistence.RagDocumentJpaRepository
import ru.kuzdikenov.ragupdater.persistence.RagRepositoryEntity
import ru.kuzdikenov.ragupdater.persistence.RagRepositoryJpaRepository
import ru.kuzdikenov.ragupdater.persistence.RagSyncJobEntity
import ru.kuzdikenov.ragupdater.persistence.RagSyncJobJpaRepository
import ru.kuzdikenov.ragupdater.persistence.RagSyncStateEntity
import ru.kuzdikenov.ragupdater.persistence.RagSyncStateJpaRepository
import java.time.Instant
import java.util.UUID

@Service
class DocumentSyncService(
    private val jobRepository: RagSyncJobJpaRepository,
    private val repositoryRepository: RagRepositoryJpaRepository,
    private val stateRepository: RagSyncStateJpaRepository,
    private val documentRepository: RagDocumentJpaRepository,
    private val gitProviderClient: GitProviderClient,
    private val ragPlatformClient: RagPlatformClient,
    private val docIdService: DocIdService,
    private val contentHashService: ContentHashService,
) {
    @Transactional
    fun process(jobId: UUID) {
        val job = jobRepository.findById(jobId).orElseThrow()
        check(job.status == SyncJobStatus.PROCESSING) { "Job $jobId находится не в состоянии PROCESSING" }
        val repository = repositoryRepository.findById(requireNotNull(job.repositoryId)).orElseThrow()
        val pathRegex = Regex(repository.docsPathRegex)

        when (job.jobType) {
            SyncJobType.INCREMENTAL -> syncIncremental(repository, job, pathRegex)
            SyncJobType.RECONCILE -> reconcile(repository, job, pathRegex)
        }
        recordSuccessfulState(repository, job)
    }

    private fun syncIncremental(repository: RagRepositoryEntity, job: RagSyncJobEntity, pathRegex: Regex) {
        val currentState = stateRepository.findByRepositoryIdAndBranch(requireNotNull(repository.id), job.branch)
        val fromSha = currentState?.lastSuccessfulCommitSha ?: job.beforeSha
        val changes = gitProviderClient.compareChanges(repository.projectId, fromSha, job.afterSha, pathRegex)
        changes.forEach { change -> processChange(repository, job, change) }
    }

    private fun processChange(repository: RagRepositoryEntity, job: RagSyncJobEntity, change: GitFileChange) {
        when (change.changeType) {
            GitChangeType.ADDED, GitChangeType.MODIFIED -> upsertPath(repository, job, requireNotNull(change.newPath))
            GitChangeType.DELETED -> deletePath(repository, job, requireNotNull(change.oldPath))
            GitChangeType.RENAMED -> {
                // TODO(PRODUCTION): сохранять идентичность при переименовании, если frontmatter содержит стабильный doc_id.
                change.oldPath?.let { deletePath(repository, job, it) }
                change.newPath?.let { upsertPath(repository, job, it) }
            }
        }
    }

    private fun reconcile(repository: RagRepositoryEntity, job: RagSyncJobEntity, pathRegex: Regex) {
        val pathsInGit = gitProviderClient.listFiles(repository.projectId, job.afterSha, pathRegex).toSet()
        pathsInGit.forEach { upsertPath(repository, job, it) }
        documentRepository.findAllByRepositoryIdAndBranch(requireNotNull(repository.id), job.branch)
            .filter { it.path !in pathsInGit }
            .forEach { deleteDocument(repository, job, it) }
    }

    private fun upsertPath(repository: RagRepositoryEntity, job: RagSyncJobEntity, path: String) {
        val content = gitProviderClient.readFile(repository.projectId, path, job.afterSha)
        val contentHash = contentHashService.hash(content)
        val docId = docIdService.resolve(repository.projectPath, job.branch, path, content)
        val repositoryId = requireNotNull(repository.id)
        val existing = documentRepository.findByRepositoryIdAndBranchAndDocId(repositoryId, job.branch, docId)
        if (existing != null && existing.contentHash == contentHash) {
            existing.commitSha = job.afterSha
            existing.path = path
            return
        }
        val existingAtPath = documentRepository.findByRepositoryIdAndBranchAndPath(repositoryId, job.branch, path)
        if (existingAtPath != null && existingAtPath.docId != docId) deleteDocument(repository, job, existingAtPath)

        val response = ragPlatformClient.upsertDocument(
            RagUpsertDocumentRequest(
                collection = repository.ragCollection,
                docId = docId,
                path = path,
                content = content,
                metadata = metadata(repository, job, path, docId, contentHash),
            ),
        )
        val document = existing ?: RagDocumentEntity(repositoryId = repositoryId, branch = job.branch, docId = docId)
        document.path = path
        document.contentHash = contentHash
        document.commitSha = job.afterSha
        document.ragDocumentId = response.ragDocumentId
        documentRepository.save(document)
    }

    private fun deletePath(repository: RagRepositoryEntity, job: RagSyncJobEntity, path: String) {
        val document = documentRepository.findByRepositoryIdAndBranchAndPath(requireNotNull(repository.id), job.branch, path) ?: return
        deleteDocument(repository, job, document)
    }

    private fun deleteDocument(repository: RagRepositoryEntity, job: RagSyncJobEntity, document: RagDocumentEntity) {
        ragPlatformClient.deleteDocument(
            RagDeleteDocumentRequest(
                collection = repository.ragCollection,
                docId = document.docId,
                metadata = metadata(repository, job, document.path, document.docId, document.contentHash),
            ),
        )
        documentRepository.delete(document)
    }

    private fun recordSuccessfulState(repository: RagRepositoryEntity, job: RagSyncJobEntity) {
        val state = stateRepository.findByRepositoryIdAndBranch(requireNotNull(repository.id), job.branch)
            ?: RagSyncStateEntity(repositoryId = repository.id, branch = job.branch)
        state.lastSuccessfulCommitSha = job.afterSha
        state.lastSuccessAt = Instant.now()
        state.status = SyncJobStatus.SUCCESS
        stateRepository.save(state)
    }

    private fun metadata(
        repository: RagRepositoryEntity,
        job: RagSyncJobEntity,
        path: String,
        docId: String,
        contentHash: String,
    ) = mapOf(
        "projectId" to repository.projectId,
        "projectPath" to repository.projectPath,
        "branch" to job.branch,
        "commitSha" to job.afterSha,
        "path" to path,
        "docId" to docId,
        "contentHash" to contentHash,
        "sourceType" to "generated-md",
    )
}
