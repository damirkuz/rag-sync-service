package ru.kuzdikenov.ragupdater.domain

enum class SyncJobStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    IGNORED,
}
