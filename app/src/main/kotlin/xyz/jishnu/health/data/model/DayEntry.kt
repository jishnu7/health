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
    val nowMs: Long = System.currentTimeMillis(),
) {
    val isOngoing: Boolean = session != null && session.endMs == null
    val fastHours: Double = session?.let {
        val end = it.endMs ?: nowMs
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
        nowMs: Long = System.currentTimeMillis(),
    ): List<DayEntry> {
        val byKey = sortedMapOf<Long, Pair<FastingSessionEntity?, WeightEntryEntity?>>()

        sessions.forEach { s ->
            val key = dayKeyFor(s.startMs, zone)
            byKey.merge(key, s to null) { existing, incoming ->
                pickBetterSession(existing.first, incoming.first) to existing.second
            }
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
                    nowMs = nowMs,
                )
            }
    }

    private fun pickBetterSession(
        a: FastingSessionEntity?,
        b: FastingSessionEntity?,
    ): FastingSessionEntity? = when {
        a == null -> b
        b == null -> a
        a.endMs == null -> a
        b.endMs == null -> b
        else -> if (a.startMs >= b.startMs) a else b
    }
}
