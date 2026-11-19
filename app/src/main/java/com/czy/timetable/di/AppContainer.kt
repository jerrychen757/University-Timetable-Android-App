package com.czy.timetable.di

import android.content.Context
import com.czy.timetable.AppDatabase
import com.czy.timetable.CourseDao
import com.czy.timetable.NCNUApiManager
import com.czy.timetable.SettingsManager

interface AppContainer {
    val courseDao: CourseDao
    val settingsManager: SettingsManager
    val apiManager: NCNUApiManager
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val courseDao: CourseDao by lazy {
        AppDatabase.getDatabase(context).courseDao()
    }
    
    override val settingsManager: SettingsManager by lazy {
        SettingsManager(context)
    }
    
    override val apiManager: NCNUApiManager by lazy {
        NCNUApiManager()
    }
}
