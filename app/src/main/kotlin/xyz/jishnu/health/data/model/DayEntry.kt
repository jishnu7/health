package xyz.jishnu.health.data.model

import xyz.jishnu.health.data.local.FastingSessionEntity
import xyz.jishnu.health.data.local.WeightEntryEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * One per calendar day. Aggregates every fasting session that started on that day
 * (so the History list and chart show a single row/point per date), plus that day's
 * weight entry. Underlying multi-session data is preserved — DayDetail can still
 * navigate to a specific session via `sessionId`.
 */
data class DayEntry(
    val dayKey: Long,
    val date: LocalDate,
    val sessions: List<FastingSessionEntity>,
    val weight: WeightEntryEntity?,
    val nowMs: Long = System.currentTimeMillis(),
) {
    val ongoingSession: FastingSessionEntity? = sessions.firstOrNull { it.endMs == null }
    val isOngoing: Boolean = ongoingSession != null

    /** The session to open by default when the user taps the row: ongoing first, else the longest. */
    val primarySession: FastingSessionEntity? = ongoingSession
        ?: sessions.maxByOrNull { (it.endMs ?: nowMs) - it.startMs }

    /** Longest single session for the day — used as the headline duration in History and as the chart's Y value. */
    val fastHours: Double = sessions.maxOfOrNull { s -> sessionHours(s) } ?: 0.0

    /** Sum across every session for the day — used for the "Total fasted" stat. */
    val totalFastHours: Double = sessions.sumOf { s -> sessionHours(s) }

    val sessionCount: Int = sessions.size

    private fun sessionHours(s: FastingSessionEntity): Double {
        val end = s.endMs ?: nowMs
        return ((end - s.startMs).coerceAtLeast(0L)) / 3_600_000.0
    }
}

object DayEntries {
    /**
     * Threshold (in hours) below which the "I ate" action discards the active session.
     * History always shows everything that's actually persisted.
     */
    const val MIN_QUALIFYING_HOURS = 4

    fun dayKeyFor(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val local = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
        return local.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    /**
     * Build a per-day history list. Every session that started on a given local day
     * is grouped into a single [DayEntry] for that day, sorted most-recent first.
     * Days with only a weight entry get a session-less [DayEntry].
     */
    fun merge(
        sessions: List<FastingSessionEntity>,
        weights: List<WeightEntryEntity>,
        zone: ZoneId = ZoneId.systemDefault(),
        nowMs: Long = System.currentTimeMillis(),
    ): List<DayEntry> {
        val weightByKey = weights.associateBy { it.dayKey }
        val sessionsByKey = sessions.groupBy { dayKeyFor(it.startMs, zone) }

        val allKeys = (sessionsByKey.keys + weightByKey.keys)
            .toSortedSet(compareByDescending { it })
        val entries = mutableListOf<DayEntry>()

        for (key in allKeys) {
            val daySessions = (sessionsByKey[key] ?: emptyList())
                .sortedByDescending { it.startMs }
            val weight = weightByKey[key]
            if (daySessions.isEmpty() && weight == null) continue
            entries += DayEntry(
                dayKey = key,
                date = Instant.ofEpochMilli(key).atZone(zone).toLocalDate(),
                sessions = daySessions,
                weight = weight,
                nowMs = nowMs,
            )
        }
        return entries
    }
}
