package ru.kuzdikenov.ragupdater.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RagRepositoryJpaRepository : JpaRepository<RagRepositoryEntity, UUID> {
    fun findByProjectId(projectId: String): RagRepositoryEntity?
}
