package com.example.io_motion.data.di

import javax.inject.Qualifier

/**
 * Marks the process-lifetime [kotlinx.coroutines.CoroutineScope] used for persistence writes
 * that must survive cancellation of a caller's scope (e.g. a ViewModel torn down immediately
 * after triggering a save).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
