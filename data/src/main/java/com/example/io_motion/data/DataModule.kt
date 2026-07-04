package com.example.io_motion.data

import android.content.Context
import androidx.room.Room
import com.example.io_motion.data.dao.SessionDao
import com.example.io_motion.data.repository.SessionRepository
import com.example.io_motion.data.repository.SessionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
    }
}
