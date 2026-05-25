package xyz.jishnu.health.data.repo

import kotlinx.coroutines.flow.Flow
import xyz.jishnu.health.data.local.FastingSessionDao
import xyz.jishnu.health.data.local.FastingSessionEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalFastingRepository @Inject constructor(
    private val dao: FastingSessionDao,
) : FastingRepository {
    override val activeSession: Flow<FastingSessionEntity?> = dao.observeActive()
    override val allSessions: Flow<List<FastingSessionEntity>> = dao.observeAll()
    override fun sessionsInRange(fromMs: Long, toMs: Long) = dao.observeRange(fromMs, toMs)

    override suspend fun startFast(startMs: Long, goalHours: Int, planId: String): Long =
        dao.insert(
            FastingSessionEntity(
                startMs = startMs,
                endMs = null,
                goalHours = goalHours,
                planId = planId,
            )
        )

    override suspend fun endFast(id: Long, endMs: Long) = dao.setEnd(id, endMs)
    override suspend fun updateSession(session: FastingSessionEntity) = dao.update(session)
    override suspend fun deleteSession(session: FastingSessionEntity) = dao.delete(session)
}
