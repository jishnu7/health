package xyz.jishnu.health.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.jishnu.health.data.local.FastingSessionEntity
import xyz.jishnu.health.data.local.WeightEntryEntity
import java.time.ZoneId
import java.time.ZonedDateTime

class DayEntriesTest {
    private val utc = ZoneId.of("UTC")

    private fun day(y: Int, m: Int, d: Int): Long =
        ZonedDateTime.of(y, m, d, 0, 0, 0, 0, utc).toInstant().toEpochMilli()

    @Test fun `dayKey rounds to local midnight`() {
        val noon = day(2026, 5, 25) + 12 * 3_600_000L
        assertEquals(day(2026, 5, 25), DayEntries.dayKeyFor(noon, utc))
    }

    @Test fun `merge groups by day and sorts desc`() {
        // Sessions bucket by the day they END on (see DayEntries.merge).
        val s1 = FastingSessionEntity(id = 1, startMs = day(2026, 5, 23) + 20 * 3_600_000L, endMs = day(2026, 5, 24) + 12 * 3_600_000L, goalHours = 16, planId = "16:8")
        val s2 = FastingSessionEntity(id = 2, startMs = day(2026, 5, 24) + 20 * 3_600_000L, endMs = day(2026, 5, 25) + 12 * 3_600_000L, goalHours = 16, planId = "16:8")
        val w1 = WeightEntryEntity(id = 1, dayKey = day(2026, 5, 24), weightKg =178.0, createdMs = 0)
        val w2 = WeightEntryEntity(id = 2, dayKey = day(2026, 5, 25), weightKg =177.8, createdMs = 0)

        val merged = DayEntries.merge(listOf(s1, s2), listOf(w1, w2), utc)
        assertEquals(2, merged.size)
        assertEquals(day(2026, 5, 25), merged[0].dayKey)
        assertEquals(day(2026, 5, 24), merged[1].dayKey)
        assertEquals(1, merged[0].sessions.size)
        assertNotNull(merged[0].weight)
    }

    @Test fun `merge handles day with only weight`() {
        val w = WeightEntryEntity(id = 1, dayKey = day(2026, 5, 25), weightKg =180.0, createdMs = 0)
        val merged = DayEntries.merge(emptyList(), listOf(w), utc)
        assertEquals(1, merged.size)
        assertTrue(merged[0].sessions.isEmpty())
        assertNotNull(merged[0].weight)
        assertEquals(0.0, merged[0].fastHours, 0.0)
    }

    @Test fun `fastHours is longest single session for the day`() {
        val short = FastingSessionEntity(id = 1, startMs = day(2026, 5, 25) + 0, endMs = day(2026, 5, 25) + 5 * 3_600_000L, goalHours = 16, planId = "16:8")
        val long = FastingSessionEntity(id = 2, startMs = day(2026, 5, 25) + 6 * 3_600_000L, endMs = day(2026, 5, 25) + 22 * 3_600_000L, goalHours = 16, planId = "16:8")
        val merged = DayEntries.merge(listOf(short, long), emptyList(), utc)
        assertEquals(1, merged.size)
        assertEquals(16.0, merged[0].fastHours, 0.0001)
        assertEquals(21.0, merged[0].totalFastHours, 0.0001)
        assertEquals(2, merged[0].sessionCount)
    }

    @Test fun `withCarriedWeight carries last recorded weight into weightless days`() {
        val wStart = WeightEntryEntity(id = 1, dayKey = day(2026, 5, 20), weightKg = 80.0, createdMs = 0)
        val fast21 = FastingSessionEntity(id = 1, startMs = day(2026, 5, 20) + 20 * 3_600_000L, endMs = day(2026, 5, 21) + 12 * 3_600_000L, goalHours = 16, planId = "16:8")
        val fast22 = FastingSessionEntity(id = 2, startMs = day(2026, 5, 21) + 20 * 3_600_000L, endMs = day(2026, 5, 22) + 12 * 3_600_000L, goalHours = 16, planId = "16:8")
        val wEnd = WeightEntryEntity(id = 2, dayKey = day(2026, 5, 23), weightKg = 79.0, createdMs = 0)

        val merged = DayEntries.merge(listOf(fast21, fast22), listOf(wStart, wEnd), utc)
        val carried = DayEntries.withCarriedWeight(merged).sortedBy { it.dayKey }

        assertEquals(4, carried.size)
        // 05-20: recorded 80.0
        assertEquals(80.0, carried[0].effectiveWeightKg!!, 0.0)
        assertTrue(carried[0].hasRecordedWeight)
        // 05-21 & 05-22: weightless fasting days carry 80.0 forward
        assertEquals(80.0, carried[1].effectiveWeightKg!!, 0.0)
        assertFalse(carried[1].hasRecordedWeight)
        assertEquals(80.0, carried[2].effectiveWeightKg!!, 0.0)
        assertFalse(carried[2].hasRecordedWeight)
        // 05-23: a new recording overrides the carry
        assertEquals(79.0, carried[3].effectiveWeightKg!!, 0.0)
        assertTrue(carried[3].hasRecordedWeight)
    }

    @Test fun `withCarriedWeight does not fill before the first recorded weight`() {
        val fastEarly = FastingSessionEntity(id = 1, startMs = day(2026, 5, 18) + 20 * 3_600_000L, endMs = day(2026, 5, 19) + 12 * 3_600_000L, goalHours = 16, planId = "16:8")
        val w = WeightEntryEntity(id = 1, dayKey = day(2026, 5, 20), weightKg = 80.0, createdMs = 0)

        val merged = DayEntries.merge(listOf(fastEarly), listOf(w), utc)
        val carried = DayEntries.withCarriedWeight(merged).sortedBy { it.dayKey }

        // 05-19 precedes every recording → nothing to carry
        assertNull(carried[0].effectiveWeightKg)
        assertFalse(carried[0].hasRecordedWeight)
        assertEquals(80.0, carried[1].effectiveWeightKg!!, 0.0)
    }

    @Test fun `single completed session`() {
        val s = FastingSessionEntity(id = 1, startMs = 0, endMs = 16 * 3_600_000L, goalHours = 16, planId = "16:8")
        val merged = DayEntries.merge(listOf(s), emptyList(), utc)
        assertEquals(16.0, merged[0].fastHours, 0.0001)
        assertNull(merged[0].weight)
    }
}
