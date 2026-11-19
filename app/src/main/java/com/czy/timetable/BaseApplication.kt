package com.czy.timetable

import android.app.Application
import com.czy.timetable.di.AppContainer
import com.czy.timetable.di.DefaultAppContainer

class BaseApplication : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
