package com.hfad.atomgit.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.hfad.atomgit.data.repository.DefaultThemePreferencesRepository
import com.hfad.atomgit.data.repository.ThemePreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ThemePreferences

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindThemePreferencesRepository(
        impl: DefaultThemePreferencesRepository,
    ): ThemePreferencesRepository

    companion object {
        @Provides
        @Singleton
        @ThemePreferences
        fun provideThemePreferencesDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("theme_preferences") },
        )
    }
}
