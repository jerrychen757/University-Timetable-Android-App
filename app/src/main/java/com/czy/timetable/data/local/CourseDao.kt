package com.czy.timetable

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    // 1. 取得所有課程
    // 只要資料庫有變動，UI 自動更新，不用手動重新抓資料
    @Query("SELECT * FROM courses")
    fun getAllCourses(): Flow<List<Course>>

    // 2. 取得特定一堂課 (用來檢查某個時段有沒有課)
    @Query("SELECT * FROM courses WHERE dayIndex = :dayIndex AND periodId = :periodId LIMIT 1")
    suspend fun getCourse(dayIndex: Int, periodId: String): Course?

    // 3. 新增或更新課程
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Unit

    // 4. 批次新增課程openBatchModal多選功能
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<Course>): Unit

    // 依主鍵更新整列，保留原本的 id，避免鬧鐘的 requestCode 被換掉
    @Update
    suspend fun updateCourses(courses: List<Course>): Unit

    // 5. 刪除部分課程
    @Delete
    suspend fun deleteCourse(course: Course): Unit

    // 6. 清空所有課程
    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses(): Unit

    // 7. 依課程名稱刪除同名課程的所有時段 (區分或忽略大小寫)
    @Query("DELETE FROM courses WHERE UPPER(name) = UPPER(:name)")
    suspend fun deleteCoursesByName(name: String): Unit

    // 8. 依 ID 刪除單一課程
    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteCourseById(id: Int): Unit
}
