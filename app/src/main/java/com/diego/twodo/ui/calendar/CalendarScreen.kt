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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    val completedTasks by viewModel.completedTasks.collectAsState()
    val labels by viewModel.labels.collectAsState()
    val taskLabelMap by viewModel.taskLabelMap.collectAsState()
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var editingTaskId by remember { mutableStateOf<Long?>(null) }
    var taskToDeleteId by remember { mutableStateOf<Long?>(null) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val tasksForDay = remember(tasks, selectedEpochDay) { tasks.filterDueOn(selectedDate) }
    val completedForDay = remember(completedTasks, selectedEpochDay) {
        completedTasks.filterCompletedOn(selectedDate)
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
                        onDelete = { taskToDeleteId = it },
                        labels = taskLabelMap[task.id].orEmpty(),
                    )
                }
            }
            if (completedForDay.isNotEmpty()) {
                item(key = "completed_section") {
                    SectionHeader(
                        title = stringResource(R.string.calendar_completed_title),
                        dateLabel = selectedDate.format(sectionDateFormatter),
                    )
                }
                items(completedForDay, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onToggle = viewModel::toggleTaskCompletion,
                        onEdit = { editingTaskId = it.id },
                        onDelete = { taskToDeleteId = it },
                        labels = taskLabelMap[task.id].orEmpty(),
                    )
                }
            }
        }
    }

    (tasks + completedTasks).firstOrNull { it.id == editingTaskId }?.let { task ->
        TaskComposerSheet(
            onDismiss = { editingTaskId = null },
            onSubmit = { _, _, _, _, _ -> editingTaskId = null },
            autoSave = { title, bucket, dueDate, reminderAt, labelIds ->
                viewModel.updateTask(task.id, title, bucket, dueDate, reminderAt, labelIds)
            },
            labels = labels,
            onCreateLabel = viewModel::createLabel,
            initialTitle = task.title,
            initialBucket = task.bucket,
            initialDueDate = task.dueDate,
            initialReminderAt = task.reminderAt,
            initialLabelIds = taskLabelMap[task.id].orEmpty().map { it.id }.toSet(),
        )
    }

    if (taskToDeleteId != null) {
        DeleteTaskDialog(
            onDismiss = { taskToDeleteId = null },
            onConfirm = {
                val id = taskToDeleteId
                if (id != null) viewModel.deleteTask(id)
                taskToDeleteId = null
            },
        )
    }
}

private fun List<Task>.filterDueOn(date: LocalDate): List<Task> {
    val zone = ZoneId.systemDefault()
    val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val isToday = date == LocalDate.now()
    return filter { task ->
        val dueDate = task.dueDate
        when {
            dueDate == null -> isToday
            dueDate >= dayStart && dueDate < dayEnd -> true
            else -> false
        }
    }.sortedForDisplay()
}

private fun List<Task>.filterCompletedOn(date: LocalDate): List<Task> {
    val zone = ZoneId.systemDefault()
    val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return filter { it.completedAt != null && it.completedAt >= dayStart && it.completedAt < dayEnd }
}

@Composable
private fun DeleteTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_confirm_title)) },
        text = { Text(stringResource(R.string.delete_confirm_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.delete_confirm_action),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.composer_cancel))
            }
        },
    )
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
