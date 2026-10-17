package com.diego.twodo.data.repository

import com.diego.twodo.data.classifier.HeuristicClassifier
import com.diego.twodo.domain.classifier.LlmClassifier
import com.diego.twodo.domain.model.BucketSource
import com.diego.twodo.domain.model.Task
import com.diego.twodo.domain.model.TaskBucket
import com.diego.twodo.domain.repository.ClassificationRepository
import com.diego.twodo.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassificationRepositoryImpl @Inject constructor(
    private val taskRepository: TaskRepository,
    private val heuristicClassifier: HeuristicClassifier,
    private val llmClassifier: LlmClassifier,
) : ClassificationRepository {

    private val bgDispatcher: CoroutineDispatcher = Dispatchers.IO

    override suspend fun captureTask(
        title: String,
        userSelectedBucket: TaskBucket?,
        dueDate: Long?,
        reminderAt: Long?,
    ): Task = withContext(bgDispatcher) {
        val trimmedTitle = title.trim()

        if (userSelectedBucket != null) {
            val id = taskRepository.addTask(
                title = trimmedTitle,
                bucket = userSelectedBucket,
                source = BucketSource.USER,
                dueDate = dueDate,
                reminderAt = reminderAt,
            )
            return@withContext taskRepository.getTaskByIdSync(id)
                ?: Task(id = id, title = trimmedTitle, bucket = userSelectedBucket, bucketSource = BucketSource.USER)
        }

        // Instant heuristic classification
        val heuristicResult = heuristicClassifier.classify(trimmedTitle)
        val taskId = taskRepository.addTask(
            title = trimmedTitle,
            bucket = heuristicResult.category,
            source = BucketSource.HEURISTIC,
            dueDate = dueDate,
            reminderAt = reminderAt,
        )

        val task = taskRepository.getTaskByIdSync(taskId)
            ?: Task(id = taskId, title = trimmedTitle, bucket = heuristicResult.category, bucketSource = BucketSource.HEURISTIC)

        // Queue for background LLM classification if heuristic left it unclassified
        if (heuristicResult.category == TaskBucket.UNCLASSIFIED) {
            processUnclassifiedTask(task)
        }

        return@withContext task
    }

    override suspend fun processUnclassifiedQueue() = withContext(bgDispatcher) {
        val unclassifiedTasks = taskRepository.getUnclassifiedTasksSync()
        for (task in unclassifiedTasks) {
            // User manually overridden tasks must never be reclassified
            if (task.bucketSource == BucketSource.USER) continue

            processUnclassifiedTask(task)
        }
    }

    private suspend fun processUnclassifiedTask(task: Task) {
        if (task.bucketSource == BucketSource.USER) return

        val result = llmClassifier.classifyTask(task.title) ?: return
        taskRepository.updateTaskBucket(
            id = task.id,
            bucket = result.category,
            source = BucketSource.LLM,
        )
    }
}
