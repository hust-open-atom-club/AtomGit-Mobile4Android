package com.hfad.atomgit.di

import com.hfad.atomgit.data.repository.DefaultFeedRepository
import com.hfad.atomgit.data.repository.DefaultFollowRepository
import com.hfad.atomgit.data.repository.DefaultRepoContentRepository
import com.hfad.atomgit.data.repository.DefaultRepoDetailRepository
import com.hfad.atomgit.data.repository.DefaultRepoRepository
import com.hfad.atomgit.data.repository.DefaultTodoRepository
import com.hfad.atomgit.data.repository.DefaultUserProfileRepository
import com.hfad.atomgit.data.repository.FeedRepository
import com.hfad.atomgit.data.repository.FollowRepository
import com.hfad.atomgit.data.repository.RepoContentRepository
import com.hfad.atomgit.data.repository.RepoDetailRepository
import com.hfad.atomgit.data.repository.RepoRepository
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
    abstract fun bindRepoDetailRepository(impl: DefaultRepoDetailRepository): RepoDetailRepository

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

    @Binds
    @Singleton
    abstract fun bindRepoContentRepository(
        impl: DefaultRepoContentRepository,
    ): RepoContentRepository

    @Binds
    @Singleton
    abstract fun bindRepoRepository(impl: DefaultRepoRepository): RepoRepository
}
