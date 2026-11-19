package com.czy.timetable.calendar

import com.czy.timetable.Course
import com.czy.timetable.PeriodDef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamCalendarEventTest {
    private val examDay = "2026-11-03"

    @Test fun isoDateUsesScheduleDayIndex() {
        assertEquals(6, dayIndexForIsoDate("2026-11-01"))
        assertEquals(0, dayIndexForIsoDate("2026-11-02"))
        assertEquals(1, dayIndexForIsoDate(examDay))
        assertEquals(3, dayIndexForIsoDate("2026-11-05"))
        assertEquals(null, dayIndexForIsoDate("2026-02-31"))
        assertEquals(null, dayIndexForIsoDate("11/03"))
    }

    @Test fun threePeriodsOfOneExamOnThatDayBecomeOneRange() {
        val tuesday = dayIndexForIsoDate(examDay)!!
        val events = buildExamEvents(
            listOf(
                course("工程數學(上)", "z", day = tuesday, id = 3, midtermDate = examDay),
                course("工程數學(上)", "c", day = tuesday, id = 1, midtermDate = examDay),
                course("工程數學(上)", "d", day = tuesday, id = 2, midtermDate = examDay),
                course("工程數學(上)", "e", day = 3, id = 4, midtermDate = examDay),
                course("工程數學(上)", "f", day = 3, id = 5, midtermDate = examDay)
            ),
            examDay
        )

        val event = events.single()
        assertEquals(listOf("c", "d", "z"), event.periodIds)
        assertEquals("10:10", event.startTime)
        assertEquals("13:00", event.endTime)
        assertEquals("10:10–13:00", event.timeRange)
        assertEquals("期中考", event.examType)
        assertEquals("工程數學(上) (期中考) 10:10–13:00", event.displayText())
        assertEquals(tuesday, event.dayIndex)
    }

    @Test fun periodLetterCaseAndDuplicateRowsStillMerge() {
        val tuesday = dayIndexForIsoDate(examDay)!!
        val events = buildExamEvents(
            listOf(
                course("工程數學(上)", "C", day = tuesday, id = 1, midtermDate = examDay),
                course("工程數學(上)", "c", day = tuesday, id = 2, midtermDate = examDay),
                course("工程數學(上)", "D", day = tuesday, id = 3, midtermDate = examDay)
            ),
            examDay
        )

        val event = events.single()
        assertEquals(listOf("c", "d"), event.periodIds)
        assertEquals("10:10–12:00", event.timeRange)
    }

    @Test fun gapBetweenPeriodsStaysTwoEvents() {
        val tuesday = dayIndexForIsoDate(examDay)!!
        val events = buildExamEvents(
            listOf(
                course("工程數學(上)", "f", day = tuesday, id = 3, midtermDate = examDay),
                course("工程數學(上)", "c", day = tuesday, id = 1, midtermDate = examDay),
                course("工程數學(上)", "e", day = tuesday, id = 2, midtermDate = examDay)
            ),
            examDay
        )

        assertEquals(listOf("10:10–11:00", "13:30–15:20"), events.map { it.timeRange })
        assertEquals(listOf(listOf("c"), listOf("e", "f")), events.map { it.periodIds })
        assertTrue(events.all { it.displayText().startsWith("工程數學(上) (期中考) ") })
    }

    @Test fun lunchSlotSeparatesMorningAndAfternoonWhenItIsNotScheduled() {
        val blocks = mergeConsecutivePeriods(
            listOf(
                course("普通物理", "e"),
                course("普通物理", "c"),
                course("普通物理", "d")
            )
        )

        assertEquals(listOf("c", "d"), blocks[0].periodIds)
        assertEquals("10:10", blocks[0].startTime)
        assertEquals("12:00", blocks[0].endTime)
        assertEquals(listOf("e"), blocks[1].periodIds)
        assertEquals("13:30–14:20", formatRange(blocks[1].startTime, blocks[1].endTime))
    }

    @Test fun singlePeriodStillShowsItsOwnRange() {
        val event = buildExamEvents(
            listOf(course("英文", "a", midtermDate = examDay)),
            examDay
        ).single()

        assertEquals("08:10–09:00", event.timeRange)
        assertEquals("英文 (期中考) 08:10–09:00", event.displayText())
    }

    @Test fun differentCoursesOnTheSameDayStaySeparateAndSortByStartTime() {
        val tuesday = dayIndexForIsoDate(examDay)!!
        val events = buildExamEvents(
            listOf(
                course("工程數學(上)", "c", day = tuesday, id = 2, midtermDate = examDay),
                course("普通物理", "a", day = tuesday, id = 1, finalExamDate = examDay)
            ),
            examDay
        )

        assertEquals(listOf("普通物理", "工程數學(上)"), events.map { it.name })
        assertEquals(listOf("期末考", "期中考"), events.map { it.examType })
        assertEquals(listOf("08:10–09:00", "10:10–11:00"), events.map { it.timeRange })
    }

    @Test fun sameNameWithDifferentTeachersDoesNotMerge() {
        val events = buildExamEvents(
            listOf(
                course("工程數學(上)", "c", teacher = "甲", id = 1, midtermDate = examDay),
                course("工程數學(上)", "d", teacher = "乙", id = 2, midtermDate = examDay)
            ),
            examDay
        )

        assertEquals(2, events.size)
        assertEquals(listOf("10:10–11:00", "11:10–12:00"), events.map { it.timeRange })
    }

    @Test fun finalExamUsesItsOwnDateAndMidtermWinsWhenBothMatch() {
        val course = course(
            name = "工程數學(上)",
            period = "c",
            midtermDate = examDay,
            finalExamDate = "2026-11-05"
        )

        assertEquals("期中考", buildExamEvents(listOf(course), examDay).single().examType)
        assertEquals("期末考", buildExamEvents(listOf(course), "2026-11-05").single().examType)
        assertTrue(buildExamEvents(listOf(course), "2026-11-04").isEmpty())

        val both = course.copy(finalExamDate = examDay)
        assertEquals(listOf("期中考"), buildExamEvents(listOf(both), examDay).map { it.examType })
    }

    @Test fun classesWithoutAnExamDateAreNotCalendarEvents() {
        val tuesday = dayIndexForIsoDate(examDay)!!
        val classes = listOf(
            course("工程數學(上)", "c", day = tuesday),
            course("工程數學(上)", "d", day = tuesday),
            course("工程數學(上)", "z", day = tuesday)
        )

        assertTrue(buildExamEvents(classes, examDay).isEmpty())
        val block = mergeConsecutivePeriods(classes).single()
        assertEquals("10:10–13:00", formatRange(block.startTime, block.endTime))
    }

    @Test fun examOnADayTheCourseDoesNotMeetStillShowsTheMergedPeriods() {
        val events = buildExamEvents(
            listOf(
                course("工程數學(上)", "d", day = 3, id = 2, midtermDate = examDay),
                course("工程數學(上)", "c", day = 3, id = 1, midtermDate = examDay)
            ),
            examDay
        )

        val event = events.single()
        assertEquals(3, event.dayIndex)
        assertEquals("10:10–12:00", event.timeRange)
    }

    @Test fun blankNamesAreNotMerged() {
        val events = buildExamEvents(
            listOf(
                course("  ", "c", id = 1, midtermDate = examDay),
                course("", "d", id = 2, midtermDate = examDay)
            ),
            examDay
        )

        assertEquals(listOf("10:10–11:00", "11:10–12:00"), events.map { it.timeRange })
    }

    @Test fun unknownPeriodIsListedWithoutAFabricatedRange() {
        val events = buildExamEvents(
            listOf(
                course("自習", "q", id = 1, midtermDate = examDay),
                course("自習", "c", id = 2, midtermDate = examDay)
            ),
            examDay
        )

        assertEquals(listOf("10:10–11:00", ""), events.map { it.timeRange })
        assertEquals("自習 (期中考)", events.last().displayText())
    }

    @Test fun timeRangeUsesFirstStartAndLastEndFromTheSuppliedTable() {
        val periods = listOf(
            PeriodDef("a", "09:00", "09:40", "A"),
            PeriodDef("b", "10:00", "10:50", "B"),
            PeriodDef("gap", "10:50", "11:00", "G"),
            PeriodDef("c", "11:00", "11:50", "C")
        )
        val merged = buildExamEvents(
            listOf(
                course("實驗", "b", id = 2, midtermDate = examDay),
                course("實驗", "a", id = 1, midtermDate = examDay)
            ),
            examDay,
            periods
        )
        assertEquals("09:00–10:50", merged.single().timeRange)

        val split = buildExamEvents(
            listOf(
                course("實驗", "a", id = 1, midtermDate = examDay),
                course("實驗", "c", id = 3, midtermDate = examDay)
            ),
            examDay,
            periods
        )
        assertEquals(listOf("09:00–09:40", "11:00–11:50"), split.map { it.timeRange })
    }

    private fun formatRange(start: String, end: String) = "$start–$end"

    private fun course(
        name: String,
        period: String,
        day: Int = 1,
        teacher: String = "教師",
        id: Int? = null,
        midtermDate: String = "",
        finalExamDate: String = ""
    ) = Course(
        dayIndex = day,
        periodId = period,
        name = name,
        teacher = teacher,
        id = id,
        midtermDate = midtermDate,
        finalExamDate = finalExamDate
    )
}
