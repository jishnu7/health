package xyz.jishnu.health.domain

import xyz.jishnu.health.data.local.WeightEntryEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * One week of the weight-trend chart. [carried] marks a week that had no
 * recording of its own and continues the previous week's average as a flat
 * point (min == max == avg).
 */
data class WeeklyStat(
    val weekStart: LocalDate,
    val minKg: Double,
    val maxKg: Double,
    val avgKg: Double,
    val carried: Boolean = false,
)

object WeightTrend {
    fun isoWeekStart(date: LocalDate): LocalDate =
        date.minus(((date.dayOfWeek.value - DayOfWeek.MONDAY.value) + 7) % 7L, ChronoUnit.DAYS)

    /**
     * Bucket recorded weights into ISO weeks (min/max/avg per week), then carry
     * the previous week's average across interior empty weeks so a gap reads as
     * a flat line at the last known weight rather than collapsing out. No weeks
     * are synthesized before the first or after the last recording.
     */
    fun buildWeekly(
        entries: List<WeightEntryEntity>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<WeeklyStat> {
        if (entries.isEmpty()) return emptyList()
        val withDates = entries.map { entry ->
            val date = Instant.ofEpochMilli(entry.dayKey).atZone(zone).toLocalDate()
            date to entry.weightKg
        }.sortedBy { it.first }
        val groups = withDates.groupBy { (date, _) -> isoWeekStart(date) }.toSortedMap()
        val recorded = groups.map { (weekStart, list) ->
            val kgs = list.map { it.second }
            WeeklyStat(
                weekStart = weekStart,
                minKg = kgs.min(),
                maxKg = kgs.max(),
                avgKg = kgs.average(),
            )
        }
        val filled = mutableListOf<WeeklyStat>()
        for ((i, week) in recorded.withIndex()) {
            if (i > 0) {
                val prevAvg = recorded[i - 1].avgKg
                var cursor = recorded[i - 1].weekStart.plusWeeks(1)
                while (cursor.isBefore(week.weekStart)) {
                    filled += WeeklyStat(
                        weekStart = cursor,
                        minKg = prevAvg,
                        maxKg = prevAvg,
                        avgKg = prevAvg,
                        carried = true,
                    )
                    cursor = cursor.plusWeeks(1)
                }
            }
            filled += week
        }
        return filled
    }
}
