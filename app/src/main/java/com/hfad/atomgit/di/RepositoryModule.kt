package com.hfad.atomgit.di

import com.hfad.atomgit.data.repository.DefaultFeedRepository
import com.hfad.atomgit.data.repository.DefaultFollowRepository
import com.hfad.atomgit.data.repository.DefaultTodoRepository
import com.hfad.atomgit.data.repository.DefaultUserProfileRepository
import com.hfad.atomgit.data.repository.FeedRepository
import com.hfad.atomgit.data.repository.FollowRepository
import com.hfad.atomgit.data.repository.TodoRepository
import com.hfad.atomgit.data.repository.UserProfileRepository
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

    @Binds
    @Singleton
    abstract fun bindFollowRepository(impl: DefaultFollowRepository): FollowRepository

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(
        impl: DefaultUserProfileRepository,
    ): UserProfileRepository
}
