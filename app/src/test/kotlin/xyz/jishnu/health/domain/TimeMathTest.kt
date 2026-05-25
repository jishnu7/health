package xyz.jishnu.health.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeMathTest {
    @Test fun `fmtDuration zero`() {
        val d = TimeMath.fmtDuration(0)
        assertEquals(0, d.hours); assertEquals(0, d.minutes); assertEquals(0, d.seconds)
        assertEquals("00", d.hh); assertEquals("00", d.mm); assertEquals("00", d.ss)
    }

    @Test fun `fmtDuration 14h 23m 7s`() {
        val ms = (14 * 3600 + 23 * 60 + 7) * 1000L
        val d = TimeMath.fmtDuration(ms)
        assertEquals(14, d.hours); assertEquals(23, d.minutes); assertEquals(7, d.seconds)
    }

    @Test fun `fmtDuration negative clamps to zero`() {
        val d = TimeMath.fmtDuration(-5000)
        assertEquals(0, d.hours); assertEquals(0, d.minutes); assertEquals(0, d.seconds)
    }

    @Test fun `addHoursToTime simple add`() {
        assertEquals("06:30", TimeMath.addHoursToTime("04:30", 2.0))
    }

    @Test fun `addHoursToTime wraps past midnight`() {
        assertEquals("04:00", TimeMath.addHoursToTime("20:00", 8.0))
    }

    @Test fun `addHoursToTime fractional hours`() {
        assertEquals("21:30", TimeMath.addHoursToTime("20:00", 1.5))
    }

    @Test fun `addHoursToTime negative wraps backwards`() {
        assertEquals("22:00", TimeMath.addHoursToTime("02:00", -4.0))
    }

    @Test fun `diffHoursTime same day`() {
        assertEquals(8.0, TimeMath.diffHoursTime("08:00", "16:00"), 0.0001)
    }

    @Test fun `diffHoursTime wraps midnight`() {
        assertEquals(11.0, TimeMath.diffHoursTime("20:00", "07:00"), 0.0001)
    }

    @Test fun `diffHoursTime equal times returns 24h`() {
        assertEquals(24.0, TimeMath.diffHoursTime("12:00", "12:00"), 0.0001)
    }
}
