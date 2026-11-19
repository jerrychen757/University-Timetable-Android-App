package com.czy.timetable.calendar

import com.czy.timetable.AllPeriods
import com.czy.timetable.Course
import com.czy.timetable.PeriodDef
import com.czy.timetable.parseTimeToMinutes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

const val EXAM_TYPE_MIDTERM = "期中考"
const val EXAM_TYPE_FINAL = "期末考"

data class DayInfo(val date: Date, val dateString: String, val dayOfMonth: Int)

/**
 * 月曆某一天的一筆考試。
 *
 * 課表以一節一筆儲存，同一門課同一天的期中考／期末考會因此重複出現。
 * [buildExamEvents] 會把該日同一門課的相鄰節次併成一筆，時間取第一節開始到最後一節結束。
 */
data class ExamCalendarEvent(
    val name: String,
    val examType: String,
    val startTime: String,
    val endTime: String,
    val periodIds: List<String>,
    val dayIndex: Int
) {
    val timeRange: String
        get() = if (startTime.isNotEmpty() && endTime.isNotEmpty()) "$startTime–$endTime" else ""

    fun displayText(): String {
        val title = "$name ($examType)"
        return if (timeRange.isEmpty()) title else "$title $timeRange"
    }
}

/** 同一門課、同一天、一段連續節次。一般課程若要併時段，與此使用同一規則。 */
data class PeriodBlock(
    val courseKey: String,
    val name: String,
    val dayIndex: Int,
    val startTime: String,
    val endTime: String,
    val periodIds: List<String>
)

fun getDaysInMonth(month: Calendar): List<DayInfo?> {
    val result = mutableListOf<DayInfo?>()
    val cal = month.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
    for (i in 0 until firstDayOfWeek) {
        result.add(null)
    }

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    for (i in 1..daysInMonth) {
        cal.set(Calendar.DAY_OF_MONTH, i)
        result.add(DayInfo(cal.time, sdf.format(cal.time), i))
    }
    return result
}

/**
 * 課表 dayIndex：一 = 0 … 日 = 6。無法解析的日期回傳 null。
 */
fun dayIndexForIsoDate(date: String): Int? {
    val match = Regex("""(\d{4})-(\d{2})-(\d{2})""").matchEntire(date) ?: return null
    val year = match.groupValues[1].toInt()
    val month = match.groupValues[2].toInt()
    val day = match.groupValues[3].toInt()
    val calendar = GregorianCalendar().apply {
        isLenient = false
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
    }
    return try {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
    } catch (_: IllegalArgumentException) {
        null
    }
}

/**
 * 把同一門課、同一天裡節次順序相鄰的時段併成一段。
 * 時間取這一段第一節的開始、最後一節的結束。午休 Z 也是一節，沒有排到 Z 的話，下午就不會和上午併在一起。
 * 名稱與教師相同（忽略大小寫與前後空白）視為同一門課；空白課名不互相合併。
 */
fun mergeConsecutivePeriods(
    courses: List<Course>,
    periods: List<PeriodDef> = AllPeriods
): List<PeriodBlock> {
    if (courses.isEmpty()) return emptyList()

    val rank = HashMap<String, Int>(periods.size)
    val timing = HashMap<String, PeriodDef>(periods.size)
    periods.forEachIndexed { index, period ->
        val id = period.id.trim().lowercase()
        if (id !in rank) {
            rank[id] = index
            timing[id] = period
        }
    }

    val blocks = mutableListOf<PeriodBlock>()
    for (group in courses.groupBy { courseIdentity(it) to it.dayIndex }.values) {
        val sample = group.minWith(
            compareBy<Course>(
                { rank[it.periodId.trim().lowercase()] ?: Int.MAX_VALUE },
                { it.id ?: Int.MAX_VALUE }
            )
        )
        val ids = group.map { it.periodId.trim().lowercase() }.distinct()
        val known = ids.filter { it in rank }.sortedBy { rank.getValue(it) }
        val unknown = ids.filter { it !in rank }.sorted()

        var run = mutableListOf<PeriodDef>()
        var previousRank = Int.MIN_VALUE
        for (id in known) {
            val currentRank = rank.getValue(id)
            if (run.isNotEmpty() && currentRank == previousRank + 1) {
                run.add(timing.getValue(id))
            } else {
                if (run.isNotEmpty()) blocks.add(blockFrom(sample, run))
                run = mutableListOf(timing.getValue(id))
            }
            previousRank = currentRank
        }
        if (run.isNotEmpty()) blocks.add(blockFrom(sample, run))

        for (id in unknown) {
            blocks.add(
                PeriodBlock(
                    courseKey = courseIdentity(sample),
                    name = sample.name.trim(),
                    dayIndex = sample.dayIndex,
                    startTime = "",
                    endTime = "",
                    periodIds = listOf(id)
                )
            )
        }
    }

    return blocks.sortedWith(periodBlockOrder)
}

/**
 * 選出 [date] 當天的期中考／期末考。
 * 考試日剛好是上課日的話，只併那天的節次；若那天本來沒有課，仍列出這門課的上課時段，避免考試從月曆消失。
 * 同一天同時寫了期中與期末時，沿用原本的月曆規則，只顯示期中考。
 */
fun buildExamEvents(
    courses: List<Course>,
    date: String,
    periods: List<PeriodDef> = AllPeriods
): List<ExamCalendarEvent> {
    val meetingDay = dayIndexForIsoDate(date)
    val chosen = courses.mapNotNull { course ->
        val type = when {
            course.midtermDate == date -> EXAM_TYPE_MIDTERM
            course.finalExamDate == date -> EXAM_TYPE_FINAL
            else -> null
        }
        type?.let { course to it }
    }.groupBy { (course, type) -> courseIdentity(course) to type }
        .values
        .flatMap { rows ->
            val onMeetingDay = if (meetingDay == null) {
                emptyList()
            } else {
                rows.filter { (course, _) -> course.dayIndex == meetingDay }
            }
            if (onMeetingDay.isNotEmpty()) onMeetingDay else rows
        }

    if (chosen.isEmpty()) return emptyList()

    val typeByGroup = chosen.associate { (course, type) ->
        (courseIdentity(course) to course.dayIndex) to type
    }
    return mergeConsecutivePeriods(chosen.map { it.first }, periods).map { block ->
        ExamCalendarEvent(
            name = block.name,
            examType = typeByGroup.getValue(block.courseKey to block.dayIndex),
            startTime = block.startTime,
            endTime = block.endTime,
            periodIds = block.periodIds,
            dayIndex = block.dayIndex
        )
    }
}

private val periodBlockOrder = compareBy<PeriodBlock>(
    { it.dayIndex },
    { if (it.startTime.isEmpty()) Int.MAX_VALUE else parseTimeToMinutes(it.startTime) },
    { it.name },
    { it.courseKey },
    { it.periodIds.firstOrNull().orEmpty() }
)

private fun blockFrom(sample: Course, run: List<PeriodDef>): PeriodBlock {
    return PeriodBlock(
        courseKey = courseIdentity(sample),
        name = sample.name.trim(),
        dayIndex = sample.dayIndex,
        startTime = run.first().start,
        endTime = run.last().end,
        periodIds = run.map { it.id.lowercase() }
    )
}

private fun courseIdentity(course: Course): String {
    val name = course.name.trim()
    if (name.isEmpty()) {
        return "id:${course.id ?: System.identityHashCode(course)}"
    }
    return "${name.lowercase()}|${course.teacher.trim().lowercase()}"
}
