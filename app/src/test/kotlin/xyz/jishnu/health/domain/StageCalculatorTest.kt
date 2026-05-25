package xyz.jishnu.health.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StageCalculatorTest {
    @Test fun `fed at 0h`() = assertEquals("fed", StageCalculator.stageFor(0.0).id)
    @Test fun `fed just before early`() = assertEquals("fed", StageCalculator.stageFor(3.99).id)
    @Test fun `early at 4h`() = assertEquals("early", StageCalculator.stageFor(4.0).id)
    @Test fun `glycogen at 8h`() = assertEquals("glycogen", StageCalculator.stageFor(8.0).id)
    @Test fun `shift at 12h`() = assertEquals("shift", StageCalculator.stageFor(12.0).id)
    @Test fun `burn at 14h`() = assertEquals("burn", StageCalculator.stageFor(14.5).id)
    @Test fun `ketosis at 16h`() = assertEquals("ketosis", StageCalculator.stageFor(16.0).id)
    @Test fun `deep ketosis at 20h`() = assertEquals("deep", StageCalculator.stageFor(20.0).id)
    @Test fun `autophagy at 24h`() = assertEquals("autophagy", StageCalculator.stageFor(24.0).id)
    @Test fun `autophagy holds at 72h`() = assertEquals("autophagy", StageCalculator.stageFor(72.0).id)
    @Test fun `index for burn is 4`() = assertEquals(4, StageCalculator.indexFor(14.5))
}
