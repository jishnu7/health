package xyz.jishnu.health.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.jishnu.health.data.local.WeightEntryEntity
import java.time.ZoneId
import java.time.ZonedDateTime

class WeightTrendTest {
    private val utc = ZoneId.of("UTC")

    private fun day(y: Int, m: Int, d: Int): Long =
        ZonedDateTime.of(y, m, d, 0, 0, 0, 0, utc).toInstant().toEpochMilli()

    private fun w(dayKey: Long, kg: Double) =
        WeightEntryEntity(id = dayKey, dayKey = dayKey, weightKg = kg, createdMs = 0)

    @Test fun `empty input yields no weeks`() {
        assertTrue(WeightTrend.buildWeekly(emptyList(), utc).isEmpty())
    }

    @Test fun `entries in the same week collapse to one week with min max avg`() {
        val weeks = WeightTrend.buildWeekly(
            listOf(w(day(2026, 5, 4), 80.0), w(day(2026, 5, 6), 82.0)),
            utc,
        )
        assertEquals(1, weeks.size)
        assertEquals(80.0, weeks[0].minKg, 0.0)
        assertEquals(82.0, weeks[0].maxKg, 0.0)
        assertEquals(81.0, weeks[0].avgKg, 0.0)
        assertFalse(weeks[0].carried)
    }

    @Test fun `an interior empty week is filled flat with the previous week average`() {
        // 14 days apart => exactly one empty ISO week between the two recordings.
        val weeks = WeightTrend.buildWeekly(
            listOf(w(day(2026, 5, 4), 80.0), w(day(2026, 5, 18), 78.0)),
            utc,
        )
        assertEquals(3, weeks.size)
        assertFalse(weeks[0].carried)
        assertEquals(80.0, weeks[0].avgKg, 0.0)
        // Middle week has no recording -> carries 80.0 as a flat point.
        assertTrue(weeks[1].carried)
        assertEquals(80.0, weeks[1].avgKg, 0.0)
        assertEquals(80.0, weeks[1].minKg, 0.0)
        assertEquals(80.0, weeks[1].maxKg, 0.0)
        assertFalse(weeks[2].carried)
        assertEquals(78.0, weeks[2].avgKg, 0.0)
    }

    @Test fun `adjacent weeks are not gap-filled`() {
        // 7 days apart => consecutive ISO weeks, nothing to fill.
        val weeks = WeightTrend.buildWeekly(
            listOf(w(day(2026, 5, 4), 80.0), w(day(2026, 5, 11), 79.0)),
            utc,
        )
        assertEquals(2, weeks.size)
        assertFalse(weeks[0].carried)
        assertFalse(weeks[1].carried)
    }

    @Test fun `no synthetic weeks are added before the first or after the last recording`() {
        val weeks = WeightTrend.buildWeekly(
            listOf(w(day(2026, 5, 4), 80.0), w(day(2026, 5, 18), 78.0)),
            utc,
        )
        // Only the single interior week is synthetic; ends stay as recorded.
        assertEquals(1, weeks.count { it.carried })
        assertFalse(weeks.first().carried)
        assertFalse(weeks.last().carried)
    }
}
