package ru.kuzdikenov.ragupdater.integration

interface RagPlatformClient {
    fun upsertDocument(request: RagUpsertDocumentRequest): RagUpsertDocumentResponse
    fun deleteDocument(request: RagDeleteDocumentRequest)
}

data class RagUpsertDocumentRequest(
    val collection: String,
    val docId: String,
    val path: String,
    val content: String,
    val metadata: Map<String, String>,
)

data class RagUpsertDocumentResponse(val ragDocumentId: String)

data class RagDeleteDocumentRequest(
    val collection: String,
    val docId: String,
    val metadata: Map<String, String>,
)
