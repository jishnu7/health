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
    /**
     * Threshold (in hours) below which the "I ate" action discards the active session.
     * History always shows everything that's actually persisted — this constant is only
     * consulted when ending a fast early via "I ate".
     */
    const val MIN_QUALIFYING_HOURS = 4

    fun dayKeyFor(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val local = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
        return local.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    /**
     * Build a flat history list, one [DayEntry] per session, plus weight-only rows for
     * days with a weigh-in but no session. No duration filtering — whatever's in the DB
     * shows up. Sessions on the same day produce multiple rows.
     *
     * Sort: most-recent session first; weight-only rows sort by dayKey desc.
     * Weight attaches to the most-recent session of its day; weight-only days get
     * their own row with [DayEntry.session] = null.
     */
    fun merge(
        sessions: List<FastingSessionEntity>,
        weights: List<WeightEntryEntity>,
        zone: ZoneId = ZoneId.systemDefault(),
        nowMs: Long = System.currentTimeMillis(),
    ): List<DayEntry> {
        val weightByKey = weights.associateBy { it.dayKey }
        val sessionsByKey = sessions.groupBy { dayKeyFor(it.startMs, zone) }

        val allKeys = (sessionsByKey.keys + weightByKey.keys).toSortedSet(compareByDescending { it })
        val entries = mutableListOf<DayEntry>()

        for (key in allKeys) {
            val daySessions = sessionsByKey[key]?.sortedByDescending { it.startMs } ?: emptyList()
            val weight = weightByKey[key]
            val date = Instant.ofEpochMilli(key).atZone(zone).toLocalDate()

            if (daySessions.isEmpty()) {
                if (weight != null) {
                    entries += DayEntry(
                        dayKey = key,
                        date = date,
                        session = null,
                        weight = weight,
                        nowMs = nowMs,
                    )
                }
            } else {
                daySessions.forEachIndexed { idx, session ->
                    entries += DayEntry(
                        dayKey = key,
                        date = date,
                        session = session,
                        // Weight attaches to the most-recent session of the day only.
                        weight = if (idx == 0) weight else null,
                        nowMs = nowMs,
                    )
                }
            }
        }
        return entries
    }
}
