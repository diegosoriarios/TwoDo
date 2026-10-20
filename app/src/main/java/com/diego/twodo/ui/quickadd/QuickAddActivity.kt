package com.diego.twodo.ui.quickadd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.diego.twodo.domain.model.Label
import com.diego.twodo.domain.model.TaskBucket
import com.diego.twodo.domain.repository.ClassificationRepository
import com.diego.twodo.domain.repository.TaskRepository
import com.diego.twodo.ui.composer.TaskComposerSheet
import com.diego.twodo.ui.theme.TwoDoTheme
import com.diego.twodo.widget.TasksWidget
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class QuickAddActivity : ComponentActivity() {

    @Inject
    lateinit var classificationRepository: ClassificationRepository

    @Inject
    lateinit var taskRepository: TaskRepository

    private var labels by mutableStateOf<List<Label>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            labels = taskRepository.getLabels().first()
        }
        setContent {
            TwoDoTheme {
                TaskComposerSheet(
                    onDismiss = ::finish,
                    onSubmit = ::saveTask,
                    labels = labels,
                    onCreateLabel = { name, onCreated ->
                        lifecycleScope.launch {
                            onCreated(taskRepository.createLabel(name))
                            labels = taskRepository.getLabels().first()
                        }
                    },
                )
            }
        }
    }

    private fun saveTask(
        title: String,
        bucket: TaskBucket?,
        dueDate: Long?,
        reminderAt: Long?,
        labelIds: Set<Long>,
        isCompleted: Boolean,
    ) {
        if (title.isBlank()) {
            finish()
            return
        }
        lifecycleScope.launch {
            classificationRepository.captureTask(title.trim(), bucket, dueDate, reminderAt, labelIds)
            TasksWidget().updateAll(applicationContext)
            finish()
        }
    }
}
