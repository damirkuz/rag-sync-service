package ru.kuzdikenov.ragupdater.api

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.kuzdikenov.ragupdater.service.SyncEventService
import ru.kuzdikenov.ragupdater.service.SyncJobService
import java.util.UUID

@RestController
@RequestMapping("/api/v1/rag-sync")
class RagSyncController(
    private val syncEventService: SyncEventService,
    private val syncJobService: SyncJobService,
) {
    @PostMapping("/events/git")
    fun acceptGitEvent(@Valid @RequestBody request: GitEventRequest): ResponseEntity<AcceptedSyncResponse> {
        val job = syncEventService.acceptGitEvent(request)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(AcceptedSyncResponse(jobId = requireNotNull(job.id)))
    }

    @GetMapping("/jobs/{jobId}")
    fun getJob(@PathVariable jobId: UUID): SyncJobResponse = syncJobService.getJob(jobId)

    @PostMapping("/reconcile")
    fun reconcile(@Valid @RequestBody request: ReconcileRequest): ResponseEntity<AcceptedSyncResponse> {
        val job = syncEventService.acceptReconcile(request)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(AcceptedSyncResponse(jobId = requireNotNull(job.id)))
    }
}
