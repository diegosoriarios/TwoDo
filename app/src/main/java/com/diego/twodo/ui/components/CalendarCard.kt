package com.diego.twodo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.diego.twodo.ui.theme.CalendarDotDone
import com.diego.twodo.ui.theme.CalendarDotPending
import com.diego.twodo.ui.theme.Coral
import com.diego.twodo.ui.theme.SurfaceDark
import com.diego.twodo.ui.theme.TextPrimary
import com.diego.twodo.ui.theme.TextSecondary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private val monthHeaderFormatter = java.time.format.DateTimeFormatter.ofPattern("LLLL yyyy")

@Composable
fun CalendarCard(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    dayStatuses: Map<LocalDate, CalendarDayStatus> = emptyMap(),
) {
    var visibleMonthEpochDay by rememberSaveable {
        mutableLongStateOf(selectedDate.withDayOfMonth(1).toEpochDay())
    }
    val visibleMonth = LocalDate.ofEpochDay(visibleMonthEpochDay)
    val locale = Locale.getDefault()
    val firstDayOfWeek: DayOfWeek = WeekFields.of(locale).firstDayOfWeek

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = visibleMonth.format(monthHeaderFormatter).replaceFirstChar { it.uppercase(locale) },
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { visibleMonthEpochDay = visibleMonth.minusMonths(1).toEpochDay() },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(
                onClick = { visibleMonthEpochDay = visibleMonth.plusMonths(1).toEpochDay() },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            (0..6).map { firstDayOfWeek.plus(it.toLong()) }.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, locale).uppercase(locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val firstOfMonth = visibleMonth.withDayOfMonth(1)
        val leadingEmpty = (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val cells: List<LocalDate?> = List(leadingEmpty) { null } +
            (0 until visibleMonth.lengthOfMonth()).map { firstOfMonth.plusDays(it.toLong()) }
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) {
                            val isSelected = day == selectedDate
                            val status = dayStatuses[day]
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onDateSelected(day) },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Coral else Color.Transparent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = day.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) TextPrimary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                if (status != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (status) {
                                                    CalendarDayStatus.PENDING -> CalendarDotPending
                                                    CalendarDayStatus.COMPLETED -> CalendarDotDone
                                                },
                                            ),
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(4.dp))
                                }
                            }
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
