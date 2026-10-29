package com.diego.twodo.domain.repository

import com.diego.twodo.domain.model.ReevaluationOutcome
import com.diego.twodo.domain.model.ReevaluationSummary
import com.diego.twodo.domain.model.Task
import com.diego.twodo.domain.model.TaskBucket

interface ClassificationRepository {
    /**
     * Instantly captures a new task. Applies heuristic classification immediately.
     * If task is ambiguous, it is saved as UNCLASSIFIED and queued for background LLM classification.
     * If user explicitly selected a bucket, bucketSource is set to USER and never overridden.
     */
    suspend fun captureTask(
        title: String,
        userSelectedBucket: TaskBucket? = null,
        dueDate: Long? = null,
        reminderAt: Long? = null,
        labelIds: Set<Long> = emptySet(),
    ): Task

    /**
     * Processes all pending UNCLASSIFIED tasks using the local LLM classifier.
     * Respects user overrides (tasks with bucketSource == USER are skipped).
     */
    suspend fun processUnclassifiedQueue()

    /**
     * Re-runs classification for a single task, including tasks the user categorized manually.
     * Returns null when no confident category could be determined.
     */
    suspend fun reevaluateTask(taskId: Long): ReevaluationOutcome?

    /**
     * Re-runs classification for all active tasks. Manually categorized tasks are preserved
     * unless [includeManual] is true.
     */
    suspend fun reevaluateActiveTasks(includeManual: Boolean = false): ReevaluationSummary
}
