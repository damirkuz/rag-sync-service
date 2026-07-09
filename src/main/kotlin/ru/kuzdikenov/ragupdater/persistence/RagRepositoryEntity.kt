package ru.kuzdikenov.ragupdater.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "rag_repositories")
class RagRepositoryEntity(
    @Id @GeneratedValue
    var id: UUID? = null,
    @Column(name = "project_id", nullable = false, unique = true)
    var projectId: String = "",
    @Column(name = "project_path", nullable = false)
    var projectPath: String = "",
    @Column(name = "default_branch", nullable = false)
    var defaultBranch: String = "master",
    @Column(name = "docs_path_regex", nullable = false)
    var docsPathRegex: String = "^docs/.*\\.md$",
    @Column(name = "rag_collection", nullable = false)
    var ragCollection: String = "default",
    @Column(nullable = false)
    var enabled: Boolean = true,
) : AuditedEntity()
