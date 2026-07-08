package com.example.io_motion.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.io_motion.data.dao.DietDao
import com.example.io_motion.data.dao.SessionDao
import com.example.io_motion.data.dao.WorkoutDao
import com.example.io_motion.data.di.ApplicationScope
import com.example.io_motion.data.preferences.SettingsPreferences
import com.example.io_motion.data.repository.DietRepository
import com.example.io_motion.data.repository.DietRepositoryImpl
import com.example.io_motion.data.repository.SessionRepository
import com.example.io_motion.data.repository.SessionRepositoryImpl
import com.example.io_motion.data.repository.SettingsRepository
import com.example.io_motion.data.repository.WorkoutRepository
import com.example.io_motion.data.repository.WorkoutRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

// File name kept as "theme_prefs" (predates the accent/model-variant additions) so existing
// persisted preferences aren't lost on upgrade.
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsPreferences): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(impl: WorkoutRepositoryImpl): WorkoutRepository

    @Binds
    @Singleton
    abstract fun bindDietRepository(impl: DietRepositoryImpl): DietRepository

    companion object {
        @Provides
        @Singleton
        fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.settingsDataStore

        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "io_motion.db")
                // No destructive fallback: session history must survive schema bumps. Every
                // version increase ships an explicit Migration registered here (see MIGRATION_1_2).
                .addMigrations(MIGRATION_1_2)
                .build()

        @Provides
        @Singleton
        fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()

        @Provides
        @Singleton
        fun provideWorkoutDao(db: AppDatabase): WorkoutDao = db.workoutDao()

        @Provides
        @Singleton
        fun provideDietDao(db: AppDatabase): DietDao = db.dietDao()

        /**
         * Process-lifetime scope for persistence writes. [SupervisorJob] means one failed write
         * never cancels the scope for subsequent ones; this scope is never itself cancelled, so
         * writes started on it complete even if the caller (e.g. a ViewModel) is torn down first.
         */
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
