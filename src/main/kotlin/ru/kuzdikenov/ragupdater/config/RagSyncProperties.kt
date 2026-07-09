package ru.kuzdikenov.ragupdater.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("rag-sync")
data class RagSyncProperties(
    val apiToken: String = "local-dev-token",
    val worker: Worker = Worker(),
    val defaults: Defaults = Defaults(),
) {
    data class Worker(
        val enabled: Boolean = true,
        val fixedDelayMs: Long = 5_000,
        val batchSize: Int = 10,
    )

    data class Defaults(
        val defaultBranch: String = "master",
        val docsPathRegex: String = "^docs/.*\\.md$",
        val ragCollection: String = "default",
    )
}
