package com.diego.twodo.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import com.diego.twodo.domain.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun taskRepository(): TaskRepository
}

internal fun widgetTaskRepository(context: Context): TaskRepository =
    EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        .taskRepository()

class ToggleTaskAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val taskId = parameters[KEY_TASK_ID] ?: return
        widgetTaskRepository(context).toggleTaskCompletion(taskId, true)
        TasksWidget().update(context, glanceId)
    }

    companion object {
        val KEY_TASK_ID = ActionParameters.Key<Long>("toggle_task_id")
    }
}
