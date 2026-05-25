package xyz.jishnu.health.data.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

sealed class RangeOption(val id: String, val label: String) {
    abstract fun range(today: LocalDate): ClosedRange<LocalDate>

    object Last7 : RangeOption("7d", "Last 7 days") {
        override fun range(today: LocalDate) = today.minusDays(6)..today
    }

    object Last14 : RangeOption("14d", "Last 14 days") {
        override fun range(today: LocalDate) = today.minusDays(13)..today
    }

    object Last30 : RangeOption("30d", "Last 30 days") {
        override fun range(today: LocalDate) = today.minusDays(29)..today
    }

    object Last90 : RangeOption("90d", "Last 90 days") {
        override fun range(today: LocalDate) = today.minusDays(89)..today
    }

    object ThisWeek : RangeOption("tw", "This week") {
        override fun range(today: LocalDate) =
            today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))..today
    }

    object ThisMonth : RangeOption("tm", "This month") {
        override fun range(today: LocalDate) = today.withDayOfMonth(1)..today
    }

    object YearToDate : RangeOption("ytd", "Year to date") {
        override fun range(today: LocalDate) = today.withDayOfYear(1)..today
    }

    data class Custom(val from: LocalDate, val to: LocalDate) : RangeOption("custom", "Custom") {
        override fun range(today: LocalDate) = from..to
    }

    companion object {
        val quickOptions: List<RangeOption> by lazy { listOf(Last7, Last14, Last30, Last90, ThisWeek, ThisMonth, YearToDate) }
        val default: RangeOption by lazy { Last14 }

        fun byId(id: String): RangeOption? = quickOptions.firstOrNull { it.id == id }

        fun dayCount(option: RangeOption, today: LocalDate): Long {
            val r = option.range(today)
            return ChronoUnit.DAYS.between(r.start, r.endInclusive) + 1
        }
    }
}
