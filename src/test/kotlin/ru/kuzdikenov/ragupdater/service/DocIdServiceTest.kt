package ru.kuzdikenov.ragupdater.service

import kotlin.test.Test
import kotlin.test.assertEquals

class DocIdServiceTest {
    private val service = DocIdService()

    @Test
    fun `uses doc id from frontmatter`() {
        val content = """
            ---
            title: Example
            doc_id: payment-overview
            ---
            # Payment
        """.trimIndent()

        assertEquals("payment-overview", service.resolve("backend/payment", "master", "docs/payment.md", content))
    }

    @Test
    fun `falls back to project branch and path`() {
        assertEquals(
            "backend/payment:master:docs/payment.md",
            service.resolve("backend/payment", "master", "docs/payment.md", "# Payment"),
        )
    }
}
