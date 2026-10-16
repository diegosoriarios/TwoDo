package com.diego.twodo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.diego.twodo.data.local.converters.Converters
import com.diego.twodo.data.local.dao.TaskDao
import com.diego.twodo.data.local.entity.TaskEntity

@Database(
    entities = [TaskEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TwoDoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
