package com.diego.twodo.widget

import android.app.Activity
import android.os.Bundle
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
                widgetTaskRepository(this@WidgetToggleTrampolineActivity)
                    .toggleTaskCompletion(taskId, true)
                TasksWidget().updateAll(applicationContext)
            } finally {
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "com.diego.twodo.widget.extra.TASK_ID"
        val KEY_TASK_ID = ActionParameters.Key<Long>(EXTRA_TASK_ID)
    }
}
