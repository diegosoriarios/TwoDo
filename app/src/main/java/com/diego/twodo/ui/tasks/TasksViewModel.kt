package com.diego.twodo.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diego.twodo.domain.manager.ModelManager
import com.diego.twodo.domain.model.BucketSource
import com.diego.twodo.domain.model.ModelStatus
import com.diego.twodo.domain.model.Task
import com.diego.twodo.domain.model.TaskBucket
import com.diego.twodo.domain.repository.ClassificationRepository
import com.diego.twodo.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val classificationRepository: ClassificationRepository,
    private val modelManager: ModelManager,
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = taskRepository.getActiveTasks()
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

    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { classificationRepository.captureTask(title.trim()) }
    }

    fun toggleTaskCompletion(id: Long, isCompleted: Boolean) {
        viewModelScope.launch { taskRepository.toggleTaskCompletion(id, isCompleted) }
    }

    fun overrideBucket(id: Long, bucket: TaskBucket) {
        viewModelScope.launch {
            taskRepository.updateTaskBucket(id, bucket, BucketSource.USER)
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch { taskRepository.deleteTaskById(id) }
    }
}
