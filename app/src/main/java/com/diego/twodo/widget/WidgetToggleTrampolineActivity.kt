package com.diego.twodo.widget

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WidgetToggleTrampolineActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) {
            finish()
            return
        }
        scope.launch {
            try {
                val repository = widgetTaskRepository(this@WidgetToggleTrampolineActivity)
                val task = repository.getTaskByIdSync(taskId) ?: return@launch
                val target = !task.isCompleted
                Log.d(WIDGET_LOG_TAG, "toggle taskId=$taskId -> isCompleted=$target")
                repository.toggleTaskCompletion(taskId, target)
                TasksWidget().updateAll(applicationContext)
                delay(CHECKED_FEEDBACK_MS)
                TasksWidget().updateAll(applicationContext)
            } finally {
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "com.diego.twodo.widget.extra.TASK_ID"
        val KEY_TASK_ID = ActionParameters.Key<Long>(EXTRA_TASK_ID)
        private const val CHECKED_FEEDBACK_MS = 2_000L
        private const val WIDGET_LOG_TAG = "TwoDoWidget"
    }
}
