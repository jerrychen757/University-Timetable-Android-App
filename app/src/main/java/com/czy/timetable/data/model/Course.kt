package com.czy.timetable

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    val dayIndex: Int, // 0-4 (或 0-6 如果包含週末)
    val periodId: String, // a, b, c, d, e, f, g...
    val name: String,
    val location: String = "",
    val teacher: String = "",
    val colorIndex: Int = 0,
    val memo: String = "",
    val shouldNotify: Boolean = false,
    val notificationMinutes: Int = 10,
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val midtermDate: String = "", // 儲存 YYYY-MM-DD
    val finalExamDate: String = "",
    val reminderMode: Int = 0 // 0: 一般通知, 1: 鬧鐘提醒
)
