package com.diego.twodo.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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

    var showComposer by rememberSaveable { mutableStateOf(false) }
    var taskToRelabelId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(focusQuickAdd) {
        if (focusQuickAdd) {
            showComposer = true
            onQuickAddHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        AppHeader()
        if (modelStatus !is ModelStatus.Ready || !viewModel.canUseLlm) {
            AiSetupBanner(onOpenModelSetup = onOpenModelSetup)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "summary") { SummaryGrid(tasks = tasks) }
            item(key = "section") {
                SectionHeader(
                    title = stringResource(R.string.task_list_title),
                    dateLabel = LocalDate.now().format(sectionDateFormatter),
                )
            }
            if (tasks.isEmpty()) {
                item(key = "empty") { EmptyState(modifier = Modifier.fillMaxWidth()) }
            } else {
                items(tasks.sortedForDisplay(), key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onToggle = viewModel::toggleTaskCompletion,
                        onRelabel = { taskToRelabelId = it.id },
                        onDelete = viewModel::deleteTask,
                    )
                }
            }
            item(key = "add") {
                AddControl(onClick = { showComposer = true })
            }
        }
    }

    if (showComposer) {
        TaskComposerSheet(
            onDismiss = { showComposer = false },
            onSubmit = { title, bucket, dueDate, reminderAt ->
                viewModel.addTask(title, bucket, dueDate, reminderAt)
                showComposer = false
            },
        )
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
private fun AddControl(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, TextSecondary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.add_label),
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
        )
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
