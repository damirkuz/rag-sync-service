package ru.kuzdikenov.ragupdater.persistence

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import ru.kuzdikenov.ragupdater.domain.SyncJobStatus
import java.util.UUID

interface RagSyncJobJpaRepository : JpaRepository<RagSyncJobEntity, UUID> {
    fun findByRepositoryIdAndBranchAndAfterSha(repositoryId: UUID, branch: String, afterSha: String): RagSyncJobEntity?
    fun findByStatusOrderByCreatedAtAsc(status: SyncJobStatus, pageable: Pageable): List<RagSyncJobEntity>
}
