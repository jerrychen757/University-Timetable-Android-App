package com.czy.timetable.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czy.timetable.Course
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarHeader(
    currentMonth: Calendar,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
        }

        val monthYearFormat = SimpleDateFormat("yyyy 年 MM 月", Locale.getDefault())
        Text(
            text = monthYearFormat.format(currentMonth.time),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
        }
    }
}

@Composable
fun WeekdaysHeader() {
    val weekdays = listOf("日", "一", "二", "三", "四", "五", "六")
    Row(modifier = Modifier.fillMaxWidth()) {
        weekdays.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CalendarDayItem(
    dayInfo: DayInfo,
    courses: List<Course>,
    holidays: Map<String, String>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isHoliday = holidays.containsKey(dayInfo.dateString)
    val hasMidterm = courses.any { it.midtermDate == dayInfo.dateString }
    val hasFinal = courses.any { it.finalExamDate == dayInfo.dateString }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    isHoliday -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dayInfo.dayOfMonth.toString(),
                fontSize = 16.sp,
                fontWeight = if (isHoliday || hasMidterm || hasFinal) FontWeight.Bold else FontWeight.Normal,
                color = if (isHoliday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 4.dp)) {
                if (hasMidterm) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFFA500)))
                }
                if (hasFinal) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Red))
                }
            }
        }
    }
}

@Composable
fun DayDetailsCard(
    dateStr: String,
    isHoliday: Boolean,
    holidayName: String?,
    events: List<ExamCalendarEvent>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = dateStr,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isHoliday) {
                Text("🎉 國定假日: ${holidayName ?: ""}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (events.isEmpty() && !isHoliday) {
                Text("這天沒有特別的考程或假日。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                events.forEach { event ->
                    val indicatorColor = if (event.examType == EXAM_TYPE_MIDTERM) Color(0xFFFFA500) else Color.Red
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(indicatorColor))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = event.displayText(),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
