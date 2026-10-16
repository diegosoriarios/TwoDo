package com.diego.twodo.domain.model

data class ClassificationResult(
    val category: TaskBucket,
    val reason: String,
    val source: BucketSource
)
