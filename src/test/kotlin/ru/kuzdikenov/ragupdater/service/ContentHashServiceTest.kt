package ru.kuzdikenov.ragupdater.service

import kotlin.test.Test
import kotlin.test.assertEquals

class ContentHashServiceTest {
    private val service = ContentHashService()

    @Test
    fun `same text has same hash`() {
        assertEquals(service.hash("hello\nworld"), service.hash("hello\nworld"))
    }

    @Test
    fun `CRLF and LF are normalized`() {
        assertEquals(service.hash("hello\r\nworld  \r\n"), service.hash("hello\nworld\n"))
    }
}
