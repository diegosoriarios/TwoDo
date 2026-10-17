package com.diego.twodo.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diego.twodo.R
import com.diego.twodo.domain.model.Task
import com.diego.twodo.ui.components.AppHeader
import com.diego.twodo.ui.components.CalendarCard
import com.diego.twodo.ui.components.SectionHeader
import com.diego.twodo.ui.components.TaskRow
import com.diego.twodo.ui.components.sectionDateFormatter
import com.diego.twodo.ui.components.sortedForDisplay
import com.diego.twodo.ui.composer.TaskComposerSheet
import com.diego.twodo.ui.tasks.TasksViewModel
import com.diego.twodo.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun CalendarScreen(
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val tasks by viewModel.tasks.collectAsState()
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var editingTaskId by remember { mutableStateOf<Long?>(null) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val tasksForDay = remember(tasks, selectedEpochDay) {
        tasks.filterDueOn(selectedDate)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
            if (tasksForDay.isEmpty()) {
                item(key = "empty") {
                    CalendarEmptyState(modifier = Modifier.fillMaxWidth())
                }
            } else {
                items(tasksForDay, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onToggle = viewModel::toggleTaskCompletion,
                        onEdit = { editingTaskId = it.id },
                        onDelete = viewModel::deleteTask,
                    )
                }
            }
        }
    }

    tasks.firstOrNull { it.id == editingTaskId }?.let { task ->
        TaskComposerSheet(
            onDismiss = { editingTaskId = null },
            onSubmit = { title, bucket, dueDate, reminderAt ->
                viewModel.updateTask(task.id, title, bucket, dueDate, reminderAt)
                editingTaskId = null
            },
            initialTitle = task.title,
            initialBucket = task.bucket,
            initialDueDate = task.dueDate,
            initialReminderAt = task.reminderAt,
        )
    }
}

private fun List<Task>.filterDueOn(date: LocalDate): List<Task> {
    val zone = ZoneId.systemDefault()
    val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return filter { it.dueDate != null && it.dueDate >= dayStart && it.dueDate < dayEnd }
        .sortedForDisplay()
}

@Composable
private fun CalendarEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "🗓️", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.calendar_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.calendar_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
