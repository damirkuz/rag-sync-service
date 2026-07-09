package ru.kuzdikenov.ragupdater.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RagDocumentJpaRepository : JpaRepository<RagDocumentEntity, UUID> {
    fun findByRepositoryIdAndBranchAndDocId(repositoryId: UUID, branch: String, docId: String): RagDocumentEntity?
    fun findByRepositoryIdAndBranchAndPath(repositoryId: UUID, branch: String, path: String): RagDocumentEntity?
    fun findAllByRepositoryIdAndBranch(repositoryId: UUID, branch: String): List<RagDocumentEntity>
}
