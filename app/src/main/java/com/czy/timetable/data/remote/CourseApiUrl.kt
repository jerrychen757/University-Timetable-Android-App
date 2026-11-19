package com.czy.timetable

/**
 * 使用者在設定裡填的開課 API 網址。
 * `{year}` 與 `{semester}` 會在匯入時替換成所選學年與學期。
 * 空白表示尚未設定，呼叫端不應連線。
 */
object CourseApiUrl {
    const val YEAR_PLACEHOLDER = "{year}"
    const val SEMESTER_PLACEHOLDER = "{semester}"

    const val EXAMPLE_TEMPLATE =
        "https://example.edu/api/courses?year={year}&semester={semester}"

    fun isConfigured(template: String?): Boolean = !template.isNullOrBlank()

    fun resolve(template: String?, year: String, semester: String): String? {
        val trimmed = template?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return trimmed
            .replace(YEAR_PLACEHOLDER, year)
            .replace(SEMESTER_PLACEHOLDER, semester)
    }
}
