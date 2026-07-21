package xyz.jishnu.health.domain

import xyz.jishnu.health.data.local.FastingSessionEntity
import xyz.jishnu.health.data.model.DayEntries
import java.time.LocalDate
import java.time.ZoneId

/** One day's cell. [level]: 0 nothing (no fast or under 12h), 1 short, 2 goal met, 3 exceeded goal. */
data class CalendarDay(
    val dayKey: Long,
    val date: LocalDate,
    val fastHours: Double,
    val level: Int,
)

/** A month name to draw above the week column at [weekIndex]. */
data class MonthLabel(val weekIndex: Int, val text: String)

/**
 * A year of fasting activity as GitHub-style week columns. Each column holds 7
 * entries (Mon..Sun); a null entry is padding for days after today in the final
 * (partial) week.
 */
data class FastingCalendar(
    val weeks: List<List<CalendarDay?>>,
    val monthLabels: List<MonthLabel>,
    val daysFasted: Int,
    val goalMetDays: Int,
    val longestStreak: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    companion object {
        val EMPTY = FastingCalendar(emptyList(), emptyList(), 0, 0, 0, LocalDate.MIN, LocalDate.MIN)
    }
}

object FastingCalendarBuilder {
    private val MONTHS = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )

    /** A fast shorter than this (hours) doesn't register on the calendar. */
    const val MIN_FAST_HOURS = 12.0

    /**
     * Cell intensity: 0 nothing (no fast, or under [MIN_FAST_HOURS]); 1 short
     * (>= 12h but under goal); 2 goal met ([goal, goal+1h)); 3 exceeded
     * (>= goal + 1h). Met and exceeded are split so a long fast reads darker
     * than one that just reached the goal.
     */
    fun level(fastHours: Double, goalHours: Int): Int {
        if (fastHours < MIN_FAST_HOURS) return 0
        if (goalHours <= 0) return 3
        val goal = goalHours.toDouble()
        return when {
            fastHours >= goal + 1.0 -> 3
            fastHours >= goal -> 2
            else -> 1
        }
    }

    fun build(
        sessions: List<FastingSessionEntity>,
        goalHours: Int,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        weeksBack: Int = 52,
        nowMs: Long = System.currentTimeMillis(),
    ): FastingCalendar {
        // Longest fasting session ending on each day — reuse the tested merge.
        val hoursByKey: Map<Long, Double> = DayEntries.merge(sessions, emptyList(), zone, nowMs)
            .associate { it.dayKey to it.fastHours }

        val start = WeightTrend.isoWeekStart(today.minusWeeks(weeksBack.toLong()))
        val weeks = mutableListOf<List<CalendarDay?>>()
        val monthLabels = mutableListOf<MonthLabel>()
        var cursor = start
        var weekIndex = 0
        var lastLabeledMonth = -1
        var daysFasted = 0
        var goalMetDays = 0
        var streak = 0
        var longest = 0

        while (!cursor.isAfter(today)) {
            if (cursor.monthValue != lastLabeledMonth) {
                monthLabels += MonthLabel(weekIndex, MONTHS[cursor.monthValue - 1])
                lastLabeledMonth = cursor.monthValue
            }
            val column = ArrayList<CalendarDay?>(7)
            for (d in 0 until 7) {
                val date = cursor.plusDays(d.toLong())
                if (date.isAfter(today)) {
                    column.add(null)
                    continue
                }
                val dayKey = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val hours = hoursByKey[dayKey] ?: 0.0
                val lvl = level(hours, goalHours)
                column.add(CalendarDay(dayKey, date, hours, lvl))
                // A day only counts as fasted once it clears the 12h floor (level >= 1).
                if (lvl >= 1) daysFasted++
                // Reaching the goal (met or exceeded) counts toward the stats.
                if (lvl >= 2) {
                    goalMetDays++
                    streak++
                    longest = maxOf(longest, streak)
                } else {
                    streak = 0
                }
            }
            weeks.add(column)
            cursor = cursor.plusWeeks(1)
            weekIndex++
        }
        return FastingCalendar(weeks, monthLabels, daysFasted, goalMetDays, longest, start, today)
    }
}
