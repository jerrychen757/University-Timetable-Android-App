package com.czy.timetable

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 定義淺色模式的顏色 (保留你原本精美的設計)
private val LightColors = lightColorScheme(
    primary = Color(0xFF5A52FF),
    background = Color(0xFFF8F9FA), // 淺灰背景
    surface = Color.White,          // 白色卡片
    onBackground = Color(0xFF111111), // 深色文字
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFE5E7EB)     // 淺灰邊框
)

// 定義深色模式的顏色 (超有質感的暗黑系)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF7A75FF),    // 夜間模式的紫色要稍微亮一點才好看
    background = Color(0xFF121212), // 純黑/深灰背景
    surface = Color(0xFF1E1E1E),    // 深灰卡片
    onBackground = Color(0xFFE5E7EB), // 白色文字
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFAAAEB6),
    outline = Color(0xFF374151)     // 深色邊框
)

@Composable
fun AppTheme(
    themeMode: Int, // 0 = 跟隨系統, 1 = 淺色, 2 = 深色
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        content = content
    )
}