package xyz.jishnu.health.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.jishnu.health.data.model.Units

class WeightMathTest {
    @Test fun `lbToKg 178_4`() {
        assertEquals(80.92, WeightMath.lbToKg(178.4), 0.01)
    }

    @Test fun `roundtrip lb-kg-lb`() {
        val original = 165.5
        assertEquals(original, WeightMath.kgToLb(WeightMath.lbToKg(original)), 1e-9)
    }

    @Test fun `fmtWeight metric`() {
        val f = WeightMath.fmtWeight(80.9, Units.Metric)
        assertEquals("80.9", f.value); assertEquals("kg", f.unit)
    }

    @Test fun `fmtWeight imperial`() {
        val f = WeightMath.fmtWeight(80.9, Units.Imperial)
        assertEquals("178.4", f.value); assertEquals("lb", f.unit)
    }

    @Test fun `deltaToKg metric is identity`() {
        assertEquals(1.0, WeightMath.deltaToKg(1.0, Units.Metric), 1e-9)
    }

    @Test fun `deltaToKg imperial converts lb to kg`() {
        assertEquals(0.45359, WeightMath.deltaToKg(1.0, Units.Imperial), 1e-4)
    }
}
