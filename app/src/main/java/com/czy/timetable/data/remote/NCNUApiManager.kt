package com.czy.timetable

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import java.net.URL
import java.util.Calendar

fun interface CourseHttpFetcher {
    fun fetchText(url: String): String
}

class NCNUApiManager(
    private val httpFetcher: CourseHttpFetcher = CourseHttpFetcher { urlString ->
        val connection = URL(urlString).openConnection()
        connection.connectTimeout = 5000
        connection.readTimeout = 10000
        connection.getInputStream().bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
) {
    
    suspend fun fetchAllCourses(
        year: String = "114",
        semester: String = "2",
        urlTemplate: String = ""
    ): List<Course> = withContext(Dispatchers.IO) {
        val urlString = CourseApiUrl.resolve(urlTemplate, year, semester)
            ?: return@withContext emptyList()

        val maxRetries = 3
        var currentAttempt = 0
        var backoffDelay = 1000L

        while (currentAttempt < maxRetries) {
            try {
                return@withContext withTimeout(15000L) { // 15s timeout per attempt
                    val result = mutableListOf<Course>()
                    Log.d("NCNUApiManager", "Fetching from: $urlString, attempt: ${currentAttempt + 1}")
                    val jsonString = httpFetcher.fetchText(urlString)
                    
                    val jsonArray = try {
                        if (jsonString.trim().startsWith("{")) {
                            val root = org.json.JSONObject(jsonString)
                            val ncnuObj = root.optJSONObject("course_ncnu")
                            if (ncnuObj != null) {
                                ncnuObj.optJSONArray("item") ?: JSONArray()
                            } else {
                                root.optJSONArray("items") ?: root.optJSONArray("data") ?: root.optJSONArray("course_ncnu") ?: JSONArray()
                            }
                        } else {
                            JSONArray(jsonString)
                        }
                    } catch (e: Exception) {
                        Log.e("NCNUApiManager", "JSON parsing error", e)
                        JSONArray()
                    }

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        
                        val name = obj.optString("課名", obj.optString("CourseName", obj.optString("course_cname", obj.optString("name", ""))))
                        val teacher = obj.optString("老師", obj.optString("Teacher", obj.optString("teacher", "")))
                        val timeStr = obj.optString("時間", obj.optString("Time", obj.optString("time", "")))
                        val location = obj.optString("地點", obj.optString("Place", obj.optString("location", "")))
                        val faculty = obj.optString("學院", obj.optString("Faculty", obj.optString("faculty", "")))
                        val department = obj.optString("系所", obj.optString("Department", obj.optString("department", "")))
                        val note = obj.optString("備註", obj.optString("Note", obj.optString("note", "")))
                        
                        var finalName = name
                        if (note.contains("單週") || note.contains("雙週") || note.contains("單周") || note.contains("雙周")) {
                            finalName = "$name ($note)"
                        }
                        
                        if (finalName.isNotEmpty() && timeStr.isNotEmpty()) {
                            val parsed = convertNCNUTimeStringToCourses(finalName, teacher, timeStr, location)
                            parsed.forEach {
                                val updatedMemo = if (faculty.isNotEmpty() || department.isNotEmpty()) {
                                    "$faculty|$department"
                                } else ""
                                result.add(it.copy(memo = updatedMemo))
                            }
                        }
                    }
                    result
                }
            } catch (e: Exception) {
                currentAttempt++
                Log.e("NCNUApiManager", "Error fetching courses (attempt $currentAttempt)", e)
                if (currentAttempt >= maxRetries) {
                    return@withContext emptyList()
                }
                delay(backoffDelay)
                backoffDelay *= 2
            }
        }
        emptyList()
    }

    /**
     * 解析常見的大學課表時間字串 (如: "1ij4i", "2ef,3g", "2ef-3g", "2(EF)~3(G)" 等跨天格式)
     */
    fun convertNCNUTimeStringToCourses(name: String, teacher: String, timeStr: String, location: String): List<Course> {
        val tempCourses = mutableListOf<Course>()
        if (timeStr.isBlank()) return tempCourses

        // 1. 清理括號、冒號等符號
        val cleaned = timeStr.replace(Regex("[()（）:：]"), "").trim()
        if (cleaned.isEmpty()) return tempCourses

        val pattern = Regex("(星期[一二三四五六日天]|週[一二三四五六日天]|[一二三四五六日天]|[1-7])")
        val matches = pattern.findAll(cleaned).toList()

        val validPeriods = setOf('x', 'y', 'a', 'b', 'c', 'd', 'z', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm')

        if (matches.isEmpty()) {
            for (char in cleaned.lowercase()) {
                if (char in validPeriods) {
                    tempCourses.add(
                        Course(
                            dayIndex = 0,
                            periodId = char.toString(),
                            name = name,
                            teacher = teacher,
                            location = location
                        )
                    )
                }
            }
            return tempCourses
        }

        var currentDayIndex: Int? = null

        for (i in matches.indices) {
            val match = matches[i]
            val dayStr = match.value
            val startIndex = match.range.last + 1
            val endIndex = if (i + 1 < matches.size) matches[i + 1].range.first else cleaned.length
            
            val periodSubstr = cleaned.substring(startIndex, endIndex)

            val dayIndex = when {
                dayStr.contains("星期一") || dayStr.contains("週一") || dayStr == "一" || dayStr == "1" -> 0
                dayStr.contains("星期二") || dayStr.contains("週二") || dayStr == "二" || dayStr == "2" -> 1
                dayStr.contains("星期三") || dayStr.contains("週三") || dayStr == "三" || dayStr == "3" -> 2
                dayStr.contains("星期四") || dayStr.contains("週四") || dayStr == "四" || dayStr == "4" -> 3
                dayStr.contains("星期五") || dayStr.contains("週五") || dayStr == "五" || dayStr == "5" -> 4
                dayStr.contains("星期六") || dayStr.contains("週六") || dayStr == "六" || dayStr == "6" -> 5
                dayStr.contains("星期日") || dayStr.contains("星期天") || dayStr.contains("週日") || dayStr == "日" || dayStr == "天" || dayStr == "7" -> 6
                else -> null
            }

            if (dayIndex != null) {
                currentDayIndex = dayIndex
            }

            val targetDay = currentDayIndex ?: 0
            for (char in periodSubstr.lowercase()) {
                if (char in validPeriods) {
                    tempCourses.add(
                        Course(
                            dayIndex = targetDay,
                            periodId = char.toString(),
                            name = name,
                            teacher = teacher,
                            location = location
                        )
                    )
                }
            }
        }

        return tempCourses
    }

    // 取得當前學年與學期
    fun getCurrentYearAndSemester(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // 1-12
        
        // 台灣學年度 (民國年) = 西元年 - 1911
        // 8月(含)到隔年1月(含) 為 第 1 學期
        // 2月(含)到 7月(含) 為 第 2 學期
        
        val academicYear = if (month >= 8) {
            (currentYear - 1911).toString()
        } else {
            (currentYear - 1912).toString()
        }
        
        val semester = if (month !in 2..7) "1" else "2"
        
        return academicYear to semester
    }
}
