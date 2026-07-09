package ru.kuzdikenov.ragupdater.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RagSyncStateJpaRepository : JpaRepository<RagSyncStateEntity, UUID> {
    fun findByRepositoryIdAndBranch(repositoryId: UUID, branch: String): RagSyncStateEntity?
}
