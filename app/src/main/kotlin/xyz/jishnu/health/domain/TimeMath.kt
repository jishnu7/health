package xyz.jishnu.health.domain

import xyz.jishnu.health.data.model.Units
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

data class HmsDuration(
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
) {
    val hh: String get() = hours.toString().padStart(2, '0')
    val mm: String get() = minutes.toString().padStart(2, '0')
    val ss: String get() = seconds.toString().padStart(2, '0')
}

object TimeMath {
    fun fmtDuration(ms: Long): HmsDuration {
        val totalSec = max(0L, ms / 1000L)
        val h = (totalSec / 3600L).toInt()
        val m = ((totalSec % 3600L) / 60L).toInt()
        val s = (totalSec % 60L).toInt()
        return HmsDuration(h, m, s)
    }

    fun fmtTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
        val local = instant.atZone(zone).toLocalTime()
        return DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).format(local)
    }

    fun fmtDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("EEE, MMM d", locale).format(date)

    fun addHoursToTime(hhmm: String, hours: Double): String {
        val (h, m) = hhmm.split(":").map { it.toInt() }
        val totalMinutes = h * 60 + m + (hours * 60).roundToInt()
        val wrapped = ((totalMinutes % (24 * 60)) + 24 * 60) % (24 * 60)
        val oh = wrapped / 60
        val om = wrapped % 60
        return "${oh.toString().padStart(2, '0')}:${om.toString().padStart(2, '0')}"
    }

    fun diffHoursTime(start: String, end: String): Double {
        val (sh, sm) = start.split(":").map { it.toInt() }
        val (eh, em) = end.split(":").map { it.toInt() }
        var total = (eh * 60 + em) - (sh * 60 + sm)
        if (total <= 0) total += 24 * 60
        return total / 60.0
    }

    fun parseTime(hhmm: String): LocalTime {
        val (h, m) = hhmm.split(":").map { it.toInt() }
        return LocalTime.of(h, m)
    }
}

object WeightMath {
    private const val KG_PER_LB = 0.45359237
    fun lbToKg(lb: Double): Double = lb * KG_PER_LB
    fun kgToLb(kg: Double): Double = kg / KG_PER_LB

    data class FormattedWeight(val value: String, val unit: String)

    /** Storage is always kg; this formats it for the requested display unit. */
    fun fmtWeight(weightKg: Double, units: Units): FormattedWeight = when (units) {
        Units.Metric -> FormattedWeight("%.1f".format(Locale.US, weightKg), "kg")
        Units.Imperial -> FormattedWeight("%.1f".format(Locale.US, kgToLb(weightKg)), "lb")
    }

    /** Convert a delta expressed in the user's currently-displayed unit into kg. */
    fun deltaToKg(deltaInDisplayUnit: Double, units: Units): Double = when (units) {
        Units.Metric -> deltaInDisplayUnit
        Units.Imperial -> lbToKg(deltaInDisplayUnit)
    }
}
