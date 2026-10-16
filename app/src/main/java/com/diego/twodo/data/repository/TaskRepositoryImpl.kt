package com.diego.twodo.data.repository

import com.diego.twodo.data.local.dao.TaskDao
import com.diego.twodo.data.local.entity.TaskEntity
import com.diego.twodo.domain.model.BucketSource
import com.diego.twodo.domain.model.Task
import com.diego.twodo.domain.model.TaskBucket
import com.diego.twodo.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getActiveTasks(): Flow<List<Task>> {
        return taskDao.getActiveTasks().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getActiveTasksByBucket(bucket: TaskBucket): Flow<List<Task>> {
        return taskDao.getActiveTasksByBucket(bucket).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getUnclassifiedTasks(): Flow<List<Task>> {
        return taskDao.getUnclassifiedTasks().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getUnclassifiedTasksSync(): List<Task> {
        return taskDao.getUnclassifiedTasksSync().map { it.toDomain() }
    }

    override fun getTaskById(id: Long): Flow<Task?> {
        return taskDao.getTaskById(id).map { it?.toDomain() }
    }

    override suspend fun getTaskByIdSync(id: Long): Task? {
        return taskDao.getTaskByIdSync(id)?.toDomain()
    }

    override suspend fun addTask(
        title: String,
        bucket: TaskBucket,
        source: BucketSource
    ): Long {
        val entity = TaskEntity(
            title = title,
            bucket = bucket,
            bucketSource = source,
            isCompleted = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return taskDao.insertTask(entity)
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun toggleTaskCompletion(id: Long, isCompleted: Boolean) {
        taskDao.updateTaskCompletion(id, isCompleted, System.currentTimeMillis())
    }

    override suspend fun updateTaskBucket(
        id: Long,
        bucket: TaskBucket,
        source: BucketSource
    ) {
        taskDao.updateTaskBucket(id, bucket, source, System.currentTimeMillis())
    }

    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toEntity())
    }

    override suspend fun deleteTaskById(id: Long) {
        taskDao.deleteTaskById(id)
    }

    private fun TaskEntity.toDomain(): Task = Task(
        id = id,
        title = title,
        bucket = bucket,
        bucketSource = bucketSource,
        isCompleted = isCompleted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Task.toEntity(): TaskEntity = TaskEntity(
        id = id,
        title = title,
        bucket = bucket,
        bucketSource = bucketSource,
        isCompleted = isCompleted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
