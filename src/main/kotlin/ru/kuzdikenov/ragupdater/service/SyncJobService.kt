package ru.kuzdikenov.ragupdater.service

import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.kuzdikenov.ragupdater.api.SyncJobResponse
import ru.kuzdikenov.ragupdater.api.toResponse
import ru.kuzdikenov.ragupdater.domain.SyncJobStatus
import ru.kuzdikenov.ragupdater.persistence.RagRepositoryJpaRepository
import ru.kuzdikenov.ragupdater.persistence.RagSyncJobJpaRepository
import java.time.Instant
import java.util.UUID

@Service
class SyncJobService(
    private val jobRepository: RagSyncJobJpaRepository,
    private val repositoryRepository: RagRepositoryJpaRepository,
) {
    @Transactional
    fun claimPendingJobs(batchSize: Int): List<UUID> {
        // TODO(PRODUCTION): защитить захват job распределённой блокировкой по repository_id + branch.
        return jobRepository.findByStatusOrderByCreatedAtAsc(SyncJobStatus.PENDING, PageRequest.of(0, batchSize))
            .map { job ->
                job.status = SyncJobStatus.PROCESSING
                job.startedAt = Instant.now()
                job.attempts += 1
                requireNotNull(job.id)
            }
    }

    @Transactional
    fun markSuccess(jobId: UUID) {
        val job = jobRepository.findById(jobId).orElseThrow()
        job.status = SyncJobStatus.SUCCESS
        job.finishedAt = Instant.now()
        job.errorMessage = null
    }

    @Transactional
    fun markFailed(jobId: UUID, error: Throwable) {
        val job = jobRepository.findById(jobId).orElseThrow()
        job.status = SyncJobStatus.FAILED
        job.finishedAt = Instant.now()
        job.errorMessage = (error.message ?: error.javaClass.simpleName).take(4_000)
    }

    @Transactional(readOnly = true)
    fun getJob(jobId: UUID): SyncJobResponse {
        val job = jobRepository.findById(jobId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Job синхронизации $jobId не найдена") }
        val repository = repositoryRepository.findById(requireNotNull(job.repositoryId))
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Репозиторий для job $jobId не найден") }
        return job.toResponse(repository.projectPath)
    }
}
