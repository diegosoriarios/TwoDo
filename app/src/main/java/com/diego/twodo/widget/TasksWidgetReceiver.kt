package com.diego.twodo.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class TasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: TasksWidget = TasksWidget()
}
