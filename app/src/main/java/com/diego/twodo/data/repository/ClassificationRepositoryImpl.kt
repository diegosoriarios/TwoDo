package com.diego.twodo.data.repository

import com.diego.twodo.data.classifier.HeuristicClassifier
import com.diego.twodo.domain.classifier.LlmClassifier
import com.diego.twodo.domain.model.BucketSource
import com.diego.twodo.domain.model.ClassificationResult
import com.diego.twodo.domain.model.ReevaluationOutcome
import com.diego.twodo.domain.model.ReevaluationSummary
import com.diego.twodo.domain.model.Task
import com.diego.twodo.domain.model.TaskBucket
import com.diego.twodo.domain.repository.ClassificationRepository
import com.diego.twodo.domain.repository.ClassificationScheduler
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
    private val classificationScheduler: ClassificationScheduler,
) : ClassificationRepository {

    private val bgDispatcher: CoroutineDispatcher = Dispatchers.IO

    override suspend fun captureTask(
        title: String,
        userSelectedBucket: TaskBucket?,
        dueDate: Long?,
        reminderAt: Long?,
        labelIds: Set<Long>,
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
            taskRepository.setTaskLabels(id, labelIds)
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
        taskRepository.setTaskLabels(taskId, labelIds)

        val task = taskRepository.getTaskByIdSync(taskId)
            ?: Task(id = taskId, title = trimmedTitle, bucket = heuristicResult.category, bucketSource = BucketSource.HEURISTIC)

        // Capture stays instant; the LLM refines ambiguous tasks off the capture path.
        if (heuristicResult.category == TaskBucket.UNCLASSIFIED && llmClassifier.isModelLoaded()) {
            classificationScheduler.schedule()
        }

        return@withContext task
    }

    override suspend fun processUnclassifiedQueue() = withContext(bgDispatcher) {
        if (!llmClassifier.isModelLoaded()) return@withContext

        val unclassifiedTasks = taskRepository.getUnclassifiedTasksSync()
        for (task in unclassifiedTasks) {
            // User manually overridden tasks must never be reclassified
            if (task.bucketSource == BucketSource.USER) continue

            processUnclassifiedTask(task)
        }
    }

    override suspend fun reevaluateTask(taskId: Long): ReevaluationOutcome? = withContext(bgDispatcher) {
        val task = taskRepository.getTaskByIdSync(taskId) ?: return@withContext null
        val result = resolveClassification(task.title) ?: return@withContext null

        if (result.category != task.bucket) {
            taskRepository.updateTaskBucket(
                id = task.id,
                bucket = result.category,
                source = result.source,
            )
        }

        ReevaluationOutcome(
            taskId = task.id,
            previousBucket = task.bucket,
            newBucket = result.category,
            source = result.source,
            reason = result.reason,
        )
    }

    override suspend fun reevaluateActiveTasks(
        includeManual: Boolean,
    ): ReevaluationSummary = withContext(bgDispatcher) {
        val activeTasks = taskRepository.getActiveTasksSync()
        var evaluated = 0
        var updated = 0
        var skipped = 0

        for (task in activeTasks) {
            if (!includeManual && task.bucketSource == BucketSource.USER) {
                skipped++
                continue
            }

            val result = resolveClassification(task.title)
            if (result == null) {
                skipped++
                continue
            }

            evaluated++
            if (result.category != task.bucket) {
                taskRepository.updateTaskBucket(
                    id = task.id,
                    bucket = result.category,
                    source = result.source,
                )
                updated++
            }
        }

        ReevaluationSummary(
            evaluated = evaluated,
            updated = updated,
            skipped = skipped,
        )
    }

    /** Rules first; the LLM is only consulted when the heuristic cannot classify the title. */
    private suspend fun resolveClassification(title: String): ClassificationResult? {
        val heuristicResult = heuristicClassifier.classify(title)
        if (heuristicResult.category != TaskBucket.UNCLASSIFIED) {
            return heuristicResult
        }
        return llmClassifier.classifyTask(title)
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
