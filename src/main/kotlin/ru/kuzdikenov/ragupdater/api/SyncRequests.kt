package ru.kuzdikenov.ragupdater.api

import jakarta.validation.constraints.NotBlank

data class GitEventRequest(
    @field:NotBlank val projectId: String,
    @field:NotBlank val projectPath: String,
    @field:NotBlank val branch: String,
    val beforeSha: String? = null,
    @field:NotBlank val afterSha: String,
)

data class ReconcileRequest(
    @field:NotBlank val projectId: String,
    @field:NotBlank val projectPath: String,
    @field:NotBlank val branch: String,
    @field:NotBlank val commitSha: String,
)
