package com.czy.timetable

import android.Manifest
import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    settingsManager: SettingsManager,
    courses: List<Course>,
    onImport: (List<Course>) -> Unit,
    onResetAllData: () -> Unit,
    onShowCourseSearch: () -> Unit,
    onApplyPreClassToAll: (enabled: Boolean, minutes: Int, onFinished: (Int) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    val activity = context as? Activity
    val notificationHelper = remember { NotificationHelper(context) }
    val showVersionDialog = remember { mutableStateOf(false) }

    val weekendVisible by settingsManager.weekendVisibleFlow.collectAsState(initial = false)
    val globalNotify by settingsManager.globalNotifyFlow.collectAsState(initial = true)
    val fullPeriods by settingsManager.fullPeriodsFlow.collectAsState(initial = false)
    val themeMode by settingsManager.themeModeFlow.collectAsState(initial = 0)
    val reminderMode by settingsManager.reminderModeFlow.collectAsState(initial = 0)
    val preClassEnabled by settingsManager.preClassNotifyEnabledFlow.collectAsState(initial = PreClassNotification.DEFAULT_ENABLED)
    val preClassMinutes by settingsManager.preClassNotifyMinutesFlow.collectAsState(initial = PreClassNotification.DEFAULT_MINUTES)
    var pendingPreClassApply by remember { mutableStateOf<Pair<Boolean, Int>?>(null) }
    var isApplyingPreClass by remember { mutableStateOf(false) }

    val vacationEnabled by settingsManager.vacationModeEnabledFlow.collectAsState(initial = false)
    val winterStart by settingsManager.winterVacationStartFlow.collectAsState(initial = "")
    val winterEnd by settingsManager.winterVacationEndFlow.collectAsState(initial = "")
    val summerStart by settingsManager.summerVacationStartFlow.collectAsState(initial = "")
    val summerEnd by settingsManager.summerVacationEndFlow.collectAsState(initial = "")

    val schoolName by settingsManager.schoolNameFlow.collectAsState(initial = "")
    val studentName by settingsManager.studentNameFlow.collectAsState(initial = "")
    val studentId by settingsManager.studentIdFlow.collectAsState(initial = "")
    val courseApiUrl by settingsManager.courseApiUrlFlow.collectAsState(initial = "")

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) notificationHelper.sendTestNotification(reminderMode) else Toast.makeText(context, "請允許通知權限喔！", Toast.LENGTH_SHORT).show()
    }

    // 1. 備份 JSON 選擇器
    val exportJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { DataManager.exportToJson(context, it, courses) }
    }

    // 2. 還原 JSON 選擇器
    val importJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val imported = DataManager.importFromJson(context, it)
            if (imported.isNotEmpty()) {
                onImport(imported)
                onDismiss()
            }
        }
    }

    // 3. 匯出 PDF 選擇器
    val exportPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let {
            DataManager.exportToPdf(context, it, courses, weekendVisible, fullPeriods, schoolName, studentName, studentId)
        }
    }

    val reminderText = if (reminderMode == 1) "鬧鐘鈴聲" else "一般通知"
    val themeText = when(themeMode) { 1 -> "淺色模式"; 2 -> "深色模式"; else -> "跟隨系統" }

    var currentMenu by remember { mutableStateOf("main") }

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(0.9f).padding(horizontal = 24.dp, vertical = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showPrivacyDialog) {
                AlertDialog(
                    onDismissRequest = { showPrivacyDialog = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    title = {
                        Text("隱私權條款", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                "1. 資料儲存\n本應用程式（排課小幫手）之課表資料皆儲存於您的設備本機端（SQLite），我們不會將您的個人排課資料上傳至任何外部伺服器。\n\n" +
                                        "2. 檔案存取權限\n當您使用「匯出」或「還原」功能時，應用程式需要存取您指定的儲存空間，以進行檔案讀寫，確保資料安全備份。\n\n" +
                                        "3. 廣告與第三方服務\n本應用程式使用 Google AdMob 提供廣告服務。Google 可能會使用 Cookie 或行動廣告識別碼來投遞個人化廣告。您可以隨時在手機系統設定中重置您的廣告識別碼。\n\n" +
                                        "4. 聯絡我們\n若對本隱私權條款有任何疑問，歡迎隨時透過支援管道與開發團隊聯繫。",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPrivacyDialog = false }) {
                            Text("我已了解", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }

            pendingPreClassApply?.let { (enabled, minutes) ->
                AlertDialog(
                    onDismissRequest = { if (!isApplyingPreClass) pendingPreClassApply = null },
                    containerColor = MaterialTheme.colorScheme.surface,
                    title = {
                        Text("套用到所有課程？", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    },
                    text = {
                        Text(
                            if (enabled) {
                                "所有課程的上課前提醒會改成「開啟，提前 $minutes 分鐘」。各別課程原本的設定會被取代；同一門課若跨多節，只會在最早的那一節提醒。套用後仍可打開單一課程再調整。"
                            } else {
                                "所有課程的上課前提醒都會關閉，提前時間會改成 $minutes 分鐘。各別課程原本的設定會被取代。套用後仍可打開單一課程再調整。"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { pendingPreClassApply = null },
                            enabled = !isApplyingPreClass
                        ) {
                            Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                isApplyingPreClass = true
                                onApplyPreClassToAll(enabled, minutes) { count ->
                                    isApplyingPreClass = false
                                    pendingPreClassApply = null
                                    val message = when {
                                        count < 0 -> "套用失敗，請稍後再試"
                                        count == 0 -> "目前沒有課程可套用"
                                        enabled -> "已套用到 $count 個時段：上課前 $minutes 分鐘提醒"
                                        else -> "已關閉全部 $count 個時段的上課前提醒"
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = !isApplyingPreClass
                        ) {
                            Text(if (isApplyingPreClass) "套用中…" else "套用", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    icon = { Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(36.dp)) },
                    title = {
                        Text("確定要重置所有資料嗎？", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    },
                    text = {
                        Text(
                            "這將會永久刪除您所有的課表、地點與教師資訊，且此動作無法復原！\n\n強烈建議您先使用「資料管理」功能備份目前的課表。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onResetAllData()
                                showResetDialog = false
                                onDismiss()
                                Toast.makeText(context, "資料已全部清除", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("確認刪除", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            when (currentMenu) {
                "main" -> {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("設定", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                            Icon(Icons.Default.Close, contentDescription = "關閉", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }

                    // ✅ 整合課程通知提醒、提醒方式、測試通知至「課程通知與提醒」子選單
                    SettingClickableCard(
                        icon = Icons.Outlined.Notifications,
                        title = "課程通知與提醒",
                        subtitle = if (globalNotify) "已開啟 ($reminderText)" else "全域提醒已關閉"
                    ) {
                        currentMenu = "notification"
                    }

                    SettingClickableCard(
                        icon = Icons.Outlined.DateRange,
                        title = "課表顯示與假期",
                        subtitle = "週末模式、完整時段與寒暑假設定",
                        iconTint = Color(0xFFF59E0B)
                    ) {
                        currentMenu = "schedule_display"
                    }

                    SettingClickableCard(icon = Icons.Outlined.BrightnessMedium, title = "外觀主題", subtitle = themeText) {
                        currentMenu = "theme"
                    }

                    SettingClickableCard(icon = Icons.Outlined.CloudUpload, title = "資料管理", subtitle = "備份、還原與匯出 PDF") {
                        currentMenu = "data"
                    }

                    SettingClickableCard(
                        icon = Icons.Outlined.Search,
                        title = "從開課資訊匯入",
                        subtitle = if (CourseApiUrl.isConfigured(courseApiUrl)) {
                            "搜尋學校開課資訊並勾選課程"
                        } else {
                            "請先填寫開課 API 網址"
                        },
                        iconTint = Color(0xFF10B981)
                    ) {
                        currentMenu = "course_import"
                    }

                    SettingClickableCard(icon = Icons.Outlined.Security, title = "隱私權條款", iconTint = Color(0xFF3B82F6)) {
                        showPrivacyDialog = true
                    }
                    SettingClickableCard(icon = Icons.Outlined.FavoriteBorder, title = "贊助開發者", subtitle = "觀看廣告支持我們", iconTint = Color(0xFFEF4444), titleColor = Color(0xFFEF4444)) {
                        activity?.let { AdManager.showAd(it) }
                    }
                    SettingClickableCard(icon = Icons.Outlined.Delete, title = "重置所有資料", iconTint = Color(0xFFEF4444), titleColor = Color(0xFFEF4444)) {
                        showResetDialog = true
                    }
                    SettingClickableCard(
                        icon = Icons.Outlined.Info,
                        title = "版本資訊",
                        subtitle = "v1.0.5 (點擊查看更新日誌)",
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        showVersionDialog.value = true
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                "notification" -> {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { currentMenu = "main" }, modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onBackground)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("課程通知與提醒", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }

                    SettingToggleCard(
                        icon = Icons.Outlined.Notifications,
                        title = "課程提醒通知",
                        subtitle = "全域開啟或關閉所有上課提醒",
                        checked = globalNotify
                    ) {
                        scope.launch { settingsManager.toggleGlobalNotify(it) }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("上課前提醒", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))

                    SettingToggleCard(
                        icon = Icons.Outlined.Alarm,
                        title = "統一上課前提醒",
                        subtitle = if (preClassEnabled) {
                            "新課程預設上課前 $preClassMinutes 分鐘通知"
                        } else {
                            "新課程預設不通知"
                        },
                        checked = preClassEnabled
                    ) { enabled ->
                        scope.launch { settingsManager.setPreClassNotifyEnabled(enabled) }
                    }

                    Text("提前時間", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                    PreClassMinutePicker(selectedMinutes = preClassMinutes) { minutes ->
                        scope.launch { settingsManager.setPreClassNotifyMinutes(minutes) }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val enabled = settingsManager.preClassNotifyEnabledFlow.first()
                                val minutes = PreClassNotification.sanitizeMinutes(
                                    settingsManager.preClassNotifyMinutesFlow.first()
                                )
                                pendingPreClassApply = enabled to minutes
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("套用到所有課程", fontWeight = FontWeight.Bold)
                    }

                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).padding(12.dp)) {
                        Text(
                            "這裡的開關與時間是新課程，以及從開課資訊匯入的課程的預設。已經在課表上的課程要按「套用到所有課程」才會一起改；在那之前，各別設定會保留。若上方的「課程提醒通知」是關閉的，就算已套用也不會跳出通知。",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("提醒方式", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))

                    ThemeOption(title = "一般通知 (預設)", selected = reminderMode == 0) {
                        scope.launch { settingsManager.setReminderMode(0) }
                    }
                    ThemeOption(title = "鬧鐘鈴聲 (較響亮)", selected = reminderMode == 1) {
                        scope.launch {
                            settingsManager.setReminderMode(1)
                            if (!notificationHelper.canUseFullScreenIntent()) {
                                Toast.makeText(context, "請開啟「全螢幕通知」權限以確保鬧鐘正常運作", Toast.LENGTH_LONG).show()
                                notificationHelper.openFullScreenIntentSettings()
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).padding(12.dp)) {
                        Text("提示：若選擇「鬧鐘鈴聲」，提醒時會播放系統預設鬧鐘音效，適合怕錯過課程的同學。", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, lineHeight = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    SettingClickableCard(
                        icon = Icons.AutoMirrored.Outlined.Send,
                        title = "測試通知",
                        subtitle = "立即發送測試通知至您的手機"
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            notificationHelper.sendTestNotification(reminderMode)
                        }
                    }
                }

                "theme" -> {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { currentMenu = "main" }, modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onBackground)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("外觀主題", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }

                    ThemeOption(title = "跟隨系統", selected = themeMode == 0) { scope.launch { settingsManager.setThemeMode(0) } }
                    ThemeOption(title = "淺色模式", selected = themeMode == 1) { scope.launch { settingsManager.setThemeMode(1) } }
                    ThemeOption(title = "深色模式", selected = themeMode == 2) { scope.launch { settingsManager.setThemeMode(2) } }
                }

                "data" -> {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { currentMenu = "main" }, modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onBackground)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("資料管理", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }

                    SettingClickableCard(icon = Icons.Outlined.Backup, title = "備份課表資料", subtitle = "將課表匯出為 JSON 檔案") {
                        exportJsonLauncher.launch("my_schedule_backup.json")
                    }
                    SettingClickableCard(icon = Icons.Outlined.Restore, title = "還原課表資料", subtitle = "從備份的 JSON 檔案載入課表") {
                        importJsonLauncher.launch(arrayOf("application/json"))
                    }
                    SettingClickableCard(icon = Icons.Outlined.PictureAsPdf, title = "匯出 PDF 課表", subtitle = "產生可列印的精美課表 PDF") {
                        exportPdfLauncher.launch("我的課表.pdf")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("個人化設定 (用於 PDF 抬頭)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
                    
                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { scope.launch { settingsManager.savePdfInfo(it, studentName, studentId) } },
                        label = { Text("學校名稱") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { scope.launch { settingsManager.savePdfInfo(schoolName, it, studentId) } },
                        label = { Text("姓名") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = studentId,
                        onValueChange = { scope.launch { settingsManager.savePdfInfo(schoolName, studentName, it) } },
                        label = { Text("學號") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("開課資訊匯入", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
                    CourseApiUrlField(courseApiUrl) { scope.launch { settingsManager.saveCourseApiUrl(it) } }
                }

                "course_import" -> {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { currentMenu = "main" }, modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onBackground)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("從開課資訊匯入", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }

                    CourseApiUrlField(courseApiUrl) { scope.launch { settingsManager.saveCourseApiUrl(it) } }

                    Button(
                        onClick = { onShowCourseSearch() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("搜尋並匯入課程", fontWeight = FontWeight.Bold)
                    }
                }

                "schedule_display" -> {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { currentMenu = "main" }, modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onBackground)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("課表顯示與假期", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }

                    SettingToggleCard(icon = Icons.Outlined.DateRange, title = "週末模式", subtitle = "顯示週六與週日課表", checked = weekendVisible) { scope.launch { settingsManager.toggleWeekendVisible(it) } }
                    SettingToggleCard(icon = Icons.Outlined.Schedule, title = "完整時段", subtitle = "顯示早自習與晚上課程", checked = fullPeriods) { scope.launch { settingsManager.toggleFullPeriods(it) } }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("假期模式設定", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))

                    SettingToggleCard(
                        icon = Icons.Outlined.BeachAccess,
                        title = "啟用假期模式",
                        subtitle = "假期期間自動暫停所有課程提醒",
                        checked = vacationEnabled
                    ) {
                        scope.launch { settingsManager.setVacationModeEnabled(it) }
                    }

                    if (vacationEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("寒假日期設定", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = winterStart,
                                onValueChange = { scope.launch { settingsManager.saveVacationDates(it, winterEnd, summerStart, summerEnd) } },
                                label = { Text("開始日期") },
                                placeholder = { Text("YYYY-MM-DD") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = winterEnd,
                                onValueChange = { scope.launch { settingsManager.saveVacationDates(winterStart, it, summerStart, summerEnd) } },
                                label = { Text("結束日期") },
                                placeholder = { Text("YYYY-MM-DD") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("暑假日期設定", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = summerStart,
                                onValueChange = { scope.launch { settingsManager.saveVacationDates(winterStart, winterEnd, it, summerEnd) } },
                                label = { Text("開始日期") },
                                placeholder = { Text("YYYY-MM-DD") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = summerEnd,
                                onValueChange = { scope.launch { settingsManager.saveVacationDates(winterStart, winterEnd, summerStart, it) } },
                                label = { Text("結束日期") },
                                placeholder = { Text("YYYY-MM-DD") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        Button(
                            onClick = {
                                val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                scope.launch {
                                    settingsManager.saveVacationDates(
                                        "$year-01-15", "$year-02-20",
                                        "$year-07-01", "$year-08-31"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("智慧預填今年建議日期")
                        }

                        Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).padding(16.dp)) {
                            Text("提示：請使用 YYYY-MM-DD 格式，系統將在指定範圍內自動過濾掉課程提醒。", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }

    if (showVersionDialog.value) {
        VersionUpdateDialog(onDismiss = { showVersionDialog.value = false })
    }
}

@Composable
private fun CourseApiUrlField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("開課 API 網址") },
        placeholder = { Text(CourseApiUrl.EXAMPLE_TEMPLATE) },
        supportingText = {
            Text(
                "匯入時會把 {year} 換成所選學年、{semester} 換成學期。例如：${CourseApiUrl.EXAMPLE_TEMPLATE}。留空則不會連線。"
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        minLines = 2,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
    )
}

@Composable
fun SettingToggleCard(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked, onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
fun SettingClickableCard(icon: ImageVector, title: String, subtitle: String? = null, iconTint: Color? = null, titleColor: Color? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background((iconTint ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint ?: MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = titleColor ?: MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PreClassMinutePicker(selectedMinutes: Int, onSelect: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PreClassNotification.MINUTE_OPTIONS.forEach { min ->
            val selected = selectedMinutes == min
            Surface(
                onClick = { onSelect(min) },
                shape = RoundedCornerShape(12.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$min 分鐘",
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeOption(title: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontSize = 16.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun VersionUpdateDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更新日誌 - v1.0.5", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("✨ v1.0.5 新功能與修正：", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("• 升級底層依賴，修復 Protobuf 安全性風險。")
                Text("• 修復 Android 15 中廢棄的無邊框顯示 API 相關問題。")
                Spacer(modifier = Modifier.height(12.dp))
                Text("✨ 過往功能：", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("• 新增「跨兩天課程 (如 1ij, 4i)」自訂時間與節次設定。")
                Text("• 整合「課程通知與提醒」設定選項至獨立子選單。")
                Text("• 新增「匯出 PDF 課表」功能，支援個人姓名、學號與學校抬頭。")
                Text("• 新增「資料備份與還原」功能，可匯出 JSON 保存課表。")
                Text("• 新增「開課資訊匯入」功能。")
                Spacer(modifier = Modifier.height(12.dp))
                Text("🛠️ 過往優化與修正：", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("• 修正同名課程修改資訊或標籤顏色時無法同步更新的問題。")
                Text("• 優化卡片式主題 UI 與深色模式顏色呈現。")
                Text("• 提升 Room 資料庫存取與背景提醒穩定度。")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("關閉")
            }
        }
    )
}
