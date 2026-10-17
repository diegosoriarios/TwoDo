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
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import com.diego.twodo.R
import com.diego.twodo.domain.model.Task
import com.diego.twodo.ui.quickadd.QuickAddActivity
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

private val WidgetBackground = ColorProvider(Color(0xFF121212))
private val WidgetSurface = ColorProvider(Color(0xFF252525))
private val WidgetHeaderBar = ColorProvider(Color(0xFF2E2E2E))
private val WidgetTextPrimary = ColorProvider(Color(0xFFFFFFFF))
private val WidgetTextSecondary = ColorProvider(Color(0xFFA0A0A0))
private val WidgetCoral = ColorProvider(Color(0xFFD35D47))
private val WidgetWhite = ColorProvider(Color(0xFFFFFFFF))

private data class WidgetStrings(
    val appName: String,
    val today: String,
    val emptyTitle: String,
    val addTaskCd: String,
    val more: String?,
)

class TasksWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val taskRepository = widgetTaskRepository(context)
        val tasks = taskRepository.getActiveTasks().first()
        val zone = ZoneId.systemDefault()
        val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val tomorrowStart = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val dueToday = tasks
            .filter { it.dueDate != null && it.dueDate < tomorrowStart }
            .sortedWith(compareBy({ it.dueDate }, { it.createdAt }))
        val sorted = dueToday
        val strings = WidgetStrings(
            appName = context.getString(R.string.app_name),
            today = context.getString(R.string.widget_today),
            emptyTitle = context.getString(R.string.widget_today_empty_title),
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
                .cornerRadius(16.dp),
        ) {
            WidgetHeader(strings)
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .padding(12.dp),
            ) {
                if (tasks.isEmpty()) {
                    EmptyState(strings.emptyTitle)
                } else {
                    Column {
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
        }
    }

    @Composable
    private fun WidgetHeader(strings: WidgetStrings) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(40.dp)
                .background(WidgetHeaderBar)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_logo),
                contentDescription = null,
                modifier = GlanceModifier.size(20.dp),
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = strings.appName,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = WidgetTextPrimary,
                ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = strings.today,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = WidgetTextSecondary,
                ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Box(
                modifier = GlanceModifier
                    .size(28.dp)
                    .cornerRadius(4.dp)
                    .background(WidgetCoral)
                    .clickable(actionStartActivity<QuickAddActivity>()),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_add),
                    contentDescription = strings.addTaskCd,
                    modifier = GlanceModifier.size(18.dp),
                    colorFilter = ColorFilter.tint(WidgetWhite),
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

    private companion object {
        const val MAX_TASK_ROWS = 6
    }
}
