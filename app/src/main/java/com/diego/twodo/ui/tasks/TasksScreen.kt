package com.diego.twodo.ui.tasks

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diego.twodo.R
import com.diego.twodo.domain.model.Label
import com.diego.twodo.domain.model.ModelStatus
import com.diego.twodo.domain.model.Task
import com.diego.twodo.domain.model.TaskBucket
import com.diego.twodo.ui.components.AppHeader
import com.diego.twodo.ui.components.SectionHeader
import com.diego.twodo.ui.components.SummaryCard
import com.diego.twodo.ui.components.TaskRow
import com.diego.twodo.ui.components.sectionDateFormatter
import com.diego.twodo.ui.components.sortedForDisplay
import com.diego.twodo.ui.composer.TaskComposerSheet
import com.diego.twodo.ui.theme.Coral
import com.diego.twodo.ui.theme.SurfaceDark
import com.diego.twodo.ui.theme.TextPrimary
import com.diego.twodo.ui.theme.TextSecondary
import java.time.LocalDate

@Composable
fun TasksScreen(
    focusQuickAdd: Boolean = false,
    onQuickAddHandled: () -> Unit = {},
    onOpenModelSetup: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val tasks by viewModel.tasks.collectAsState()
    val modelStatus by viewModel.modelStatus.collectAsState()
    val labels by viewModel.labels.collectAsState()
    val taskLabelMap by viewModel.taskLabelMap.collectAsState()

    var showComposer by rememberSaveable { mutableStateOf(false) }
    var editingTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    var taskToDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var filterLabelId by rememberSaveable { mutableStateOf<Long?>(null) }
    val filteredTasks = remember(tasks, taskLabelMap, filterLabelId) {
        if (filterLabelId == null) {
            tasks
        } else {
            tasks.filter { task ->
                taskLabelMap[task.id].orEmpty().any { it.id == filterLabelId }
            }
        }
    }

    LaunchedEffect(focusQuickAdd) {
        if (focusQuickAdd) {
            showComposer = true
            onQuickAddHandled()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader()
            if (modelStatus !is ModelStatus.Ready || !viewModel.canUseLlm) {
                AiSetupBanner(onOpenModelSetup = onOpenModelSetup)
            }
            if (labels.isNotEmpty()) {
                LabelFilterRow(
                    labels = labels,
                    selectedLabelId = filterLabelId,
                    onSelect = { filterLabelId = it },
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "summary") { SummaryGrid(tasks = tasks) }
                item(key = "section") {
                    SectionHeader(
                        title = stringResource(R.string.task_list_title),
                        dateLabel = LocalDate.now().format(sectionDateFormatter),
                    )
                }
                if (filteredTasks.isEmpty()) {
                    item(key = "empty") { EmptyState(modifier = Modifier.fillMaxWidth()) }
                } else {
                    items(filteredTasks.sortedForDisplay(), key = { it.id }) { task ->
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
        FloatingActionButton(
            onClick = { showComposer = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            shape = RoundedCornerShape(4.dp),
            containerColor = Coral,
            contentColor = TextPrimary,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.task_add_cd),
            )
        }
    }

    if (showComposer) {
        TaskComposerSheet(
            onDismiss = { showComposer = false },
            onSubmit = { title, bucket, dueDate, reminderAt, labelIds ->
                viewModel.addTask(title, bucket, dueDate, reminderAt, labelIds)
                showComposer = false
            },
            labels = labels,
            onCreateLabel = viewModel::createLabel,
        )
    }

    tasks.firstOrNull { it.id == editingTaskId }?.let { task ->
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
        AlertDialog(
            onDismissRequest = { taskToDeleteId = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    val id = taskToDeleteId
                    if (id != null) viewModel.deleteTask(id)
                    taskToDeleteId = null
                }) {
                    Text(
                        text = stringResource(R.string.delete_confirm_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDeleteId = null }) {
                    Text(stringResource(R.string.composer_cancel))
                }
            },
        )
    }
}

@Composable
private fun LabelFilterRow(
    labels: List<Label>,
    selectedLabelId: Long?,
    onSelect: (Long?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedLabelId == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.filter_all)) },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = SurfaceDark,
                labelColor = TextSecondary,
                selectedContainerColor = Coral,
                selectedLabelColor = TextPrimary,
            ),
        )
        labels.forEach { label ->
            FilterChip(
                selected = selectedLabelId == label.id,
                onClick = { onSelect(label.id) },
                label = { Text(label.name) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = SurfaceDark,
                    labelColor = TextSecondary,
                    selectedContainerColor = Coral,
                    selectedLabelColor = TextPrimary,
                ),
            )
        }
    }
}

@Composable
private fun SummaryGrid(tasks: List<Task>) {
    val total = tasks.size.coerceAtLeast(1).toFloat()
    val twoMin = tasks.count { it.bucket == TaskBucket.TWO_MIN }
    val thirtyMin = tasks.count { it.bucket == TaskBucket.THIRTY_MIN }
    val long = tasks.count { it.bucket == TaskBucket.LONG }
    val unclassified = tasks.count { it.bucket == TaskBucket.UNCLASSIFIED }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                count = twoMin,
                label = stringResource(R.string.card_two_min_label),
                progress = twoMin / total,
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                count = thirtyMin,
                label = stringResource(R.string.card_thirty_min_label),
                progress = thirtyMin / total,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                count = long,
                label = stringResource(R.string.card_long_label),
                progress = long / total,
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                count = unclassified,
                label = stringResource(R.string.card_unclassified_label),
                progress = unclassified / total,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AiSetupBanner(onOpenModelSetup: () -> Unit) {
    Surface(color = SurfaceDark, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.banner_ai_off),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenModelSetup) {
                Text(
                    text = stringResource(R.string.banner_set_up),
                    color = Coral,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "🎉", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.tasks_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.tasks_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
