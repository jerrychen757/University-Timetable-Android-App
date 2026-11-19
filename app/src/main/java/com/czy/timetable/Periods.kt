package com.czy.timetable

/**
 * 課表節次與上課時間。
 * 月曆上的考試時段沿用這張表：相鄰節次（含午休 Z，介於 D 與 E 之間）才視為連續。
 */
data class PeriodDef(
    val id: String,
    val start: String,
    val end: String,
    val label: String,
    val isBreak: Boolean = false
)

val AllPeriods = listOf(
    PeriodDef("x", "06:10", "07:00", "X"), PeriodDef("y", "07:10", "08:00", "Y"),
    PeriodDef("a", "08:10", "09:00", "A"), PeriodDef("b", "09:10", "10:00", "B"),
    PeriodDef("c", "10:10", "11:00", "C"), PeriodDef("d", "11:10", "12:00", "D"),
    PeriodDef("z", "12:00", "13:00", "Z", isBreak = true),
    PeriodDef("e", "13:30", "14:20", "E"), PeriodDef("f", "14:30", "15:20", "F"),
    PeriodDef("g", "15:30", "16:20", "G"), PeriodDef("h", "16:30", "17:20", "H"),
    PeriodDef("i", "17:30", "18:20", "I"), PeriodDef("j", "18:30", "19:20", "J"),
    PeriodDef("k", "19:30", "20:20", "K"), PeriodDef("l", "20:30", "21:20", "L"), PeriodDef("m", "21:30", "22:20", "M")
)

fun parseTimeToMinutes(timeStr: String): Int {
    val parts = timeStr.split(":")
    if (parts.size == 2) return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
    return 0
}
