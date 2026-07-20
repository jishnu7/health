# Fasting Calendar Heatmap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a GitHub-style fasting contribution heatmap to the Progress screen — a full-year, horizontally-scrollable grid where each day's cell shade reflects that day's fast length as a fraction of the goal.

**Architecture:** A pure domain builder (`FastingCalendarBuilder`) turns the existing fasting sessions into a grid of week-columns plus summary stats (unit-tested). `ProgressViewModel` computes it once per state emission and exposes it on `ProgressUiState`. A new `FastingCalendarCard` Composable draws the grid on a `Canvas` (mirroring `FastChart`'s Canvas/text/tap patterns), inside a horizontal scroll, with a pinned weekday gutter; tapping a cell reuses the existing `onOpenDay` navigation to Day Detail.

**Tech Stack:** Kotlin, Jetpack Compose (Canvas, `rememberTextMeasurer`, `horizontalScroll`), Hilt, JUnit4.

## Global Constraints

- Kotlin 2.2.10 / AGP 8.13.2 / KSP `ksp.useKSP2=false` — do not change build config.
- Reuse `DayEntries.merge` for per-day fast hours (longest session ending that day); do not reinvent session bucketing.
- Weeks start Monday (ISO), matching `WeightTrend.isoWeekStart`.
- Theme via `IntermTheme.colors` / `IntermTheme.typography`; card wrapper is `IntermCard`. No hard-coded colors in Composables except the intensity ramp helper.
- Cell intensity is goal-relative: level 0 (no fast), then `<50%`, `<85%`, `<100%`, `>=100%` (goal met).
- Verify builds with `make build`, unit tests with `make test`. All commit messages: plain, no Claude/AI attribution.

---

### Task 1: `FastingCalendarBuilder` domain (grid + levels + stats)

**Files:**
- Create: `app/src/main/kotlin/xyz/jishnu/health/domain/FastingCalendar.kt`
- Test: `app/src/test/kotlin/xyz/jishnu/health/domain/FastingCalendarTest.kt`

**Interfaces:**
- Consumes: `xyz.jishnu.health.data.local.FastingSessionEntity`, `xyz.jishnu.health.data.model.DayEntries.merge(sessions, weights, zone, nowMs)`.
- Produces:
  - `data class CalendarDay(dayKey: Long, date: LocalDate, fastHours: Double, level: Int)`
  - `data class MonthLabel(weekIndex: Int, text: String)`
  - `data class FastingCalendar(weeks: List<List<CalendarDay?>>, monthLabels: List<MonthLabel>, daysFasted: Int, goalMetDays: Int, longestStreak: Int, startDate: LocalDate, endDate: LocalDate)` with `companion object { val EMPTY }`
  - `object FastingCalendarBuilder { fun level(fastHours: Double, goalHours: Int): Int; fun build(sessions: List<FastingSessionEntity>, goalHours: Int, today: LocalDate, zone: ZoneId = ZoneId.systemDefault(), weeksBack: Int = 52, nowMs: Long = System.currentTimeMillis()): FastingCalendar }`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/kotlin/xyz/jishnu/health/domain/FastingCalendarTest.kt`:

```kotlin
package xyz.jishnu.health.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.jishnu.health.data.local.FastingSessionEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class FastingCalendarTest {
    private val utc = ZoneId.of("UTC")

    private fun ms(y: Int, m: Int, d: Int, h: Int): Long =
        ZonedDateTime.of(y, m, d, h, 0, 0, 0, utc).toInstant().toEpochMilli()

    /** A completed session ending at [endY-endM-endD] hh:00 that lasted [hours]. */
    private fun session(id: Long, endY: Int, endM: Int, endD: Int, endH: Int, hours: Int) =
        FastingSessionEntity(
            id = id,
            startMs = ms(endY, endM, endD, endH) - hours * 3_600_000L,
            endMs = ms(endY, endM, endD, endH),
            goalHours = 16,
            planId = "16:8",
        )

    @Test fun `level buckets by fraction of goal`() {
        assertEquals(0, FastingCalendarBuilder.level(0.0, 16))
        assertEquals(1, FastingCalendarBuilder.level(4.0, 16))   // 25%
        assertEquals(2, FastingCalendarBuilder.level(8.0, 16))   // 50%
        assertEquals(3, FastingCalendarBuilder.level(14.0, 16))  // 87.5%
        assertEquals(4, FastingCalendarBuilder.level(16.0, 16))  // 100%
        assertEquals(4, FastingCalendarBuilder.level(20.0, 16))  // over goal
    }

    @Test fun `grid starts on Monday and ends at today`() {
        val today = LocalDate.of(2026, 7, 20) // a Monday
        val cal = FastingCalendarBuilder.build(emptyList(), 16, today, utc)
        // first cell of the first column is a Monday
        assertEquals(DayOfWeek.MONDAY, cal.weeks.first()[0]!!.date.dayOfWeek)
        // last non-null cell is today
        val lastReal = cal.weeks.flatten().filterNotNull().maxByOrNull { it.date }!!
        assertEquals(today, lastReal.date)
        // ~53 columns of 7 rows
        assertTrue(cal.weeks.size in 52..54)
        assertTrue(cal.weeks.all { it.size == 7 })
    }

    @Test fun `days after today are padding nulls`() {
        val today = LocalDate.of(2026, 7, 22) // a Wednesday
        val cal = FastingCalendarBuilder.build(emptyList(), 16, today, utc)
        val lastCol = cal.weeks.last()
        // Wed = index 2 (Mon=0); Thu..Sun should be null padding
        assertNotNull(lastCol[2])
        assertNull(lastCol[3])
        assertNull(lastCol[6])
    }

    @Test fun `stats count fasted, goal-met and longest streak`() {
        val today = LocalDate.of(2026, 7, 20)
        val sessions = listOf(
            session(1, 2026, 7, 20, 8, 16), // today: goal met
            session(2, 2026, 7, 19, 8, 16), // goal met
            session(3, 2026, 7, 18, 8, 17), // goal met -> streak of 3
            session(4, 2026, 7, 16, 8, 10), // fasted but short (no streak)
        )
        val cal = FastingCalendarBuilder.build(sessions, 16, today, utc)
        assertEquals(4, cal.daysFasted)
        assertEquals(3, cal.goalMetDays)
        assertEquals(3, cal.longestStreak)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `make test`
Expected: FAIL — `Unresolved reference: FastingCalendarBuilder` (compile error).

- [ ] **Step 3: Write minimal implementation**

Create `app/src/main/kotlin/xyz/jishnu/health/domain/FastingCalendar.kt`:

```kotlin
package xyz.jishnu.health.domain

import xyz.jishnu.health.data.local.FastingSessionEntity
import xyz.jishnu.health.data.model.DayEntries
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** One day's cell. [level] is 0 (no fast) .. 4 (goal met). */
data class CalendarDay(
    val dayKey: Long,
    val date: LocalDate,
    val fastHours: Double,
    val level: Int,
)

/** A month name to draw above the week column at [weekIndex]. */
data class MonthLabel(val weekIndex: Int, val text: String)

/**
 * A year of fasting activity as GitHub-style week columns. Each column holds 7
 * entries (Mon..Sun); a null entry is padding for days after today in the final
 * (partial) week.
 */
data class FastingCalendar(
    val weeks: List<List<CalendarDay?>>,
    val monthLabels: List<MonthLabel>,
    val daysFasted: Int,
    val goalMetDays: Int,
    val longestStreak: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    companion object {
        val EMPTY = FastingCalendar(emptyList(), emptyList(), 0, 0, 0, LocalDate.MIN, LocalDate.MIN)
    }
}

object FastingCalendarBuilder {
    private val MONTHS = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )

    fun level(fastHours: Double, goalHours: Int): Int {
        if (fastHours <= 0.0) return 0
        if (goalHours <= 0) return 4
        return when (fastHours / goalHours) {
            in 1.0..Double.MAX_VALUE -> 4
            in 0.85..1.0 -> 3
            in 0.5..0.85 -> 2
            else -> 1
        }
    }

    private fun isoWeekStart(date: LocalDate): LocalDate =
        date.minus(((date.dayOfWeek.value - DayOfWeek.MONDAY.value) + 7) % 7L, ChronoUnit.DAYS)

    fun build(
        sessions: List<FastingSessionEntity>,
        goalHours: Int,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        weeksBack: Int = 52,
        nowMs: Long = System.currentTimeMillis(),
    ): FastingCalendar {
        // Longest fasting session ending on each day — reuse the tested merge.
        val hoursByKey: Map<Long, Double> = DayEntries.merge(sessions, emptyList(), zone, nowMs)
            .associate { it.dayKey to it.fastHours }

        val start = isoWeekStart(today.minusWeeks(weeksBack.toLong()))
        val weeks = mutableListOf<List<CalendarDay?>>()
        val monthLabels = mutableListOf<MonthLabel>()
        var cursor = start
        var weekIndex = 0
        var lastLabeledMonth = -1
        var daysFasted = 0
        var goalMetDays = 0
        var streak = 0
        var longest = 0

        while (!cursor.isAfter(today)) {
            if (cursor.monthValue != lastLabeledMonth) {
                monthLabels += MonthLabel(weekIndex, MONTHS[cursor.monthValue - 1])
                lastLabeledMonth = cursor.monthValue
            }
            val column = ArrayList<CalendarDay?>(7)
            for (d in 0 until 7) {
                val date = cursor.plusDays(d.toLong())
                if (date.isAfter(today)) {
                    column.add(null)
                    continue
                }
                val dayKey = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val hours = hoursByKey[dayKey] ?: 0.0
                val lvl = level(hours, goalHours)
                column.add(CalendarDay(dayKey, date, hours, lvl))
                if (hours > 0.0) daysFasted++
                if (lvl == 4) {
                    goalMetDays++
                    streak++
                    longest = maxOf(longest, streak)
                } else {
                    streak = 0
                }
            }
            weeks.add(column)
            cursor = cursor.plusWeeks(1)
            weekIndex++
        }
        return FastingCalendar(weeks, monthLabels, daysFasted, goalMetDays, longest, start, today)
    }
}
```

Note: the `when` uses half-open intent via ordering — `in 1.0..` wins first, so `0.85..1.0` effectively means `[0.85,1.0)` and `0.5..0.85` means `[0.5,0.85)` for the reached branches. Exact-boundary 0.85 → level 3, exact 1.0 → level 4, matching the test.

- [ ] **Step 4: Run test to verify it passes**

Run: `make test`
Expected: PASS (all `FastingCalendarTest` cases green, existing tests still green).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/xyz/jishnu/health/domain/FastingCalendar.kt app/src/test/kotlin/xyz/jishnu/health/domain/FastingCalendarTest.kt
git commit -m "add fasting calendar domain builder"
```

---

### Task 2: `FastingCalendarCard` Composable

**Files:**
- Create: `app/src/main/kotlin/xyz/jishnu/health/ui/components/FastingCalendarCard.kt`

**Interfaces:**
- Consumes: `FastingCalendar`, `CalendarDay` (Task 1); `IntermTheme.colors`/`.typography`; `onDayClick: (Long) -> Unit`.
- Produces: `@Composable fun FastingCalendarCard(calendar: FastingCalendar, onDayClick: (dayKey: Long) -> Unit, modifier: Modifier = Modifier)` and `@Composable fun calendarLevelColors(): List<Color>` (5 entries, index = level).

This task has no unit test (Canvas UI). It is verified by compiling; end-to-end visual verification happens in Task 3 on the emulator.

- [ ] **Step 1: Create the component file**

Create `app/src/main/kotlin/xyz/jishnu/health/ui/components/FastingCalendarCard.kt`:

```kotlin
package xyz.jishnu.health.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import xyz.jishnu.health.domain.FastingCalendar
import xyz.jishnu.health.ui.theme.IntermTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Level 0..4 intensity ramp, tuned per theme (index = CalendarDay.level). */
@Composable
fun calendarLevelColors(): List<Color> =
    if (IntermTheme.colors.isDark) {
        listOf(
            Color(0xFF2A2822), Color(0xFF2C4A3B), Color(0xFF35604D), Color(0xFF4E9576), Color(0xFF7DD3A8),
        )
    } else {
        listOf(
            Color(0xFFEAE6DB), Color(0xFFCBDDCF), Color(0xFF97BEA5), Color(0xFF598872), Color(0xFF2A4D3E),
        )
    }

@Composable
fun FastingCalendarCard(
    calendar: FastingCalendar,
    onDayClick: (dayKey: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = IntermTheme.colors
    if (calendar.weeks.isEmpty()) return
    val ramp = calendarLevelColors()
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val scroll = rememberScrollState()

    // Fixed cell geometry (dp).
    val cell = 13.dp
    val gap = 3.dp
    val gutter = 26.dp
    val monthH = 16.dp
    val rowStride = cell + gap
    val gridWidth = rowStride * calendar.weeks.size
    val gridHeight = monthH + rowStride * 7

    val monthStyle = IntermTheme.typography.mono.copy(fontSize = 9.5.sp, color = c.muted)
    val dowStyle = IntermTheme.typography.mono.copy(fontSize = 9.sp, color = c.muted)
    val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

    IntermCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "FASTING CALENDAR",
                style = IntermTheme.typography.hEyebrow,
                color = c.muted,
            )
            Spacer(Modifier.height(6.dp))
            val span = "${calendar.startDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))} – " +
                calendar.endDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))
            Text(text = span, style = IntermTheme.typography.caption, color = c.muted)

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                CalStat(value = calendar.daysFasted.toString(), label = "Days fasted")
                CalStat(value = calendar.goalMetDays.toString(), label = "Goal met")
                CalStat(value = "${calendar.longestStreak}d", label = "Longest streak")
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Top) {
                // Pinned weekday gutter (Mon / Wed / Fri).
                Canvas(
                    modifier = Modifier
                        .width(gutter)
                        .height(gridHeight),
                ) {
                    val strideP = rowStride.toPx()
                    val cellP = cell.toPx()
                    val top = monthH.toPx()
                    listOf(0 to "Mon", 2 to "Wed", 4 to "Fri").forEach { (rowIdx, label) ->
                        val measured = measurer.measure(AnnotatedString(label), dowStyle)
                        drawText(
                            measurer,
                            label,
                            topLeft = Offset(0f, top + rowIdx * strideP + (cellP - measured.size.height) / 2f),
                            style = dowStyle,
                        )
                    }
                }
                // Scrollable grid.
                Box(modifier = Modifier.horizontalScroll(scroll)) {
                    Canvas(
                        modifier = Modifier
                            .width(gridWidth)
                            .height(gridHeight)
                            .pointerInput(calendar.weeks.size) {
                                detectTapGestures { tap ->
                                    val strideP = rowStride.toPx()
                                    val top = monthH.toPx()
                                    val col = (tap.x / strideP).toInt()
                                    val row = ((tap.y - top) / strideP).toInt()
                                    if (col in calendar.weeks.indices && row in 0..6) {
                                        calendar.weeks[col][row]?.let { onDayClick(it.dayKey) }
                                    }
                                }
                            },
                    ) {
                        val strideP = rowStride.toPx()
                        val cellP = cell.toPx()
                        val top = monthH.toPx()
                        val radius = with(density) { 3.dp.toPx() }
                        // month labels
                        calendar.monthLabels.forEach { ml ->
                            drawText(
                                measurer,
                                ml.text,
                                topLeft = Offset(ml.weekIndex * strideP, 0f),
                                style = monthStyle,
                            )
                        }
                        // cells
                        calendar.weeks.forEachIndexed { col, week ->
                            week.forEachIndexed { row, day ->
                                if (day == null) return@forEachIndexed
                                val x = col * strideP
                                val y = top + row * strideP
                                drawRoundRect(
                                    color = ramp[day.level],
                                    topLeft = Offset(x, y),
                                    size = Size(cellP, cellP),
                                    cornerRadius = CornerRadius(radius, radius),
                                )
                                if (day.date == calendar.endDate) {
                                    drawRoundRect(
                                        color = c.accent,
                                        topLeft = Offset(x, y),
                                        size = Size(cellP, cellP),
                                        cornerRadius = CornerRadius(radius, radius),
                                        style = Stroke(width = with(density) { 1.5.dp.toPx() }),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Less", style = IntermTheme.typography.caption, color = c.muted)
                Spacer(Modifier.width(6.dp))
                ramp.forEach { color ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text("More", style = IntermTheme.typography.caption, color = c.muted)
            }
        }
    }

    // Open scrolled to the most recent weeks.
    LaunchedEffect(calendar.weeks.size) { scroll.scrollTo(scroll.maxValue) }
}

@Composable
private fun CalStat(value: String, label: String) {
    val c = IntermTheme.colors
    Column {
        Text(value, style = IntermTheme.typography.mono.copy(fontSize = 20.sp), color = c.ink)
        Text(label, style = IntermTheme.typography.hEyebrow, color = c.muted)
    }
}
```

Note: `Text` is `androidx.compose.material3.Text` — add `import androidx.compose.material3.Text` (the codebase's standard import; confirm against `WeightTrendCard.kt`).

- [ ] **Step 2: Verify it compiles**

Run: `make build`
Expected: `BUILD SUCCESSFUL`. If `Text` is unresolved, add `import androidx.compose.material3.Text`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/xyz/jishnu/health/ui/components/FastingCalendarCard.kt
git commit -m "add fasting calendar heatmap component"
```

---

### Task 3: Wire calendar into ProgressViewModel and ProgressScreen

**Files:**
- Modify: `app/src/main/kotlin/xyz/jishnu/health/vm/ProgressViewModel.kt`
- Modify: `app/src/main/kotlin/xyz/jishnu/health/ui/screens/progress/ProgressScreen.kt`

**Interfaces:**
- Consumes: `FastingCalendarBuilder.build(...)` (Task 1), `FastingCalendarCard(...)` (Task 2), the existing `onOpenDay: (Long, Long?) -> Unit` already passed to `ProgressScreen`.
- Produces: `ProgressUiState.calendar: FastingCalendar`.

- [ ] **Step 1: Add `calendar` to `ProgressUiState`**

In `ProgressViewModel.kt`, add the import and the field.

Add import near the other model imports:
```kotlin
import xyz.jishnu.health.domain.FastingCalendar
import xyz.jishnu.health.domain.FastingCalendarBuilder
```

In the `data class ProgressUiState(...)` add:
```kotlin
    val calendar: FastingCalendar = FastingCalendar.EMPTY,
```

- [ ] **Step 2: Compute the calendar in the state `combine`**

In `ProgressViewModel.kt`, inside the `combine { sessions, weights, settings, range -> ... }` block, after `val plan = Plans.byId(settings.planId)` and `val today = LocalDate.now()` (both already present), add:

```kotlin
        val calendar = FastingCalendarBuilder.build(
            sessions = sessions,
            goalHours = plan.fastHours,
            today = today,
            zone = zone,
        )
```

Then add `calendar = calendar,` to the `ProgressUiState(...)` constructor call at the end of the block.

- [ ] **Step 3: Insert the card in `ProgressScreen`**

In `ProgressScreen.kt`, add import:
```kotlin
import xyz.jishnu.health.ui.components.FastingCalendarCard
```

Immediately after the chart `IntermCard { ... }` block (the one ending at the closing brace after `FastChart(...)`), and before `Spacer(Modifier.height(14.dp))` / `SummaryGrid(state)`, insert:

```kotlin
                Spacer(Modifier.height(14.dp))
                FastingCalendarCard(
                    calendar = state.calendar,
                    onDayClick = { dayKey -> onOpenDay(dayKey, null) },
                    modifier = Modifier.fillMaxWidth(),
                )
```

- [ ] **Step 4: Verify it compiles and tests pass**

Run: `make build && make test`
Expected: `BUILD SUCCESSFUL` for both.

- [ ] **Step 5: Verify on emulator**

```bash
# boot emulator, install, complete onboarding (seeds mock fasting data), open Progress
~/Library/Android/sdk/emulator/emulator -avd Pixel_10_Pro_XL -no-snapshot-save -no-boot-anim &
# wait for boot, then:
make run
```
Then: open the app → Progress tab. Confirm:
- A "FASTING CALENDAR" card appears below the fast/weight chart, above the Avg-fast summary.
- The grid shows green cells of varying intensity, opens scrolled to the right (today, terracotta ring), scrolls horizontally, Mon/Wed/Fri gutter pinned.
- Tapping a cell opens that day's Day Detail screen.
- Toggle system dark mode → the ramp and card adapt.
Capture a screenshot to confirm.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/xyz/jishnu/health/vm/ProgressViewModel.kt app/src/main/kotlin/xyz/jishnu/health/ui/screens/progress/ProgressScreen.kt
git commit -m "show fasting calendar heatmap on progress screen"
```

---

## Self-Review

**Spec coverage:**
- 5-step goal-relative intensity → Task 1 `level()` + Task 2 `calendarLevelColors()`. ✓
- Full year, horizontal scroll, Mon-at-top → Task 1 grid (`weeksBack=52`, `isoWeekStart`) + Task 2 `horizontalScroll` + pinned gutter + auto-scroll-to-end. ✓
- New card below the line chart, above summary → Task 3 Step 3 insertion point. ✓
- Tap → Day Detail → Task 2 `onDayClick` + Task 3 `onOpenDay(dayKey, null)`. ✓
- Stats: days fasted / goal met / longest streak → Task 1 stats + Task 2 `CalStat` row. ✓
- Today marker → Task 2 accent ring on `day.date == calendar.endDate`. ✓

**Placeholder scan:** none — every step has concrete code/commands.

**Type consistency:** `FastingCalendar`, `CalendarDay`, `MonthLabel`, `FastingCalendarBuilder.build(...)`, `level(...)`, `FastingCalendarCard(calendar, onDayClick, modifier)`, `calendarLevelColors()` used identically across tasks. `onOpenDay(dayKey, null)` matches the existing `(Long, Long?) -> Unit` signature.

**Open verification risk:** the `when(fraction)` range boundaries in `level()` rely on branch ordering for half-open semantics; the Task 1 test pins levels at 4/8/14/16h to catch a regression.
