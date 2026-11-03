package com.hfad.atomgit.di

import com.hfad.atomgit.data.repository.DefaultFeedRepository
import com.hfad.atomgit.data.repository.DefaultTodoRepository
import com.hfad.atomgit.data.repository.FeedRepository
import com.hfad.atomgit.data.repository.TodoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFeedRepository(impl: DefaultFeedRepository): FeedRepository

    @Binds
    @Singleton
    abstract fun bindTodoRepository(impl: DefaultTodoRepository): TodoRepository
}
