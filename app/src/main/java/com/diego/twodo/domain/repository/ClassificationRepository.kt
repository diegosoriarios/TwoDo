package com.diego.twodo.domain.repository

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
}
