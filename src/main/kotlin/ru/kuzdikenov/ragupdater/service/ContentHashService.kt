package ru.kuzdikenov.ragupdater.service

import org.springframework.stereotype.Service
import java.security.MessageDigest

@Service
class ContentHashService {
    fun hash(content: String): String {
        val normalized = content
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lineSequence()
            .joinToString("\n") { it.trimEnd() }
        val bytes = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
