package com.czy.timetable

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withTranslation

object DataManager {
    fun exportToJson(context: Context, uri: Uri, courses: List<Course>) {
        try {
            val jsonArray = JSONArray()
            courses.forEach { c ->
                val obj = JSONObject()
                obj.put("dayIndex", c.dayIndex)
                obj.put("periodId", c.periodId)
                obj.put("name", c.name)
                obj.put("location", c.location)
                obj.put("teacher", c.teacher)
                obj.put("colorIndex", c.colorIndex)
                obj.put("memo", c.memo)
                obj.put("shouldNotify", c.shouldNotify)
                obj.put("notificationMinutes", c.notificationMinutes)
                obj.put("midtermDate", c.midtermDate)
                obj.put("finalExamDate", c.finalExamDate)
                jsonArray.put(obj)
            }
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(jsonArray.toString(4).toByteArray())
            }
            Toast.makeText(context, "備份成功！", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, "備份失敗", Toast.LENGTH_SHORT).show()
        }
    }

    fun importFromJson(context: Context, uri: Uri): List<Course> {
        val importedCourses = mutableListOf<Course>()
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()
                .use { it?.readText() }
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                importedCourses.add(
                    Course(
                        dayIndex = obj.getInt("dayIndex"),
                        periodId = obj.getString("periodId"),
                        name = obj.getString("name"),
                        location = obj.optString("location", ""),
                        teacher = obj.optString("teacher", ""),
                        colorIndex = obj.optInt("colorIndex", 0),
                        memo = obj.optString("memo", ""),
                        shouldNotify = obj.optBoolean("shouldNotify", false),
                        notificationMinutes = obj.optInt("notificationMinutes", 15),
                        midtermDate = obj.optString("midtermDate", ""),
                        finalExamDate = obj.optString("finalExamDate", "")
                    )
                )
            }
            Toast.makeText(context, "成功載入 ${importedCourses.size} 堂課！", Toast.LENGTH_SHORT)
                .show()
        } catch (_: Exception) {
            Toast.makeText(context, "檔案格式錯誤或讀取失敗", Toast.LENGTH_SHORT).show()
        }
        return importedCourses
    }

    fun exportToPdf(
        context: Context,
        uri: Uri,
        courses: List<Course>,
        weekendVisible: Boolean,
        fullPeriodsVisible: Boolean,
        schoolName: String,
        studentName: String,
        studentId: String
    ) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val textPaint = TextPaint().apply {
                color = android.graphics.Color.BLACK; textSize = 9f; typeface = Typeface.DEFAULT
            }
            val headerPaint = Paint().apply {
                color = android.graphics.Color.BLACK; textSize = 16f; typeface =
                Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
            }
            val infoPaint = Paint().apply {
                color = android.graphics.Color.BLACK; textSize = 12f; typeface = Typeface.DEFAULT
            }
            val linePaint = Paint().apply {
                color = android.graphics.Color.BLACK; strokeWidth = 1f; style = Paint.Style.STROKE
            }
            // ✅ 新增午休專用的畫筆 (粗體、稍微大一點的灰色字)
            val lunchPaint = TextPaint().apply {
                color = android.graphics.Color.DKGRAY; textSize = 11f; typeface =
                Typeface.DEFAULT_BOLD
            }

            canvas.drawText("$schoolName 個人課表", 595f / 2, 50f, headerPaint)
            canvas.drawText("姓名: $studentName", 40f, 85f, infoPaint)
            canvas.drawText("學號: $studentId", 40f, 105f, infoPaint)

            val startX = 30f
            var currentY = 125f
            val colWidthPeriod = 30f
            val colWidthTime = 40f

            val daysOfWeek = if (weekendVisible) {
                listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
            } else {
                listOf("星期一", "星期二", "星期三", "星期四", "星期五")
            }
            val dayCount = daysOfWeek.size
            val colWidthDay = (595f - 60f - colWidthPeriod - colWidthTime) / dayCount.toFloat()

            val allPeriods = listOf(
                "x" to "06:10\n07:00", "y" to "07:10\n08:00", "a" to "08:10\n09:00",
                "b" to "09:10\n10:00", "c" to "10:10\n11:00", "d" to "11:10\n12:00",
                "z" to "12:10\n13:00", "e" to "13:30\n14:20", "f" to "14:30\n15:20",
                "g" to "15:30\n16:20", "h" to "16:30\n17:20", "i" to "17:30\n18:20",
                "j" to "18:30\n19:20", "k" to "19:30\n20:20", "l" to "20:30\n21:20",
                "m" to "21:30\n22:20"
            )

            val periodsList = if (fullPeriodsVisible) {
                allPeriods
            } else {
                allPeriods.filter { (id, _) -> id in listOf("a", "b", "c", "d", "z", "e", "f", "g", "h", "i", "j") }
            }

            fun drawCellText(text: String, x: Float, y: Float, width: Float): Float {
                if (text.isEmpty()) return 0f
                val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width.toInt())
                    .setAlignment(Layout.Alignment.ALIGN_CENTER).setLineSpacing(0f, 1f)
                    .setIncludePad(false).build()
                
                canvas.withTranslation(x, y) {
                    staticLayout.draw(this)
                }
                return staticLayout.height.toFloat()
            }

            var currentX = startX
            canvas.drawLine(startX, currentY, 595f - 30f, currentY, linePaint)

            drawCellText("節", currentX, currentY + 5f, colWidthPeriod)
            currentX += colWidthPeriod
            drawCellText("時間", currentX, currentY + 5f, colWidthTime)
            currentX += colWidthTime

            for (day in daysOfWeek) {
                drawCellText(day, currentX, currentY + 5f, colWidthDay)
                currentX += colWidthDay
            }

            currentY += 25f
            canvas.drawLine(startX, currentY, 595f - 30f, currentY, linePaint)

            // ✅ 紀錄午休的 Y 座標，等等畫垂直線時要用來「斷開」線條
            var lunchStartY = 0f
            var lunchEndY = 0f

            for ((periodId, timeStr) in periodsList) {
                // ✅ 智慧判斷：這節是 Z(午休)，而且「所有天數的 Z 節都沒有排課」才觸發合併
                val shouldMergeLunch =
                    (periodId == "z") && courses.none { it.periodId.lowercase() == "z" }

                var maxRowHeight = 25f
                val rowTexts = Array(dayCount) { "" }

                if (!shouldMergeLunch) {
                    for (dayIdx in 0 until dayCount) {
                        val course =
                            courses.find { it.dayIndex == dayIdx && it.periodId.lowercase() == periodId }
                        if (course != null) {
                            val text =
                                "${course.name}\n${course.teacher}\n${course.location}".trim()
                            rowTexts[dayIdx] = text
                            val tempLayout = StaticLayout.Builder.obtain(
                                text,
                                0,
                                text.length,
                                textPaint,
                                colWidthDay.toInt() - 4
                            ).build()
                            if (tempLayout.height > maxRowHeight) maxRowHeight =
                                tempLayout.height.toFloat()
                        }
                    }
                }

                val rowHeight = maxRowHeight + 10f

                // ✅ 標記午休的範圍
                if (shouldMergeLunch) {
                    lunchStartY = currentY
                    lunchEndY = currentY + rowHeight
                }

                currentX = startX
                drawCellText(periodId.uppercase(), currentX, currentY + 5f, colWidthPeriod)
                currentX += colWidthPeriod

                drawCellText(timeStr, currentX, currentY + 5f, colWidthTime)
                currentX += colWidthTime

                if (shouldMergeLunch) {
                    // ✅ 畫合併的「午休」文字 (置中於整個課表區域)
                    val mergedWidth = colWidthDay * dayCount
                    val text = "午    休"
                    val staticLayout = StaticLayout.Builder.obtain(
                        text,
                        0,
                        text.length,
                        lunchPaint,
                        mergedWidth.toInt()
                    )
                        .setAlignment(Layout.Alignment.ALIGN_CENTER).build()

                    val yOffset = currentY + (rowHeight - staticLayout.height) / 2f
                    canvas.withTranslation(currentX, yOffset) {
                        staticLayout.draw(this)
                    }
                } else {
                    for (dayIdx in 0 until dayCount) {
                        drawCellText(
                            rowTexts[dayIdx],
                            currentX + 2f,
                            currentY + 5f,
                            colWidthDay - 4f
                        )
                        currentX += colWidthDay
                    }
                }

                currentY += rowHeight
                canvas.drawLine(startX, currentY, 595f - 30f, currentY, linePaint)
            }

            // ================= 畫垂直分隔線 (加入斷線魔法) =================
            currentX = startX
            canvas.drawLine(currentX, 125f, currentX, currentY, linePaint) // 最左邊框
            currentX += colWidthPeriod
            canvas.drawLine(currentX, 125f, currentX, currentY, linePaint) // 節次右邊
            currentX += colWidthTime
            canvas.drawLine(currentX, 125f, currentX, currentY, linePaint) // 時間右邊

            for (i in 0 until dayCount) {
                currentX += colWidthDay
                if (i == dayCount - 1) {
                    // ✅ 最右邊框：一筆畫到底
                    canvas.drawLine(currentX, 125f, currentX, currentY, linePaint)
                } else {

                    // ✅ 內部線遇到午休要「斷開」，才能製造出合併的視覺效果！
                    if (lunchStartY > 0f) {
                        canvas.drawLine(currentX, 125f, currentX, lunchStartY, linePaint) // 上半部
                        canvas.drawLine(currentX, lunchEndY, currentX, currentY, linePaint) // 下半部
                    } else {
                        canvas.drawLine(currentX, 125f, currentX, currentY, linePaint)
                    }
                }
            }

            val sdf =
                java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault())
            canvas.drawText(
                "列印日期/時間: ${sdf.format(java.util.Date())}",
                startX,
                currentY + 20f,
                textPaint
            )

            document.finishPage(page)
            context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
            document.close()
            Toast.makeText(context, "完美課表 PDF 匯出成功！", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "PDF 匯出失敗", Toast.LENGTH_SHORT).show()
        }
    }
}
