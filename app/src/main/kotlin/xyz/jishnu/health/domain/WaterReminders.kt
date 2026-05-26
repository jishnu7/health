package xyz.jishnu.health.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The day's water intake is paced across five windows. Each window contributes a
 * share of the daily goal, and we nudge the user at the end of the window only
 * if they haven't hit the cumulative target by then.
 */
data class WaterWindow(
    val index: Int,
    val startHour: Int,
    val endHour: Int,
    val sharePct: Int,
    val cumulativePct: Int,
) {
    fun label(): String = "${hour12(startHour)}–${hour12(endHour)}"

    private fun hour12(h: Int): String {
        val hh = ((h + 11) % 12) + 1
        val ampm = if (h < 12) "AM" else "PM"
        return "$hh $ampm"
    }
}

object WaterReminders {
    val windows: List<WaterWindow> = run {
        val raw = listOf(
            Triple(7, 10, 20),
            Triple(10, 13, 20),
            Triple(13, 16, 25),
            Triple(16, 19, 20),
            Triple(19, 22, 15),
        )
        var acc = 0
        raw.mapIndexed { i, (s, e, p) ->
            acc += p
            WaterWindow(index = i, startHour = s, endHour = e, sharePct = p, cumulativePct = acc)
        }
    }

    fun cumulativeTargetMl(windowIndex: Int, goalMl: Int): Int {
        val pct = windows.getOrNull(windowIndex)?.cumulativePct ?: return goalMl
        return (goalMl * pct / 100.0).toInt()
    }

    /**
     * Find the next end-of-window time after [nowMs]. If all of today's windows have
     * already passed, returns the first window of the following day.
     */
    fun nextTrigger(nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): NextTrigger {
        val today = LocalDate.now(zone)
        for (w in windows) {
            val candidateMs = today.atTime(LocalTime.of(w.endHour, 0)).atZone(zone).toInstant().toEpochMilli()
            if (candidateMs > nowMs) return NextTrigger(w.index, candidateMs)
        }
        val tomorrow = today.plusDays(1)
        val first = windows.first()
        val firstMs = tomorrow.atTime(LocalTime.of(first.endHour, 0)).atZone(zone).toInstant().toEpochMilli()
        return NextTrigger(first.index, firstMs)
    }

    data class NextTrigger(val windowIndex: Int, val triggerAtMs: Long)
}
