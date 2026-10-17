package com.diego.twodo.di

import android.content.Context
import androidx.room.Room
import com.diego.twodo.data.local.TwoDoDatabase
import com.diego.twodo.data.local.dao.TaskDao
import com.diego.twodo.data.repository.TaskRepositoryImpl
import com.diego.twodo.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): TwoDoDatabase {
        return Room.databaseBuilder(
            context,
            TwoDoDatabase::class.java,
            "twodo_database",
        ).addMigrations(
            TwoDoDatabase.MIGRATION_1_2,
            TwoDoDatabase.MIGRATION_2_3,
        ).build()
    }

    @Provides
    fun provideTaskDao(database: TwoDoDatabase): TaskDao {
        return database.taskDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        taskRepositoryImpl: TaskRepositoryImpl,
    ): TaskRepository
}
