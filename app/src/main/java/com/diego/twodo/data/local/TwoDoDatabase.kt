package com.diego.twodo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.diego.twodo.data.local.converters.Converters
import com.diego.twodo.data.local.dao.LabelDao
import com.diego.twodo.data.local.dao.TaskDao
import com.diego.twodo.data.local.entity.LabelEntity
import com.diego.twodo.data.local.entity.TaskEntity
import com.diego.twodo.data.local.entity.TaskLabelJoin

@Database(
    entities = [TaskEntity::class, LabelEntity::class, TaskLabelJoin::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TwoDoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun labelDao(): LabelDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN dueDate INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderAt INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN completedAt INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `labels` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `task_labels` (" +
                        "`taskId` INTEGER NOT NULL, " +
                        "`labelId` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`taskId`, `labelId`), " +
                        "FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`labelId`) REFERENCES `labels`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_task_labels_labelId` ON `task_labels` (`labelId`)"
                )
            }
        }
    }
}
