package com.czy.timetable

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    private val notifyChannelId = "class_reminder_channel"
    private val alarmChannelId = "class_alarm_channel" // ✅ 新增專屬的鬧鐘頻道

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. 一般通知頻道
            val notifyChannel = NotificationChannel(notifyChannelId, "上課提醒 (一般通知)", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(notifyChannel)

            // 2. ✅ 鬧鐘頻道 (掛載系統鬧鐘音效)
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val alarmChannel = NotificationChannel(alarmChannelId, "上課提醒 (鬧鐘聲)", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "使用鬧鐘音效來提醒上課"
                setSound(alarmSound, audioAttributes) // 強制設定為鬧鐘聲音
                setBypassDnd(true) // 允許突破勿擾模式！
            }
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    /**
     * Android 14+ (API 34) 針對 USE_FULL_SCREEN_INTENT 權限有新的限制。
     * 除非是鬧鐘或通話類應用，否則可能需要引導用戶手動開啟。
     */
    fun canUseFullScreenIntent(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return manager.canUseFullScreenIntent()
        }
        return true
    }

    /**
     * 引導用戶前往設定頁面開啟「全螢幕意圖」權限
     */
    fun openFullScreenIntentSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    // ✅ 修改發送測試通知的功能，根據 mode 決定用哪種聲音
    fun sendTestNotification(mode: Int) {
        val isAlarm = mode == 1
        val channelId = if (isAlarm) alarmChannelId else notifyChannelId
        val title = if (isAlarm) "鬧鐘測試成功！" else "通知測試成功！"
        val text = if (isAlarm) "這是一則鬧鐘級別的提醒，聲音會比較大喔！" else "這是一般推播通知的聲音。"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // 如果是鬧鐘模式，加上 ALARM 屬性
        if (isAlarm) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1001, builder.build())
    }

    fun sendClassAlarm(courseName: String, location: String, mode: Int) {
        val isAlarm = mode == 1
        val channelId = if (isAlarm) alarmChannelId else notifyChannelId
        val locText = if (location.isNotEmpty()) "上課地點：$location" else "該準備上課囉！"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("⏰ $courseName 即將開始")
            .setContentText(locText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (isAlarm) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(courseName.hashCode(), builder.build())
    }
}
