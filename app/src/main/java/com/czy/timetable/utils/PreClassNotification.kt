package com.czy.timetable

/**
 * 統一上課前提醒。
 *
 * 設定頁存的是預設值：新課程，以及從開課資訊匯入的課程，會套用它。
 * 已經在課表上的課程要等使用者按下「套用到所有課程」才改寫，
 * 所以只改設定不會清掉各別課程原本的提醒。
 *
 * 套用時沿用編輯課程的規則：同一門課、同一天只有最早的節次會響鈴，
 * 其餘欄位（提醒方式、地點、備註等）維持原樣。
 */
object PreClassNotification {
    const val DEFAULT_ENABLED = false
    const val DEFAULT_MINUTES = 15

    val MINUTE_OPTIONS = listOf(5, 10, 15, 30)

    /** 與課表節次、編輯課程的時段順序相同。 */
    val periodOrder = listOf(
        "x", "y", "a", "b", "c", "d", "z", "e", "f", "g", "h", "i", "j", "k", "l", "m"
    )

    fun sanitizeMinutes(minutes: Int): Int =
        if (minutes in MINUTE_OPTIONS) minutes else DEFAULT_MINUTES

    fun applyToAll(courses: List<Course>, enabled: Boolean, minutes: Int): List<Course> {
        val lead = sanitizeMinutes(minutes)
        if (!enabled) {
            return courses.map { course ->
                if (!course.shouldNotify && course.notificationMinutes == lead) course
                else course.copy(shouldNotify = false, notificationMinutes = lead)
            }
        }

        val notifyIndices = courses.withIndex()
            .groupBy { groupKey(it.value) }
            .values
            .map { group ->
                group.minWith(
                    compareBy<IndexedValue<Course>>(
                        { periodRank(it.value.periodId) },
                        { it.value.id ?: Int.MAX_VALUE },
                        { it.index }
                    )
                ).index
            }
            .toSet()

        return courses.mapIndexed { index, course ->
            val shouldNotify = index in notifyIndices
            if (course.shouldNotify == shouldNotify && course.notificationMinutes == lead) course
            else course.copy(shouldNotify = shouldNotify, notificationMinutes = lead)
        }
    }

    private fun groupKey(course: Course): String {
        val name = course.name.trim()
        if (name.isEmpty()) {
            return "id:${course.id ?: System.identityHashCode(course)}|${course.dayIndex}"
        }
        return "${name.lowercase()}|${course.dayIndex}"
    }

    private fun periodRank(periodId: String): Int {
        val index = periodOrder.indexOf(periodId.lowercase())
        return if (index >= 0) index else periodOrder.size
    }
}
