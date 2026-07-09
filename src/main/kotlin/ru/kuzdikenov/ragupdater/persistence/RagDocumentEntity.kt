package ru.kuzdikenov.ragupdater.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "rag_documents")
class RagDocumentEntity(
    @Id @GeneratedValue
    var id: UUID? = null,
    @Column(name = "repository_id", nullable = false)
    var repositoryId: UUID? = null,
    @Column(nullable = false)
    var branch: String = "",
    @Column(nullable = false)
    var path: String = "",
    @Column(name = "doc_id", nullable = false)
    var docId: String = "",
    @Column(name = "content_hash", nullable = false)
    var contentHash: String = "",
    @Column(name = "commit_sha", nullable = false)
    var commitSha: String = "",
    @Column(name = "rag_document_id", nullable = false)
    var ragDocumentId: String = "",
) : AuditedEntity()
