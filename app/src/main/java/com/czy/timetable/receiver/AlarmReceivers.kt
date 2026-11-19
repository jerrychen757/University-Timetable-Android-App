package com.czy.timetable.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.czy.timetable.AlarmManagerHelper
import com.czy.timetable.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 1. 開機接收器：手機一開機，立刻命令 AlarmManagerHelper 重新排程所有鬧鐘
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    AlarmManagerHelper.syncAllAlarms(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

// 2. 響鈴接收器：時間一到就發出聲音，並預約下週的鬧鐘
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val courseName = intent.getStringExtra("COURSE_NAME") ?: "課程"
        val location = intent.getStringExtra("COURSE_LOCATION") ?: ""
        val mode = intent.getIntExtra("REMINDER_MODE", 0)

        // 💡 觸發通知 / 鬧鐘聲
        val helper = NotificationHelper(context)
        helper.sendClassAlarm(courseName, location, mode)

        // 💡 響鈴完畢後，立刻讓同步中心重新運算，把這堂課的鬧鐘自動設定到下個禮拜的同一時間！
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmManagerHelper.syncAllAlarms(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}