package com.diego.twodo.ui.quickadd

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.diego.twodo.domain.model.Label
import com.diego.twodo.domain.model.Task
import com.diego.twodo.domain.model.TaskBucket
import com.diego.twodo.domain.repository.TaskRepository
import com.diego.twodo.ui.composer.TaskComposerSheet
import com.diego.twodo.ui.theme.TwoDoTheme
import com.diego.twodo.widget.TasksWidget
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class EditTaskActivity : ComponentActivity() {

    @Inject
    lateinit var taskRepository: TaskRepository

    private var task by mutableStateOf<Task?>(null)
    private var labels by mutableStateOf<List<Label>>(emptyList())
    private var taskLabels by mutableStateOf<List<Label>>(emptyList())
    private val updateMutex = Mutex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loadTask(intent.getLongExtra(EXTRA_TASK_ID, -1L))
        setContent {
            TwoDoTheme {
                task?.let { current ->
                    TaskComposerSheet(
                        onDismiss = ::finish,
                        onSubmit = { title, bucket, dueDate, reminderAt, labelIds, _ ->
                            saveTask(current.id, title, bucket, dueDate, reminderAt, labelIds)
                        },
                        autoSave = { title, bucket, dueDate, reminderAt, labelIds, isCompleted ->
                            persistTask(current.id, title, bucket, dueDate, reminderAt, labelIds, isCompleted)
                        },
                        labels = labels,
                        onCreateLabel = { name, onCreated ->
                            lifecycleScope.launch {
                                onCreated(taskRepository.createLabel(name))
                                labels = taskRepository.getLabels().first()
                            }
                        },
                        initialTitle = current.title,
                        initialBucket = current.bucket,
                        initialDueDate = current.dueDate,
                        initialReminderAt = current.reminderAt,
                        initialLabelIds = taskLabels.map { it.id }.toSet(),
                        initialIsCompleted = current.isCompleted,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadTask(intent.getLongExtra(EXTRA_TASK_ID, -1L))
    }

    private fun loadTask(taskId: Long) {
        if (taskId == -1L) {
            finish()
            return
        }
        lifecycleScope.launch {
            val loaded = taskRepository.getTaskByIdSync(taskId)
            if (loaded == null) {
                finish()
                return@launch
            }
            task = loaded
            labels = taskRepository.getLabels().first()
            taskLabels = taskRepository.getTaskLabelMap().first()[taskId].orEmpty()
        }
    }

    private fun persistTask(
        taskId: Long,
        title: String,
        bucket: TaskBucket?,
        dueDate: Long?,
        reminderAt: Long?,
        labelIds: Set<Long>,
        isCompleted: Boolean,
    ) {
        if (title.isBlank()) return
        lifecycleScope.launch {
            withContext(NonCancellable) {
                updateMutex.withLock {
                    taskRepository.updateTaskDetails(taskId, title.trim(), bucket, dueDate, reminderAt, labelIds)
                    val currentTask = taskRepository.getTaskByIdSync(taskId)
                    if (currentTask != null && currentTask.isCompleted != isCompleted) {
                        taskRepository.toggleTaskCompletion(taskId, isCompleted)
                    }
                    TasksWidget().updateAll(applicationContext)
                }
            }
        }
    }

    private fun saveTask(
        taskId: Long,
        title: String,
        bucket: TaskBucket?,
        dueDate: Long?,
        reminderAt: Long?,
        labelIds: Set<Long>,
    ) {
        if (title.isBlank()) {
            finish()
            return
        }
        lifecycleScope.launch {
            withContext(NonCancellable) {
                updateMutex.withLock {
                    taskRepository.updateTaskDetails(taskId, title.trim(), bucket, dueDate, reminderAt, labelIds)
                    TasksWidget().updateAll(applicationContext)
                }
            }
            finish()
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "edit_task_id"
    }
}
