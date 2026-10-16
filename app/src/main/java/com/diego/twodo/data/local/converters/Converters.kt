package com.diego.twodo.data.local.converters

import androidx.room.TypeConverter
import com.diego.twodo.domain.model.BucketSource
import com.diego.twodo.domain.model.TaskBucket

class Converters {
    @TypeConverter
    fun fromTaskBucket(bucket: TaskBucket): String = bucket.name

    @TypeConverter
    fun toTaskBucket(value: String): TaskBucket = try {
        TaskBucket.valueOf(value)
    } catch (e: Exception) {
        TaskBucket.UNCLASSIFIED
    }

    @TypeConverter
    fun fromBucketSource(source: BucketSource): String = source.name

    @TypeConverter
    fun toBucketSource(value: String): BucketSource = try {
        BucketSource.valueOf(value)
    } catch (e: Exception) {
        BucketSource.HEURISTIC
    }
}
