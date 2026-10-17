package com.diego.twodo.ui.tasks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diego.twodo.domain.manager.ModelManager
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

    val modelStatus: StateFlow<ModelStatus> = modelManager.modelStatus

    val canUseLlm: Boolean
        get() = modelManager.canUseLlm()

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
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            classificationRepository.captureTask(title.trim(), bucket, dueDate, reminderAt)
            TasksWidget().updateAll(context)
        }
    }

    fun updateTask(
        id: Long,
        title: String,
        bucket: TaskBucket?,
        dueDate: Long?,
        reminderAt: Long?,
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskRepository.updateTaskDetails(id, title.trim(), bucket, dueDate, reminderAt)
            TasksWidget().updateAll(context)
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
