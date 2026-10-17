package com.diego.twodo.data.repository

import com.diego.twodo.data.local.dao.TaskDao
import com.diego.twodo.data.local.entity.TaskEntity
import com.diego.twodo.domain.model.BucketSource
import com.diego.twodo.domain.model.Task
import com.diego.twodo.domain.model.TaskBucket
import com.diego.twodo.domain.repository.TaskRepository
import com.diego.twodo.notifications.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val reminderScheduler: ReminderScheduler,
) : TaskRepository {

    override fun getActiveTasks(): Flow<List<Task>> {
        return taskDao.getActiveTasks().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getCompletedTasks(): Flow<List<Task>> {
        return taskDao.getCompletedTasks().map { entities -> entities.map { it.toDomain() } }
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
        source: BucketSource,
        dueDate: Long?,
        reminderAt: Long?,
    ): Long {
        val now = System.currentTimeMillis()
        val entity = TaskEntity(
            title = title,
            bucket = bucket,
            bucketSource = source,
            isCompleted = false,
            dueDate = dueDate,
            reminderAt = reminderAt,
            createdAt = now,
            updatedAt = now
        )
        val id = taskDao.insertTask(entity)
        if (reminderAt != null) {
            reminderScheduler.schedule(id, title, reminderAt)
        }
        return id
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun updateTaskDetails(
        id: Long,
        title: String,
        bucket: TaskBucket?,
        dueDate: Long?,
        reminderAt: Long?,
    ) {
        val current = taskDao.getTaskByIdSync(id) ?: return
        val newBucket = bucket ?: current.bucket
        val newSource =
            if (bucket != null && bucket != current.bucket) BucketSource.USER else current.bucketSource
        taskDao.updateTask(
            current.copy(
                title = title,
                bucket = newBucket,
                bucketSource = newSource,
                dueDate = dueDate,
                reminderAt = reminderAt,
                updatedAt = System.currentTimeMillis(),
            )
        )
        when {
            reminderAt == null -> reminderScheduler.cancel(id)
            reminderAt != current.reminderAt || title != current.title ->
                reminderScheduler.schedule(id, title, reminderAt)
        }
    }

    override suspend fun toggleTaskCompletion(id: Long, isCompleted: Boolean) {
        val task = taskDao.getTaskByIdSync(id) ?: return
        val now = System.currentTimeMillis()
        taskDao.updateTaskCompletion(
            id = id,
            isCompleted = isCompleted,
            completedAt = if (isCompleted) now else null,
            updatedAt = now,
        )
        if (isCompleted) {
            reminderScheduler.cancel(id)
        } else {
            task.reminderAt
                ?.takeIf { it > now }
                ?.let { reminderScheduler.schedule(id, task.title, it) }
        }
    }

    override suspend fun updateTaskBucket(
        id: Long,
        bucket: TaskBucket,
        source: BucketSource
    ) {
        taskDao.updateTaskBucket(id, bucket, source, System.currentTimeMillis())
    }

    override suspend fun deleteTask(task: Task) {
        reminderScheduler.cancel(task.id)
        taskDao.deleteTask(task.toEntity())
    }

    override suspend fun deleteTaskById(id: Long) {
        reminderScheduler.cancel(id)
        taskDao.deleteTaskById(id)
    }

    private fun TaskEntity.toDomain(): Task = Task(
        id = id,
        title = title,
        bucket = bucket,
        bucketSource = bucketSource,
        isCompleted = isCompleted,
        completedAt = completedAt,
        dueDate = dueDate,
        reminderAt = reminderAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Task.toEntity(): TaskEntity = TaskEntity(
        id = id,
        title = title,
        bucket = bucket,
        bucketSource = bucketSource,
        isCompleted = isCompleted,
        completedAt = completedAt,
        dueDate = dueDate,
        reminderAt = reminderAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
