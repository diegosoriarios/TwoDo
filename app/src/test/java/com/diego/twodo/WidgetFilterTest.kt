package com.diego.twodo

import com.diego.twodo.domain.model.Task
import com.diego.twodo.domain.model.TaskBucket
import com.diego.twodo.widget.WidgetFilter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WidgetFilterTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val todayStart: Long =
        LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    private val tomorrowStart: Long =
        LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun task(
        id: Long,
        bucket: TaskBucket = TaskBucket.TWO_MIN,
        dueDate: Long? = null,
        createdAt: Long = id,
    ) = Task(
        id = id,
        title = "task $id",
        bucket = bucket,
        dueDate = dueDate,
        createdAt = createdAt,
    )

    private val tasks = listOf(
        task(1, TaskBucket.TWO_MIN, dueDate = todayStart),
        task(2, TaskBucket.THIRTY_MIN, dueDate = null),
        task(3, TaskBucket.LONG, dueDate = todayStart - 1),
        task(4, TaskBucket.TWO_MIN, dueDate = tomorrowStart),
        task(5, TaskBucket.THIRTY_MIN, dueDate = tomorrowStart + 1),
        task(6, TaskBucket.UNCLASSIFIED, dueDate = todayStart),
    )

    @Test
    fun `today includes due today, undated and overdue, excludes future`() {
        val result = WidgetFilter.TODAY.apply(tasks, zone).map { it.id }
        assertEquals(listOf(3L, 1L, 2L, 6L), result)
    }

    @Test
    fun `two min includes only two min bucket regardless of date`() {
        val result = WidgetFilter.TWO_MIN.apply(tasks, zone).map { it.id }
        assertEquals(listOf(1L, 4L), result)
    }

    @Test
    fun `thirty min includes only thirty min bucket regardless of date`() {
        val result = WidgetFilter.THIRTY_MIN.apply(tasks, zone).map { it.id }
        assertEquals(listOf(2L, 5L), result)
    }

    @Test
    fun `previous includes only overdue`() {
        val result = WidgetFilter.PREVIOUS.apply(tasks, zone).map { it.id }
        assertEquals(listOf(3L), result)
    }

    @Test
    fun `future includes only tasks due tomorrow or later`() {
        val result = WidgetFilter.FUTURE.apply(tasks, zone).map { it.id }
        assertEquals(listOf(4L, 5L), result)
    }

    @Test
    fun `all includes everything sorted by bucket then createdAt`() {
        val result = WidgetFilter.ALL.apply(tasks, zone).map { it.id }
        assertEquals(listOf(1L, 4L, 2L, 5L, 3L, 6L), result)
    }

    @Test
    fun `undated task with two min bucket is not previous`() {
        val result = WidgetFilter.PREVIOUS.apply(listOf(task(1, dueDate = null)), zone)
        assertEquals(0, result.size)
    }

    @Test
    fun `today sorts undated tasks as today by createdAt`() {
        val result = WidgetFilter.TODAY.apply(tasks, zone)
            .filter { it.dueDate == null || it.dueDate >= todayStart }
            .map { it.id }
        assertEquals(listOf(1L, 2L, 6L), result)
    }
}
