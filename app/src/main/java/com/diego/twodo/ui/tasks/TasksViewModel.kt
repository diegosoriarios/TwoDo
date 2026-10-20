package com.diego.twodo.ui.tasks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diego.twodo.domain.manager.ModelManager
import com.diego.twodo.domain.model.Label
import com.diego.twodo.domain.model.ModelStatus
import com.diego.twodo.domain.model.Task
import com.diego.twodo.domain.model.TaskBucket
import com.diego.twodo.domain.repository.ClassificationRepository
import com.diego.twodo.domain.repository.TaskRepository
import com.diego.twodo.widget.TasksWidget
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val classificationRepository: ClassificationRepository,
    private val modelManager: ModelManager,
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = taskRepository.getActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val completedTasks: StateFlow<List<Task>> = taskRepository.getCompletedTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val labels: StateFlow<List<Label>> = taskRepository.getLabels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val taskLabelMap: StateFlow<Map<Long, List<Label>>> = taskRepository.getTaskLabelMap()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val modelStatus: StateFlow<ModelStatus> = modelManager.modelStatus

    val canUseLlm: Boolean
        get() = modelManager.canUseLlm()

    private val updateMutex = Mutex()

    init {
        viewModelScope.launch {
            if (modelManager.canUseLlm()) {
                classificationRepository.processUnclassifiedQueue()
            }
        }
    }

    fun addTask(
        title: String,
        bucket: TaskBucket? = null,
        dueDate: Long? = null,
        reminderAt: Long? = null,
        labelIds: Set<Long> = emptySet(),
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            classificationRepository.captureTask(title.trim(), bucket, dueDate, reminderAt, labelIds)
            TasksWidget().updateAll(context)
        }
    }

    fun createLabel(name: String, onCreated: (Long) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            onCreated(taskRepository.createLabel(name))
        }
    }

    fun updateTask(
        id: Long,
        title: String,
        bucket: TaskBucket?,
        dueDate: Long?,
        reminderAt: Long?,
        labelIds: Set<Long>,
        isCompleted: Boolean,
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            updateMutex.withLock {
                taskRepository.updateTaskDetails(id, title.trim(), bucket, dueDate, reminderAt, labelIds)
                val currentTask = taskRepository.getTaskByIdSync(id)
                if (currentTask != null && currentTask.isCompleted != isCompleted) {
                    taskRepository.toggleTaskCompletion(id, isCompleted)
                }
                TasksWidget().updateAll(context)
            }
        }
    }

    fun toggleTaskCompletion(id: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(id, isCompleted)
            TasksWidget().updateAll(context)
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            taskRepository.deleteTaskById(id)
            TasksWidget().updateAll(context)
        }
    }
}
