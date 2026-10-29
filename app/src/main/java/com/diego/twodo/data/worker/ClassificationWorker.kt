package com.diego.twodo.data.worker

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.diego.twodo.domain.manager.ModelManager
import com.diego.twodo.domain.repository.ClassificationRepository
import com.diego.twodo.widget.TasksWidget
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class ClassificationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ClassificationWorkerEntryPoint::class.java,
        )
        return try {
            // Ensure the singleton model manager has configured the classifier in this process,
            // which matters when WorkManager starts a fresh process for this worker.
            entryPoint.modelManager()
            entryPoint.classificationRepository().processUnclassifiedQueue()
            TasksWidget().updateAll(applicationContext)
            Result.success()
        } catch (throwable: Throwable) {
            Log.e(TAG, "Background classification failed", throwable)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "twodo_classification_queue"
        private const val TAG = "ClassificationWorker"
        private const val MAX_RETRIES = 2
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ClassificationWorkerEntryPoint {
    fun classificationRepository(): ClassificationRepository
    fun modelManager(): ModelManager
}
