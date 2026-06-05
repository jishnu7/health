// app.jsx — Fast app design canvas wiring

// Global defaults context — shared theme + demo speed + default plan across all artboards.
// Each artboard's FastProvider reads from this and syncs.
const DefaultsCtx = React.createContext(null);

function DefaultsProvider({ children, value }) {
  return <DefaultsCtx.Provider value={value}>{children}</DefaultsCtx.Provider>;
}

// Re-wrap FastProvider so it accepts initial overrides for isFasting + start offset.
// We do this with a thin wrapper that injects state via key prop changes.
function ScopedFastProvider({ initialFasting = true, initialOffsetH = 14.4, hasLastFast = true, children }) {
  const defaults = React.useContext(DefaultsCtx) || {};
  return (
    <FastInjector
      initialFasting={initialFasting}
      initialOffsetH={initialOffsetH}
      hasLastFast={hasLastFast}
      plan={defaults.plan}
      units={defaults.units}
      speed={defaults.speed}>
      {children}
    </FastInjector>
  );
}

// This wraps FastProvider but provides initial overrides.
// FastProvider already has its own useState defaults; we override here by re-mounting if key state changes externally.
// Simpler: we shadow it with our own state, then provide context.
function FastInjector({ initialFasting, initialOffsetH, hasLastFast = true, plan: defaultPlan, units: defaultUnits, speed: defaultSpeed, children }) {
  const [, setTick] = React.useState(0);
  React.useEffect(() => {
    const t = setInterval(() => setTick((x) => x + 1), 1000);
    return () => clearInterval(t);
  }, []);

  const [fastStartMs, setFastStartMs] = React.useState(() => Date.now() - initialOffsetH * 3600000);
  const [isFasting, setIsFasting] = React.useState(initialFasting);
  const [plan, setPlan] = React.useState(defaultPlan || '16:8');
  const [units, setUnits] = React.useState(defaultUnits || 'imperial');
  const [reminderTime, setReminderTime] = React.useState('07:30');
  const [fastStartTime, setFastStartTime] = React.useState('20:00');
  const [fastingReminder, setFastingReminder] = React.useState(true);
  const [weightReminder, setWeightReminder] = React.useState(true);

  // Water
  const [waterGoal, setWaterGoal] = React.useState(2500); // ml
  const [waterLog, setWaterLog] = React.useState(() => {
    const t = new Date();
    const at = (h, m) => { const d = new Date(t); d.setHours(h, m, 0, 0); return d; };
    return [
      { time: at(7, 30), ml: 250 },
      { time: at(9, 15), ml: 500 },
      { time: at(11, 45), ml: 350 },
      { time: at(14, 10), ml: 500 },
    ];
  });
  const addWater = (ml) => setWaterLog((l) => [...l, { time: new Date(), ml }]);
  const removeWaterAt = (idx) => setWaterLog((l) => l.filter((_, i) => i !== idx));
  const waterTotal = waterLog.reduce((a, b) => a + b.ml, 0);
  const waterProgress = Math.min(1, waterTotal / waterGoal);

  // Sync from global defaults when they change
  React.useEffect(() => { if (defaultPlan) setPlan(defaultPlan); }, [defaultPlan]);
  React.useEffect(() => { if (defaultUnits) setUnits(defaultUnits); }, [defaultUnits]);

  const speed = defaultSpeed || 1;
  const planObj = PLANS.find((p) => p.id === plan) || PLANS[1];
  const elapsedMs = isFasting ? (Date.now() - fastStartMs) * speed : 0;
  const elapsedH = elapsedMs / 3600000;
  const goalH = planObj.fast;
  const progress = Math.min(1, elapsedH / goalH);
  const stage = stageForHours(elapsedH);
  const stageIdx = STAGES.indexOf(stage);

  const startFast = () => { setFastStartMs(Date.now()); setIsFasting(true); };
  const endFast = () => { setIsFasting(false); };
  const resetFast = () => { setFastStartMs(Date.now()); setIsFasting(true); };
  // Resume a previously-ended fast: keep its original start so elapsed continues.
  const resumeFast = (startMs) => { setFastStartMs(startMs != null ? startMs : fastStartMs); setIsFasting(true); };

  const history = React.useMemo(() => {
    const days = 84;
    const out = [];
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    let seed = 42;
    const rnd = () => { seed = (seed * 9301 + 49297) % 233280; return seed / 233280; };
    for (let i = days - 1; i >= 0; i--) {
      const d = new Date(today.getTime() - i * 86400000);
      const baseFast = 15 + Math.sin(i / 2) * 2 + rnd() * 1.5;
      const baseWeight = 172 + i * 0.16 + (rnd() - 0.5) * 1.4;
      const baseWater = 2000 + rnd() * 1000;
      out.push({
        date: d,
        fastHours: Math.max(0, baseFast),
        weight: baseWeight,
        waterMl: Math.round(baseWater),
      });
    }
    return out;
  }, []);

  // A representative most-recent completed fast (for the home recap card).
  // First-run installs have none yet.
  const lastFast = React.useMemo(() => {
    if (!hasLastFast) return null;
    const end = new Date(); end.setHours(11, 47, 0, 0);
    const durationH = 16.2;
    return { startMs: end.getTime() - durationH * 3600000, endMs: end.getTime(), durationH, goalH: 16, planLabel: '16:8' };
  }, [hasLastFast]);

  const api = {
    isFasting, fastStartMs, elapsedMs, elapsedH, goalH, progress,
    stage, stageIdx, stages: STAGES,
    plan, planObj, plans: PLANS, setPlan,
    units, setUnits,
    reminderTime, setReminderTime,
    fastStartTime, setFastStartTime,
    fastingReminder, setFastingReminder,
    weightReminder, setWeightReminder,
    waterGoal, setWaterGoal, waterLog, addWater, removeWaterAt, waterTotal, waterProgress,
    startFast, endFast, resetFast, resumeFast,
    speed,
    history,
    lastFast,
  };

  return <FastCtx.Provider value={api}>{children}</FastCtx.Provider>;
}

// ─────────────────────────────────────────────────────────────
// Minimal Android frame — replaces AndroidDevice so we can theme the
// status bar / device chrome to match our cream/charcoal palette.
// ─────────────────────────────────────────────────────────────
function Frame({ dark = false, children }) {
  const bg = dark ? '#14130f' : '#f6f3ee';
  const fg = dark ? '#f6f3ee' : '#14130f';
  return (
    <div style={{
      width: 412, height: 892, borderRadius: 18, overflow: 'hidden',
      background: bg,
      border: '8px solid ' + (dark ? '#0a0907' : '#cfcbc0'),
      display: 'flex', flexDirection: 'column', boxSizing: 'border-box',
      position: 'relative',
    }}>
      {/* Status bar */}
      <div style={{
        height: 36, display: 'flex', alignItems: 'center',
        justifyContent: 'space-between', padding: '0 18px',
        position: 'relative',
        fontFamily: 'Geist, sans-serif', fontWeight: 500,
        color: fg, fontSize: 13,
        flexShrink: 0,
      }}>
        <span className="tnum">9:41</span>
        {/* punch hole */}
        <div style={{
          position: 'absolute', left: '50%', top: 9, transform: 'translateX(-50%)',
          width: 18, height: 18, borderRadius: 100, background: dark ? '#000' : '#2e2e2e',
        }} />
        <span style={{ display: 'inline-flex', gap: 6, alignItems: 'center' }}>
          <svg width="14" height="10" viewBox="0 0 14 10" fill="currentColor"><path d="M7 9.5L.5 4a8 8 0 0113 0L7 9.5z"/></svg>
          <svg width="14" height="10" viewBox="0 0 14 10" fill="currentColor"><rect x="0" y="6" width="3" height="4" rx="0.5"/><rect x="4" y="3" width="3" height="7" rx="0.5"/><rect x="8" y="0" width="3" height="10" rx="0.5"/></svg>
          <span style={{ fontSize: 11 }} className="tnum">82%</span>
          <svg width="18" height="10" viewBox="0 0 18 10" fill="none" stroke="currentColor" strokeWidth="1.2"><rect x="0.6" y="0.6" width="14" height="8.8" rx="1.6"/><rect x="2" y="2" width="9" height="6" fill="currentColor"/><rect x="15.5" y="3" width="1.5" height="4" fill="currentColor" rx="0.7"/></svg>
        </span>
      </div>

      {/* App content */}
      <div className={'fast-app' + (dark ? ' dark' : '')} style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        {children}
      </div>

      {/* Gesture nav pill */}
      <div style={{ height: 18, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'transparent', flexShrink: 0 }}>
        <div style={{
          width: 108, height: 4, borderRadius: 2,
          background: fg, opacity: 0.35,
        }} />
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Phone — a single artboard's app shell with bottom-nav routing.
// Each Phone gets its own ScopedFastProvider so its fasting state is
// independent (lets us show "active" + "inactive" home side by side).
// ─────────────────────────────────────────────────────────────
function Phone({ start = 'home', isFasting = true, offsetH = 14.4, dark = false, fixed = null, hasLastFast = true }) {
  return (
    <Frame dark={dark}>
      <ScopedFastProvider initialFasting={isFasting} initialOffsetH={offsetH} hasLastFast={hasLastFast}>
        <PhoneShell start={start} fixed={fixed} />
      </ScopedFastProvider>
    </Frame>
  );
}

function PhoneShell({ start, fixed }) {
  const [page, setPage] = React.useState(start);
  const [modal, setModal] = React.useState(null);
  const [dayKey, setDayKey] = React.useState(null);
  const effective = fixed || page;
  const nav = (id, payload) => {
    setPage(id);
    if (payload && payload.dayKey != null) setDayKey(payload.dayKey);
  };
  const showStages = () => setModal('stages');

  // Render modal as overlay
  const screen = (() => {
    switch (effective) {
      case 'home': return <HomeScreen onNav={nav} onShowStages={showStages} />;
      case 'weight': return <WeightScreen onNav={nav} />;
      case 'water': return <WaterScreen onNav={nav} />;
      case 'progress': return <ProgressScreen onNav={nav} />;
      case 'history': return <HistoryScreen onNav={nav} />;
      case 'settings': return <SettingsScreen onNav={nav} onBack={() => setPage('home')} />;
      case 'plan-picker': return <PlanPickerScreen onBack={() => setPage('settings')} />;
      case 'day-detail': return <DayDetailScreen dayKey={dayKey} onNav={nav} onBack={() => setPage('progress')} />;
      case 'stages': return <StagesScreen onNav={nav} onBack={() => setPage('home')} />;
      case 'onboard-welcome': return <OnboardWelcomeScreen />;
      case 'onboard-plan': return <OnboardPlanScreen />;
      case 'onboard-reminders': return <OnboardRemindersScreen />;
      case 'notif': return <NotificationPanelScreen />;
      default: return <HomeScreen onNav={nav} onShowStages={showStages} />;
    }
  })();

  return (
    <div style={{ position: 'relative', height: '100%', overflow: 'hidden' }}>
      {screen}
      {modal === 'stages' && (
        <div style={{ position: 'absolute', inset: 0, background: 'var(--bg)' }}>
          <StagesScreen onBack={() => setModal(null)} />
        </div>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Main App
// ─────────────────────────────────────────────────────────────
function App() {
  const [t, setTweak] = useTweaks(/*EDITMODE-BEGIN*/{
    "dark": false,
    "speed": 1,
    "defaultPlan": "16:8",
    "defaultUnits": "metric"
  }/*EDITMODE-END*/);

  const defaults = {
    dark: t.dark,
    speed: Number(t.speed),
    plan: t.defaultPlan,
    units: t.defaultUnits,
  };

  return (
    <DefaultsProvider value={defaults}>
      <DesignCanvas>
        <DCSection id="home" title="Home" subtitle="The main screen across its states. Tap Start/End to switch.">
          <DCArtboard id="home-firstrun" label="Home · First run (no last fast)" width={412} height={892}>
            <Phone start="home" isFasting={false} offsetH={0} dark={t.dark} hasLastFast={false} />
          </DCArtboard>
          <DCArtboard id="home-active" label="Home · Fasting active" width={412} height={892}>
            <Phone start="home" isFasting={true} offsetH={14.4} dark={t.dark} />
          </DCArtboard>
          <DCArtboard id="home-inactive" label="Home · Not fasting (returning)" width={412} height={892}>
            <Phone start="home" isFasting={false} offsetH={0} dark={t.dark} />
          </DCArtboard>
          <DCArtboard id="stages-detail" label="Stages · Tap current stage card" width={412} height={892}>
            <Phone start="stages" isFasting={true} offsetH={14.4} dark={t.dark} fixed="stages" />
          </DCArtboard>
        </DCSection>

        <DCSection id="weight-progress" title="Weight & Water & Progress" subtitle="Daily logging and the trend">
          <DCArtboard id="weight" label="Weight entry" width={412} height={892}>
            <Phone start="weight" isFasting={true} offsetH={14.4} dark={t.dark} />
          </DCArtboard>
          <DCArtboard id="water" label="Water tracking" width={412} height={892}>
            <Phone start="water" isFasting={true} offsetH={14.4} dark={t.dark} />
          </DCArtboard>
          <DCArtboard id="progress" label="Progress · Chart + history" width={412} height={892}>
            <Phone start="progress" isFasting={true} offsetH={14.4} dark={t.dark} />
          </DCArtboard>
          <DCArtboard id="day-detail" label="Day detail · Edit entry" width={412} height={892}>
            <Phone start="day-detail" isFasting={true} offsetH={14.4} dark={t.dark} fixed="day-detail" />
          </DCArtboard>
        </DCSection>

        <DCSection id="settings-notif" title="Settings & system" subtitle="Preferences and the notification panel preview">
          <DCArtboard id="settings" label="Settings" width={412} height={892}>
            <Phone start="settings" isFasting={true} offsetH={14.4} dark={t.dark} />
          </DCArtboard>
          <DCArtboard id="notif-panel" label="Android notification panel" width={412} height={892}>
            <Phone start="notif" isFasting={true} offsetH={14.4} dark={t.dark} fixed="notif" />
          </DCArtboard>
        </DCSection>

        <DCSection id="onboarding" title="Onboarding" subtitle="First-run setup, three steps">
          <DCArtboard id="onb-welcome" label="01 · Welcome" width={412} height={892}>
            <Phone start="onboard-welcome" isFasting={false} dark={t.dark} fixed="onboard-welcome" />
          </DCArtboard>
          <DCArtboard id="onb-plan" label="02 · Choose plan" width={412} height={892}>
            <Phone start="onboard-plan" isFasting={false} dark={t.dark} fixed="onboard-plan" />
          </DCArtboard>
          <DCArtboard id="onb-reminders" label="03 · Reminders" width={412} height={892}>
            <Phone start="onboard-reminders" isFasting={false} dark={t.dark} fixed="onboard-reminders" />
          </DCArtboard>
        </DCSection>

        <DCSection id="dark" title="Dark mode" subtitle="Same app, dark theme">
          <DCArtboard id="home-dark" label="Home · Dark" width={412} height={892}>
            <Phone start="home" isFasting={true} offsetH={14.4} dark={true} />
          </DCArtboard>
          <DCArtboard id="progress-dark" label="Progress · Dark" width={412} height={892}>
            <Phone start="progress" isFasting={true} offsetH={14.4} dark={true} />
          </DCArtboard>
          <DCArtboard id="notif-dark" label="Notification · Dark" width={412} height={892}>
            <Phone start="notif" isFasting={true} offsetH={14.4} dark={true} fixed="notif" />
          </DCArtboard>
        </DCSection>
      </DesignCanvas>

      <TweaksPanel title="Tweaks">
        <TweakSection title="Theme">
          <TweakToggle label="Dark mode (global)" value={t.dark}
            onChange={(v) => setTweak('dark', v)} />
        </TweakSection>
        <TweakSection title="Demo">
          <TweakRadio label="Time speed" value={String(t.speed)}
            options={[
              { value: '1', label: '1×' },
              { value: '60', label: '60×' },
              { value: '600', label: '600×' },
            ]}
            onChange={(v) => setTweak('speed', Number(v))} />
          <div style={{ fontSize: 11, color: 'var(--twk-muted, #888)', padding: '0 2px 4px' }}>
            Tap "Start fasting" on a phone, then bump speed up to fast-forward through stages.
          </div>
        </TweakSection>
        <TweakSection title="Defaults">
          <TweakSelect label="Default plan" value={t.defaultPlan}
            options={PLANS.map((p) => ({ value: p.id, label: p.label + ' · ' + p.sub }))}
            onChange={(v) => setTweak('defaultPlan', v)} />
          <TweakRadio label="Units" value={t.defaultUnits}
            options={[{ value: 'imperial', label: 'lb' }, { value: 'metric', label: 'kg' }]}
            onChange={(v) => setTweak('defaultUnits', v)} />
        </TweakSection>
      </TweaksPanel>
    </DefaultsProvider>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
