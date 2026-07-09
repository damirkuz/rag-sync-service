package ru.kuzdikenov.ragupdater.api

import ru.kuzdikenov.ragupdater.domain.SyncJobStatus
import ru.kuzdikenov.ragupdater.persistence.RagSyncJobEntity
import java.time.Instant
import java.util.UUID

data class AcceptedSyncResponse(val status: String = "accepted", val jobId: UUID)

data class SyncJobResponse(
    val jobId: UUID,
    val status: SyncJobStatus,
    val projectPath: String,
    val branch: String,
    val beforeSha: String?,
    val afterSha: String,
    val createdAt: Instant,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val errorMessage: String?,
)

fun RagSyncJobEntity.toResponse(projectPath: String) = SyncJobResponse(
    jobId = requireNotNull(id),
    status = status,
    projectPath = projectPath,
    branch = branch,
    beforeSha = beforeSha,
    afterSha = afterSha,
    createdAt = createdAt,
    startedAt = startedAt,
    finishedAt = finishedAt,
    errorMessage = errorMessage,
)
