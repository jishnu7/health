package xyz.jishnu.health.data.model

import xyz.jishnu.health.data.local.FastingSessionEntity
import xyz.jishnu.health.data.local.WeightEntryEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayEntry(
    val dayKey: Long,
    val date: LocalDate,
    val session: FastingSessionEntity?,
    val weight: WeightEntryEntity?,
) {
    val fastHours: Double = session?.let {
        val end = it.endMs ?: it.startMs
        ((end - it.startMs).coerceAtLeast(0L)) / 3_600_000.0
    } ?: 0.0
}

object DayEntries {
    fun dayKeyFor(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val local = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
        return local.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun merge(
        sessions: List<FastingSessionEntity>,
        weights: List<WeightEntryEntity>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<DayEntry> {
        val byKey = sortedMapOf<Long, Pair<FastingSessionEntity?, WeightEntryEntity?>>()

        sessions.forEach { s ->
            val key = dayKeyFor(s.startMs, zone)
            byKey.merge(key, s to null) { a, b -> (b.first ?: a.first) to a.second }
        }
        weights.forEach { w ->
            byKey.merge(w.dayKey, null to w) { a, b -> a.first to (b.second ?: a.second) }
        }

        return byKey.entries
            .sortedByDescending { it.key }
            .map { (key, sw) ->
                DayEntry(
                    dayKey = key,
                    date = Instant.ofEpochMilli(key).atZone(zone).toLocalDate(),
                    session = sw.first,
                    weight = sw.second,
                )
            }
    }
}
