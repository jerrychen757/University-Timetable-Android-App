package com.czy.timetable.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val courses by viewModel.allCourses.collectAsState()
    val holidays by viewModel.holidays.collectAsState()

    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDateInfo by remember { mutableStateOf<DayInfo?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        CalendarHeader(
            currentMonth = currentMonth,
            onPreviousMonth = {
                val cal = currentMonth.clone() as Calendar
                cal.add(Calendar.MONTH, -1)
                currentMonth = cal
            },
            onNextMonth = {
                val cal = currentMonth.clone() as Calendar
                cal.add(Calendar.MONTH, 1)
                currentMonth = cal
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        WeekdaysHeader()
        Spacer(modifier = Modifier.height(8.dp))

        val daysInMonth = getDaysInMonth(currentMonth)
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.weight(1f) // 讓出下方空間
        ) {
            items(daysInMonth) { dayInfo ->
                if (dayInfo == null) {
                    Box(modifier = Modifier.aspectRatio(1f))
                } else {
                    val activeDateInfo = selectedDateInfo ?: run {
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        daysInMonth.find { it?.dateString == todayStr }
                    }
                    val isSelected = activeDateInfo?.dateString == dayInfo.dateString
                    
                    CalendarDayItem(
                        dayInfo = dayInfo,
                        courses = courses,
                        holidays = holidays,
                        isSelected = isSelected,
                        onClick = { selectedDateInfo = dayInfo }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        val activeDateInfo = selectedDateInfo ?: run {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            daysInMonth.find { it?.dateString == todayStr }
        }

        if (activeDateInfo != null) {
            val dateStr = activeDateInfo.dateString
            val isHoliday = holidays.containsKey(dateStr)

            DayDetailsCard(
                dateStr = dateStr,
                isHoliday = isHoliday,
                holidayName = holidays[dateStr],
                events = buildExamEvents(courses, dateStr)
            )
        }
    }
}
