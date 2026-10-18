package com.diego.twodo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalState
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.state.PreferencesGlanceStateDefinition
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
import com.diego.twodo.ui.quickadd.EditTaskActivity
import com.diego.twodo.ui.quickadd.QuickAddActivity
import kotlinx.coroutines.flow.first

private val WidgetBackground = ColorProvider(Color(0xFF121212))
private val WidgetSurface = ColorProvider(Color(0xFF252525))
private val WidgetHeaderBar = ColorProvider(Color(0xFF2E2E2E))
private val WidgetTextPrimary = ColorProvider(Color(0xFFFFFFFF))
private val WidgetTextSecondary = ColorProvider(Color(0xFFA0A0A0))
private val WidgetCoral = ColorProvider(Color(0xFFD35D47))
private val WidgetWhite = ColorProvider(Color(0xFFFFFFFF))

private data class WidgetStrings(
    val appName: String,
    val filterLabel: String,
    val filterCd: String,
    val emptyTitle: String,
    val addTaskCd: String,
    val checkCd: String,
    val more: String?,
)

private val KEY_EDIT_TASK_ID = ActionParameters.Key<Long>(EditTaskActivity.EXTRA_TASK_ID)
private val KEY_FILTER_APP_WIDGET_ID = ActionParameters.Key<Int>(FilterPickerActivity.EXTRA_APP_WIDGET_ID)
private val KEY_FILTER_CURRENT = ActionParameters.Key<String>(FilterPickerActivity.EXTRA_CURRENT_FILTER)

class TasksWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val taskRepository = widgetTaskRepository(context)
        val tasks = taskRepository.getActiveTasks().first()
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val filter = (LocalState.current as? Preferences).widgetFilterOrDefault()
            val sorted = filter.apply(tasks)
            val strings = WidgetStrings(
                appName = context.getString(R.string.app_name),
                filterLabel = context.getString(filter.labelRes()),
                filterCd = context.getString(R.string.widget_filter_cd),
                emptyTitle = if (filter == WidgetFilter.TODAY) {
                    context.getString(R.string.widget_today_empty_title)
                } else {
                    context.getString(R.string.widget_generic_empty_title)
                },
                addTaskCd = context.getString(R.string.task_add_cd),
                checkCd = context.getString(R.string.widget_check_cd),
                more = if (sorted.size > MAX_TASK_ROWS) {
                    context.getString(R.string.widget_more_format, sorted.size - MAX_TASK_ROWS)
                } else {
                    null
                },
            )
            TasksWidgetContent(sorted, strings, appWidgetId, filter)
        }
    }

    @Composable
    private fun TasksWidgetContent(
        tasks: List<Task>,
        strings: WidgetStrings,
        appWidgetId: Int,
        filter: WidgetFilter,
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetBackground)
                .cornerRadius(16.dp),
        ) {
            WidgetHeader(strings, appWidgetId, filter)
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
                            TaskRow(task, strings.checkCd)
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
    private fun WidgetHeader(strings: WidgetStrings, appWidgetId: Int, filter: WidgetFilter) {
        val filterAction = actionStartActivity<FilterPickerActivity>(
            parameters = actionParametersOf(
                KEY_FILTER_APP_WIDGET_ID to appWidgetId,
                KEY_FILTER_CURRENT to filter.name,
            ),
        )
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
            Box(
                modifier = GlanceModifier.clickable(filterAction),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.filterLabel,
                    style = TextStyle(fontSize = 12.sp, color = WidgetTextSecondary),
                    maxLines = 1,
                    modifier = GlanceModifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
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
    private fun TaskRow(task: Task, checkCd: String) {
        val toggleAction = actionRunCallback<ToggleTaskAction>(
            parameters = actionParametersOf(ToggleTaskAction.KEY_TASK_ID to task.id),
        )
        val editAction = actionStartActivity<EditTaskActivity>(
            parameters = actionParametersOf(KEY_EDIT_TASK_ID to task.id),
        )
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(40.dp)
                .cornerRadius(4.dp)
                .background(WidgetSurface)
                .clickable(editAction)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_checkbox_unchecked),
                contentDescription = checkCd,
                modifier = GlanceModifier
                    .size(18.dp)
                    .clickable(toggleAction),
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
