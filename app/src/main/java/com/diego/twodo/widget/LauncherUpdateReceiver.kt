package com.diego.twodo.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.diego.twodo.data.worker.WidgetRefreshWorker

class LauncherUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_REPLACED) return
        val replacedPackage = intent.data?.schemeSpecificPart ?: return
        if (replacedPackage != LAUNCHER_PACKAGE) return
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_LAUNCHER_UPDATE_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build(),
        )
    }

    companion object {
        const val LAUNCHER_PACKAGE = "com.example.foz"
        const val UNIQUE_LAUNCHER_UPDATE_WORK = "tasks_widget_launcher_update_refresh"
    }
}
