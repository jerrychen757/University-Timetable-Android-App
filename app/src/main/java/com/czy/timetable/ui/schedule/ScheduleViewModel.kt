package com.czy.timetable


import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ScheduleViewModel(
    private val dao: CourseDao
) : ViewModel() {

    // --- 日期與時間計算相關 ---
    val actualTodayIndex: Int
        get() {
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            return if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
        }

    val currentWeekDates: List<String>
        get() {
            val dates = mutableListOf<String>()
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val diff = if (dayOfWeek == Calendar.SUNDAY) -6 else 2 - dayOfWeek
            calendar.add(Calendar.DAY_OF_MONTH, diff)
            repeat(7) {
                dates.add(dateFormat.format(calendar.time))
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            return dates
        }

    // 1. 課表狀態
    val allCourses: StateFlow<List<Course>> = dao.getAllCourses().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 2. 國定假日狀態 (Map 的 Key 是日期 "YYYY-MM-DD"，Value 是節日名稱 "國慶日")
    private val _holidays = MutableStateFlow<Map<String, String>>(emptyMap())
    val holidays: StateFlow<Map<String, String>> = _holidays.asStateFlow()

    init {
        // ViewModel 一建立，就立刻在背景去抓當年度的假日
        fetchTaiwanHolidays()
    }

    // 自動抓取台灣國定假日 API
    private fun fetchTaiwanHolidays() {
        viewModelScope.launch(Dispatchers.IO) { // 切換到 IO 背景執行緒，不卡畫面
            try {
                // 取得今年年份
                val year = Calendar.getInstance().get(Calendar.YEAR)
                // 使用開源的台灣政府行政機關辦公日曆表 API
                val urlString = "https://cdn.jsdelivr.net/gh/ruyut/TaiwanCalendar/data/$year.json"

                // 發送網路請求並讀取 JSON 字串
                val jsonString = URL(urlString).readText()
                val jsonArray = JSONArray(jsonString)

                val holidayMap = mutableMapOf<String, String>()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val isDayOff = item.getBoolean("isHoliday")
                    val description = item.getString("description")
                    val date = item.getString("date") // 格式為 "20240101"

                    // 如果是放假日，且有節日名稱 (過濾掉普通的星期六、星期日)
                    if (isDayOff && description.isNotEmpty() && description != "星期六" && description != "星期日") {
                        // 將 "20240101" 轉換成 "2024-01-01" 以方便後續與 UI 日期比對
                        val formattedDate = "${date.substring(0, 4)}-${date.substring(4, 6)}-${date.substring(6, 8)}"
                        holidayMap[formattedDate] = description
                    }
                }

                // 將整理好的假日資料推送到前端 UI
                _holidays.value = holidayMap

            } catch (e: Exception) {
                e.printStackTrace()
                // 如果沒有網路或抓取失敗，就先保持空 Map，不影響主要課表功能
            }
        }
    }


    fun insertCourses(courses: List<Course>) {
        viewModelScope.launch { dao.insertCourses(courses) }
    }

    /**
     * 把統一上課前提醒寫進每一筆現有課程。
     * 只更新有變動的列，並保留 id。課表 Flow 更新後，畫面既有的鬧鐘同步會重排提醒。
     * [onApplied] 在主執行緒收到更新的時段數；失敗時為 -1。
     */
    fun applyPreClassNotificationToAll(
        enabled: Boolean,
        minutes: Int,
        onApplied: (Int) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = try {
                val current = dao.getAllCourses().first()
                val updated = PreClassNotification.applyToAll(current, enabled, minutes)
                val changed = updated.filterIndexed { index, course ->
                    course.id != null && course != current[index]
                }
                if (changed.isNotEmpty()) dao.updateCourses(changed)
                current.size
            } catch (e: Exception) {
                e.printStackTrace()
                -1
            }
            withContext(Dispatchers.Main) { onApplied(count) }
        }
    }

    fun updateCourseGroup(oldCourse: Course?, newCourses: List<Course>) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldName = oldCourse?.name ?: ""
            if (oldName.isNotBlank()) {
                dao.deleteCoursesByName(oldName)
            } else if (oldCourse?.id != null) {
                dao.deleteCourseById(oldCourse.id)
            }
            dao.insertCourses(newCourses)
        }
    }

    fun deleteCourseGroup(course: Course) {
        viewModelScope.launch(Dispatchers.IO) {
            if (course.name.isNotBlank()) {
                dao.deleteCoursesByName(course.name)
            } else if (course.id != null) {
                dao.deleteCourseById(course.id)
            }
        }
    }

    fun deleteAllCourses() {
        viewModelScope.launch(Dispatchers.IO) {
            // 呼叫 dao 或是 repository 的 deleteAllCourses()
            dao.deleteAllCourses()
        }
    }

    companion object {
        fun provideFactory(
            dao: CourseDao
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
                    return ScheduleViewModel(dao) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
