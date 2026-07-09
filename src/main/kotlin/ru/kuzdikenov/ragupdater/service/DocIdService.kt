package ru.kuzdikenov.ragupdater.service

import org.springframework.stereotype.Service

@Service
class DocIdService {
    fun resolve(projectPath: String, branch: String, path: String, content: String): String {
        return frontMatterDocId(content) ?: "$projectPath:$branch:$path"
    }

    private fun frontMatterDocId(content: String): String? {
        if (!content.startsWith("---")) return null
        val frontMatterEnd = content.indexOf("\n---", startIndex = 3)
        if (frontMatterEnd < 0) return null
        val frontMatter = content.substring(3, frontMatterEnd)
        return Regex("(?m)^\\s*doc_id\\s*:\\s*['\\\"]?([^'\\\"\\r\\n]+)")
            .find(frontMatter)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }
}
