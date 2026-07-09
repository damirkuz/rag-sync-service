package ru.kuzdikenov.ragupdater.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import ru.kuzdikenov.ragupdater.RagUpdaterApplication
import ru.kuzdikenov.ragupdater.domain.SyncJobStatus
import ru.kuzdikenov.ragupdater.domain.SyncJobType
import ru.kuzdikenov.ragupdater.integration.GitChangeType
import ru.kuzdikenov.ragupdater.integration.GitFileChange
import ru.kuzdikenov.ragupdater.integration.GitProviderClient
import ru.kuzdikenov.ragupdater.integration.RagDeleteDocumentRequest
import ru.kuzdikenov.ragupdater.integration.RagPlatformClient
import ru.kuzdikenov.ragupdater.integration.RagUpsertDocumentRequest
import ru.kuzdikenov.ragupdater.integration.RagUpsertDocumentResponse
import ru.kuzdikenov.ragupdater.persistence.RagDocumentEntity
import ru.kuzdikenov.ragupdater.persistence.RagDocumentJpaRepository
import ru.kuzdikenov.ragupdater.persistence.RagRepositoryEntity
import ru.kuzdikenov.ragupdater.persistence.RagRepositoryJpaRepository
import ru.kuzdikenov.ragupdater.persistence.RagSyncJobEntity
import ru.kuzdikenov.ragupdater.persistence.RagSyncJobJpaRepository
import ru.kuzdikenov.ragupdater.persistence.RagSyncStateJpaRepository
import kotlin.test.assertEquals

@SpringBootTest(
    classes = [RagUpdaterApplication::class, DocumentSyncServiceTest.TestClients::class],
    properties = ["rag-sync.worker.enabled=false"],
)
class DocumentSyncServiceTest @Autowired constructor(
    private val documentSyncService: DocumentSyncService,
    private val hashService: ContentHashService,
    private val git: RecordingGitProviderClient,
    private val rag: RecordingRagPlatformClient,
    private val documentRepository: RagDocumentJpaRepository,
    private val stateRepository: RagSyncStateJpaRepository,
    private val jobRepository: RagSyncJobJpaRepository,
    private val repositoryRepository: RagRepositoryJpaRepository,
) {
    @BeforeEach
    fun reset() {
        documentRepository.deleteAll()
        stateRepository.deleteAll()
        jobRepository.deleteAll()
        repositoryRepository.deleteAll()
        git.reset()
        rag.reset()
    }

    @Test
    fun `modified file invokes upsert`() {
        val job = jobForChange(GitFileChange(null, "docs/example.md", GitChangeType.MODIFIED))
        git.contents["docs/example.md"] = "# updated"

        documentSyncService.process(requireNotNull(job.id))

        assertEquals(1, rag.upserts.size)
        assertEquals("docs/example.md", rag.upserts.single().path)
    }

    @Test
    fun `deleted file invokes delete`() {
        val job = jobForChange(GitFileChange("docs/example.md", null, GitChangeType.DELETED))
        documentRepository.save(
            RagDocumentEntity(
                repositoryId = job.repositoryId,
                branch = job.branch,
                path = "docs/example.md",
                docId = "docs/example",
                contentHash = "hash",
                commitSha = "before",
                ragDocumentId = "remote-1",
            ),
        )

        documentSyncService.process(requireNotNull(job.id))

        assertEquals(1, rag.deletes.size)
        assertEquals(0, documentRepository.count())
    }

    @Test
    fun `unchanged content hash skips repeat upsert`() {
        val content = "# unchanged"
        val job = jobForChange(GitFileChange(null, "docs/example.md", GitChangeType.MODIFIED))
        git.contents["docs/example.md"] = content
        documentRepository.save(
            RagDocumentEntity(
                repositoryId = job.repositoryId,
                branch = job.branch,
                path = "docs/example.md",
                docId = "backend/payments:master:docs/example.md",
                contentHash = hashService.hash(content),
                commitSha = "before",
                ragDocumentId = "remote-1",
            ),
        )

        documentSyncService.process(requireNotNull(job.id))

        assertEquals(0, rag.upserts.size)
    }

    private fun jobForChange(change: GitFileChange): RagSyncJobEntity {
        val repository = repositoryRepository.save(
            RagRepositoryEntity(projectId = "123", projectPath = "backend/payments", defaultBranch = "master"),
        )
        git.changes = listOf(change)
        return jobRepository.save(
            RagSyncJobEntity(
                repositoryId = repository.id,
                jobType = SyncJobType.INCREMENTAL,
                branch = "master",
                beforeSha = "before",
                afterSha = "after",
                status = SyncJobStatus.PROCESSING,
            ),
        )
    }

    @TestConfiguration
    class TestClients {
        @Bean
        @Primary
        fun recordingGitProviderClient() = RecordingGitProviderClient()

        @Bean
        @Primary
        fun recordingRagPlatformClient() = RecordingRagPlatformClient()
    }
}

class RecordingGitProviderClient : GitProviderClient {
    var changes: List<GitFileChange> = emptyList()
    val contents = mutableMapOf<String, String>()

    override fun compareChanges(projectId: String, fromSha: String?, toSha: String, pathRegex: Regex) = changes
    override fun readFile(projectId: String, path: String, ref: String) = contents.getValue(path)
    override fun listFiles(projectId: String, ref: String, pathRegex: Regex) = contents.keys.filter { pathRegex.matches(it) }
    fun reset() {
        changes = emptyList()
        contents.clear()
    }
}

class RecordingRagPlatformClient : RagPlatformClient {
    val upserts = mutableListOf<RagUpsertDocumentRequest>()
    val deletes = mutableListOf<RagDeleteDocumentRequest>()

    override fun upsertDocument(request: RagUpsertDocumentRequest): RagUpsertDocumentResponse {
        upserts += request
        return RagUpsertDocumentResponse("recording-${upserts.size}")
    }

    override fun deleteDocument(request: RagDeleteDocumentRequest) {
        deletes += request
    }

    fun reset() {
        upserts.clear()
        deletes.clear()
    }
}
