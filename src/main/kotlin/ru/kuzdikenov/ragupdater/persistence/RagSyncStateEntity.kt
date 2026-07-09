package ru.kuzdikenov.ragupdater.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import ru.kuzdikenov.ragupdater.domain.SyncJobStatus
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "rag_sync_state")
class RagSyncStateEntity(
    @Id @GeneratedValue
    var id: UUID? = null,
    @Column(name = "repository_id", nullable = false)
    var repositoryId: UUID? = null,
    @Column(nullable = false)
    var branch: String = "",
    @Column(name = "last_successful_commit_sha")
    var lastSuccessfulCommitSha: String? = null,
    @Column(name = "last_success_at")
    var lastSuccessAt: Instant? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SyncJobStatus = SyncJobStatus.PENDING,
) : AuditedEntity()
