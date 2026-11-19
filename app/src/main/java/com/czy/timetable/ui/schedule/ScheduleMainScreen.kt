package com.czy.timetable

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduleMainScreen(
    viewModel: ScheduleViewModel,
    settingsManager: SettingsManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val courses by viewModel.allCourses.collectAsState(initial = emptyList())
    val holidays by viewModel.holidays.collectAsState(initial = emptyMap())

    val weekendVisibleState = settingsManager.weekendVisibleFlow.collectAsState(initial = null)
    val weekendVisible = weekendVisibleState.value ?: false
    val fullPeriods by settingsManager.fullPeriodsFlow.collectAsState(initial = false)
    val globalNotify by settingsManager.globalNotifyFlow.collectAsState(initial = true)
    val reminderMode by settingsManager.reminderModeFlow.collectAsState(initial = 0)

    val vacationEnabled by settingsManager.vacationModeEnabledFlow.collectAsState(initial = false)
    val winterStart by settingsManager.winterVacationStartFlow.collectAsState(initial = "")
    val winterEnd by settingsManager.winterVacationEndFlow.collectAsState(initial = "")
    val summerStart by settingsManager.summerVacationStartFlow.collectAsState(initial = "")
    val summerEnd by settingsManager.summerVacationEndFlow.collectAsState(initial = "")

    val actualTodayIndex = viewModel.actualTodayIndex
    val currentWeekDates = viewModel.currentWeekDates
    val visibleDays = if (weekendVisible) listOf("一", "二", "三", "四", "五", "六", "日") else listOf("一", "二", "三", "四", "五")

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { visibleDays.size })

    var isFirstLaunch by remember { mutableStateOf(true) }
    LaunchedEffect(weekendVisibleState.value) {
        if (isFirstLaunch && weekendVisibleState.value != null) {
            val target = if (!weekendVisible && actualTodayIndex > 4) 0 else actualTodayIndex
            if (target < visibleDays.size) {
                pagerState.scrollToPage(target)
                isFirstLaunch = false
            }
        }
    }

    LaunchedEffect(courses, globalNotify, reminderMode, vacationEnabled, winterStart, winterEnd, summerStart, summerEnd) {
        withContext(Dispatchers.IO) { AlarmManagerHelper.syncAllAlarms(context) }
    }

    // --- 彈出視窗狀態 ---
    val showSettingsDialog = remember { mutableStateOf(false) }
    val showEditDialog = remember { mutableStateOf(false) }
    val showCourseSearch = remember { mutableStateOf(false) }
    val showSelectionDialog = remember { mutableStateOf(false) }

    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var selectedPeriodId by remember { mutableStateOf("") }
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var isBatchMode by remember { mutableStateOf(false) }
    var coursesToSelect by remember { mutableStateOf<List<Course>>(emptyList()) }

    Scaffold(
        topBar = {
            Column {
                ScheduleTopBar(
                    onSettingsClick = { showSettingsDialog.value = true },
                    onAddClick = {
                        editingCourse = null
                        selectedDayIndex = pagerState.currentPage
                        selectedPeriodId = "a"
                        isBatchMode = true
                        showEditDialog.value = true
                    }
                )

                // 假期模式橫幅
                if (vacationEnabled) {
                    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val isInVacation = (winterStart.isNotEmpty() && winterEnd.isNotEmpty() && todayStr >= winterStart && todayStr <= winterEnd) ||
                            (summerStart.isNotEmpty() && summerEnd.isNotEmpty() && todayStr >= summerStart && todayStr <= summerEnd)

                    if (isInVacation) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFFF7ED), // 淺橘色背景
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.BeachAccess, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "目前處於假期期間，課程提醒已自動暫停",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF9A3412)
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFFFED7AA))
                    }
                }
            }
        },
        floatingActionButton = {
            val isTodayInVisibleDays = weekendVisible || actualTodayIndex <= 4
            if (isTodayInVisibleDays && pagerState.currentPage != actualTodayIndex) {
                ExtendedFloatingActionButton(
                    onClick = {
                        scope.launch { if (actualTodayIndex < visibleDays.size) pagerState.animateScrollToPage(actualTodayIndex) }
                    },
                    containerColor = Color(0xFF5A52FF), contentColor = Color.White, shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "回今天")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("回今天", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            DayTabs(
                visibleDays = visibleDays,
                currentDayIndex = pagerState.currentPage,
                onDaySelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                hasHolidayDot = { tabIndex -> holidays.containsKey(currentWeekDates.getOrNull(tabIndex)) }
            )

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                ScheduleGrid(
                    courses = courses,
                    currentDayIndex = page,
                    showFullPeriods = fullPeriods,
                    globalNotify = globalNotify,
                    actualTodayIndex = actualTodayIndex,
                    onCourseClick = { periodId, course ->
                        selectedPeriodId = periodId
                        selectedDayIndex = page
                        editingCourse = course
                        isBatchMode = false
                        showEditDialog.value = true
                    }
                )
            }
        }
    }

    // --- 集中管理對話框 ---
    AppDialogs(
        viewModel = viewModel,
        settingsManager = settingsManager,
        showEditDialog = showEditDialog,
        showSettingsDialog = showSettingsDialog,
        showCourseSearch = showCourseSearch,
        showSelectionDialog = showSelectionDialog,
        editingCourse = editingCourse,
        selectedDayIndex = selectedDayIndex,
        selectedPeriodId = selectedPeriodId,
        coursesToSelect = coursesToSelect
    )
}

@Composable
fun AppDialogs(
    viewModel: ScheduleViewModel,
    settingsManager: SettingsManager,
    showEditDialog: MutableState<Boolean>,
    showSettingsDialog: MutableState<Boolean>,
    showCourseSearch: MutableState<Boolean>,
    showSelectionDialog: MutableState<Boolean>,
    editingCourse: Course?,
    selectedDayIndex: Int,
    selectedPeriodId: String,
    coursesToSelect: List<Course>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val courses by viewModel.allCourses.collectAsState(initial = emptyList())
    val preClassDefaults by produceState<Pair<Boolean, Int>?>(initialValue = null, settingsManager) {
        combine(
            settingsManager.preClassNotifyEnabledFlow,
            settingsManager.preClassNotifyMinutesFlow
        ) { enabled, minutes ->
            enabled to PreClassNotification.sanitizeMinutes(minutes)
        }.collect { value = it }
    }

    fun insertWithPreClassDefault(selected: List<Course>) {
        scope.launch {
            val enabled = settingsManager.preClassNotifyEnabledFlow.first()
            val minutes = settingsManager.preClassNotifyMinutesFlow.first()
            viewModel.insertCourses(PreClassNotification.applyToAll(selected, enabled, minutes))
        }
    }

    if (showEditDialog.value) {
        CourseEditDialog(
            course = editingCourse,
            initialDayIndex = selectedDayIndex,
            initialPeriodId = selectedPeriodId,
            allCourses = courses,
            defaultShouldNotify = preClassDefaults?.first,
            defaultNotificationMinutes = preClassDefaults?.second,
            onDismiss = { showEditDialog.value = false },
            onSave = { selectedCourses ->
                viewModel.updateCourseGroup(oldCourse = editingCourse, newCourses = selectedCourses)
                showEditDialog.value = false
            },
            onDelete = { course ->
                viewModel.deleteCourseGroup(course)
                showEditDialog.value = false
            }
        )
    }

    if (showSettingsDialog.value) {
        SettingsBottomSheet(
            settingsManager = settingsManager,
            courses = courses,
            onImport = { importedCourses ->
                // 還原備份要保留檔案裡原本的課前提醒，不改成統一預設。
                viewModel.insertCourses(importedCourses)
                showSettingsDialog.value = false
            },
            onResetAllData = { viewModel.deleteAllCourses() },
            onShowCourseSearch = {
                showCourseSearch.value = true
                showSettingsDialog.value = false
            },
            onApplyPreClassToAll = { enabled, minutes, onFinished ->
                viewModel.applyPreClassNotificationToAll(enabled, minutes, onFinished)
            },
            onDismiss = { showSettingsDialog.value = false }
        )
    }

    if (showCourseSearch.value) {
        CourseSearchScreen(
            onDismiss = { showCourseSearch.value = false },
            onCoursesSelected = { selectedCourses ->
                insertWithPreClassDefault(selectedCourses)
                Toast.makeText(context, "成功匯入 ${selectedCourses.size} 堂課！", Toast.LENGTH_LONG).show()
            }
        )
    }

    if (showSelectionDialog.value) {
        CourseSelectionDialog(
            courses = coursesToSelect,
            onDismiss = { showSelectionDialog.value = false },
            onConfirm = { selectedCourses ->
                insertWithPreClassDefault(selectedCourses)
                showSelectionDialog.value = false
                Toast.makeText(context, "成功匯入 ${selectedCourses.size} 堂課！", Toast.LENGTH_LONG).show()
            }
        )
    }
}
