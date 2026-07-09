package ru.kuzdikenov.ragupdater.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.kuzdikenov.ragupdater.api.GitEventRequest
import ru.kuzdikenov.ragupdater.api.ReconcileRequest
import ru.kuzdikenov.ragupdater.config.RagSyncProperties
import ru.kuzdikenov.ragupdater.domain.SyncJobStatus
import ru.kuzdikenov.ragupdater.domain.SyncJobType
import ru.kuzdikenov.ragupdater.persistence.RagRepositoryEntity
import ru.kuzdikenov.ragupdater.persistence.RagRepositoryJpaRepository
import ru.kuzdikenov.ragupdater.persistence.RagSyncJobEntity
import ru.kuzdikenov.ragupdater.persistence.RagSyncJobJpaRepository

@Service
class SyncEventService(
    private val repositoryRepository: RagRepositoryJpaRepository,
    private val jobRepository: RagSyncJobJpaRepository,
    private val properties: RagSyncProperties,
) {
    @Transactional
    fun acceptGitEvent(request: GitEventRequest): RagSyncJobEntity = createJob(
        projectId = request.projectId,
        projectPath = request.projectPath,
        branch = request.branch,
        beforeSha = request.beforeSha?.takeIf { it.isNotBlank() },
        afterSha = request.afterSha,
        jobType = SyncJobType.INCREMENTAL,
    )

    @Transactional
    fun acceptReconcile(request: ReconcileRequest): RagSyncJobEntity = createJob(
        projectId = request.projectId,
        projectPath = request.projectPath,
        branch = request.branch,
        beforeSha = null,
        afterSha = request.commitSha,
        jobType = SyncJobType.RECONCILE,
    )

    private fun createJob(
        projectId: String,
        projectPath: String,
        branch: String,
        beforeSha: String?,
        afterSha: String,
        jobType: SyncJobType,
    ): RagSyncJobEntity {
        val repository = findOrCreateRepository(projectId, projectPath)
        val repositoryId = requireNotNull(repository.id)
        jobRepository.findByRepositoryIdAndBranchAndAfterSha(repositoryId, branch, afterSha)?.let { return it }

        val status = if (!repository.enabled || branch != repository.defaultBranch) SyncJobStatus.IGNORED else SyncJobStatus.PENDING
        return jobRepository.save(
            RagSyncJobEntity(
                repositoryId = repositoryId,
                jobType = jobType,
                branch = branch,
                beforeSha = beforeSha,
                afterSha = afterSha,
                status = status,
            ),
        )
    }

    private fun findOrCreateRepository(projectId: String, projectPath: String): RagRepositoryEntity {
        repositoryRepository.findByProjectId(projectId)?.let { return it }
        // TODO(PRODUCTION): вместо автоматической регистрации использовать явный whitelist или регистрацию репозиториев.
        return repositoryRepository.save(
            RagRepositoryEntity(
                projectId = projectId,
                projectPath = projectPath,
                defaultBranch = properties.defaults.defaultBranch,
                docsPathRegex = properties.defaults.docsPathRegex,
                ragCollection = properties.defaults.ragCollection,
            ),
        )
    }
}
