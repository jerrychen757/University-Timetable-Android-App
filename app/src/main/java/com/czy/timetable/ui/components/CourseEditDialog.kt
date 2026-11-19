package com.czy.timetable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CourseEditDialog(
    course: Course?,
    initialDayIndex: Int,
    initialPeriodId: String,
    allCourses: List<Course> = emptyList(),
    defaultShouldNotify: Boolean? = null,
    defaultNotificationMinutes: Int? = null,
    onDismiss: () -> Unit,
    onSave: (List<Course>) -> Unit,
    onDelete: (Course) -> Unit
) {
    // 找出目前這門課程的所有現有時段 (用同名比對)
    val sameNameCourses = remember(course, allCourses) {
        if (course != null && course.name.isNotBlank()) {
            allCourses.filter { it.name.equals(course.name, ignoreCase = true) }
        } else if (course != null) {
            listOf(course)
        } else {
            emptyList()
        }
    }

    var name by remember { mutableStateOf(course?.name ?: "") }
    var location by remember { mutableStateOf(course?.location ?: "") }
    var teacher by remember { mutableStateOf(course?.teacher ?: "") }
    var midtermDate by remember { mutableStateOf(course?.midtermDate ?: "") }
    var finalExamDate by remember { mutableStateOf(course?.finalExamDate ?: "") }
    var memo by remember { mutableStateOf(course?.memo ?: "") }
    var userEditedNotification by remember { mutableStateOf(false) }
    var shouldNotify by remember {
        mutableStateOf(course?.shouldNotify ?: (defaultShouldNotify ?: PreClassNotification.DEFAULT_ENABLED))
    }
    var notificationMinutes by remember {
        mutableIntStateOf(
            course?.notificationMinutes ?: PreClassNotification.sanitizeMinutes(
                defaultNotificationMinutes ?: PreClassNotification.DEFAULT_MINUTES
            )
        )
    }

    // 新課程等設定讀完後套用統一預設；已經在編輯的課程維持自己的值
    LaunchedEffect(defaultShouldNotify, defaultNotificationMinutes) {
        if (course != null || userEditedNotification) return@LaunchedEffect
        if (defaultShouldNotify == null || defaultNotificationMinutes == null) return@LaunchedEffect
        shouldNotify = defaultShouldNotify
        notificationMinutes = PreClassNotification.sanitizeMinutes(defaultNotificationMinutes)
    }
    var reminderMode by remember { mutableIntStateOf(course?.reminderMode ?: 0) } // 0: 通知, 1: 鬧鐘
    var colorIndex by remember { mutableIntStateOf(course?.colorIndex ?: 0) }

    // 日期格式驗證函數 (允許空白，若有填寫則必須是 YYYY-MM-DD)
    fun isValidDate(dateStr: String): Boolean {
        if (dateStr.isEmpty()) return true
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            sdf.isLenient = false
            sdf.parse(dateStr)
            true
        } catch (_: Exception) {
            false
        }
    }

    val isMidtermError = !isValidDate(midtermDate)
    val isFinalError = !isValidDate(finalExamDate)

    val daysGrouped = remember(sameNameCourses) {
        sameNameCourses.groupBy { it.dayIndex }
    }
    val distinctDays = remember(daysGrouped) {
        daysGrouped.keys.sorted()
    }

    // 是否開啟跨兩天課程
    var isTwoDays by remember {
        mutableStateOf(distinctDays.size >= 2)
    }

    var day1Index by remember {
        mutableIntStateOf(if (distinctDays.isNotEmpty()) distinctDays[0] else initialDayIndex)
    }
    var day1Periods by remember {
        mutableStateOf(
            if (distinctDays.isNotEmpty()) {
                daysGrouped[distinctDays[0]]?.map { it.periodId.lowercase() }?.toSet() ?: emptySet()
            } else if (initialPeriodId.isNotBlank()) {
                setOf(initialPeriodId.lowercase())
            } else {
                setOf("a")
            }
        )
    }

    var day2Index by remember {
        mutableIntStateOf(
            if (distinctDays.size >= 2) distinctDays[1]
            else if (day1Index == 3) 0 else 3
        )
    }
    var day2Periods by remember {
        mutableStateOf(
            if (distinctDays.size >= 2) {
                daysGrouped[distinctDays[1]]?.map { it.periodId.lowercase() }?.toSet() ?: emptySet()
            } else {
                setOf("a")
            }
        )
    }

    val daysList = listOf("週一", "週二", "週三", "週四", "週五", "週六", "週日")
    val periodList = PreClassNotification.periodOrder
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF121212)
    val lightTagColors = listOf(Color(0xFFE0E7FF), Color(0xFFD1FAE5), Color(0xFFFEF3C7), Color(0xFFFFEDD5), Color(0xFFF3E8FF), Color(0xFFFCE7F3))
    val darkTagColors = listOf(Color(0xFF3730A3), Color(0xFF065F46), Color(0xFF78350F), Color(0xFF7C2D12), Color(0xFF4C1D95), Color(0xFF831843))
    val tagColors = if (isDark) darkTagColors else lightTagColors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.92f)
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 標題與關閉按鈕
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val titleText = if (course == null) {
                    "新增課程 (${daysList.getOrElse(initialDayIndex) { "週一" }})"
                } else {
                    "編輯課程 (${course.name})"
                }
                Text(
                    text = titleText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "關閉", tint = MaterialTheme.colorScheme.onBackground)
                }
            }

            // 課程基本資訊
            CustomInputField(label = "課程名稱 *", value = name, onValueChange = { name = it }, placeholder = "例如：微積分")

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CustomInputField(
                    label = "地點", value = location, onValueChange = { location = it }, placeholder = "A103",
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.weight(1f)
                )
                CustomInputField(
                    label = "教師", value = teacher, onValueChange = { teacher = it }, placeholder = "王老師",
                    leadingIcon = { Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.weight(1f)
                )
            }

            // 考程日期
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CustomInputField(
                    label = "期中考日期", value = midtermDate, onValueChange = { midtermDate = it },
                    placeholder = "YYYY-MM-DD", modifier = Modifier.weight(1f),
                    isError = isMidtermError
                )
                CustomInputField(
                    label = "期末考日期", value = finalExamDate, onValueChange = { finalExamDate = it },
                    placeholder = "YYYY-MM-DD", modifier = Modifier.weight(1f),
                    isError = isFinalError
                )
            }

            // --- 上課時間與節次設定區塊 (單天 / 雙天) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ✅ 跨兩天課程按鈕/開關
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "多時段課程",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "開啟後可設定兩天不同的上課時間與節次",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isTwoDays,
                            onCheckedChange = { isTwoDays = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                // --- 第一天設定 ---
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isTwoDays) "第一天上課時間" else "上課時間",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text("選擇星期", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        daysList.forEachIndexed { idx, label ->
                            val isSelected = day1Index == idx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { day1Index = idx }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("選擇節次", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        periodList.forEach { pid ->
                            val isSelected = day1Periods.contains(pid)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        day1Periods = if (isSelected) {
                                            if (day1Periods.size > 1) day1Periods - pid else day1Periods
                                        } else {
                                            day1Periods + pid
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    pid.uppercase(),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // --- 第二天設定 (跨兩天開啟時才顯示) ---
                AnimatedVisibility(visible = isTwoDays) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = "第二天上課時間",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text("選擇星期", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            daysList.forEachIndexed { idx, label ->
                                val isSelected = day2Index == idx
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { day2Index = idx }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("選擇節次", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            periodList.forEach { pid ->
                                val isSelected = day2Periods.contains(pid)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            day2Periods = if (isSelected) {
                                                if (day2Periods.size > 1) day2Periods - pid else day2Periods
                                            } else {
                                                day2Periods + pid
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        pid.uppercase(),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 備忘錄
            CustomInputField(
                label = "備忘錄", value = memo, onValueChange = { memo = it },
                placeholder = "例如：記得帶課本、期中考範圍...", singleLine = false,
                modifier = Modifier.heightIn(min = 90.dp)
            )

            // 提醒面板
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("上課前提醒", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
                    }
                    Switch(
                        checked = shouldNotify,
                        onCheckedChange = {
                            userEditedNotification = true
                            shouldNotify = it
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
                Text(
                    "只影響這門課。想一次改全部，請到設定 → 課程通知與提醒。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                AnimatedVisibility(visible = shouldNotify) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val timeOptions = listOf(5, 10, 15, 30)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            timeOptions.forEach { min ->
                                val isSelected = notificationMinutes == min
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            userEditedNotification = true
                                            notificationMinutes = min
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$min 分鐘", color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val modes = listOf(0 to "一般通知", 1 to "鬧鐘提醒")
                            modes.forEach { (mode, label) ->
                                val isSelected = reminderMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { reminderMode = mode }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 標籤顏色選擇
            Column {
                Text("標籤顏色", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    tagColors.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { colorIndex = index }
                                .border(
                                    width = if (colorIndex == index) 3.dp else 0.dp,
                                    color = if (colorIndex == index) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 儲存與刪除按鈕
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val coursesToSave = mutableListOf<Course>()
                        // 第一天
                        val sortedDay1Periods = day1Periods.sortedBy { periodList.indexOf(it) }
                        sortedDay1Periods.forEachIndexed { index, pId ->
                            val applyNotify = if (index == 0) shouldNotify else false
                            coursesToSave.add(
                                Course(
                                    dayIndex = day1Index, periodId = pId, name = name,
                                    location = location, teacher = teacher, colorIndex = colorIndex,
                                    memo = memo, shouldNotify = applyNotify,
                                    notificationMinutes = notificationMinutes,
                                    midtermDate = midtermDate, finalExamDate = finalExamDate,
                                    reminderMode = reminderMode
                                )
                            )
                        }

                        // 第二天 (如果開啟跨兩天課程)
                        if (isTwoDays && day2Periods.isNotEmpty()) {
                            val sortedDay2Periods = day2Periods.sortedBy { periodList.indexOf(it) }
                            sortedDay2Periods.forEachIndexed { index, pId ->
                                val applyNotify = if (index == 0) shouldNotify else false
                                coursesToSave.add(
                                    Course(
                                        dayIndex = day2Index, periodId = pId, name = name,
                                        location = location, teacher = teacher, colorIndex = colorIndex,
                                        memo = memo, shouldNotify = applyNotify,
                                        notificationMinutes = notificationMinutes,
                                        midtermDate = midtermDate, finalExamDate = finalExamDate,
                                        reminderMode = reminderMode
                                    )
                                )
                            }
                        }
                        onSave(coursesToSave)
                    },
                    enabled = name.isNotBlank() && !isMidtermError && !isFinalError,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("儲存設定", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }

                if (course != null) {
                    TextButton(
                        onClick = { onDelete(course) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("刪除此課程 (所有時段)", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomInputField(
    label: String, value: String, onValueChange: (String) -> Unit, placeholder: String,
    modifier: Modifier = Modifier, singleLine: Boolean = true, leadingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false
) {
    Column(modifier = modifier) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange, placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f)) },
            leadingIcon = leadingIcon, singleLine = singleLine, shape = RoundedCornerShape(12.dp),
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                errorBorderColor = MaterialTheme.colorScheme.error,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
