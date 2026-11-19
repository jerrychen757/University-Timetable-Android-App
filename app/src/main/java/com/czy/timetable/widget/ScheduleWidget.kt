package com.czy.timetable.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.GlanceTheme
import kotlinx.coroutines.flow.first
import java.util.Calendar
import com.czy.timetable.AppDatabase
import com.czy.timetable.MainActivity

// 1. 這是小工具的畫面與邏輯核心
class ScheduleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 從資料庫抓取所有課程
        val db = AppDatabase.getDatabase(context)
        val allCourses = db.courseDao().getAllCourses().first()

        // 計算今天是星期幾
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val todayIndex = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
        val days = listOf("一", "二", "三", "四", "五", "六", "日")

        // 篩選出「今天」的課，並依照節次排序
        val todayCourses = allCourses.filter { it.dayIndex == todayIndex }.sortedBy { it.periodId }

        provideContent {
            GlanceTheme {
                // 💡 這裡用的都是 GlanceModifier 和 Glance 的 UI 元件
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.background) // 使用主題背景色
                        .padding(16.dp)
                        // ✅ 使用 Intent 版本來解決 "No type arguments expected" 的編譯錯誤
                        .clickable(onClick = actionStartActivity(Intent(context, MainActivity::class.java)))
                ) {
                    // 標題列
                    Text(
                        text = "週${days[todayIndex]} 的課表",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = GlanceTheme.colors.primary // 使用主題主要顏色
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(12.dp))

                    if (todayCourses.isEmpty()) {
                        Text(
                            text = "今天沒有課喔！好好休息吧 🎉",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        )
                    } else {
                        // 把今天的課一堂一堂列出來
                        todayCourses.forEach { course ->
                            Row(
                                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 節次
                                Text(
                                    text = "第 ${course.periodId.uppercase()} 節",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = GlanceTheme.colors.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                )
                                Spacer(modifier = GlanceModifier.width(12.dp))
                                // 課名與教室
                                Column {
                                    Text(
                                        text = course.name,
                                        style = TextStyle(
                                            fontWeight = FontWeight.Medium,
                                            color = GlanceTheme.colors.onSurface,
                                            fontSize = 14.sp
                                        ),
                                        maxLines = 1
                                    )
                                    if (course.location.isNotEmpty()) {
                                        Text(
                                            text = course.location,
                                            style = TextStyle(
                                                color = GlanceTheme.colors.primary,
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2. 這是負責接收系統廣播的接收器 (讓 Android 知道有這個 Widget 存在)
class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()
}
