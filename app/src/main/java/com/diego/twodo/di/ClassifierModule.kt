package com.diego.twodo.di

import com.diego.twodo.data.classifier.LocalLlmClassifier
import com.diego.twodo.data.repository.ClassificationRepositoryImpl
import com.diego.twodo.domain.classifier.LlmClassifier
import com.diego.twodo.domain.repository.ClassificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClassifierModule {

    @Binds
    @Singleton
    abstract fun bindLlmClassifier(
        localLlmClassifier: LocalLlmClassifier,
    ): LlmClassifier

    @Binds
    @Singleton
    abstract fun bindClassificationRepository(
        classificationRepositoryImpl: ClassificationRepositoryImpl,
    ): ClassificationRepository
}
