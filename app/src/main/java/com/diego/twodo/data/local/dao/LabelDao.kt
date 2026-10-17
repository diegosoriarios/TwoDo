package com.diego.twodo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.diego.twodo.data.local.entity.LabelEntity
import com.diego.twodo.data.local.entity.TaskLabelJoin
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {

    @Query("SELECT * FROM labels ORDER BY name COLLATE NOCASE ASC")
    fun getLabels(): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): LabelEntity?

    @Insert
    suspend fun insertLabel(label: LabelEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertJoin(join: TaskLabelJoin)

    @Query("DELETE FROM task_labels WHERE taskId = :taskId")
    suspend fun clearJoinsForTask(taskId: Long)

    @Query("SELECT * FROM task_labels")
    fun getAllJoins(): Flow<List<TaskLabelJoin>>
}
