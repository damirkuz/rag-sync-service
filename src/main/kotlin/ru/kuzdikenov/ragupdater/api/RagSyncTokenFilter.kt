package ru.kuzdikenov.ragupdater.api

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import ru.kuzdikenov.ragupdater.config.RagSyncProperties

@Component
class RagSyncTokenFilter(private val properties: RagSyncProperties) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.requestURI.startsWith("/api/v1/rag-sync/")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (request.getHeader("X-Rag-Sync-Token") != properties.apiToken) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Заголовок X-Rag-Sync-Token отсутствует или содержит неверный токен")
            return
        }
        filterChain.doFilter(request, response)
    }
}
