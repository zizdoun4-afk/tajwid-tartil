package com.example.domain.model

enum class MemorizationStatus {
    NEW,
    LEARNING,
    REVIEW,
    MEMORIZED;

    companion object {
        fun fromString(value: String): MemorizationStatus {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                NEW
            }
        }
    }
}
