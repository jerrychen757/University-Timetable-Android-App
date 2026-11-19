package com.czy.timetable.calendar

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar

class CalendarUtilsTest {
    private fun month(year: Int, month: Int) = GregorianCalendar(year, month, 15, 12, 0).apply {
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    @Test fun leapFebruaryContains29DaysAndThursdayPadding() {
        val days = getDaysInMonth(month(2024, Calendar.FEBRUARY))
        assertEquals(4, days.takeWhile { it == null }.size)
        assertEquals((1..29).toList(), days.filterNotNull().map { it.dayOfMonth })
    }

    @Test fun commonFebruaryHasNoLeapDay() {
        assertEquals(28, getDaysInMonth(month(2025, Calendar.FEBRUARY)).filterNotNull().size)
    }

    @Test fun sundayStartNeedsNoPadding() {
        assertEquals(1, getDaysInMonth(month(2026, Calendar.NOVEMBER)).first()?.dayOfMonth)
    }

    @Test fun generatingMonthDoesNotChangeSelectedDate() {
        val selected = month(2024, Calendar.DECEMBER)
        val before = selected.clone() as Calendar
        getDaysInMonth(selected)
        assertEquals(before, selected)
    }
}
