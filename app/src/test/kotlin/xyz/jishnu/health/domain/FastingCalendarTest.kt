package xyz.jishnu.health.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.jishnu.health.data.local.FastingSessionEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class FastingCalendarTest {
    private val utc = ZoneId.of("UTC")

    private fun ms(y: Int, m: Int, d: Int, h: Int): Long =
        ZonedDateTime.of(y, m, d, h, 0, 0, 0, utc).toInstant().toEpochMilli()

    /** A completed session ending at [endY-endM-endD] hh:00 that lasted [hours]. */
    private fun session(id: Long, endY: Int, endM: Int, endD: Int, endH: Int, hours: Int) =
        FastingSessionEntity(
            id = id,
            startMs = ms(endY, endM, endD, endH) - hours * 3_600_000L,
            endMs = ms(endY, endM, endD, endH),
            goalHours = 16,
            planId = "16:8",
        )

    @Test fun `level applies a 12h floor then short, met, exceeded`() {
        assertEquals(0, FastingCalendarBuilder.level(0.0, 16))   // no fast
        assertEquals(0, FastingCalendarBuilder.level(8.0, 16))   // under 12h -> not a fast
        assertEquals(0, FastingCalendarBuilder.level(11.9, 16))  // just under the floor
        assertEquals(1, FastingCalendarBuilder.level(12.0, 16))  // short (12h, under goal)
        assertEquals(1, FastingCalendarBuilder.level(15.9, 16))  // still short
        assertEquals(2, FastingCalendarBuilder.level(16.0, 16))  // goal met
        assertEquals(2, FastingCalendarBuilder.level(16.9, 16))  // met, under goal+1h
        assertEquals(3, FastingCalendarBuilder.level(17.0, 16))  // exceeded (goal + 1h)
        assertEquals(3, FastingCalendarBuilder.level(20.0, 16))  // well beyond goal
        // Floor holds for a lower plan too.
        assertEquals(1, FastingCalendarBuilder.level(12.0, 14))  // short on a 14h goal
        assertEquals(2, FastingCalendarBuilder.level(14.0, 14))  // met on a 14h goal
    }

    @Test fun `grid starts on Monday and ends at today`() {
        val today = LocalDate.of(2026, 7, 20) // a Monday
        val cal = FastingCalendarBuilder.build(emptyList(), 16, today, utc)
        // first cell of the first column is a Monday
        assertEquals(DayOfWeek.MONDAY, cal.weeks.first()[0]!!.date.dayOfWeek)
        // last non-null cell is today
        val lastReal = cal.weeks.flatten().filterNotNull().maxByOrNull { it.date }!!
        assertEquals(today, lastReal.date)
        // ~53 columns of 7 rows
        assertTrue(cal.weeks.size in 52..54)
        assertTrue(cal.weeks.all { it.size == 7 })
    }

    @Test fun `days after today are padding nulls`() {
        val today = LocalDate.of(2026, 7, 22) // a Wednesday
        val cal = FastingCalendarBuilder.build(emptyList(), 16, today, utc)
        // Grid still aligns to Monday even when today is mid-week.
        assertEquals(DayOfWeek.MONDAY, cal.weeks.first()[0]!!.date.dayOfWeek)
        val lastCol = cal.weeks.last()
        // Wed = index 2 (Mon=0); Thu..Sun should be null padding
        assertNotNull(lastCol[2])
        assertNull(lastCol[3])
        assertNull(lastCol[6])
    }

    @Test fun `stats respect the 12h floor and count met plus exceeded`() {
        val today = LocalDate.of(2026, 7, 20)
        val sessions = listOf(
            session(1, 2026, 7, 20, 8, 16), // met (level 2)
            session(2, 2026, 7, 19, 8, 16), // met (level 2)
            session(3, 2026, 7, 18, 8, 18), // exceeded (level 3) -> streak of 3
            session(4, 2026, 7, 16, 8, 13), // short but >= 12h (fasted, no streak)
            session(5, 2026, 7, 15, 8, 8),  // under 12h -> not counted as fasted
        )
        val cal = FastingCalendarBuilder.build(sessions, 16, today, utc)
        // Only 12h+ days count as fasted (the 8h day is excluded).
        assertEquals(4, cal.daysFasted)
        // Reaching the goal counts whether met or exceeded.
        assertEquals(3, cal.goalMetDays)
        assertEquals(3, cal.longestStreak)
    }
}
