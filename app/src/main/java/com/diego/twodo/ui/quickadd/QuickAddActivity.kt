package com.diego.twodo.ui.quickadd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.diego.twodo.domain.model.TaskBucket
import com.diego.twodo.domain.repository.ClassificationRepository
import com.diego.twodo.ui.composer.TaskComposerSheet
import com.diego.twodo.ui.theme.TwoDoTheme
import com.diego.twodo.widget.TasksWidget
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuickAddActivity : ComponentActivity() {

    @Inject
    lateinit var classificationRepository: ClassificationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TwoDoTheme {
                TaskComposerSheet(
                    onDismiss = ::finish,
                    onSubmit = ::saveTask,
                )
            }
        }
    }

    private fun saveTask(
        title: String,
        bucket: TaskBucket?,
        dueDate: Long?,
        reminderAt: Long?,
    ) {
        if (title.isBlank()) {
            finish()
            return
        }
        lifecycleScope.launch {
            classificationRepository.captureTask(title.trim(), bucket, dueDate, reminderAt)
            TasksWidget().updateAll(applicationContext)
            finish()
        }
    }
}
