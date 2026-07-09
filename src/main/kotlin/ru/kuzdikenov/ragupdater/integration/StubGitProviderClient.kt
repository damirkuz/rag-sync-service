package ru.kuzdikenov.ragupdater.integration

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/** Безопасный неактивный адаптер: он не обращается к внешним системам. */
@Component
class StubGitProviderClient : GitProviderClient {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun compareChanges(projectId: String, fromSha: String?, toSha: String, pathRegex: Regex): List<GitFileChange> {
        logger.warn("Вызван StubGitProviderClient.compareChanges для проекта {}; изменения не возвращаются", projectId)
        // TODO(PRODUCTION): заменить StubGitProviderClient настоящей реализацией GitLab API.
        return emptyList()
    }

    override fun readFile(projectId: String, path: String, ref: String): String {
        logger.warn("Вызван StubGitProviderClient.readFile для {}/{}; внешний запрос не выполнялся", projectId, path)
        // TODO(PRODUCTION): получать содержимое файла из настроенного Git-провайдера.
        return ""
    }

    override fun listFiles(projectId: String, ref: String, pathRegex: Regex): List<String> {
        logger.warn("Вызван StubGitProviderClient.listFiles для проекта {}; файлы не возвращаются", projectId)
        // TODO(PRODUCTION): получать список Markdown-файлов из настроенного Git-провайдера.
        return emptyList()
    }
}
