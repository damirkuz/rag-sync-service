package ru.kuzdikenov.ragupdater.service

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.kuzdikenov.ragupdater.config.RagSyncProperties

@Component
@ConditionalOnProperty(prefix = "rag-sync.worker", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class SyncWorker(
    private val syncJobService: SyncJobService,
    private val documentSyncService: DocumentSyncService,
    private val properties: RagSyncProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${rag-sync.worker.fixed-delay-ms:5000}")
    fun processPendingJobs() {
        syncJobService.claimPendingJobs(properties.worker.batchSize).forEach { jobId ->
            try {
                documentSyncService.process(jobId)
                syncJobService.markSuccess(jobId)
            } catch (error: Exception) {
                logger.error("Job синхронизации RAG {} завершилась с ошибкой", jobId, error)
                syncJobService.markFailed(jobId, error)
            }
        }
    }
}
