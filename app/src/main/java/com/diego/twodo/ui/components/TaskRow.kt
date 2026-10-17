package com.diego.twodo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.diego.twodo.R
import com.diego.twodo.domain.model.Task
import com.diego.twodo.domain.model.TaskBucket
import com.diego.twodo.ui.theme.Coral
import com.diego.twodo.ui.theme.SurfaceDark
import com.diego.twodo.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dueDateFormatter = DateTimeFormatter.ofPattern("MMM d")
private const val COMPLETION_ANIM_MILLIS = 350

fun List<Task>.sortedForDisplay(): List<Task> =
    sortedWith(compareBy({ bucketOrder(it.bucket) }, { it.createdAt }))

private fun bucketOrder(bucket: TaskBucket): Int = when (bucket) {
    TaskBucket.TWO_MIN -> 0
    TaskBucket.THIRTY_MIN -> 1
    TaskBucket.LONG -> 2
    TaskBucket.UNCLASSIFIED -> 3
}

@Composable
fun TaskRow(
    task: Task,
    onToggle: (Long, Boolean) -> Unit,
    onEdit: (Task) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var completing by remember { mutableStateOf(false) }
    val rowAlpha by animateFloatAsState(
        targetValue = if (completing) 0f else 1f,
        animationSpec = tween(durationMillis = COMPLETION_ANIM_MILLIS),
        label = "task_row_alpha",
    )

    LaunchedEffect(completing) {
        if (completing) {
            delay(COMPLETION_ANIM_MILLIS.toLong())
            onToggle(task.id, true)
        }
    }

    Row(
        modifier = modifier
            .alpha(rowAlpha)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceDark)
            .clickable(enabled = !completing) { onEdit(task) }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SquareCheckbox(
            checked = task.isCompleted || completing,
            onCheckedChange = {
                if (completing) return@SquareCheckbox
                if (task.isCompleted) {
                    onToggle(task.id, false)
                } else {
                    completing = true
                }
            },
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (task.isCompleted) TextSecondary else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp),
        )
        if (task.dueDate != null) {
            Text(
                text = Instant.ofEpochMilli(task.dueDate).atZone(ZoneId.systemDefault())
                    .format(dueDateFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = Coral,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                enabled = !completing,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.task_more_cd),
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.task_edit)) },
                    onClick = {
                        menuExpanded = false
                        onEdit(task)
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
