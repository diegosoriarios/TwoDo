package com.diego.twodo.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diego.twodo.R
import com.diego.twodo.ui.components.AppHeader
import com.diego.twodo.ui.components.CalendarCard
import com.diego.twodo.ui.components.SectionHeader
import com.diego.twodo.ui.components.TaskRow
import com.diego.twodo.ui.components.sectionDateFormatter
import com.diego.twodo.ui.components.sortedForDisplay
import com.diego.twodo.ui.tasks.BucketOverrideSheet
import com.diego.twodo.ui.tasks.TasksViewModel
import java.time.LocalDate

@Composable
fun CalendarScreen(
    onToggleCalendar: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val tasks by viewModel.tasks.collectAsState()
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var taskToRelabelId by remember { mutableStateOf<Long?>(null) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(calendarHighlighted = true, onCalendarClick = onToggleCalendar)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "calendar") {
                CalendarCard(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedEpochDay = it.toEpochDay() },
                )
            }
            item(key = "section") {
                SectionHeader(
                    title = stringResource(R.string.task_list_title),
                    dateLabel = selectedDate.format(sectionDateFormatter),
                )
            }
            items(tasks.sortedForDisplay(), key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    onToggle = viewModel::toggleTaskCompletion,
                    onRelabel = { taskToRelabelId = it.id },
                    onDelete = viewModel::deleteTask,
                )
            }
        }
    }

    taskToRelabelId?.let { taskId ->
        BucketOverrideSheet(
            onDismiss = { taskToRelabelId = null },
            onSelect = { bucket ->
                viewModel.overrideBucket(taskId, bucket)
                taskToRelabelId = null
            },
        )
    }
}
