package com.czy.timetable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun ScheduleTopBar(onSettingsClick: () -> Unit, onAddClick: () -> Unit) {
    Row(
        // ✅ 背景換成聰明底色
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("我的課表", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("點擊課程編輯，或使用新增按鈕", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onSettingsClick, modifier = Modifier.clip(CircleShape).background(Color.Transparent).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape).size(40.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "設定", tint = MaterialTheme.colorScheme.onBackground)
            }
            Button(
                onClick = onAddClick, colors = ButtonDefaults.buttonColors(containerColor = Color(
                    0xFF28C592
                )
                ),
                shape = RoundedCornerShape(20.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("新增", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun DayTabs(visibleDays: List<String>, currentDayIndex: Int, onDaySelected: (Int) -> Unit, hasHolidayDot: (Int) -> Boolean) {
    Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        visibleDays.forEachIndexed { index, day ->
            val isSelected = currentDayIndex == index
            Column(modifier = Modifier.weight(1f).clickable { onDaySelected(index) }.padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("週$day", fontSize = 16.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (hasHolidayDot(index)) Box(modifier = Modifier.padding(start = 4.dp).size(6.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                }
                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent))
            }
        }
    }
}

val lightTagColors = listOf(Color(0xFFE0E7FF), Color(0xFFD1FAE5), Color(0xFFFEF3C7), Color(0xFFFFEDD5), Color(0xFFF3E8FF), Color(0xFFFCE7F3))
val darkTagColors = listOf(Color(0xFF3730A3), Color(0xFF065F46), Color(0xFF78350F), Color(0xFF7C2D12), Color(0xFF4C1D95), Color(0xFF831843))

@Composable
fun ScheduleGrid(courses: List<Course>, currentDayIndex: Int, showFullPeriods: Boolean, globalNotify: Boolean, actualTodayIndex: Int, onCourseClick: (periodId: String, course: Course?) -> Unit) {
    val periods = if (showFullPeriods) AllPeriods else AllPeriods.filter { it.id !in listOf("x", "y", "k", "l", "m") }

    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); currentTime = Calendar.getInstance() } }

    val currentTotalMinutes = currentTime.get(Calendar.HOUR_OF_DAY) * 60 + currentTime.get(Calendar.MINUTE)

    // ✅ 網格背景換成聰明底色
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)) {
        periods.forEach { period ->
            val courseInThisPeriod = courses.find { it.dayIndex == currentDayIndex && it.periodId == period.id }
            val startMin = parseTimeToMinutes(period.start)
            val endMin = parseTimeToMinutes(period.end)
            val isCurrentPeriod = (currentDayIndex == actualTodayIndex) && (currentTotalMinutes in startMin..endMin)

            if (period.isBreak) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = if (isCurrentPeriod) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, thickness = if (isCurrentPeriod) 2.dp else 1.dp)
                    Text(" 午休 (${period.start} ~ ${period.end}) ", color = if (isCurrentPeriod) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = if(isCurrentPeriod) FontWeight.Bold else FontWeight.Medium)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = if (isCurrentPeriod) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, thickness = if (isCurrentPeriod) 2.dp else 1.dp)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.width(50.dp).padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = period.label, fontSize = 26.sp, fontWeight = FontWeight.Black, color = if(isCurrentPeriod) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f))
                        Text(text = period.start, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                        Text(text = period.end, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    if (courseInThisPeriod != null) {
                        val isDark = MaterialTheme.colorScheme.background == Color(0xFF121212)
                        val currentTagColors = if (isDark) darkTagColors else lightTagColors
                        val baseColor = currentTagColors.getOrElse(courseInThisPeriod.colorIndex) { currentTagColors[0] }

                        val bgColor = baseColor
                        val courseTextColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF283593)
                        val notifyIconColor = if (isDark) Color(0xFFF3F4F6).copy(alpha = 0.8f) else Color(0xFF5A67D8)

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 95.dp) // ✅ 2. 固定最小高度：就算沒填地點老師，也不會縮水！
                                .clickable { onCourseClick(period.id, courseInThisPeriod) },
                            shape = RoundedCornerShape(16.dp),
                            color = bgColor,
                            // 深色模式下給一個明顯一點的半透明同色系邊框
                            border = if (isCurrentPeriod) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else if (isDark) BorderStroke(1.dp, baseColor.copy(alpha = 0.5f))
                            else null,
                            shadowElevation = 0.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(courseInThisPeriod.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = courseTextColor, modifier = Modifier.weight(1f))
                                    if (globalNotify && courseInThisPeriod.shouldNotify) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                                            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = notifyIconColor, modifier = Modifier.size(16.dp))
                                            Text("${courseInThisPeriod.notificationMinutes}m", fontSize = 12.sp, color = notifyIconColor, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                // ✅ 避免沒有地點老師時，排版擠在一起，我們讓 Spacer 也有自適應能力
                                Spacer(modifier = Modifier.weight(1f))

                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp), // 上下換行時也保留 8.dp 的間距
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (courseInThisPeriod.location.isNotEmpty()) InfoPill(icon = Icons.Outlined.LocationOn, text = courseInThisPeriod.location, isDark = isDark)
                                    if (courseInThisPeriod.teacher.isNotEmpty()) InfoPill(icon = Icons.Outlined.Person, text = courseInThisPeriod.teacher, isDark = isDark)
                                }
                            }
                        }
                    }
                     else {
                        Box(
                            modifier = Modifier
                                .weight(1f).height(95.dp).clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(width = if (isCurrentPeriod) 2.dp else 1.dp, color = if (isCurrentPeriod) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(16.dp))
                                .clickable { onCourseClick(period.id, null) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+ 點擊新增", color = if (isCurrentPeriod) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun InfoPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, isDark: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            // 深色模式用半透明白底，淺色模式用 80% 白底
            .background(if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.8f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDark) Color(0xFFD1D5DB) else Color(0xFF5C96CB),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            // 深色模式文字用純白，淺色用深藍
            color = if (isDark) Color.White else Color(0xFF283593),
            fontWeight = FontWeight.Medium
        )
    }
}
