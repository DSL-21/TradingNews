package com.tradingnews.app.data

import java.time.Instant

data class EconomicEvent(
    val id: String,
    val time: Instant,
    val country: String,
    val category: String,
    val event: String,
    val previous: String,
    val forecast: String,
    val actual: String,
    val importance: Int,
    val currency: String,
    val timeIsEstimated: Boolean,
    val updatedAt: Instant?
) {
    val importanceLabel: String
        get() = when (importance) {
            3 -> "HIGH"
            2 -> "MEDIUM"
            else -> "LOW"
        }

    val isReleased: Boolean
        get() = actual.isNotBlank()
}

enum class CalendarLoadStatus {
    LOADING,
    READY,
    UNCONFIGURED,
    OFFLINE,
    ERROR
}
