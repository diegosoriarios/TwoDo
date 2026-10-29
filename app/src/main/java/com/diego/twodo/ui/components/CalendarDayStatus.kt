package com.diego.twodo.ui.components

import com.diego.twodo.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class CalendarDayStatus {
    PENDING,
    COMPLETED,
}

fun calendarDayStatuses(
    tasks: List<Task>,
    zone: ZoneId = ZoneId.systemDefault(),
): Map<LocalDate, CalendarDayStatus> {
    val today = LocalDate.now(zone)
    val statuses = mutableMapOf<LocalDate, CalendarDayStatus>()
    tasks.forEach { task ->
        val day = task.dueDate
            ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            ?: today
        val current = statuses[day]
        statuses[day] = if (!task.isCompleted || current == CalendarDayStatus.PENDING) {
            CalendarDayStatus.PENDING
        } else {
            CalendarDayStatus.COMPLETED
        }
    }
    return statuses
}
