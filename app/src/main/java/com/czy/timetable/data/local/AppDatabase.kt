package com.czy.timetable

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// 告訴系統這是一個資料庫，裡面有 Course 這張表，目前版本為 2
@Database(entities = [Course::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // 連結DAO
    abstract fun courseDao(): CourseDao

    companion object {
        // 資料庫遷移：從版本 1 升級到 2，新增 reminderMode 欄位
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE courses ADD COLUMN reminderMode INTEGER NOT NULL DEFAULT 0")
            }
        }

        // @Volatile 確保這個變數的修改對所有執行緒都是立即更新的
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 取得資料庫實體 (這叫做 Singleton 單例模式)
        // 為了節省效能並避免資料衝突，我們確保整個 App 永遠只會有「一個」資料庫連線
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "course_database" // 這是實際存在手機裡的資料庫檔案名稱
                )
                .addMigrations(MIGRATION_1_2) // 註冊遷移邏輯
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
