package com.diego.twodo

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.diego.twodo.data.worker.WidgetRefreshWorker
import com.diego.twodo.notifications.ModelDownloadNotification
import com.diego.twodo.notifications.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class TwoDoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        ModelDownloadNotification.createChannel(this)
        scheduleWidgetRefresh()
    }

    private fun scheduleWidgetRefresh() {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            UNIQUE_WIDGET_REFRESH_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val UNIQUE_WIDGET_REFRESH_WORK = "tasks_widget_periodic_refresh"
    }
}
