package com.diego.twodo.domain.repository

import com.diego.twodo.domain.model.BucketSource
import com.diego.twodo.domain.model.Task
import com.diego.twodo.domain.model.TaskBucket
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getActiveTasks(): Flow<List<Task>>
    fun getCompletedTasks(): Flow<List<Task>>
    fun getAllTasks(): Flow<List<Task>>
    fun getActiveTasksByBucket(bucket: TaskBucket): Flow<List<Task>>
    fun getUnclassifiedTasks(): Flow<List<Task>>
    suspend fun getUnclassifiedTasksSync(): List<Task>
    fun getTaskById(id: Long): Flow<Task?>
    suspend fun getTaskByIdSync(id: Long): Task?
    suspend fun addTask(
        title: String,
        bucket: TaskBucket = TaskBucket.UNCLASSIFIED,
        source: BucketSource = BucketSource.HEURISTIC,
        dueDate: Long? = null,
        reminderAt: Long? = null,
    ): Long
    suspend fun updateTask(task: Task)
    suspend fun updateTaskDetails(
        id: Long,
        title: String,
        bucket: TaskBucket?,
        dueDate: Long?,
        reminderAt: Long?,
    )
    suspend fun toggleTaskCompletion(id: Long, isCompleted: Boolean)
    suspend fun updateTaskBucket(id: Long, bucket: TaskBucket, source: BucketSource)
    suspend fun deleteTask(task: Task)
    suspend fun deleteTaskById(id: Long)
}
