package com.diego.twodo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.diego.twodo.MainActivity
import com.diego.twodo.R
import com.diego.twodo.domain.model.Task
import com.diego.twodo.domain.model.TaskBucket
import kotlinx.coroutines.flow.first

private data class WidgetStrings(
    val appName: String,
    val pendingCount: String,
    val emptyTitle: String,
    val addTask: String,
    val more: String?,
)

class TasksWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val taskRepository = widgetTaskRepository(context)
        val tasks = taskRepository.getActiveTasks().first()
        val sorted = tasks.sortedWith(
            compareBy({ bucketOrder(it.bucket) }, { it.createdAt }),
        )
        val strings = WidgetStrings(
            appName = context.getString(R.string.app_name),
            pendingCount = context.getString(R.string.widget_pending_count, tasks.size),
            emptyTitle = context.getString(R.string.widget_empty_title),
            addTask = context.getString(R.string.widget_add_task),
            more = if (sorted.size > MAX_TASK_ROWS) {
                context.getString(R.string.widget_more_format, sorted.size - MAX_TASK_ROWS)
            } else {
                null
            },
        )
        provideContent {
            GlanceTheme {
                TasksWidgetContent(sorted, strings)
            }
        }
    }

    @Composable
    private fun TasksWidgetContent(tasks: List<Task>, strings: WidgetStrings) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(12.dp),
        ) {
            WidgetHeader(tasks.size, strings)
            if (tasks.isEmpty()) {
                EmptyState(strings.emptyTitle)
            } else {
                tasks.take(MAX_TASK_ROWS).forEach { task ->
                    TaskRow(task)
                }
                strings.more?.let { extraText ->
                    Text(
                        text = extraText,
                        style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurface),
                        modifier = GlanceModifier.padding(vertical = 2.dp),
                    )
                }
                Spacer(modifier = GlanceModifier.height(4.dp))
                AddTaskButton(strings.addTask)
            }
        }
    }

    @Composable
    private fun WidgetHeader(pendingCount: Int, strings: WidgetStrings) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.appName,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GlanceTheme.colors.onSurface,
                ),
            )
            Text(
                text = strings.pendingCount,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.onSurface,
                    textAlign = TextAlign.End,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }

    @Composable
    private fun EmptyState(emptyTitle: String) {
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🎉", style = TextStyle(fontSize = 24.sp))
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = emptyTitle,
                    style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurface),
                )
            }
        }
    }

    @Composable
    private fun TaskRow(task: Task) {
        val toggleAction = actionRunCallback<ToggleTaskAction>(
            parameters = actionParametersOf(ToggleTaskAction.KEY_TASK_ID to task.id),
        )
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(toggleAction)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckBox(checked = false, onCheckedChange = toggleAction)
            Text(
                text = bucketEmoji(task.bucket),
                style = TextStyle(fontSize = 13.sp),
                modifier = GlanceModifier.padding(horizontal = 6.dp),
            )
            Text(
                text = task.title,
                style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurface),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }

    @Composable
    private fun AddTaskButton(addTaskLabel: String) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.primary)
                .clickable(actionStartActivity<MainActivity>())
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = addTaskLabel,
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onPrimary,
                ),
            )
        }
    }

    private fun bucketOrder(bucket: TaskBucket): Int = when (bucket) {
        TaskBucket.TWO_MIN -> 0
        TaskBucket.THIRTY_MIN -> 1
        TaskBucket.LONG -> 2
        TaskBucket.UNCLASSIFIED -> 3
    }

    private fun bucketEmoji(bucket: TaskBucket): String = when (bucket) {
        TaskBucket.TWO_MIN -> "⚡"
        TaskBucket.THIRTY_MIN -> "⏱️"
        TaskBucket.LONG -> "🏗️"
        TaskBucket.UNCLASSIFIED -> "🤔"
    }

    private companion object {
        const val MAX_TASK_ROWS = 6
    }
}
