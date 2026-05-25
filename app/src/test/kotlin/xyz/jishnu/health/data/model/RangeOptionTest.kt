package xyz.jishnu.health.data.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RangeOptionTest {
    private val today = LocalDate.of(2026, 5, 25)  // Monday

    @Test fun `last 7 days`() {
        val r = RangeOption.Last7.range(today)
        assertEquals(LocalDate.of(2026, 5, 19), r.start)
        assertEquals(today, r.endInclusive)
        assertEquals(7, RangeOption.dayCount(RangeOption.Last7, today))
    }

    @Test fun `last 14 days`() {
        val r = RangeOption.Last14.range(today)
        assertEquals(LocalDate.of(2026, 5, 12), r.start)
        assertEquals(today, r.endInclusive)
    }

    @Test fun `this week from Monday`() {
        val r = RangeOption.ThisWeek.range(today)
        assertEquals(today, r.start)  // 2026-05-25 is a Monday
        assertEquals(today, r.endInclusive)
    }

    @Test fun `this week from Wednesday`() {
        val wednesday = LocalDate.of(2026, 5, 27)
        val r = RangeOption.ThisWeek.range(wednesday)
        assertEquals(LocalDate.of(2026, 5, 25), r.start)
        assertEquals(wednesday, r.endInclusive)
    }

    @Test fun `this month`() {
        val r = RangeOption.ThisMonth.range(today)
        assertEquals(LocalDate.of(2026, 5, 1), r.start)
    }

    @Test fun `ytd`() {
        val r = RangeOption.YearToDate.range(today)
        assertEquals(LocalDate.of(2026, 1, 1), r.start)
    }

    @Test fun `custom range`() {
        val custom = RangeOption.Custom(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 15))
        val r = custom.range(today)
        assertEquals(LocalDate.of(2026, 5, 1), r.start)
        assertEquals(LocalDate.of(2026, 5, 15), r.endInclusive)
    }

    @Test fun `byId resolves quick options`() {
        assertEquals(RangeOption.Last30, RangeOption.byId("30d"))
        assertEquals(null, RangeOption.byId("nope"))
    }
}
