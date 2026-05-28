package com.example

import android.app.Application
import android.util.Log
import com.example.util.ErrorLogger
import com.example.worker.InteractionTrackingWorker

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Setup self-monitoring for crashes immediately
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            ErrorLogger.log(this, "CRASH: ${throwable.message}")
            throwable.stackTrace.take(5).forEach {
                ErrorLogger.log(this, "  at $it")
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Schedule periodic interaction log checks (every 15 mins) on application lifecycle startup
        try {
            InteractionTrackingWorker.schedule(this)
        } catch (e: Exception) {
            Log.e("MyApplication", "Error scheduling InteractionTrackingWorker", e)
        }
    }
}
