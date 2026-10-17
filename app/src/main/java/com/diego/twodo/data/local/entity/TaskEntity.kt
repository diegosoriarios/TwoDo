package com.diego.twodo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.diego.twodo.domain.model.BucketSource
import com.diego.twodo.domain.model.TaskBucket

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val bucket: TaskBucket = TaskBucket.UNCLASSIFIED,
    val bucketSource: BucketSource = BucketSource.HEURISTIC,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val dueDate: Long? = null,
    val reminderAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
