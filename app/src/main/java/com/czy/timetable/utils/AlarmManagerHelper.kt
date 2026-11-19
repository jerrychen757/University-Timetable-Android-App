package com.czy.timetable

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.czy.timetable.receiver.AlarmReceiver
import kotlinx.coroutines.flow.first
import java.util.Calendar

object AlarmManagerHelper {

    // ✅ 同步並重設所有鬧鐘 (只要課表有變、或是重開機，就呼叫它)
    suspend fun syncAllAlarms(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val courses = db.courseDao().getAllCourses().first()
        val settings = SettingsManager(context)
        val globalNotify = settings.globalNotifyFlow.first()
        val vacationEnabled = settings.vacationModeEnabledFlow.first()
        val winterStart = settings.winterVacationStartFlow.first()
        val winterEnd = settings.winterVacationEndFlow.first()
        val summerStart = settings.summerVacationStartFlow.first()
        val summerEnd = settings.summerVacationEndFlow.first()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        courses.forEach { course ->
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("COURSE_NAME", course.name)
                putExtra("COURSE_LOCATION", course.location)
                putExtra("REMINDER_MODE", course.reminderMode) // ✅ 使用課程專屬的提醒模式
            }

            // 使用 course.id 作為專屬的 RequestCode
            val pendingIntent = PendingIntent.getBroadcast(
                context, course.id ?: 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 1. 為了確保不重複，先取消這堂課原本的舊鬧鐘
            alarmManager.cancel(pendingIntent)

            // 2. 如果全域通知有開，且這堂課也有開提醒，就精算時間並設定新鬧鐘！
            if (globalNotify && course.shouldNotify) {
                val alarmTime = calculateNextAlarmTime(course.dayIndex, course.periodId, course.notificationMinutes)
                
                // ✅ 檢查是否處於假期模式且鬧鐘時間在假期內
                val isInVacation = if (vacationEnabled) {
                    checkIsInVacation(alarmTime, winterStart, winterEnd, summerStart, summerEnd)
                } else false

                if (!isInVacation) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
                    }
                }
            }
        }
    }

    // ✅ 檢查給定的時間戳是否落在假期內
    private fun checkIsInVacation(timeMillis: Long, wStart: String, wEnd: String, sStart: String, sEnd: String): Boolean {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val checkDate = dateFormat.format(java.util.Date(timeMillis))

        fun isBetween(target: String, start: String, end: String): Boolean {
            if (start.isEmpty() || end.isEmpty()) return false
            return target >= start && target <= end
        }

        return isBetween(checkDate, wStart, wEnd) || isBetween(checkDate, sStart, sEnd)
    }

    // ✅ 精算「下一次」響鈴的 Unix 時間戳
    private fun calculateNextAlarmTime(dayIndex: Int, periodId: String, advanceMinutes: Int): Long {
        // 定義每節課的開始時間 (時, 分)
        val periodTimes = mapOf(
            "x" to Pair(6, 10), "y" to Pair(7, 10), "a" to Pair(8, 10), "b" to Pair(9, 10),
            "c" to Pair(10, 10), "d" to Pair(11, 10), "e" to Pair(13, 30), "f" to Pair(14, 30),
            "g" to Pair(15, 30), "h" to Pair(16, 30), "i" to Pair(17, 30), "j" to Pair(18, 30),
            "k" to Pair(19, 30), "l" to Pair(20, 30), "m" to Pair(21, 30)
        )
        val (hour, minute) = periodTimes[periodId.lowercase()] ?: Pair(8, 0)

        val calendar = Calendar.getInstance()
        val currentMillis = calendar.timeInMillis

        // Android 系統：日=1, 一=2... 六=7。我們的 dayIndex：一=0... 日=6
        val targetDayOfWeek = if (dayIndex == 6) Calendar.SUNDAY else dayIndex + 2

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // 扣掉提前提醒的分鐘數
        calendar.add(Calendar.MINUTE, -advanceMinutes)

        // 把日期往後推，直到星期幾對上為止
        while (calendar.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // 如果算出來的時間比「現在」還要早 (代表這禮拜的這堂課已經上完了)，就直接排到下個禮拜！
        if (calendar.timeInMillis <= currentMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 7)
        }

        return calendar.timeInMillis
    }
}
