package xyz.jishnu.health.data.repo

import kotlinx.coroutines.flow.Flow
import xyz.jishnu.health.data.local.FastingSessionEntity

interface FastingRepository {
    val activeSession: Flow<FastingSessionEntity?>
    val allSessions: Flow<List<FastingSessionEntity>>
    fun sessionsInRange(fromMs: Long, toMs: Long): Flow<List<FastingSessionEntity>>
    /** Sessions whose [endMs] falls inside the half-open range. Ongoing sessions are excluded. */
    fun sessionsEndingInRange(fromMs: Long, toMs: Long): Flow<List<FastingSessionEntity>>
    suspend fun sessionById(id: Long): FastingSessionEntity?

    suspend fun startFast(startMs: Long, goalHours: Int, planId: String): Long
    suspend fun endFast(id: Long, endMs: Long)
    suspend fun updateSession(session: FastingSessionEntity)
    suspend fun deleteSession(session: FastingSessionEntity)

    /**
     * Finish an active session by setting [endMs], OR delete it if the resulting
     * duration is below the minimum-qualifying threshold (Stage 2 / 4 hours).
     * Returns true if the session was retained, false if discarded.
     */
    suspend fun finishOrDiscard(session: FastingSessionEntity, endMs: Long): Boolean
}
