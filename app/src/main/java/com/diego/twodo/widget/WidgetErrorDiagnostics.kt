package com.diego.twodo.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import com.diego.twodo.R
import java.io.File
import java.util.Date

/**
 * TEMPORARY DIAGNOSTICS — remove once the widget crash is diagnosed and fixed.
 *
 * To remove: delete this file, delete res/layout/widget_error_diagnostics.xml,
 * and delete the onCompositionError override in TasksWidget.
 *
 * When the Glance composition fails, this renders the exception class, message,
 * cause chain and top stack frames directly on the widget (release builds show
 * obfuscated frame names; the exception/cause names stay readable), logs the
 * full trace, and appends it to filesDir/widget_errors.log.
 */
object WidgetErrorDiagnostics {

    private const val TAG = "TwoDoWidget"
    private const val MAX_LOG_FILE_CHARS = 262_144
    private const val MAX_WIDGET_TEXT_CHARS = 4_000
    private const val MAX_CAUSE_DEPTH = 4
    private const val FRAMES_PER_CAUSE = 6

    fun report(context: Context, appWidgetId: Int, throwable: Throwable) {
        Log.e(TAG, "Widget composition failed (appWidgetId=$appWidgetId)", throwable)
        appendToLog(context, appWidgetId, throwable)
        showErrorOnWidget(context, appWidgetId, throwable)
    }

    private fun appendToLog(context: Context, appWidgetId: Int, throwable: Throwable) {
        try {
            val file = File(context.filesDir, "widget_errors.log")
            var text = if (file.exists()) file.readText() else ""
            text += "\n===== ${Date()} appWidgetId=$appWidgetId =====\n" +
                Log.getStackTraceString(throwable) + "\n"
            if (text.length > MAX_LOG_FILE_CHARS) {
                text = text.substring(text.length - MAX_LOG_FILE_CHARS / 2)
            }
            file.writeText(text)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to persist widget error trace", t)
        }
    }

    private fun showErrorOnWidget(context: Context, appWidgetId: Int, throwable: Throwable) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_error_diagnostics)
            views.setTextViewText(R.id.widget_diag_exception, summarize(throwable))
            views.setTextViewText(R.id.widget_diag_frames, stackFrames(throwable))
            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to render widget error UI", t)
        }
    }

    private fun summarize(throwable: Throwable): String = buildString {
        var cause: Throwable? = throwable
        var depth = 0
        while (cause != null && depth < MAX_CAUSE_DEPTH) {
            if (depth > 0) append("\nCaused by: ")
            append(cause.javaClass.name)
            cause.message?.let { append(": ").append(it) }
            cause = cause.cause?.takeIf { it !== cause }
            depth++
        }
    }.take(MAX_WIDGET_TEXT_CHARS)

    private fun stackFrames(throwable: Throwable): String = buildString {
        var cause: Throwable? = throwable
        var depth = 0
        while (cause != null && depth < MAX_CAUSE_DEPTH) {
            if (depth > 0) append("\n--\n")
            cause.stackTrace.take(FRAMES_PER_CAUSE).forEach { element ->
                append(element.className).append('.').append(element.methodName)
                append('(').append(element.lineNumber).append(")\n")
            }
            cause = cause.cause?.takeIf { it !== cause }
            depth++
        }
    }.take(MAX_WIDGET_TEXT_CHARS)
}
