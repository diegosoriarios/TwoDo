package com.diego.twodo.domain.model

data class ReevaluationOutcome(
    val taskId: Long,
    val previousBucket: TaskBucket,
    val newBucket: TaskBucket,
    val source: BucketSource,
    val reason: String,
) {
    val changed: Boolean
        get() = previousBucket != newBucket
}

data class ReevaluationSummary(
    val evaluated: Int,
    val updated: Int,
    val skipped: Int,
)
