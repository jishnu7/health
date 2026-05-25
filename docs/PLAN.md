# Plan: Implement "Interm" intermittent fasting app — native Android (Kotlin + Compose)

## Context

The repo contains only `docs/` — a Claude Design handoff bundle for an intermittent-fasting app. We are shipping it as **Interm**, package `xyz.jishnu.health`. The prototype at `docs/project/index.html` is a React + Babel-in-browser design canvas that arranges 12 screen mockups inside fake 412×892 Android frames. We will recreate those screens **pixel-perfectly as a real native Android app** — Kotlin + Jetpack Compose, no WebView, no React.

Source of truth for visuals: `docs/project/styles.css` (tokens), `docs/project/screens.jsx` (per-screen layout), `docs/project/shared.jsx` (reusable components, helpers, constants). The prototype's product name "Fast" is rebranded to "Interm" in all user-facing copy; the domain word "fasting" (the activity) is unchanged.

**Out of scope (prototype-only):** `design-canvas.jsx`, `tweaks-panel.jsx`, `android-frame.jsx`, and the `Frame` component in `app.jsx`. These exist only to mock the device on desktop; on a real phone the system provides the chrome.

---

## High-level architecture

| Concern | Choice |
|---|---|
| App / package | **Interm** / `xyz.jishnu.health` |
| Language / UI | Kotlin + Jetpack Compose (Material 3 as the base, custom-themed) |
| Min / target SDK | 26 / 35 |
| Build | Gradle Kotlin DSL, AGP 8.x, Kotlin 2.x, Compose BOM (latest) |
| Navigation | `androidx.navigation:navigation-compose` with typed routes |
| State | `ViewModel` + `StateFlow`; UI collects via `collectAsStateWithLifecycle` |
| DI | Hilt |
| Local persistence | Room (sessions, weight entries) + DataStore Preferences (settings) |
| Backend | **Deferred** — repository interfaces are wire-ready for a remote impl; no backend code in v1 |
| Notifications | Real foreground-service "sticky fast" notification + `AlarmManager` for daily reminders |
| Charts | Vico (`com.patrykandpatrick.vico`) for the dual-axis Progress chart |
| Fonts | Geist + Geist Mono bundled as `.ttf` in `res/font/` |

Single module (`:app`).

---

## Phased delivery

**Each phase ends at a buildable, installable APK.** Run `./gradlew :app:installDebug` at the end of every phase and walk through the verification steps before moving on. Later phases reuse code from earlier ones — no rework.

---

### Phase 0 — Project scaffold (✅ builds, ✅ installs, shows a blank screen)

**Goal:** A green-light Android project with the toolchain wired up. Opens to a single screen that says "Interm" in the right font and theme colors. Nothing else.

**Deliverables**
- `app/build.gradle.kts` (`applicationId = "xyz.jishnu.health"`, `namespace = "xyz.jishnu.health"`), root `build.gradle.kts`, `settings.gradle.kts` (`rootProject.name = "Interm"`), `gradle.properties`, `gradle/wrapper/`.
- `AndroidManifest.xml` declaring `IntermApplication` (`@HiltAndroidApp`) + `MainActivity`. App label = "Interm".
- Hilt set up (root module empty for now).
- Compose + Compose Navigation + Hilt + Lifecycle + ViewModel + DataStore + Room (deps declared, not used yet).
- `MainActivity` → `setContent { IntermTheme { Surface { Text("Interm") } } }`.
- Bare `IntermTheme` composable using Material 3 defaults (real tokens come in Phase 1).
- App icon (placeholder is fine).

**Verify**
- `./gradlew :app:assembleDebug` succeeds.
- App installs on Pixel 6 API 34 emulator and opens to "Interm" on a cream background.

---

### Phase 1 — Design system + component gallery (✅ all primitives render correctly in both themes)

**Goal:** Every reusable visual element from `shared.jsx` and `styles.css` exists as a Compose composable, exposed through a developer-only "gallery" screen so we can eyeball-check them against the prototype.

**Deliverables**
- `ui/theme/`
  - `Color.kt` — `IntermColors` data class, `lightIntermColors()`, `darkIntermColors()` with the exact hex values from `styles.css`.
  - `Type.kt` — Geist + GeistMono `FontFamily`; `TextStyle` for `hDisplay`, `hTitle`, `hEyebrow`, `body`, `caption`.
  - `Tokens.kt` — `IntermTokens` carrying everything `MaterialTheme.colorScheme` can't (`card`, `border`, `border2`, `primarySoft`, `accentSoft`, `subtle`, etc.) + `LocalIntermTokens`.
  - `Theme.kt` — `IntermTheme { content }` picks light/dark from system, provides both `MaterialTheme` and `LocalIntermTokens`.
- `res/font/` — bundled Geist + Geist Mono `.ttf` files (Regular, Medium for Geist; Regular for Mono).
- `ui/components/`
  - `IntermButtons.kt` — `IntermPrimaryButton`, `IntermGhostButton`, `IntermSoftButton`, `IntermDangerButton`. 52dp pill, 24dp horizontal padding, `scale(0.98)` press.
  - `IntermCard.kt` — 18dp radius, 1dp border, `card` bg, 18dp inner padding.
  - `IntermToggle.kt` — 44×26 track, 20dp thumb, 18dp travel, 180ms ease. Custom, NOT Material `Switch`.
  - `IntermSegmented.kt` — pill row in `border2` container.
  - `IntermStageChip.kt`.
  - `IntermTopBar.kt` — port of `FastHeader`: 56dp min-height, optional left/right icon slots, centered title.
  - `BottomNav.kt` — 3 tabs (Home, Weight, Progress); active item icon background = `primarySoft`.
  - `ProgressRing.kt` — `Canvas` composable with two arcs (track + progress), centered content slot, optional dashed variant.
  - `StageDots.kt` — 8 dots, filled up to current index.
  - `IntermIcons.kt` — `ImageVector` equivalents of every icon in `shared.jsx`: Home/Scale/Chart/History/Settings/Plus/Minus/Bell/Back/Check/Chevron/Flame/Drop/Food/Stop/Play.
- `ui/screens/dev/ComponentGalleryScreen.kt` — vertically stacks every component in every state; toggled on via `BuildConfig.DEBUG`.

**Verify**
- Open the gallery in light mode → every component matches the prototype.
- Toggle system dark mode → every component matches the prototype's dark variant.
- Press buttons → see the scale-down animation.
- Toggle the `IntermToggle` → smooth slide.

---

### Phase 2 — Core fasting loop: Home + Stages (✅ a usable timer)

**Goal:** A real, usable fasting timer. Start a fast, watch elapsed time tick, see the current metabolic stage, tap the chip to open the Stages list, end the fast or hit "I ate" to reset. State is in-memory only (persistence comes in Phase 3).

**Deliverables**
- `data/model/` — `Plan`, `Stage`, `Units` data classes.
- `data/constants/Stages.kt` — port of `STAGES` array (8 phases, start hour, range, title, body, benefits, hue).
- `data/constants/Plans.kt` — port of `PLANS` (14:10, 16:8, 18:6, 20:4, 23:1).
- `domain/StageCalculator.kt` — `stageFor(elapsedHours: Double): Stage`. Unit-tested.
- `domain/TimeMath.kt` — `fmtDuration`, `fmtTime`, `fmtDate`, `addHoursToTime` (with midnight wrap), `diffHoursTime`. Unit-tested.
- `vm/FastingViewModel.kt` — Hilt-scoped, exposes `StateFlow<FastingUiState>` with `isFasting`, `fastStartMs`, `elapsedMs`, `progress`, `stage`, `goalH`, `plan`. Internal `flow { while (true) { emit(now); delay(1_000) } }` drives the live tick. Actions: `startFast()`, `endFast()`, `resetFast()`, `setPlan()`. Default plan: 16:8.
- `ui/screens/home/HomeScreen.kt` — large `ProgressRing` with mono elapsed-time inside, stage chip below, primary CTA ("Start fasting" / "End fast"), secondary "I ate" link, `BottomNav` at the bottom. Tap stage chip → navigate to Stages.
- `ui/screens/stages/StagesScreen.kt` — full-screen list of 8 stage cards; current stage highlighted with `primary` border + `primarySoft` bg.
- `ui/nav/IntermNavHost.kt` — `NavHost` with `home` (start), `stages`, `weight` (placeholder), `progress` (placeholder).

**Verify**
- Open app → Home with "Start fasting" button.
- Tap Start → ring fills over time, chip updates as stages cross thresholds (use a `BuildConfig.DEBUG`-only 60× speed multiplier in `FastingViewModel` to exercise this quickly).
- Tap stage chip → Stages screen, current row highlighted.
- Back → Home, fast still running.
- Tap "End fast" → ring empties, button reverts to "Start fasting".
- Kill app → state lost (expected; Phase 3 fixes this).

---

### Phase 3 — Persistence + Settings + Plan Picker (✅ state survives restart)

**Goal:** The user's fast, plan, units, and preferences persist across process death and reboots. Settings screen exposes all preferences. Plan Picker lets the user choose between the 5 IF protocols.

**Deliverables**
- `data/local/`
  - `IntermDatabase.kt` (Room) with `FastingSessionDao`, `WeightEntryDao` (entry dao unused this phase but defined to avoid migration in Phase 4).
  - `FastingSessionEntity` (id, startMs, endMs?, goalH, planId, note?).
  - `SettingsDataStore.kt` — `Flow`-based DataStore Preferences wrapper for `plan`, `units`, `fastStartTime`, `reminderTime`, `fastingReminderOn`, `weightReminderOn`, `darkMode`, `onboarded`.
- `data/repo/`
  - `FastingRepository` (interface) + `LocalFastingRepository` (Room-backed).
  - `SettingsRepository` (interface) + `DataStoreSettingsRepository`.
  - Hilt `@Module` binding interface → impl.
- Update `FastingViewModel` to read/write through the repositories; on init, restore the active fast (if any) from the most recent session with `endMs == null`.
- `ui/screens/settings/SettingsScreen.kt` — three sections (Fasting / Weight / About). Toggle rows for fasting reminders, sticky notification, weigh-in reminder. Time rows (24h `TimePickerDialog`). Plan row navigates to Plan Picker. Units segmented (metric / imperial).
- `ui/screens/settings/PlanPickerScreen.kt` — 5 radio cards showing label + subtitle + fast/eat window breakdown. Save commits to `SettingsRepository.plan` and pops back.

**Verify**
- Start a fast → kill app from recents → reopen → fast continues with correct elapsed time.
- Change plan in Settings → Home goal ring updates.
- Toggle units → weight values (will show in Phase 4) flip lb/kg.
- Verify Room DB exists on device: `adb shell run-as xyz.jishnu.health ls databases/`.

---

### Phase 4 — Weight + History + Day Detail (✅ logging + editing past days)

**Goal:** Log a weight, see it on the History screen alongside fasting data, tap a day to edit fasting window + weight + notes.

**Deliverables**
- `data/model/WeightEntry.kt`, `data/model/DayEntry.kt` (merge of session + weight for a given dayKey).
- `WeightEntryEntity` + `WeightEntryDao` (already declared in Phase 3, now used).
- `domain/WeightMath.kt` — `fmtWeight(lb: Double, units: Units)`, `lbToKg`. Unit-tested.
- `data/repo/WeightRepository` + Room impl.
- `ui/screens/weight/WeightScreen.kt` — large mono numeric, ±/±0.1 stepper, quick-delta chips, previous weight, 7-day average, optional notes. Save persists a `WeightEntry`.
- `ui/screens/history/HistoryScreen.kt` — `LazyColumn` showing merged day entries (most recent first). Row: date, fast hours, weight, progress bar to goal.
- `ui/screens/daydetail/DayDetailScreen.kt` — hero card (duration + GOAL/SHORT chip + progress bar with goal tick); editable start/end time (`TimePickerDialog`) with auto-recomputed duration; editable weight ±buttons with delta vs previous; notes; Delete + Save.
- Seed 14 days of mock data on first run, gated on `BuildConfig.DEBUG`, mirroring `app.jsx:63–81`.
- Wire History row tap → DayDetail with `dayKey` arg.

**Verify**
- Log a weight → appears on History.
- Tap an older day → DayDetail opens with that day's data.
- Edit times → duration updates live.
- Save → row reflects edits.
- Delete → row disappears.
- Switch units → all weights re-render with new unit.

---

### Phase 5 — Progress chart (✅ chart + range picker + summary)

**Goal:** A dual-axis chart of weight + fasting hours over time, with range filtering and summary stats.

**Deliverables**
- `ui/screens/progress/FastChart.kt` — Vico dual-axis line chart. Weight on left axis in `primary`, fasting hours on right axis in `accent` (85% opacity, slightly thinner stroke).
- `ui/screens/progress/ProgressScreen.kt` — range segmented (7d / 14d / 30d / 90d / Week / Month / YTD) + custom range picker (`DateRangePicker`), summary cards (avg fast, weight change, total fasted, streak), merged history list at the bottom.
- Add Vico dep: `com.patrykandpatrick.vico:compose-m3:<latest>`.
- Reuse `WeightRepository.observeRange(from, to)` + `FastingRepository.observeRange(from, to)`.

**Verify**
- Open Progress → chart renders without overlapping axis labels.
- Flip ranges → chart re-fits.
- Pick a custom range → chart shows only that range.
- Tap a history row → DayDetail (same as Phase 4).

---

### Phase 6 — Onboarding (✅ first-run flow gated on `Settings.onboarded`)

**Goal:** New users see the 3-step onboarding the first time they open the app. Returning users skip it.

**Deliverables**
- `ui/screens/onboarding/OnboardWelcomeScreen.kt` — eyebrow, title, demo `ProgressRing` at 66%, `StageDots` 1/3, "Get started" primary + "I already have an account" ghost. Copy: "Welcome to Interm".
- `ui/screens/onboarding/OnboardPlanScreen.kt` — step 2/3, reuses `PlanPickerScreen` body, Continue → Reminders.
- `ui/screens/onboarding/OnboardRemindersScreen.kt` — step 3/3, units segmented, fast start time, weigh-in time, fasting reminders toggle, "Start fasting" → Home.
- `MainActivity` reads `SettingsRepository.onboarded` once at startup and picks `onboarding` vs `home` as the `NavHost` start destination.
- "Start fasting" CTA on step 3 calls `FastingViewModel.startFast()` AND sets `onboarded = true`.

**Verify**
- Fresh install → onboarding starts.
- Walk through 1 → 2 → 3 → land on Home with an active fast.
- Kill and reopen → straight to Home (no onboarding).
- `adb shell pm clear xyz.jishnu.health` → onboarding again.

---

### Phase 7 — Real notifications + in-app preview (✅ sticky fast notification + reminders fire)

**Goal:** The fasting timer shows up in the notification shade with progress + actions, and daily reminders fire at the configured times. Plus a static in-app preview screen for parity with the prototype.

**Deliverables**
- `notifications/NotifChannels.kt` — declare `fasting_sticky` (LOW, ongoing), `fasting_reminders` (DEFAULT). Created in `IntermApplication.onCreate`.
- `notifications/FastingForegroundService.kt` — started by `FastingViewModel.startFast()` via `Context.startForegroundService`; updates a `RemoteViews`-based notification every 60 seconds with progress, stage, elapsed, remaining. Two `PendingIntent` actions: End fast, I ate.
- `notifications/FastingNotificationBuilder.kt` — collapsed + expanded layouts (`res/layout/notif_fasting_*.xml`) matching `NotificationPanelScreen` design. Progress arc drawn into a `Bitmap` via a `Canvas` helper (Compose can't render into RemoteViews).
- `notifications/ReminderScheduler.kt` — `AlarmManager.setExactAndAllowWhileIdle` for daily fast-start + weigh-in. Re-schedule on `BOOT_COMPLETED`.
- `ui/screens/notif/NotificationPreviewScreen.kt` — static in-app render of the notification design (same layout primitives as the Compose UI, not the real notification). Reachable from Settings → About → "Preview notification" (or from a dev menu).
- Permissions in manifest: `POST_NOTIFICATIONS` (request on Android 13+), `SCHEDULE_EXACT_ALARM` (request via `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`), `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `RECEIVE_BOOT_COMPLETED`.

**Verify**
- Start a fast → notification appears in shade with progress.
- Wait a minute → progress updates.
- Tap "End fast" action → service stops, notification dismissed, Home reflects ended state.
- Set fasting reminder to "now + 1 min" → reminder notification fires.
- Reboot device with an active fast → notification re-appears.

---

### Phase 8 — Polish, accessibility, verification

**Goal:** Production-ready quality bar.

**Deliverables**
- Content descriptions on every icon button.
- Touch targets ≥ 48dp.
- `TalkBack` walkthrough of the main flows; fix label/order issues.
- Rotation: every screen survives configuration change (ViewModel scoping should already handle this; verify text inputs preserve state).
- Process death: trigger "Don't keep activities" → walk every flow.
- Light / dark visual diff against the prototype screenshots — eyeball any token drift.
- Lint clean: `./gradlew :app:lintDebug`.
- Unit tests green: `./gradlew :app:testDebugUnitTest`.
- README at the repo root explaining the build, the deferred backend integration point (the repo interfaces), and how to swap in a remote source.

**Verify** — full QA matrix from the original verification section (onboarding → home → weight → progress → settings → notification → reboot).

---

## Project layout (cumulative across phases)

```
/app
  build.gradle.kts                            // applicationId = "xyz.jishnu.health"
  src/main/
    AndroidManifest.xml                       // android:label="Interm"
    kotlin/xyz/jishnu/health/
      IntermApplication.kt        [P0]
      MainActivity.kt             [P0, updated P2/P6]
      ui/theme/                   [P1]
      ui/components/              [P1]
      ui/screens/
        home/                     [P2]
        stages/                   [P2]
        settings/                 [P3]
        weight/                   [P4]
        history/                  [P4]
        daydetail/                [P4]
        progress/                 [P5]
        onboarding/               [P6]
        notif/                    [P7]
        dev/ComponentGallery.kt   [P1, debug-only]
      ui/nav/IntermNavHost.kt     [P2, expanded each phase]
      data/
        model/                    [P2/P4]
        constants/                [P2]
        local/                    [P3/P4]
        repo/                     [P3/P4]
      domain/                     [P2/P4]
      vm/FastingViewModel.kt      [P2, persistence added P3]
      notifications/              [P7]
    res/
      font/                       [P1]
      values/strings.xml          // app_name = "Interm"
      layout/notif_fasting_*.xml  [P7]
      drawable/, mipmap-*/        [P0]
```

---

## Design tokens (from `styles.css` → Compose)

**Light**
- `bg #f6f3ee`, `surface #fdfbf7`, `card #ffffff`
- `ink #14130f`, `ink2 #3a3a36`, `muted #8a857c`, `subtle #b8b3a7`
- `border #e8e3d8`, `border2 #f0ecdf`
- `primary #2a4d3e`, `primary2 #3d6b56`, `primarySoft #e7eee8`
- `accent #d97757`, `accentSoft #f7e7df`
- `danger #b54734`, `warn #c69142`

**Dark**
- `bg #14130f`, `surface #1c1b17`, `card #24221d`
- `ink #f6f3ee`, `ink2 #d8d3c8`, `muted #8a857c`, `subtle #5a574f`
- `border #2d2a23`, `border2 #26241e`
- `primary #7dd3a8`, `primary2 #9adbb7`, `primarySoft #1f2a23`
- `accent #e89074`, `accentSoft #2e211b`
- `danger #e87164`, `warn #d9a35a`

**Type scale** (Geist; Geist Mono for `hDisplay` and tabular numbers)
- `hDisplay`: GeistMono 56sp / w400 / -4% letter-spacing / lh 1.0
- `hTitle`: Geist 28sp / w500 / -2% / lh 1.1
- `hEyebrow`: Geist 11sp / w500 / +12% / UPPER
- `body`: Geist 14sp / lh 1.5 / color = ink2
- `caption`: Geist 12sp / color = muted

**Spacing / shape**
- Card: 18dp padding, 18dp radius, 1dp border = `border`
- Button: 52dp height, fully rounded pill, 24dp horizontal padding, 15sp / w500
- Segmented: 999dp pills inside `border2` container, 3dp padding
- Toggle: 44×26dp track, 20dp thumb, 18dp travel, 180ms ease
- Bottom-nav item icon: 48×28 rounded 14dp; active bg = `primarySoft`, active color = `primary`

---

## Persistence & the backend deferral

Local-only in v1. Repositories are interfaces in `data/repo/`; Room/DataStore impls live alongside; Hilt binds interface → impl. When a backend is chosen, add a `RemoteFastingRepository` and either swap the binding or compose a sync-through-network strategy — **no callers change**. The README at the repo root will call this out as the integration point.

---

## Explicit non-goals (v1)

- No backend / sync / accounts.
- No widgets, no Wear OS, no tablet-optimized layouts.
- No analytics, no crash reporting.
- English only (string ids ready, only `values/strings.xml`).
- No `DesignCanvas`, `TweaksPanel`, or fake device-frame chrome — those were prototype tooling.
