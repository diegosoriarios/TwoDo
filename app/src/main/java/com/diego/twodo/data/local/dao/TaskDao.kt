package com.diego.twodo.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.diego.twodo.data.local.entity.TaskEntity
import com.diego.twodo.domain.model.BucketSource
import com.diego.twodo.domain.model.TaskBucket
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND bucket = :bucket ORDER BY createdAt DESC")
    fun getActiveTasksByBucket(bucket: TaskBucket): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE bucket = :bucket ORDER BY createdAt DESC")
    fun getTasksByBucket(bucket: TaskBucket): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE bucket = 'UNCLASSIFIED' AND isCompleted = 0 ORDER BY createdAt ASC")
    fun getUnclassifiedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE bucket = 'UNCLASSIFIED' AND isCompleted = 0 ORDER BY createdAt ASC")
    suspend fun getUnclassifiedTasksSync(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskById(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskByIdSync(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTaskCompletion(id: Long, isCompleted: Boolean, updatedAt: Long)

    @Query("UPDATE tasks SET bucket = :bucket, bucketSource = :bucketSource, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTaskBucket(
        id: Long,
        bucket: TaskBucket,
        bucketSource: BucketSource,
        updatedAt: Long
    )

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
}
