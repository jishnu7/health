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
    /** Null when the user hasn't logged a session for this day yet. */
    val startTime: String? = null,
    val endTime: String? = null,
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
    /**
     * Calendar day the start instant lives on. For overnight ongoing fasts
     * (started yesterday, still running today) this is the previous day; the
     * dayKey ([date]) is the end day. Both are needed so the duration
     * calculation in this VM matches what [FastingViewModel] shows on Home.
     */
    val startDate: LocalDate? = null,
    /**
     * Calendar day the end instant lives on. Defaults to the dayKey's [date]
     * (end-day bucketing) but the user can override it for multi-day fasts
     * where the heuristic gets it wrong.
     */
    val endDate: LocalDate? = null,
    val otherActiveSession: Boolean = false,
) {
    private val startInstantMs: Long?
        get() = startTime?.let {
            (startDate ?: date).atTime(TimeMath.parseTime(it)).atZone(zone).toInstant().toEpochMilli()
        }

    private val endInstantMs: Long?
        get() = endTime?.let {
            (endDate ?: date).atTime(TimeMath.parseTime(it)).atZone(zone).toInstant().toEpochMilli()
        }

    val durationHours: Double = when {
        startTime == null -> 0.0
        endTime != null -> {
            val sMs = startInstantMs
            val eMs = endInstantMs
            if (sMs != null && eMs != null) {
                max(0L, eMs - sMs) / 3_600_000.0
            } else {
                TimeMath.diffHoursTime(startTime, endTime)
            }
        }
        isOngoing -> startInstantMs?.let { max(0L, nowMs - it) / 3_600_000.0 } ?: 0.0
        else -> 0.0
    }

    val displayedEndTime: String? = when {
        endTime != null -> endTime
        isOngoing -> {
            val end = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalTime()
            fmtLt(end)
        }
        else -> null
    }

    val hasSession: Boolean = startTime != null || sessionId != null

    val goalMet: Boolean = hasSession && durationHours >= goalHours

    val isToday: Boolean = date == LocalDate.now(zone)

    /**
     * True when the user is looking at today's row, the loaded session is
     * completed (has an endMs) and there is no other fast currently running.
     * The Resume button uses this to expose an "undo End Fast" affordance.
     */
    val canResume: Boolean = isToday && hasSession && !isOngoing && sessionId != null && !otherActiveSession
}

@HiltViewModel
class DayDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val fastingRepo: FastingRepository,
    private val weightRepo: WeightRepository,
    private val waterRepo: WaterRepository,
    private val settingsRepo: SettingsRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
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
        val activeSession = fastingRepo.activeSession.first()
        val ongoing = activeSession?.takeIf { it.endMs == null && nowMs in dayStart until dayEnd }
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

        // When there's no session for this day, leave the fields blank so the
        // UI reads as "no data" instead of suggesting fake defaults that the
        // user might accidentally save.
        val startStr: String?
        val endStr: String?
        val isOngoing: Boolean
        val startDate: LocalDate?
        val endDate: LocalDate?

        if (session != null) {
            val startZdt = Instant.ofEpochMilli(session.startMs).atZone(zone)
            startStr = fmtLt(startZdt.toLocalTime())
            startDate = startZdt.toLocalDate()
            if (session.endMs != null) {
                val endZdt = Instant.ofEpochMilli(session.endMs).atZone(zone)
                endStr = fmtLt(endZdt.toLocalTime())
                endDate = endZdt.toLocalDate()
                isOngoing = false
            } else {
                endStr = null
                endDate = null
                isOngoing = true
            }
        } else {
            startStr = null
            endStr = null
            isOngoing = false
            startDate = null
            endDate = null
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
            startDate = startDate,
            endDate = endDate,
            // An active session counts as "other" only if it's not the one we
            // just loaded. The Resume button uses this to refuse when a
            // different fast is already running.
            otherActiveSession = activeSession != null && activeSession.id != session?.id,
        )
    }

    fun setStart(hhmm: String) {
        _state.update { s ->
            // First time the user picks a start, anchor the start to dayKey's
            // date. For existing sessions we keep whatever startDate load()
            // captured so an overnight ongoing fast doesn't "jump" forward.
            val date = s.startDate ?: s.date
            s.copy(startTime = hhmm, startDate = date)
        }
    }

    fun setEnd(hhmm: String) {
        _state.update { s ->
            // Mirror setStart: anchor a brand-new end to the dayKey's date.
            // Existing sessions keep whatever endDate load() captured.
            val endDate = s.endDate ?: s.date
            s.copy(endTime = hhmm, endDate = endDate, isOngoing = false)
        }
    }

    fun setStartDate(date: LocalDate) {
        _state.update { it.copy(startDate = date) }
    }

    fun setEndDate(date: LocalDate) {
        _state.update { it.copy(endDate = date) }
    }

    fun setNotes(value: String) { _state.update { it.copy(notes = value) } }
    fun setWeightKg(kg: Double) { _state.update { it.copy(weightKg = kg) } }
    fun bumpWeightKg(deltaKg: Double) { _state.update { it.copy(weightKg = it.weightKg + deltaKg) } }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val s = _state.value
        val zone = s.zone

        // Only touch the fasting tables if the user has actually entered a
        // start time. A blank row should never silently create a session.
        val startStr = s.startTime
        if (startStr != null) {
            val startLt = TimeMath.parseTime(startStr)
            val endLt = s.endTime?.let { TimeMath.parseTime(it) }
            val sessionId = s.sessionId
            val existing = sessionId?.let { fastingRepo.sessionById(it) }
            // s.date is the dayKey's date — which under end-date bucketing is the
            // day the session ENDS on, not necessarily the day it started.
            //   - completed + overnight (startHH > endHH): start was the day before
            //   - completed + same-day: start is on s.date
            //   - ongoing: keep the existing session's start date (we have no way
            //     to pick a date when there's no end), just swap the HH:mm portion
            // Prefer the start date carried in state (set by load() from the
            // session's real startMs, or by setStart() when the user picks one
            // for a brand-new entry). Fall back to inferring overnight from
            // HH:mm wraparound when state didn't carry a date.
            val startInstant: Long = when {
                s.startDate != null ->
                    s.startDate.atTime(startLt).atZone(zone).toInstant().toEpochMilli()
                endLt != null && startLt > endLt ->
                    s.date.minusDays(1).atTime(startLt).atZone(zone).toInstant().toEpochMilli()
                else ->
                    s.date.atTime(startLt).atZone(zone).toInstant().toEpochMilli()
            }
            val endInstant: Long? = endLt?.let { lt ->
                // Prefer the user-picked / loaded endDate. Fall back to the
                // dayKey's date (end-day bucketing) for legacy sessions that
                // never had endDate populated.
                val endDate = s.endDate ?: s.date
                endDate.atTime(lt).atZone(zone).toInstant().toEpochMilli()
            }
            if (existing != null) {
                fastingRepo.updateSession(
                    existing.copy(
                        startMs = startInstant,
                        endMs = endInstant,
                        note = s.notes.ifBlank { null },
                    )
                )
            } else if (endInstant != null) {
                fastingRepo.startFast(startInstant, s.goalHours, "16:8").also { newId ->
                    val inserted = fastingRepo.sessionById(newId)
                    inserted?.let { fastingRepo.updateSession(it.copy(endMs = endInstant, note = s.notes.ifBlank { null })) }
                }
            }
        }
        weightRepo.upsertForDay(s.dayKey, s.weightKg, s.notes.ifBlank { null })
        onDone()
    }

    /**
     * Re-opens a session that was already ended — sets endMs back to null and
     * (re)starts the foreground service if sticky notification is on. Bails
     * silently if anything has changed since [load] ran (a different fast is
     * now active, or the session was deleted).
     */
    fun resumeFast(onDone: () -> Unit) = viewModelScope.launch {
        val s = _state.value
        // Resume the session that's currently displayed in this DayDetail — the
        // one the user is looking at when they tap the inline Resume pill.
        val id = s.sessionId ?: run { onDone(); return@launch }
        val existing = fastingRepo.sessionById(id) ?: run { onDone(); return@launch }
        if (existing.endMs == null) { onDone(); return@launch } // already ongoing — nothing to do
        val nowActive = fastingRepo.activeSession.first()
        if (nowActive != null && nowActive.id != id) { onDone(); return@launch } // another fast running
        fastingRepo.updateSession(existing.copy(endMs = null))
        val settings = settingsRepo.settings.first()
        if (settings.stickyNotificationOn) {
            xyz.jishnu.health.notifications.FastingForegroundService.start(appContext)
        }
        // Block until the activeSession flow reflects the resumed row. This
        // closes a race where Home re-subscribes to FastingViewModel.state and
        // reads the stale (isFasting = false) cached value before Room has
        // had a chance to re-emit.
        fastingRepo.activeSession.first { it?.id == id }
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
