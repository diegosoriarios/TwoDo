package com.diego.twodo.domain.model

data class Task(
    val id: Long = 0,
    val title: String,
    val bucket: TaskBucket = TaskBucket.UNCLASSIFIED,
    val bucketSource: BucketSource = BucketSource.HEURISTIC,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
