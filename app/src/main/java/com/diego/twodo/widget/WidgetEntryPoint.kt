package com.diego.twodo.widget

import android.content.Context
import com.diego.twodo.domain.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun taskRepository(): TaskRepository
}

internal fun widgetTaskRepository(context: Context): TaskRepository =
    EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        .taskRepository()
