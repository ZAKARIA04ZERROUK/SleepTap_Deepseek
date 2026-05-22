package com.sleeptap

import android.app.Application
import com.sleeptap.data.AppDatabase

class SleepTapApp : Application() {
    lateinit var db: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getInstance(this)
    }
}
