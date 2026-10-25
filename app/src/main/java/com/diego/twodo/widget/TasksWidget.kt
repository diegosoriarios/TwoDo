package com.diego.twodo.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.core.content.res.ResourcesCompat
import com.diego.twodo.MainActivity
import com.diego.twodo.R
import com.diego.twodo.domain.model.Task
import com.diego.twodo.ui.quickadd.EditTaskActivity
import com.diego.twodo.ui.quickadd.QuickAddActivity
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

private val WidgetBackground = ColorProvider(Color(0xFF121212))
private val WidgetSurface = ColorProvider(Color(0xFF252525))
private val WidgetHeaderBar = ColorProvider(Color(0xFF2E2E2E))
private val WidgetTextPrimary = ColorProvider(Color(0xFFFFFFFF))
private val WidgetTextSecondary = ColorProvider(Color(0xFFA0A0A0))
private val WidgetCoral = ColorProvider(Color(0xFFD35D47))

private val widgetDateFormatter = DateTimeFormatter.ofPattern("d MMM")

private const val WIDGET_LOG_TAG = "TwoDoWidget"
private const val COMPLETED_FEEDBACK_TTL_MS = 5_000L

private data class WidgetIcons(
    val logo: Bitmap,
    val add: Bitmap,
    val checkbox: Bitmap,
    val checked: Bitmap,
)

private fun Context.widgetIcons(): WidgetIcons = WidgetIcons(
    logo = iconBitmap(R.drawable.ic_twodo_logo, sizeDp = 20),
    add = iconBitmap(R.drawable.ic_add, sizeDp = 18, tint = 0xFFFFFFFF.toInt()),
    checkbox = iconBitmap(R.drawable.ic_checkbox_unchecked, sizeDp = 18),
    checked = iconBitmap(R.drawable.ic_checkbox_checked, sizeDp = 18),
)

private fun Context.iconBitmap(resId: Int, sizeDp: Int, tint: Int? = null): Bitmap {
    val drawable = ResourcesCompat.getDrawable(resources, resId, null)
        ?: error("Missing drawable resource 0x${resId.toString(16)}")
    val sizePx = max(1, (sizeDp * resources.displayMetrics.density).roundToInt())
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    if (tint != null) drawable.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

private data class WidgetStrings(
    val appName: String,
    val filterLabel: String,
    val filterCd: String,
    val emptyTitle: String,
    val addTaskCd: String,
    val checkCd: String,
    val openAppCd: String,
)

private val KEY_EDIT_TASK_ID = ActionParameters.Key<Long>(EditTaskActivity.EXTRA_TASK_ID)
private val KEY_FILTER_APP_WIDGET_ID = ActionParameters.Key<Int>(FilterPickerActivity.EXTRA_APP_WIDGET_ID)
private val KEY_FILTER_CURRENT = ActionParameters.Key<String>(FilterPickerActivity.EXTRA_CURRENT_FILTER)

class TasksWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val taskRepository = widgetTaskRepository(context)
        val tasks = taskRepository.getActiveTasksSync()
        val recentlyCompleted = taskRepository.getCompletedSinceSync(
            System.currentTimeMillis() - COMPLETED_FEEDBACK_TTL_MS,
        )
        Log.d(
            WIDGET_LOG_TAG,
            "compose: active=${tasks.size} recentCompleted=${recentlyCompleted.size}",
        )
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val icons = context.widgetIcons()
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
                openAppCd = context.getString(R.string.widget_open_app_cd),
            )
            TasksWidgetContent(sorted, recentlyCompleted, strings, appWidgetId, filter, icons)
        }
    }

    @Composable
    private fun TasksWidgetContent(
        tasks: List<Task>,
        recentlyCompleted: List<Task>,
        strings: WidgetStrings,
        appWidgetId: Int,
        filter: WidgetFilter,
        icons: WidgetIcons,
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetBackground)
                .cornerRadius(16.dp),
        ) {
            WidgetHeader(strings, appWidgetId, filter, icons)
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .padding(12.dp),
            ) {
                if (tasks.isEmpty() && recentlyCompleted.isEmpty()) {
                    EmptyState(strings.emptyTitle)
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        items(tasks) { task ->
                            Box(modifier = GlanceModifier.padding(bottom = 6.dp)) {
                                TaskRow(task, icons, isCompleted = false, checkCd = strings.checkCd)
                            }
                        }
                        items(recentlyCompleted) { task ->
                            Box(modifier = GlanceModifier.padding(bottom = 6.dp)) {
                                TaskRow(task, icons, isCompleted = true, checkCd = strings.checkCd)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun WidgetHeader(
        strings: WidgetStrings,
        appWidgetId: Int,
        filter: WidgetFilter,
        icons: WidgetIcons,
    ) {
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
                provider = ImageProvider(icons.logo),
                contentDescription = strings.openAppCd,
                modifier = GlanceModifier
                    .size(20.dp)
                    .clickable(actionStartActivity<MainActivity>()),
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
                    provider = ImageProvider(icons.add),
                    contentDescription = strings.addTaskCd,
                    modifier = GlanceModifier.size(18.dp),
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
    private fun TaskRow(
        task: Task,
        icons: WidgetIcons,
        isCompleted: Boolean,
        checkCd: String,
    ) {
        val toggleAction = actionStartActivity<WidgetToggleTrampolineActivity>(
            parameters = actionParametersOf(
                WidgetToggleTrampolineActivity.KEY_TASK_ID to task.id,
            ),
        )
        val editAction = actionStartActivity<EditTaskActivity>(
            parameters = actionParametersOf(KEY_EDIT_TASK_ID to task.id),
        )
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(4.dp)
                .background(WidgetSurface)
                .clickable(editAction)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(if (isCompleted) icons.checked else icons.checkbox),
                    contentDescription = checkCd,
                    modifier = GlanceModifier
                        .size(18.dp)
                        .clickable(toggleAction),
                )
                Spacer(modifier = GlanceModifier.width(12.dp))
                Text(
                    text = task.title,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = if (isCompleted) WidgetTextSecondary else WidgetTextPrimary,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
            if (task.dueDate != null && !isCompleted) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                ) {
                    Spacer(modifier = GlanceModifier.width(30.dp))
                    Text(
                        text = Instant.ofEpochMilli(task.dueDate).atZone(ZoneId.systemDefault())
                            .format(widgetDateFormatter),
                        style = TextStyle(fontSize = 12.sp, color = WidgetCoral),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
