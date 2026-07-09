package ru.kuzdikenov.ragupdater.integration

interface GitProviderClient {
    fun compareChanges(projectId: String, fromSha: String?, toSha: String, pathRegex: Regex): List<GitFileChange>
    fun readFile(projectId: String, path: String, ref: String): String
    fun listFiles(projectId: String, ref: String, pathRegex: Regex): List<String>
}

data class GitFileChange(
    val oldPath: String?,
    val newPath: String?,
    val changeType: GitChangeType,
)

enum class GitChangeType { ADDED, MODIFIED, DELETED, RENAMED }
