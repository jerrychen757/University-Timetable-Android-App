package com.czy.timetable.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.czy.timetable.CourseDao
import com.czy.timetable.Course
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.URL
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModelProvider

class CalendarViewModel(
    private val dao: CourseDao
) : ViewModel() {

    val allCourses: StateFlow<List<Course>> = dao.getAllCourses().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _holidays = MutableStateFlow<Map<String, String>>(emptyMap())
    val holidays: StateFlow<Map<String, String>> = _holidays

    init {
        fetchTaiwanHolidays()
    }

    private fun fetchTaiwanHolidays() {
        viewModelScope.launch {
            val fetchedHolidays = withContext(Dispatchers.IO) {
                val result = mutableMapOf<String, String>()
                try {
                    val year = Calendar.getInstance().get(Calendar.YEAR)
                    val urlString = "https://cdn.jsdelivr.net/gh/ruyut/TaiwanCalendar/data/$year.json"
                    val connection = URL(urlString).openConnection()
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    
                    val jsonString = connection.getInputStream().bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(jsonString)
                    
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val isHoliday = obj.optBoolean("isHoliday", false)
                        val description = obj.optString("description", "")
                        
                        if (isHoliday && description.isNotBlank()) {
                            val dateStr = obj.optString("date", "")
                            val formattedDate = dateStr.replace(Regex("(\\d{4})(\\d{2})(\\d{2})"), "$1-$2-$3")
                            result[formattedDate] = description
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                result
            }
            _holidays.value = fetchedHolidays
        }
    }

    companion object {
        fun provideFactory(
            dao: CourseDao
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
                    return CalendarViewModel(dao) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
