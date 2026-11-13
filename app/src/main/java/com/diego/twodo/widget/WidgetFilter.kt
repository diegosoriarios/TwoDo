package com.diego.twodo.widget

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.diego.twodo.R
import com.diego.twodo.domain.model.Task
import com.diego.twodo.domain.model.TaskBucket
import java.time.LocalDate
import java.time.ZoneId

val KEY_WIDGET_FILTER = stringPreferencesKey("tasks_filter")

fun Preferences?.widgetFilterOrDefault(): WidgetFilter =
    this?.get(KEY_WIDGET_FILTER)?.let { name -> WidgetFilter.entries.firstOrNull { it.name == name } }
        ?: WidgetFilter.TODAY

enum class WidgetFilter {
    TODAY, TWO_MIN, THIRTY_MIN, PREVIOUS, FUTURE, ALL;

    fun apply(tasks: List<Task>, zone: ZoneId = ZoneId.systemDefault()): List<Task> {
        val now = LocalDate.now(zone)
        val todayStart = now.atStartOfDay(zone).toInstant().toEpochMilli()
        val tomorrowStart = now.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val filtered = when (this) {
            TODAY -> tasks.filter { (it.dueDate ?: todayStart) < tomorrowStart }
            TWO_MIN -> tasks.filter { it.bucket == TaskBucket.TWO_MIN }
            THIRTY_MIN -> tasks.filter { it.bucket == TaskBucket.THIRTY_MIN }
            PREVIOUS -> tasks.filter { it.dueDate != null && it.dueDate < todayStart }
            FUTURE -> tasks.filter { it.dueDate != null && it.dueDate >= tomorrowStart }
            ALL -> tasks
        }
        return filtered.sortedWith(
            when (this) {
                ALL -> compareBy<Task>({ bucketOrder(it.bucket) })
                    .thenByDescending { it.createdAt }
                else -> compareBy<Task>({ it.dueDate ?: todayStart })
                    .thenByDescending { it.createdAt }
            },
        )
    }

    fun labelRes(): Int = when (this) {
        TODAY -> R.string.widget_today
        TWO_MIN -> R.string.widget_filter_two_min
        THIRTY_MIN -> R.string.widget_filter_thirty_min
        PREVIOUS -> R.string.widget_filter_previous
        FUTURE -> R.string.widget_filter_future
        ALL -> R.string.widget_filter_all
    }

    fun applySectioned(tasks: List<Task>, zone: ZoneId = ZoneId.systemDefault()): List<WidgetSection> =
        when (this) {
            TODAY -> {
                val sorted = apply(tasks, zone)
                val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
                val isPrevious: (Task) -> Boolean = { it.dueDate != null && it.dueDate < todayStart }
                val today = sorted.filterNot(isPrevious)
                val previous = sorted.filter(isPrevious)
                buildList {
                    if (today.isNotEmpty()) add(WidgetSection(R.string.widget_today, today))
                    if (previous.isNotEmpty()) add(WidgetSection(R.string.widget_filter_previous, previous))
                }
            }
            else -> listOf(WidgetSection(null, apply(tasks, zone)))
        }
}

data class WidgetSection(
    val titleRes: Int?,
    val tasks: List<Task>,
)

private fun bucketOrder(bucket: TaskBucket): Int = when (bucket) {
    TaskBucket.TWO_MIN -> 0
    TaskBucket.THIRTY_MIN -> 1
    TaskBucket.LONG -> 2
    TaskBucket.UNCLASSIFIED -> 3
}
