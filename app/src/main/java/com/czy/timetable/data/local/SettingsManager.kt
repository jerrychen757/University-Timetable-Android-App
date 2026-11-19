package com.czy.timetable

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 1. 建立 DataStore 的擴充屬性，確保整個 App 共用同一個名為 "settings" 的設定檔
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    // 2. 定義所有的 Key對應AsyncStorage 使用的字串
    companion object {
        val THEME_MODE = intPreferencesKey("themeMode") // 0: 系統, 1: 淺色, 2: 深色
        val GLOBAL_NOTIFY = booleanPreferencesKey("globalNotificationsEnabled")
        val WEEKEND_VISIBLE = booleanPreferencesKey("weekendVisible")
        val FULL_PERIODS_KEY = booleanPreferencesKey("full_periods")

        // ✅ 新增：提醒方式的 Key (0: 一般通知, 1: 鬧鐘)
        val REMINDER_MODE = intPreferencesKey("reminderMode")

        // 統一上課前提醒：新課程的預設，以及「套用到所有課程」要寫入的值
        val PRE_CLASS_NOTIFY_ENABLED = booleanPreferencesKey("preClassNotifyEnabled")
        val PRE_CLASS_NOTIFY_MINUTES = intPreferencesKey("preClassNotifyMinutes")

        val SCHOOL_NAME = stringPreferencesKey("schoolName")
        val STUDENT_NAME = stringPreferencesKey("studentName")
        val STUDENT_ID = stringPreferencesKey("studentId")

        // 開課 API 網址範本。沒有這個 key 時視為未設定，不寫入任何預設網址。
        val COURSE_API_URL = stringPreferencesKey("courseApiUrl")

        // ✅ 新增：寒暑假模式相關 Key
        val VACATION_MODE_ENABLED = booleanPreferencesKey("vacationModeEnabled")
        val WINTER_VACATION_START = stringPreferencesKey("winterVacationStart")
        val WINTER_VACATION_END = stringPreferencesKey("winterVacationEnd")
        val SUMMER_VACATION_START = stringPreferencesKey("summerVacationStart")
        val SUMMER_VACATION_END = stringPreferencesKey("summerVacationEnd")
    }

    // ==========================================
    // 讀取資料區塊 (回傳 Flow，讓 UI 可以自動監聽變化)
    // ==========================================

    val themeModeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: 0 // 預設為 0 (系統)
    }

    val globalNotifyFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GLOBAL_NOTIFY] ?: true // 預設值為 true
    }

    val weekendVisibleFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[WEEKEND_VISIBLE] ?: false // 預設值為 false
    }

    val fullPeriodsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FULL_PERIODS_KEY] ?: false
    }

    // ✅ 新增：讀取提醒方式 (預設 0 = 通知)
    val reminderModeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[REMINDER_MODE] ?: 0
    }

    // 新課程預設不通知，提前時間與編輯課程畫面的預設相同 (15 分鐘)
    val preClassNotifyEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PRE_CLASS_NOTIFY_ENABLED] ?: PreClassNotification.DEFAULT_ENABLED
    }

    val preClassNotifyMinutesFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        PreClassNotification.sanitizeMinutes(
            preferences[PRE_CLASS_NOTIFY_MINUTES] ?: PreClassNotification.DEFAULT_MINUTES
        )
    }

    val schoolNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SCHOOL_NAME] ?: ""
    }

    val courseApiUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[COURSE_API_URL] ?: ""
    }
    val studentNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[STUDENT_NAME] ?: ""
    }
    val studentIdFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[STUDENT_ID] ?: ""
    }

    // ✅ 新增：寒暑假模式讀取
    val vacationModeEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VACATION_MODE_ENABLED] ?: false
    }
    val winterVacationStartFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[WINTER_VACATION_START] ?: ""
    }
    val winterVacationEndFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[WINTER_VACATION_END] ?: ""
    }
    val summerVacationStartFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SUMMER_VACATION_START] ?: ""
    }
    val summerVacationEndFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SUMMER_VACATION_END] ?: ""
    }

    // ==========================================
    // 寫入資料區塊 (使用 suspend 讓存檔動作在背景執行，不卡畫面)
    // ==========================================

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    suspend fun toggleGlobalNotify(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GLOBAL_NOTIFY] = enabled
        }
    }

    suspend fun toggleWeekendVisible(visible: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WEEKEND_VISIBLE] = visible
        }
    }

    suspend fun toggleFullPeriods(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FULL_PERIODS_KEY] = enabled
        }
    }

    // ✅ 新增：儲存提醒方式
    suspend fun setReminderMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[REMINDER_MODE] = mode
        }
    }

    suspend fun setPreClassNotifyEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PRE_CLASS_NOTIFY_ENABLED] = enabled
        }
    }

    suspend fun setPreClassNotifyMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PRE_CLASS_NOTIFY_MINUTES] = PreClassNotification.sanitizeMinutes(minutes)
        }
    }

    suspend fun savePdfInfo(school: String, name: String, id: String) {
        context.dataStore.edit { preferences ->
            preferences[SCHOOL_NAME] = school
            preferences[STUDENT_NAME] = name
            preferences[STUDENT_ID] = id
        }
    }

    suspend fun saveCourseApiUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[COURSE_API_URL] = url
        }
    }

    // ✅ 新增：寒暑假模式儲存
    suspend fun setVacationModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VACATION_MODE_ENABLED] = enabled
        }
    }

    suspend fun saveVacationDates(winterStart: String, winterEnd: String, summerStart: String, summerEnd: String) {
        context.dataStore.edit { preferences ->
            preferences[WINTER_VACATION_START] = winterStart
            preferences[WINTER_VACATION_END] = winterEnd
            preferences[SUMMER_VACATION_START] = summerStart
            preferences[SUMMER_VACATION_END] = summerEnd
        }
    }
}
