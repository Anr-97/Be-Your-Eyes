package com.example.beyoureyes

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BeYourEyesApplication : Application() {
    companion object {
        private const val TAG = "BeYourEyesApp"
    }

    override fun onCreate() {
        Log.d(TAG, "Application onCreate starting")
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
            throwable.printStackTrace()
        }
        
        try {
            super.onCreate()
            Log.d(TAG, "Super onCreate called")
            
            // 初始化其他组件
            initializeComponents()
            
            Log.d(TAG, "Application onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in Application onCreate", e)
            e.printStackTrace()
            throw e
        }
    }
    
    private fun initializeComponents() {
        try {
            Log.d(TAG, "Initializing application components")
            // 这里可以添加其他必要的初始化代码
            Log.d(TAG, "Application components initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing application components", e)
            throw e
        }
    }
} 