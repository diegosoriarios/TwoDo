package com.diego.twodo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import com.diego.twodo.R
import com.diego.twodo.domain.model.Task
import com.diego.twodo.domain.model.TaskBucket
import com.diego.twodo.ui.quickadd.QuickAddActivity
import kotlinx.coroutines.flow.first

private val WidgetBackground = ColorProvider(Color(0xFF121212))
private val WidgetSurface = ColorProvider(Color(0xFF252525))
private val WidgetTextPrimary = ColorProvider(Color(0xFFFFFFFF))
private val WidgetTextSecondary = ColorProvider(Color(0xFFA0A0A0))
private val WidgetCoral = ColorProvider(Color(0xFFD35D47))

private data class WidgetStrings(
    val appName: String,
    val pendingCount: String,
    val emptyTitle: String,
    val addTaskCd: String,
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
            addTaskCd = context.getString(R.string.task_add_cd),
            more = if (sorted.size > MAX_TASK_ROWS) {
                context.getString(R.string.widget_more_format, sorted.size - MAX_TASK_ROWS)
            } else {
                null
            },
        )
        provideContent {
            TasksWidgetContent(sorted, strings)
        }
    }

    @Composable
    private fun TasksWidgetContent(tasks: List<Task>, strings: WidgetStrings) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetBackground)
                .padding(12.dp),
        ) {
            WidgetHeader(tasks.size, strings)
            if (tasks.isEmpty()) {
                EmptyState(strings.emptyTitle)
            } else {
                tasks.take(MAX_TASK_ROWS).forEach { task ->
                    TaskRow(task)
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
                strings.more?.let { extraText ->
                    Text(
                        text = extraText,
                        style = TextStyle(fontSize = 12.sp, color = WidgetTextSecondary),
                        modifier = GlanceModifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }

    @Composable
    private fun WidgetHeader(pendingCount: Int, strings: WidgetStrings) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.appName,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = WidgetTextPrimary,
                ),
            )
            Text(
                text = strings.pendingCount,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = WidgetTextSecondary,
                    textAlign = TextAlign.End,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            Box(
                modifier = GlanceModifier
                    .size(28.dp)
                    .clickable(actionStartActivity<QuickAddActivity>()),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_add),
                    contentDescription = strings.addTaskCd,
                    modifier = GlanceModifier.size(20.dp),
                    colorFilter = ColorFilter.tint(WidgetCoral),
                )
            }
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
                    style = TextStyle(fontSize = 14.sp, color = WidgetTextSecondary),
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
                .height(40.dp)
                .cornerRadius(4.dp)
                .background(WidgetSurface)
                .clickable(toggleAction)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_checkbox_unchecked),
                contentDescription = null,
                modifier = GlanceModifier.size(18.dp),
            )
            Spacer(modifier = GlanceModifier.width(12.dp))
            Text(
                text = task.title,
                style = TextStyle(fontSize = 14.sp, color = WidgetTextPrimary),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }

    private fun bucketOrder(bucket: TaskBucket): Int = when (bucket) {
        TaskBucket.TWO_MIN -> 0
        TaskBucket.THIRTY_MIN -> 1
        TaskBucket.LONG -> 2
        TaskBucket.UNCLASSIFIED -> 3
    }

    private companion object {
        const val MAX_TASK_ROWS = 6
    }
}
