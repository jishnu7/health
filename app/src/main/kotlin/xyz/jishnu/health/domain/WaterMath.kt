package xyz.jishnu.health.domain

import xyz.jishnu.health.data.model.Units
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Water volumes are stored in milliliters end-to-end. This helper formats them
 * for the user's currently-displayed unit (ml or fl oz) and converts back when
 * the user enters a custom amount.
 *
 * Preset quick-add values stay in ml regardless of display unit — the UI shows
 * the converted label.
 */
object WaterMath {
    private const val ML_PER_FL_OZ = 29.5735

    fun mlToOz(ml: Int): Double = ml / ML_PER_FL_OZ
    fun ozToMl(oz: Double): Int = (oz * ML_PER_FL_OZ).roundToInt()

    data class FormattedVolume(val value: String, val unit: String)

    fun fmtVolume(ml: Int, units: Units): FormattedVolume = when (units) {
        Units.Metric -> FormattedVolume(ml.toString(), "ml")
        Units.Imperial -> FormattedVolume("%.1f".format(Locale.US, mlToOz(ml)), "fl oz")
    }

    /** Convert a number the user typed into the custom-amount field into ml. */
    fun typedToMl(value: Double, units: Units): Int = when (units) {
        Units.Metric -> value.roundToInt()
        Units.Imperial -> ozToMl(value)
    }
}

/** Preset quick-add volumes in milliliters, matching the prototype. */
enum class WaterPreset(val ml: Int, val label: String) {
    Glass(250, "Glass"),
    Cup(350, "Cup"),
    Bottle(500, "Bottle"),
    Flask(750, "Flask"),
}
