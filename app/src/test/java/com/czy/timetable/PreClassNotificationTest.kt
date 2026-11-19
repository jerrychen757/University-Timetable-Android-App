package com.czy.timetable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PreClassNotificationTest {

    @Test
    fun sanitizeMinutesKeepsSupportedValuesAndReplacesOthers() {
        assertEquals(5, PreClassNotification.sanitizeMinutes(5))
        assertEquals(10, PreClassNotification.sanitizeMinutes(10))
        assertEquals(15, PreClassNotification.sanitizeMinutes(15))
        assertEquals(30, PreClassNotification.sanitizeMinutes(30))
        assertEquals(PreClassNotification.DEFAULT_MINUTES, PreClassNotification.sanitizeMinutes(0))
        assertEquals(PreClassNotification.DEFAULT_MINUTES, PreClassNotification.sanitizeMinutes(20))
        assertEquals(PreClassNotification.DEFAULT_MINUTES, PreClassNotification.sanitizeMinutes(-5))
    }

    @Test
    fun disablingClearsEveryReminderAndWritesTheLeadTime() {
        val courses = listOf(
            course(id = 1, name = "微積分", day = 0, period = "a", shouldNotify = true, minutes = 5, reminderMode = 1),
            course(id = 2, name = "微積分", day = 0, period = "b", shouldNotify = false, minutes = 5, reminderMode = 1)
        )

        val updated = PreClassNotification.applyToAll(courses, enabled = false, minutes = 30)

        assertEquals(2, updated.size)
        assertFalse(updated[0].shouldNotify)
        assertFalse(updated[1].shouldNotify)
        assertEquals(30, updated[0].notificationMinutes)
        assertEquals(30, updated[1].notificationMinutes)
        assertEquals(1, updated[0].reminderMode)
        assertEquals(1, updated[0].id)
        assertEquals("A101", updated[0].location)
        assertEquals("記得帶課本", updated[0].memo)
    }

    @Test
    fun enablingNotifiesOnlyTheEarliestPeriodOfEachClassAndDay() {
        val courses = listOf(
            course(id = 4, name = "英文", day = 0, period = "c", shouldNotify = true, minutes = 5),
            course(id = 3, name = "英文", day = 0, period = "a", shouldNotify = false, minutes = 5),
            course(id = 5, name = "英文", day = 1, period = "b", shouldNotify = false, minutes = 5),
            course(id = 6, name = "物理", day = 0, period = "a", shouldNotify = false, minutes = 10, reminderMode = 1)
        )

        val updated = PreClassNotification.applyToAll(courses, enabled = true, minutes = 15)

        assertFalse(updated[0].shouldNotify)
        assertTrue(updated[1].shouldNotify)
        assertTrue(updated[2].shouldNotify)
        assertTrue(updated[3].shouldNotify)
        assertTrue(updated.all { it.notificationMinutes == 15 })
        assertEquals(1, updated[3].reminderMode)
        assertEquals(listOf(4, 3, 5, 6), updated.map { it.id })
    }

    @Test
    fun sameNameOnTheSameDayIsGroupedWithoutRegardToCase() {
        val courses = listOf(
            course(id = 1, name = "English", day = 2, period = "f"),
            course(id = 2, name = "english", day = 2, period = "e")
        )

        val updated = PreClassNotification.applyToAll(courses, enabled = true, minutes = 10)

        assertFalse(updated[0].shouldNotify)
        assertTrue(updated[1].shouldNotify)
    }

    @Test
    fun unknownPeriodRanksAfterKnownPeriodsAndUpperCaseStillMatches() {
        val courses = listOf(
            course(id = 1, name = "實驗", day = 3, period = "Q"),
            course(id = 2, name = "實驗", day = 3, period = "A")
        )

        val updated = PreClassNotification.applyToAll(courses, enabled = true, minutes = 99)

        assertFalse(updated[0].shouldNotify)
        assertTrue(updated[1].shouldNotify)
        assertEquals(15, updated[0].notificationMinutes)
    }

    @Test
    fun aSingleUnknownPeriodStillGetsAReminder() {
        val updated = PreClassNotification.applyToAll(
            listOf(course(id = 8, name = "自習", day = 4, period = "q", shouldNotify = false)),
            enabled = true,
            minutes = 5
        )

        assertTrue(updated.single().shouldNotify)
        assertEquals(5, updated.single().notificationMinutes)
    }

    @Test
    fun blankNamesAreNotMergedIntoOneClass() {
        val courses = listOf(
            course(id = 1, name = "  ", day = 0, period = "b"),
            course(id = 2, name = "", day = 0, period = "a")
        )

        val updated = PreClassNotification.applyToAll(courses, enabled = true, minutes = 10)

        assertTrue(updated[0].shouldNotify)
        assertTrue(updated[1].shouldNotify)
    }

    @Test
    fun applyLeavesRowsUntouchedWhenTheyAlreadyMatch() {
        val original = course(id = 7, name = "通識", day = 0, period = "d", shouldNotify = true, minutes = 15)
        val updated = PreClassNotification.applyToAll(listOf(original), enabled = true, minutes = 15)

        assertSame(original, updated.single())
    }

    @Test
    fun emptyListStaysEmpty() {
        assertTrue(PreClassNotification.applyToAll(emptyList(), enabled = true, minutes = 10).isEmpty())
    }

    private fun course(
        id: Int?,
        name: String,
        day: Int,
        period: String,
        shouldNotify: Boolean = false,
        minutes: Int = 10,
        reminderMode: Int = 0
    ) = Course(
        dayIndex = day,
        periodId = period,
        name = name,
        location = "A101",
        teacher = "教師",
        colorIndex = 2,
        memo = "記得帶課本",
        shouldNotify = shouldNotify,
        notificationMinutes = minutes,
        id = id,
        midtermDate = "2026-04-01",
        finalExamDate = "2026-06-01",
        reminderMode = reminderMode
    )
}
