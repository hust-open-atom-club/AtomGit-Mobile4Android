package com.hfad.atomgit.di

import android.content.Context
import com.hfad.atomgit.data.local.security.KeystoreTokenCipher
import com.hfad.atomgit.data.local.security.TokenCipher
import com.hfad.atomgit.data.local.session.FileSessionStore
import com.hfad.atomgit.data.local.session.SessionStore
import com.hfad.atomgit.data.remote.api.CurrentUserApi
import com.hfad.atomgit.data.remote.api.RetrofitCurrentUserApi
import com.hfad.atomgit.data.repository.DefaultSessionRepository
import com.hfad.atomgit.data.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {

    @Binds
    @Singleton
    abstract fun bindTokenCipher(impl: KeystoreTokenCipher): TokenCipher

    @Binds
    @Singleton
    abstract fun bindSessionStore(impl: FileSessionStore): SessionStore

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: DefaultSessionRepository): SessionRepository

    @Binds
    @Singleton
    abstract fun bindCurrentUserApi(impl: RetrofitCurrentUserApi): CurrentUserApi

    companion object {

        /**
         * `noBackupFilesDir` 不在 Auto Backup 与设备迁移范围内
         */
        @Provides
        @Singleton
        @SessionDirectory
        fun provideSessionDirectory(@ApplicationContext context: Context): File =
            File(context.noBackupFilesDir, "session")

        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
