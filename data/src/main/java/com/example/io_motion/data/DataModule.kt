package com.example.io_motion.data

import android.content.Context
import androidx.room.Room
import com.example.io_motion.data.dao.SessionDao
import com.example.io_motion.data.di.ApplicationScope
import com.example.io_motion.data.repository.SessionRepository
import com.example.io_motion.data.repository.SessionRepositoryImpl
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

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "io_motion.db")
                .fallbackToDestructiveMigration()
                .build()

        @Provides
        @Singleton
        fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()

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
