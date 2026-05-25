package xyz.jishnu.health.data.repo

import kotlinx.coroutines.flow.Flow
import xyz.jishnu.health.data.local.FastingSessionEntity

interface FastingRepository {
    val activeSession: Flow<FastingSessionEntity?>
    val allSessions: Flow<List<FastingSessionEntity>>
    fun sessionsInRange(fromMs: Long, toMs: Long): Flow<List<FastingSessionEntity>>

    suspend fun startFast(startMs: Long, goalHours: Int, planId: String): Long
    suspend fun endFast(id: Long, endMs: Long)
    suspend fun updateSession(session: FastingSessionEntity)
    suspend fun deleteSession(session: FastingSessionEntity)
}
