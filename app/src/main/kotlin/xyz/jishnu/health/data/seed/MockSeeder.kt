package xyz.jishnu.health.data.seed

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.jishnu.health.data.local.FastingSessionDao
import xyz.jishnu.health.data.local.FastingSessionEntity
import xyz.jishnu.health.data.local.WeightEntryDao
import xyz.jishnu.health.data.local.WeightEntryEntity
import xyz.jishnu.health.data.model.DayEntries
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.sin

@Singleton
class MockSeeder @Inject constructor(
    private val sessionDao: FastingSessionDao,
    private val weightDao: WeightEntryDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun seedIfEmpty() {
        scope.launch { runSeed() }
    }

    private suspend fun runSeed() {
        val anySessions = sessionDao.observeAll().first()
        val anyWeights = weightDao.observeAll().first()
        if (anySessions.isNotEmpty() || anyWeights.isNotEmpty()) return

        val todayKey = DayEntries.dayKeyFor(System.currentTimeMillis())
        val days = 14
        var seed = 42L
        fun rnd(): Double {
            seed = (seed * 9301 + 49297) % 233280
            return seed / 233280.0
        }

        for (i in days - 1 downTo 0) {
            val dayKey = todayKey - i * 86_400_000L
            val baseFast = 15.0 + sin(i / 2.0) * 2.0 + rnd() * 1.5
            val fastHours = max(0.0, baseFast)
            val weightLb = 178.4 - i * 0.18 + (rnd() - 0.5) * 1.2

            val startMs = dayKey + 8 * 3_600_000L
            val endMs = startMs + (fastHours * 3_600_000.0).toLong()
            sessionDao.insert(
                FastingSessionEntity(
                    startMs = startMs,
                    endMs = endMs,
                    goalHours = 16,
                    planId = "16:8",
                )
            )
            weightDao.insert(
                WeightEntryEntity(
                    dayKey = dayKey,
                    weightLb = weightLb,
                    createdMs = startMs,
                )
            )
        }
    }
}
