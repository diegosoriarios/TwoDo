package com.diego.twodo.data.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.diego.twodo.domain.repository.ClassificationScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassificationSchedulerImpl @Inject constructor(
    @ApplicationContext context: Context,
) : ClassificationScheduler {

    private val workManager = WorkManager.getInstance(context)

    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<ClassificationWorker>()
            .addTag(WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(
            ClassificationWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }

    companion object {
        const val WORK_TAG = "twodo_classification"
    }
}
