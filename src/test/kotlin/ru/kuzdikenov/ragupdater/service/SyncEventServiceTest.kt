package ru.kuzdikenov.ragupdater.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.kuzdikenov.ragupdater.api.GitEventRequest
import ru.kuzdikenov.ragupdater.domain.SyncJobStatus
import ru.kuzdikenov.ragupdater.persistence.RagDocumentJpaRepository
import ru.kuzdikenov.ragupdater.persistence.RagRepositoryJpaRepository
import ru.kuzdikenov.ragupdater.persistence.RagSyncJobJpaRepository
import ru.kuzdikenov.ragupdater.persistence.RagSyncStateJpaRepository
import kotlin.test.assertEquals

@SpringBootTest(properties = ["rag-sync.worker.enabled=false"])
class SyncEventServiceTest @Autowired constructor(
    private val eventService: SyncEventService,
    private val documentRepository: RagDocumentJpaRepository,
    private val stateRepository: RagSyncStateJpaRepository,
    private val jobRepository: RagSyncJobJpaRepository,
    private val repositoryRepository: RagRepositoryJpaRepository,
) {
    @BeforeEach
    fun clearDatabase() {
        documentRepository.deleteAll()
        stateRepository.deleteAll()
        jobRepository.deleteAll()
        repositoryRepository.deleteAll()
    }

    @Test
    fun `repeated event with same target sha returns original job`() {
        val request = GitEventRequest("123", "backend/payments", "master", "before", "after")

        val first = eventService.acceptGitEvent(request)
        val duplicate = eventService.acceptGitEvent(request)

        assertEquals(first.id, duplicate.id)
        assertEquals(1, jobRepository.count())
    }

    @Test
    fun `non default branch is ignored`() {
        val job = eventService.acceptGitEvent(GitEventRequest("123", "backend/payments", "feature/demo", "before", "after"))

        assertEquals(SyncJobStatus.IGNORED, job.status)
    }
}
