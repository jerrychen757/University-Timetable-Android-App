package com.czy.timetable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun CourseSelectionDialog(
    courses: List<Course>,
    onDismiss: () -> Unit,
    onConfirm: (List<Course>) -> Unit
) {
    // 將課程按名稱和教師分組，因為一個課程可能有複數個節次
    val groupedCourses = remember(courses) {
        courses.groupBy { it.name + " (" + it.teacher + ")" }
    }
    
    val selectedGroups = remember { mutableStateMapOf<String, Boolean>().apply {
        groupedCourses.keys.forEach { put(it, true) }
    } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "請選擇要匯入的課程",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(groupedCourses.keys.toList()) { groupKey ->
                        val groupCourses = groupedCourses[groupKey] ?: emptyList()
                        val isSelected = selectedGroups[groupKey] ?: false
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedGroups[groupKey] = !isSelected }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { selectedGroups[groupKey] = it }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(text = groupKey, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = groupCourses.joinToString(", ") { "${it.dayIndex + 1}${it.periodId}" } + 
                                           " @ ${groupCourses.firstOrNull()?.location ?: "未知"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val finalSelection = mutableListOf<Course>()
                        selectedGroups.forEach { (key, selected) ->
                            if (selected) {
                                finalSelection.addAll(groupedCourses[key] ?: emptyList())
                            }
                        }
                        onConfirm(finalSelection)
                    }) {
                        Text("確認匯入")
                    }
                }
            }
        }
    }
}
