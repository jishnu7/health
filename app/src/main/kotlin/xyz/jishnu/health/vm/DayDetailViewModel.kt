package xyz.jishnu.health.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.jishnu.health.data.local.FastingSessionEntity
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.data.repo.FastingRepository
import xyz.jishnu.health.data.repo.SettingsRepository
import xyz.jishnu.health.data.repo.WaterRepository
import xyz.jishnu.health.data.repo.WeightRepository
import xyz.jishnu.health.domain.TimeMath
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.max

data class DayDetailUiState(
    val loaded: Boolean = false,
    val dayKey: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val startTime: String = "20:00",
    val endTime: String? = "12:00",
    val weightKg: Double = 70.0,
    val previousWeightKg: Double? = null,
    val notes: String = "",
    val units: Units = Units.Metric,
    val goalHours: Int = 16,
    val sessionId: Long? = null,
    val weightId: Long? = null,
    val isOngoing: Boolean = false,
    val nowMs: Long = System.currentTimeMillis(),
    val zone: ZoneId = ZoneId.systemDefault(),
    val daySessions: List<FastingSessionEntity> = emptyList(),
    val waterMl: Int = 0,
    val waterGoalMl: Int = 2500,
) {
    private val startInstantMs: Long
        get() = date.atTime(TimeMath.parseTime(startTime)).atZone(zone).toInstant().toEpochMilli()

    val durationHours: Double = if (endTime != null) {
        TimeMath.diffHoursTime(startTime, endTime)
    } else {
        max(0L, nowMs - startInstantMs) / 3_600_000.0
    }

    val displayedEndTime: String? = if (endTime != null) endTime else {
        val end = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalTime()
        fmtLt(end)
    }

    val goalMet: Boolean = durationHours >= goalHours
}

@HiltViewModel
class DayDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val fastingRepo: FastingRepository,
    private val weightRepo: WeightRepository,
    private val waterRepo: WaterRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private val dayKey: Long = savedState["dayKey"] ?: System.currentTimeMillis()
    // -1 (or absent) means "no specific session — load by day"; otherwise load that session.
    private val targetSessionId: Long? = (savedState["sessionId"] as? Long)?.takeIf { it > 0L }

    private val _state = MutableStateFlow(DayDetailUiState(dayKey = dayKey))
    val state: StateFlow<DayDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { load() }
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                val s = _state.value
                if (s.isOngoing && s.endTime == null) {
                    _state.value = s.copy(nowMs = System.currentTimeMillis())
                }
            }
        }
    }

    private suspend fun load() {
        val settings = settingsRepo.settings.first()
        val plan = xyz.jishnu.health.data.constants.Plans.byId(settings.planId)
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(dayKey).atZone(zone).toLocalDate()

        val dayStart = dayKey
        val dayEnd = dayKey + 86_400_000L
        // Sessions "belong" to the day they end on. Ongoing sessions count as
        // today's day, so include the active session when this is today's row.
        val ended = fastingRepo.sessionsEndingInRange(dayStart, dayEnd).first()
        val nowMs = System.currentTimeMillis()
        val ongoing = fastingRepo.activeSession.first()
            ?.takeIf { it.endMs == null && nowMs in dayStart until dayEnd }
        val sessions = (ended + listOfNotNull(ongoing))
            .distinctBy { it.id }
            .sortedByDescending { it.startMs }
        // Prefer the explicitly-navigated session; otherwise fall back to ongoing > longest.
        val session = targetSessionId?.let { id -> sessions.firstOrNull { it.id == id } }
            ?: sessions.firstOrNull { it.endMs == null }
            ?: sessions.maxByOrNull { (it.endMs ?: System.currentTimeMillis()) - it.startMs }
        val weight = weightRepo.findByDay(dayKey)
        val waterMl = waterRepo.totalInRange(dayStart, dayEnd).first()

        val previousWeight = weightRepo.findByDay(dayKey - 86_400_000L)?.weightKg

        val startStr: String
        val endStr: String?
        val isOngoing: Boolean

        if (session != null) {
            val startLt = Instant.ofEpochMilli(session.startMs).atZone(zone).toLocalTime()
            startStr = fmtLt(startLt)
            if (session.endMs != null) {
                val endLt = Instant.ofEpochMilli(session.endMs).atZone(zone).toLocalTime()
                endStr = fmtLt(endLt)
                isOngoing = false
            } else {
                endStr = null
                isOngoing = true
            }
        } else {
            startStr = settings.fastStartTime
            endStr = TimeMath.addHoursToTime(settings.fastStartTime, plan.fastHours.toDouble())
            isOngoing = false
        }

        _state.value = DayDetailUiState(
            loaded = true,
            dayKey = dayKey,
            date = date,
            startTime = startStr,
            endTime = endStr,
            weightKg = weight?.weightKg ?: previousWeight ?: 70.0,
            previousWeightKg = previousWeight,
            notes = weight?.notes ?: session?.note ?: "",
            units = settings.units,
            goalHours = session?.goalHours ?: plan.fastHours,
            sessionId = session?.id,
            weightId = weight?.id,
            isOngoing = isOngoing,
            nowMs = System.currentTimeMillis(),
            zone = zone,
            daySessions = sessions,
            waterMl = waterMl,
            waterGoalMl = settings.waterGoalMl,
        )
    }

    fun setStart(hhmm: String) { _state.update { it.copy(startTime = hhmm) } }

    fun setEnd(hhmm: String) {
        _state.update { it.copy(endTime = hhmm, isOngoing = false) }
    }

    fun setNotes(value: String) { _state.update { it.copy(notes = value) } }
    fun setWeightKg(kg: Double) { _state.update { it.copy(weightKg = kg) } }
    fun bumpWeightKg(deltaKg: Double) { _state.update { it.copy(weightKg = it.weightKg + deltaKg) } }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val s = _state.value
        val zone = s.zone
        val startLt = TimeMath.parseTime(s.startTime)
        val startInstant = s.date.atTime(startLt).atZone(zone).toInstant().toEpochMilli()
        val endInstant: Long? = s.endTime?.let { endStr ->
            val durationHours = TimeMath.diffHoursTime(s.startTime, endStr)
            startInstant + (durationHours * 3_600_000.0).toLong()
        }

        val sessionId = s.sessionId
        if (sessionId != null) {
            val existing = fastingRepo.sessionById(sessionId)
            if (existing != null) {
                fastingRepo.updateSession(
                    existing.copy(
                        startMs = startInstant,
                        endMs = endInstant,
                        note = s.notes.ifBlank { null },
                    )
                )
            }
        } else if (endInstant != null) {
            fastingRepo.startFast(startInstant, s.goalHours, "16:8").also { newId ->
                val inserted = fastingRepo.sessionById(newId)
                inserted?.let { fastingRepo.updateSession(it.copy(endMs = endInstant, note = s.notes.ifBlank { null })) }
            }
        }
        weightRepo.upsertForDay(s.dayKey, s.weightKg, s.notes.ifBlank { null })
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        val s = _state.value
        s.sessionId?.let { id ->
            fastingRepo.sessionById(id)?.let { fastingRepo.deleteSession(it) }
        }
        s.weightId?.let { id ->
            val existing = weightRepo.findByDay(s.dayKey)
            if (existing?.id == id) weightRepo.delete(existing)
        }
        onDone()
    }
}

private fun MutableStateFlow<DayDetailUiState>.update(block: (DayDetailUiState) -> DayDetailUiState) {
    value = block(value)
}

private fun fmtLt(lt: LocalTime): String =
    "${lt.hour.toString().padStart(2, '0')}:${lt.minute.toString().padStart(2, '0')}"
