package ru.kuzdikenov.ragupdater.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import ru.kuzdikenov.ragupdater.domain.SyncJobStatus
import ru.kuzdikenov.ragupdater.domain.SyncJobType
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "rag_sync_jobs")
class RagSyncJobEntity(
    @Id @GeneratedValue
    var id: UUID? = null,
    @Column(name = "repository_id", nullable = false)
    var repositoryId: UUID? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    var jobType: SyncJobType = SyncJobType.INCREMENTAL,
    @Column(nullable = false)
    var branch: String = "",
    @Column(name = "before_sha")
    var beforeSha: String? = null,
    @Column(name = "after_sha", nullable = false)
    var afterSha: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SyncJobStatus = SyncJobStatus.PENDING,
    @Column(nullable = false)
    var attempts: Int = 0,
    @Column(name = "error_message", length = 4_000)
    var errorMessage: String? = null,
    @Column(name = "started_at")
    var startedAt: Instant? = null,
    @Column(name = "finished_at")
    var finishedAt: Instant? = null,
) : AuditedEntity()
