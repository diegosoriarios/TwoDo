package com.diego.twodo

import com.diego.twodo.domain.model.Task
import com.diego.twodo.ui.components.CalendarDayStatus
import com.diego.twodo.ui.components.calendarDayStatuses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CalendarDayStatusTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val today: LocalDate = LocalDate.now(zone)
    private val todayMillis: Long = today.atStartOfDay(zone).toInstant().toEpochMilli()
    private val tomorrow: LocalDate = today.plusDays(1)
    private val tomorrowMillis: Long = tomorrow.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun task(
        id: Long,
        dueDate: Long? = todayMillis,
        isCompleted: Boolean = false,
    ) = Task(
        id = id,
        title = "task $id",
        dueDate = dueDate,
        isCompleted = isCompleted,
    )

    @Test
    fun `day with any pending task is pending`() {
        val statuses = calendarDayStatuses(
            listOf(
                task(1, isCompleted = true),
                task(2, isCompleted = false),
            ),
            zone,
        )
        assertEquals(CalendarDayStatus.PENDING, statuses[today])
    }

    @Test
    fun `day with only completed tasks is completed`() {
        val statuses = calendarDayStatuses(
            listOf(
                task(1, isCompleted = true),
                task(2, isCompleted = true),
            ),
            zone,
        )
        assertEquals(CalendarDayStatus.COMPLETED, statuses[today])
    }

    @Test
    fun `days without tasks have no status`() {
        val statuses = calendarDayStatuses(listOf(task(1, dueDate = tomorrowMillis)), zone)
        assertNull(statuses[today])
        assertEquals(CalendarDayStatus.PENDING, statuses[tomorrow])
    }

    @Test
    fun `different due dates are grouped separately`() {
        val statuses = calendarDayStatuses(
            listOf(
                task(1, dueDate = todayMillis, isCompleted = false),
                task(2, dueDate = tomorrowMillis, isCompleted = true),
            ),
            zone,
        )
        assertEquals(CalendarDayStatus.PENDING, statuses[today])
        assertEquals(CalendarDayStatus.COMPLETED, statuses[tomorrow])
    }

    @Test
    fun `undated tasks count as today`() {
        val statuses = calendarDayStatuses(listOf(task(1, dueDate = null)), zone)
        assertEquals(CalendarDayStatus.PENDING, statuses[today])
    }
}
