package ru.kuzdikenov.ragupdater.integration

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

/** Безопасный фиктивный адаптер: он логирует намерение, но не вызывает внешнюю RAG-платформу. */
@Component
class
StubRagPlatformClient : RagPlatformClient {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun upsertDocument(request: RagUpsertDocumentRequest): RagUpsertDocumentResponse {
        logger.info("Stub RAG: upsert документа, collection={}, docId={}, path={}", request.collection, request.docId, request.path)
        // TODO(PRODUCTION): заменить StubRagPlatformClient настоящим API-клиентом RAG-платформы.
        return RagUpsertDocumentResponse("stub-${UUID.randomUUID()}")
    }

    override fun deleteDocument(request: RagDeleteDocumentRequest) {
        logger.info("Stub RAG: удаление документа, collection={}, docId={}", request.collection, request.docId)
        // TODO(PRODUCTION): вызвать настоящий API RAG-платформы для удаления документа.
    }
}
