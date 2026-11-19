package com.czy.timetable

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.czy.timetable.ui.MainApp

class MainActivity : ComponentActivity() {

    // 宣告權限請求啟動器 (Launcher)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean ->
        // 如果有需要，可以在這裡處理權限同意或拒絕後的邏輯
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 啟動時主動檢查並請求通知權限 (Android 13 / API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        AdManager.loadAd(this)

        val appContainer = (application as BaseApplication).container
        val settingsManager = appContainer.settingsManager

        setContent {
            val viewModel: ScheduleViewModel = viewModel(
                factory = ScheduleViewModel.provideFactory(appContainer.courseDao)
            )
            val themeMode by settingsManager.themeModeFlow.collectAsState(initial = 0)
            AppTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(scheduleViewModel = viewModel, settingsManager = settingsManager)
                }
            }
        }
    }
}
