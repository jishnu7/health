package xyz.jishnu.health.domain

import xyz.jishnu.health.data.model.Sex

/**
 * Ideal-weight estimators. Inputs and outputs are in metric (cm / kg);
 * imperial conversion belongs in the UI via [WeightMath].
 *
 * All four formulas use the same shape — `base + ratePerInch · (inches above 5 ft)` —
 * with different coefficients per sex. Encoding them via [IdealFormula] lets the
 * UI iterate over the full set without four near-identical functions.
 */
enum class IdealFormula(
    val label: String,
    val year: Int,
    private val maleBaseKg: Double,
    private val maleRateKgPerIn: Double,
    private val femaleBaseKg: Double,
    private val femaleRateKgPerIn: Double,
) {
    /** Hamwi (1964). Originally derived for IV drug dosing. */
    Hamwi("Hamwi", 1964, maleBaseKg = 48.0, maleRateKgPerIn = 2.7, femaleBaseKg = 45.5, femaleRateKgPerIn = 2.2),
    /** Devine (1974). The standard for drug dosing in modern clinical practice. */
    Devine("Devine", 1974, maleBaseKg = 50.0, maleRateKgPerIn = 2.3, femaleBaseKg = 45.5, femaleRateKgPerIn = 2.3),
    /** Robinson (1983). */
    Robinson("Robinson", 1983, maleBaseKg = 52.0, maleRateKgPerIn = 1.9, femaleBaseKg = 49.0, femaleRateKgPerIn = 1.7),
    /** Miller (1983). */
    Miller("Miller", 1983, maleBaseKg = 56.2, maleRateKgPerIn = 1.41, femaleBaseKg = 53.1, femaleRateKgPerIn = 1.36);

    fun calculate(heightCm: Double, sex: Sex): Double {
        val inchesOver5Ft = (heightCm - IdealWeight.BASE_HEIGHT_CM) / IdealWeight.CM_PER_INCH
        return when (sex) {
            Sex.Male -> maleBaseKg + maleRateKgPerIn * inchesOver5Ft
            Sex.Female -> femaleBaseKg + femaleRateKgPerIn * inchesOver5Ft
        }
    }
}

object IdealWeight {

    internal const val BASE_HEIGHT_CM = 152.4
    internal const val CM_PER_INCH = 2.54

    /** Convenience aliases so existing callers don't need to know about [IdealFormula]. */
    fun hamwi(heightCm: Double, sex: Sex) = IdealFormula.Hamwi.calculate(heightCm, sex)
    fun devine(heightCm: Double, sex: Sex) = IdealFormula.Devine.calculate(heightCm, sex)
    fun robinson(heightCm: Double, sex: Sex) = IdealFormula.Robinson.calculate(heightCm, sex)
    fun miller(heightCm: Double, sex: Sex) = IdealFormula.Miller.calculate(heightCm, sex)

    /**
     * WHO healthy BMI band (18.5 – 24.9) projected onto a weight range for a
     * given height. Returned as `(lowKg, highKg)`.
     */
    fun healthyBmiRange(heightCm: Double): ClosedFloatingPointRange<Double> {
        val heightM = heightCm / 100.0
        val low = 18.5 * heightM * heightM
        val high = 24.9 * heightM * heightM
        return low..high
    }

    fun bmi(weightKg: Double, heightCm: Double): Double {
        val heightM = heightCm / 100.0
        return weightKg / (heightM * heightM)
    }
}
