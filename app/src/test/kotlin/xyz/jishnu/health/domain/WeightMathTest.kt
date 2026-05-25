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

    @Test fun `fmtWeight imperial`() {
        val f = WeightMath.fmtWeight(178.4, Units.Imperial)
        assertEquals("178.4", f.value); assertEquals("lb", f.unit)
    }

    @Test fun `fmtWeight metric`() {
        val f = WeightMath.fmtWeight(178.4, Units.Metric)
        assertEquals("80.9", f.value); assertEquals("kg", f.unit)
    }
}
