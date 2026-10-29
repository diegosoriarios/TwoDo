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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalResources
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
    onOpenModelManager: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val tasks by viewModel.tasks.collectAsState()
    val modelStatus by viewModel.modelStatus.collectAsState()
    val canUseLlm by viewModel.canUseLlm.collectAsState()
    val labels by viewModel.labels.collectAsState()
    val taskLabelMap by viewModel.taskLabelMap.collectAsState()
    val isReevaluating by viewModel.isReevaluating.collectAsState()

    var showComposer by rememberSaveable { mutableStateOf(false) }
    var editingTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    var taskToDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var filterLabelId by rememberSaveable { mutableStateOf<Long?>(null) }
    var headerMenuExpanded by remember { mutableStateOf(false) }
    var showReevaluateAllDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
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

    LaunchedEffect(viewModel) {
        viewModel.reevaluationFeedback.collect { feedback ->
            val message = when (feedback) {
                is ReevaluationFeedback.TaskResult -> {
                    val bucket = feedback.newBucket
                    when {
                        bucket == null -> resources.getString(R.string.reevaluate_task_no_category)
                        feedback.changed -> resources.getString(
                            R.string.reevaluate_task_moved_format,
                            resources.getString(bucket.labelRes()),
                        )
                        else -> resources.getString(
                            R.string.reevaluate_task_unchanged_format,
                            resources.getString(bucket.labelRes()),
                        )
                    }
                }
                is ReevaluationFeedback.AllResult -> {
                    val summary = feedback.summary
                    if (summary.evaluated == 0) {
                        resources.getString(R.string.reevaluate_all_none)
                    } else {
                        resources.getString(
                            R.string.reevaluate_all_result_format,
                            summary.evaluated,
                            summary.updated,
                        )
                    }
                }
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                actions = {
                    Box {
                        IconButton(
                            onClick = { headerMenuExpanded = true },
                            enabled = !isReevaluating,
                        ) {
                            if (isReevaluating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Coral,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(R.string.task_more_cd),
                                    tint = TextSecondary,
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = headerMenuExpanded,
                            onDismissRequest = { headerMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.task_reevaluate_all)) },
                                onClick = {
                                    headerMenuExpanded = false
                                    showReevaluateAllDialog = true
                                },
                                enabled = tasks.isNotEmpty(),
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.model_manager_menu_title)) },
                                onClick = {
                                    headerMenuExpanded = false
                                    onOpenModelManager()
                                },
                            )
                        }
                    }
                },
            )
            if (modelStatus !is ModelStatus.Ready || !canUseLlm) {
                AiSetupBanner(onOpenModelManager = onOpenModelManager)
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
                            onReevaluate = { viewModel.reevaluateTask(it.id) },
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
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = 88.dp),
        )
    }

    if (showComposer) {
        TaskComposerSheet(
            onDismiss = { showComposer = false },
            onSubmit = { title, bucket, dueDate, reminderAt, labelIds, _ ->
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
            onSubmit = { _, _, _, _, _, _ -> editingTaskId = null },
            autoSave = { title, bucket, dueDate, reminderAt, labelIds, isCompleted ->
                viewModel.updateTask(task.id, title, bucket, dueDate, reminderAt, labelIds, isCompleted)
            },
            labels = labels,
            onCreateLabel = viewModel::createLabel,
            initialTitle = task.title,
            initialBucket = task.bucket,
            initialDueDate = task.dueDate,
            initialReminderAt = task.reminderAt,
            initialLabelIds = taskLabelMap[task.id].orEmpty().map { it.id }.toSet(),
            initialIsCompleted = task.isCompleted,
        )
    }

    if (showReevaluateAllDialog) {
        AlertDialog(
            onDismissRequest = { showReevaluateAllDialog = false },
            title = { Text(stringResource(R.string.reevaluate_all_confirm_title)) },
            text = { Text(stringResource(R.string.reevaluate_all_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showReevaluateAllDialog = false
                    viewModel.reevaluateAll()
                }) {
                    Text(stringResource(R.string.reevaluate_all_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReevaluateAllDialog = false }) {
                    Text(stringResource(R.string.composer_cancel))
                }
            },
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
private fun AiSetupBanner(onOpenModelManager: () -> Unit) {
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
            TextButton(onClick = onOpenModelManager) {
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

private fun TaskBucket.labelRes(): Int = when (this) {
    TaskBucket.TWO_MIN -> R.string.bucket_two_min
    TaskBucket.THIRTY_MIN -> R.string.bucket_thirty_min
    TaskBucket.LONG -> R.string.bucket_long
    TaskBucket.UNCLASSIFIED -> R.string.bucket_unclassified
}
