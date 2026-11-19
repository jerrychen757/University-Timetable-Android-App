package com.czy.timetable

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseApiUrlTest {

    @Test
    fun resolveSubstitutesYearAndSemester() {
        val resolved = CourseApiUrl.resolve(
            "  https://example.edu/api/courses?year={year}&semester={semester}  ",
            year = "114",
            semester = "2"
        )

        assertEquals(
            "https://example.edu/api/courses?year=114&semester=2",
            resolved
        )
    }

    @Test
    fun resolveReplacesEveryPlaceholderAndLeavesTheRest() {
        val resolved = CourseApiUrl.resolve(
            "https://example.edu/{year}/courses/{semester}?again={year}",
            year = "113",
            semester = "1"
        )

        assertEquals("https://example.edu/113/courses/1?again=113", resolved)
    }

    @Test
    fun resolveReturnsNullWhenTheTemplateIsMissingOrBlank() {
        assertNull(CourseApiUrl.resolve(null, "114", "1"))
        assertNull(CourseApiUrl.resolve("", "114", "1"))
        assertNull(CourseApiUrl.resolve(" \n\t ", "114", "1"))
        assertFalse(CourseApiUrl.isConfigured(null))
        assertFalse(CourseApiUrl.isConfigured(""))
        assertFalse(CourseApiUrl.isConfigured("   "))
        assertTrue(CourseApiUrl.isConfigured(CourseApiUrl.EXAMPLE_TEMPLATE))
    }

    @Test
    fun exampleTemplateUsesTheDocumentedPlaceholders() {
        assertTrue(CourseApiUrl.EXAMPLE_TEMPLATE.contains(CourseApiUrl.YEAR_PLACEHOLDER))
        assertTrue(CourseApiUrl.EXAMPLE_TEMPLATE.contains(CourseApiUrl.SEMESTER_PLACEHOLDER))
        assertEquals(
            "https://example.edu/api/courses?year=114&semester=1",
            CourseApiUrl.resolve(CourseApiUrl.EXAMPLE_TEMPLATE, "114", "1")
        )
    }

    @Test
    fun fetchAllCoursesDoesNotCallTheNetworkWhenTheUrlIsEmpty() = runBlocking {
        var calls = 0
        val manager = NCNUApiManager(CourseHttpFetcher {
            calls += 1
            error("network should not be called")
        })

        val results = listOf(null, "", "   ", "\n").map { template ->
            manager.fetchAllCourses(
                year = "114",
                semester = "2",
                urlTemplate = template.orEmpty()
            )
        }

        assertTrue(results.all { it.isEmpty() })
        assertEquals(0, calls)
    }
}
