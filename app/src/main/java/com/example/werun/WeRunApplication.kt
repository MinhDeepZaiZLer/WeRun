package com.example.werun

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WeRunApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}