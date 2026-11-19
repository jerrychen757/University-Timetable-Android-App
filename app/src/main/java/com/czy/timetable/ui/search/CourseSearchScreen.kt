package com.czy.timetable

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseSearchScreen(
    onDismiss: () -> Unit,
    onCoursesSelected: (List<Course>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var allApiCourses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var apiNotConfigured by remember { mutableStateOf(false) }
    
    val selectedGroups = remember { mutableStateMapOf<String, Boolean>() }
    val context = LocalContext.current
    val appContainer = (context.applicationContext as BaseApplication).container
    val apiManager = remember { appContainer.apiManager }
    val courseApiUrl by appContainer.settingsManager.courseApiUrlFlow.collectAsState(initial = "")
    val initialYearSem = remember { apiManager.getCurrentYearAndSemester() }
    var selectedYear by remember { mutableStateOf(initialYearSem.first) }
    var selectedSemester by remember { mutableStateOf(initialYearSem.second) }

    // 篩選狀態
    var selectedFaculty by remember { mutableStateOf("全部") }
    var selectedDepartment by remember { mutableStateOf("全部") }

    var retryCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryCount, selectedYear, selectedSemester, courseApiUrl) {
        if (!CourseApiUrl.isConfigured(courseApiUrl)) {
            apiNotConfigured = true
            allApiCourses = emptyList()
            selectedGroups.clear()
            isLoading = false
            return@LaunchedEffect
        }
        apiNotConfigured = false
        isLoading = true
        try {
            val courses = apiManager.fetchAllCourses(selectedYear, selectedSemester, courseApiUrl)
            allApiCourses = courses
            selectedGroups.clear()
        } catch (e: Exception) {
            Log.e("CourseSearch", "Fetch error", e)
        } finally {
            isLoading = false
        }
    }

    // 提取不重複的學院與系所清單
    val faculties = remember(allApiCourses) {
        listOf("全部") + allApiCourses.map { it.memo.split("|").firstOrNull() ?: "" }
            .filter { it.isNotEmpty() }.distinct().sorted()
    }

    val departments = remember(allApiCourses, selectedFaculty) {
        val list = if (selectedFaculty == "全部") {
            allApiCourses.map { it.memo.split("|").getOrNull(1) ?: "" }
        } else {
            allApiCourses.filter { it.memo.startsWith(selectedFaculty) }
                .map { it.memo.split("|").getOrNull(1) ?: "" }
        }
        listOf("全部") + list.filter { it.isNotEmpty() }.distinct().sorted()
    }

    // 綜合篩選邏輯
    val filteredGroupedCourses = remember(searchQuery, allApiCourses, selectedFaculty, selectedDepartment) {
        allApiCourses.asSequence()
            .filter { course ->
                val (f, d) = course.memo.split("|").let { 
                    (it.getOrNull(0) ?: "") to (it.getOrNull(1) ?: "") 
                }
                val facultyMatch = selectedFaculty == "全部" || f == selectedFaculty
                val deptMatch = selectedDepartment == "全部" || d == selectedDepartment
                facultyMatch && deptMatch
            }
            .filter { it.name.contains(searchQuery, ignoreCase = true) || it.teacher.contains(searchQuery, ignoreCase = true) }
            .groupBy { it.name + " (" + it.teacher + ")" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜尋開課資訊") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    if (selectedGroups.any { it.value }) {
                        TextButton(onClick = {
                            val finalSelection = mutableListOf<Course>()
                            // Use allApiCourses to avoid missing items that are filtered out
                            val allGrouped = allApiCourses.groupBy { it.name + " (" + it.teacher + ")" }
                            selectedGroups.forEach { (key, isSelected) ->
                                if (isSelected) {
                                    finalSelection.addAll(allGrouped[key] ?: emptyList())
                                }
                            }
                            onCoursesSelected(finalSelection)
                            onDismiss()
                        }) {
                            Text("匯入 (${selectedGroups.count { it.value }})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
            // 學年與學期選擇 (保留)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var yearExpanded by remember { mutableStateOf(false) }
                var semExpanded by remember { mutableStateOf(false) }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedCard(onClick = { yearExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("$selectedYear 學年度", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Search, null, modifier = Modifier.size(14.dp))
                        }
                    }
                    DropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                        listOf("114", "113", "112").forEach { year ->
                            DropdownMenuItem(text = { Text(year) }, onClick = { selectedYear = year; yearExpanded = false })
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedCard(onClick = { semExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("第 $selectedSemester 學期", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Search, null, modifier = Modifier.size(14.dp))
                        }
                    }
                    DropdownMenu(expanded = semExpanded, onDismissRequest = { semExpanded = false }) {
                        listOf("1", "2").forEach { sem ->
                            DropdownMenuItem(text = { Text("第 $sem 學期") }, onClick = { selectedSemester = sem; semExpanded = false })
                        }
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("搜尋課程或教師") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // 學院篩選 (LazyRow)
            Text("按學院篩選", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(faculties) { faculty ->
                    FilterChip(
                        selected = selectedFaculty == faculty,
                        onClick = { 
                            selectedFaculty = faculty
                            selectedDepartment = "全部" // 重置系所
                        },
                        label = { Text(faculty) }
                    )
                }
            }

            // 系所篩選 (LazyRow)
            if (departments.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    items(departments) { dept ->
                        SuggestionChip(
                            onClick = { selectedDepartment = dept },
                            label = { Text(dept) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (selectedDepartment == dept) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (apiNotConfigured) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "尚未設定開課 API 網址，因此不會連線抓取課程。請到「設定 → 從開課資訊匯入」或「設定 → 資料管理」填寫網址後再試。",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            } else if (allApiCourses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { retryCount++ }) { Text("重試抓取資料") }
                }
            } else {
                Text(
                    "篩選後共有 ${filteredGroupedCourses.size} 門課程",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredGroupedCourses.keys.toList()) { groupKey ->
                        val groupCourses = filteredGroupedCourses[groupKey] ?: emptyList()
                        val isSelected = selectedGroups[groupKey] ?: false
                        val info = groupCourses.firstOrNull()?.memo?.split("|")
                        val deptLabel = info?.getOrNull(1) ?: ""

                        ListItem(
                            headlineContent = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(groupKey, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    if (deptLabel.isNotEmpty()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = MaterialTheme.shapes.extraSmall,
                                            modifier = Modifier.padding(start = 4.dp)
                                        ) {
                                            Text(deptLabel, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            },
                            supportingContent = {
                                Text(groupCourses.joinToString(", ") { "${it.dayIndex + 1}${it.periodId.uppercase()}" } + 
                                     " @ ${groupCourses.firstOrNull()?.location ?: "未知"}")
                            },
                            leadingContent = {
                                Checkbox(checked = isSelected, onCheckedChange = { selectedGroups[groupKey] = it })
                            },
                            modifier = Modifier.clickable { selectedGroups[groupKey] = !isSelected }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}
