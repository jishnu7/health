package xyz.jishnu.health.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.jishnu.health.data.local.FastingSessionEntity
import xyz.jishnu.health.data.local.WeightEntryEntity
import xyz.jishnu.health.data.model.DayEntries
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.data.repo.FastingRepository
import xyz.jishnu.health.data.repo.SettingsRepository
import xyz.jishnu.health.data.repo.WeightRepository
import xyz.jishnu.health.domain.TimeMath
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class DayDetailUiState(
    val loaded: Boolean = false,
    val dayKey: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val startTime: String = "20:00",
    val endTime: String = "12:00",
    val weightLb: Double = 150.0,
    val previousWeightLb: Double? = null,
    val notes: String = "",
    val units: Units = Units.Imperial,
    val goalHours: Int = 16,
    val sessionId: Long? = null,
    val weightId: Long? = null,
) {
    val durationHours: Double = TimeMath.diffHoursTime(startTime, endTime)
    val goalMet: Boolean = durationHours >= goalHours
}

@HiltViewModel
class DayDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val fastingRepo: FastingRepository,
    private val weightRepo: WeightRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private val dayKey: Long = savedState["dayKey"] ?: System.currentTimeMillis()

    private val _state = MutableStateFlow(DayDetailUiState(dayKey = dayKey))
    val state: StateFlow<DayDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val settings = settingsRepo.settings.first()
        val plan = xyz.jishnu.health.data.constants.Plans.byId(settings.planId)
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(dayKey).atZone(zone).toLocalDate()

        val dayStart = dayKey
        val dayEnd = dayKey + 86_400_000L
        val sessions = fastingRepo.sessionsInRange(dayStart, dayEnd).first()
        val session = sessions.firstOrNull()
        val weight = weightRepo.findByDay(dayKey)

        val previousWeight = weightRepo.findByDay(dayKey - 86_400_000L)?.weightLb

        val (startStr, endStr) = if (session != null) {
            val startLt = Instant.ofEpochMilli(session.startMs).atZone(zone).toLocalTime()
            val endLt = (session.endMs ?: (session.startMs + session.goalHours * 3_600_000L))
                .let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime() }
            fmtLt(startLt) to fmtLt(endLt)
        } else {
            settings.fastStartTime to TimeMath.addHoursToTime(settings.fastStartTime, plan.fastHours.toDouble())
        }

        _state.value = DayDetailUiState(
            loaded = true,
            dayKey = dayKey,
            date = date,
            startTime = startStr,
            endTime = endStr,
            weightLb = weight?.weightLb ?: previousWeight ?: 150.0,
            previousWeightLb = previousWeight,
            notes = weight?.notes ?: session?.note ?: "",
            units = settings.units,
            goalHours = session?.goalHours ?: plan.fastHours,
            sessionId = session?.id,
            weightId = weight?.id,
        )
    }

    fun setStart(hhmm: String) { _state.update { it.copy(startTime = hhmm) } }
    fun setEnd(hhmm: String) { _state.update { it.copy(endTime = hhmm) } }
    fun setNotes(value: String) { _state.update { it.copy(notes = value) } }
    fun setWeightLb(lb: Double) { _state.update { it.copy(weightLb = lb) } }
    fun bumpWeight(deltaLb: Double) { _state.update { it.copy(weightLb = it.weightLb + deltaLb) } }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val s = _state.value
        val zone = ZoneId.systemDefault()
        val startLt = TimeMath.parseTime(s.startTime)
        val endLt = TimeMath.parseTime(s.endTime)
        val startInstant = s.date.atTime(startLt).atZone(zone).toInstant().toEpochMilli()
        val durationHours = TimeMath.diffHoursTime(s.startTime, s.endTime)
        val endInstant = startInstant + (durationHours * 3_600_000.0).toLong()

        val sessionId = s.sessionId
        if (sessionId != null) {
            val existing = fastingRepo.sessionsInRange(s.dayKey, s.dayKey + 86_400_000L).first()
                .firstOrNull { it.id == sessionId }
            if (existing != null) {
                fastingRepo.updateSession(
                    existing.copy(
                        startMs = startInstant,
                        endMs = endInstant,
                        note = s.notes.ifBlank { null },
                    )
                )
            }
        } else {
            fastingRepo.startFast(startInstant, s.goalHours, "16:8").also { newId ->
                val inserted = fastingRepo.sessionsInRange(s.dayKey, s.dayKey + 86_400_000L).first()
                    .firstOrNull { it.id == newId }
                inserted?.let { fastingRepo.updateSession(it.copy(endMs = endInstant, note = s.notes.ifBlank { null })) }
            }
        }
        weightRepo.upsertForDay(s.dayKey, s.weightLb, s.notes.ifBlank { null })
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        val s = _state.value
        s.sessionId?.let { id ->
            val existing = fastingRepo.sessionsInRange(s.dayKey, s.dayKey + 86_400_000L).first()
                .firstOrNull { it.id == id }
            existing?.let { fastingRepo.deleteSession(it) }
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
