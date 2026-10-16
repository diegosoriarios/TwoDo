package com.diego.twodo.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diego.twodo.R
import com.diego.twodo.domain.model.ModelStatus
import com.diego.twodo.domain.model.Task
import com.diego.twodo.domain.model.TaskBucket

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onOpenModelSetup: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val tasks by viewModel.tasks.collectAsState()
    val modelStatus by viewModel.modelStatus.collectAsState()

    var newTaskTitle by rememberSaveable { mutableStateOf("") }
    var taskToRelabelId by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TasksTopBar(
                modelStatus = modelStatus,
                canUseLlm = viewModel.canUseLlm,
                onOpenModelSetup = onOpenModelSetup,
            )
        },
        bottomBar = {
            QuickAddBar(
                value = newTaskTitle,
                onValueChange = { newTaskTitle = it },
                onSubmit = {
                    viewModel.addTask(newTaskTitle)
                    newTaskTitle = ""
                },
            )
        },
    ) { padding ->
        if (tasks.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding))
        } else {
            TaskBucketList(
                tasks = tasks,
                contentPadding = padding,
                onToggle = viewModel::toggleTaskCompletion,
                onRelabel = { taskToRelabelId = it.id },
                onDelete = viewModel::deleteTask,
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksTopBar(
    modelStatus: ModelStatus,
    canUseLlm: Boolean,
    onOpenModelSetup: () -> Unit,
) {
    Column {
        TopAppBar(
            title = { Text(stringResource(R.string.app_name)) },
            actions = {
                when (modelStatus) {
                    is ModelStatus.Downloading -> {
                        IconButton(onClick = onOpenModelSetup) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                    is ModelStatus.Ready -> {
                        if (!canUseLlm) {
                            IconButton(onClick = onOpenModelSetup) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = stringResource(R.string.setup_title),
                                )
                            }
                        }
                    }
                    else -> {
                        IconButton(onClick = onOpenModelSetup) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.setup_title),
                            )
                        }
                    }
                }
            },
        )
        if (modelStatus !is ModelStatus.Ready || !canUseLlm) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.banner_ai_off),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onOpenModelSetup) {
                        Text(stringResource(R.string.banner_set_up))
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskBucketList(
    tasks: List<Task>,
    contentPadding: PaddingValues,
    onToggle: (Long, Boolean) -> Unit,
    onRelabel: (Task) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var twoMinExpanded by rememberSaveable { mutableStateOf(true) }
    var thirtyMinExpanded by rememberSaveable { mutableStateOf(true) }
    var longExpanded by rememberSaveable { mutableStateOf(true) }
    var unclassifiedExpanded by rememberSaveable { mutableStateOf(false) }

    val twoMinTitle = stringResource(R.string.bucket_two_min)
    val thirtyMinTitle = stringResource(R.string.bucket_thirty_min)
    val longTitle = stringResource(R.string.bucket_long)
    val unclassifiedTitle = stringResource(R.string.bucket_unclassified)

    val listPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + 16.dp,
        top = contentPadding.calculateTopPadding() + 16.dp,
        end = contentPadding.calculateEndPadding(LayoutDirection.Ltr) + 16.dp,
        bottom = contentPadding.calculateBottomPadding() + 16.dp,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = listPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        bucketSection(
            title = twoMinTitle,
            tasks = tasks.filter { it.bucket == TaskBucket.TWO_MIN },
            expanded = twoMinExpanded,
            onToggleExpanded = { twoMinExpanded = !twoMinExpanded },
            isTwoMinute = true,
            onToggle = onToggle,
            onRelabel = onRelabel,
            onDelete = onDelete,
        )
        bucketSection(
            title = thirtyMinTitle,
            tasks = tasks.filter { it.bucket == TaskBucket.THIRTY_MIN },
            expanded = thirtyMinExpanded,
            onToggleExpanded = { thirtyMinExpanded = !thirtyMinExpanded },
            onToggle = onToggle,
            onRelabel = onRelabel,
            onDelete = onDelete,
        )
        bucketSection(
            title = longTitle,
            tasks = tasks.filter { it.bucket == TaskBucket.LONG },
            expanded = longExpanded,
            onToggleExpanded = { longExpanded = !longExpanded },
            onToggle = onToggle,
            onRelabel = onRelabel,
            onDelete = onDelete,
        )
        bucketSection(
            title = unclassifiedTitle,
            tasks = tasks.filter { it.bucket == TaskBucket.UNCLASSIFIED },
            expanded = unclassifiedExpanded,
            onToggleExpanded = { unclassifiedExpanded = !unclassifiedExpanded },
            onToggle = onToggle,
            onRelabel = onRelabel,
            onDelete = onDelete,
        )
    }
}

private fun LazyListScope.bucketSection(
    title: String,
    tasks: List<Task>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    isTwoMinute: Boolean = false,
    onToggle: (Long, Boolean) -> Unit,
    onRelabel: (Task) -> Unit,
    onDelete: (Long) -> Unit,
) {
    item(key = "header_$title") {
        BucketHeader(
            title = title,
            count = tasks.size,
            expanded = expanded,
            onClick = onToggleExpanded,
        )
    }
    if (expanded) {
        if (tasks.isEmpty()) {
            item(key = "empty_$title") {
                Text(
                    text = stringResource(R.string.bucket_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
        items(tasks, key = { it.id }) { task ->
            TaskRow(
                task = task,
                isTwoMinute = isTwoMinute,
                onToggle = onToggle,
                onRelabel = onRelabel,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun BucketHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
        )
    }
}

@Composable
private fun TaskRow(
    task: Task,
    isTwoMinute: Boolean,
    onToggle: (Long, Boolean) -> Unit,
    onRelabel: (Task) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = false,
                onCheckedChange = { onToggle(task.id, it) },
            )
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onRelabel(task) },
            )
            if (isTwoMinute) {
                FilledTonalButton(onClick = { onToggle(task.id, true) }) {
                    Text(stringResource(R.string.task_do_it_now))
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.task_more_cd),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.task_change_bucket)) },
                        onClick = {
                            menuExpanded = false
                            onRelabel(task)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.task_delete)) },
                        onClick = {
                            menuExpanded = false
                            onDelete(task.id)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BucketOverrideSheet(
    onDismiss: () -> Unit,
    onSelect: (TaskBucket) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.override_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            BucketOption(
                title = stringResource(R.string.bucket_two_min),
                description = stringResource(R.string.override_two_min_desc),
            ) { onSelect(TaskBucket.TWO_MIN) }
            BucketOption(
                title = stringResource(R.string.bucket_thirty_min),
                description = stringResource(R.string.override_thirty_min_desc),
            ) { onSelect(TaskBucket.THIRTY_MIN) }
            BucketOption(
                title = stringResource(R.string.bucket_long),
                description = stringResource(R.string.override_long_desc),
            ) { onSelect(TaskBucket.LONG) }
        }
    }
}

@Composable
private fun BucketOption(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun QuickAddBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.task_add_hint)) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onSubmit()
                        keyboardController?.hide()
                    },
                ),
            )
            FilledIconButton(
                onClick = {
                    onSubmit()
                    keyboardController?.hide()
                },
                enabled = value.isNotBlank(),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.task_add_cd),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "🎉", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.tasks_empty_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.tasks_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
