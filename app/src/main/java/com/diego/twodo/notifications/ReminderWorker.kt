package com.diego.twodo.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.diego.twodo.domain.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        val title = inputData.getString(KEY_TITLE)
        if (taskId <= 0L || title.isNullOrBlank()) return@withContext Result.failure()

        val repository = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderEntryPoint::class.java,
        ).taskRepository()

        val task = repository.getTaskByIdSync(taskId)
        if (task == null || task.isCompleted) {
            return@withContext Result.success()
        }

        NotificationHelper.showTaskReminder(applicationContext, taskId, title)
        Result.success()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderEntryPoint {
        fun taskRepository(): TaskRepository
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_TITLE = "title"
    }
}
