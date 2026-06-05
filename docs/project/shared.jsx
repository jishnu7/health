// shared.jsx — Fast app: shared state, icons, components

// ─────────────────────────────────────────────────────────────
// Metabolic stages — durations are HOURS into the fast at which the
// stage begins. We display in order; current stage = last stage whose
// start ≤ elapsed.
// ─────────────────────────────────────────────────────────────
const STAGES = [
  {
    id: 'fed',
    name: 'Fed',
    short: 'Fed state',
    range: '0–4 h',
    start: 0,
    title: 'Anabolic state',
    body: 'Insulin is elevated. Your body is digesting and storing nutrients from your last meal.',
    benefits: ['Glucose is primary fuel', 'Glycogen stores filling', 'Protein synthesis active'],
    hue: 18, // terracotta-ish
  },
  {
    id: 'early',
    name: 'Early fast',
    short: 'Insulin falling',
    range: '4–8 h',
    start: 4,
    title: 'Glucose decline',
    body: 'Insulin levels drop. Blood glucose stabilizes as your body begins to use stored glycogen.',
    benefits: ['Insulin returning to baseline', 'Mild appetite reduction', 'Stable blood sugar'],
    hue: 32,
  },
  {
    id: 'glycogen',
    name: 'Glycogen burn',
    short: 'Liver glycogen',
    range: '8–12 h',
    start: 8,
    title: 'Glycogen depletion',
    body: 'The liver releases its stored glycogen to maintain blood sugar. Reserves are running down.',
    benefits: ['Liver glycogen depleting', 'Body preparing to shift fuel', 'Hunger signals quieting'],
    hue: 54,
  },
  {
    id: 'shift',
    name: 'Metabolic shift',
    short: 'Switching fuel',
    range: '12–14 h',
    start: 12,
    title: 'Fuel transition',
    body: 'With glycogen low, your body begins breaking down fat for energy. The metabolic switch is flipping.',
    benefits: ['Lipolysis begins', 'Free fatty acids rise', 'Mental clarity often reported'],
    hue: 96,
  },
  {
    id: 'burn',
    name: 'Fat burn',
    short: 'Lipolysis',
    range: '14–16 h',
    start: 14,
    title: 'Fat metabolism',
    body: 'You are now running primarily on fat. Stored triglycerides break down into fatty acids for fuel.',
    benefits: ['Fat oxidation accelerating', 'Insulin sensitivity improving', 'Steady energy'],
    hue: 140,
  },
  {
    id: 'ketosis',
    name: 'Light ketosis',
    short: 'Ketones rising',
    range: '16–20 h',
    start: 16,
    title: 'Ketogenesis begins',
    body: 'The liver starts producing ketones from fatty acids. Your brain begins using them alongside glucose.',
    benefits: ['Ketone production begins', 'Reduced inflammation', 'Improved focus'],
    hue: 162,
  },
  {
    id: 'deep',
    name: 'Deep ketosis',
    short: 'Ketogenic fuel',
    range: '20–24 h',
    start: 20,
    title: 'Fat-adapted',
    body: 'Ketone levels rise significantly. Most of your energy is now coming from fat-derived fuel.',
    benefits: ['Significant fat loss window', 'Cognitive benefits peak', 'Appetite suppression'],
    hue: 188,
  },
  {
    id: 'autophagy',
    name: 'Autophagy',
    short: 'Cellular renewal',
    range: '24+ h',
    start: 24,
    title: 'Cellular cleanup',
    body: 'Autophagy ramps up — cells recycle damaged components. Growth hormone elevates and cellular renewal begins.',
    benefits: ['Damaged proteins recycled', 'HGH elevation begins', 'Longevity pathways active'],
    hue: 220,
  },
];

function stageForHours(h) {
  let s = STAGES[0];
  for (const x of STAGES) if (x.start <= h) s = x;
  return s;
}

// Short present-tense status line per metabolic stage (home energy card).
const STAGE_MSG = {
  fed:       'Digesting your last meal',
  early:     'Insulin levels are falling',
  glycogen:  'Burning through stored carbs',
  shift:     'Switching over to fat for fuel',
  burn:      'Running primarily on body fat',
  ketosis:   'Ketones are rising',
  deep:      'Fully fat-adapted',
  autophagy: 'Cellular cleanup underway',
};
function nextStageForHours(h) { return STAGES.find((x) => x.start > h) || null; }

// ─────────────────────────────────────────────────────────────
// Energy-utilization phases — a simplified 4-phase lens over the
// fast, used by the home-screen energy card. Hours are into the fast.
// ─────────────────────────────────────────────────────────────
const ENERGY_PHASES = [
  { id: 'digest', label: 'Digesting',     start: 0,  end: 4,  msg: 'Digestion slowing' },
  { id: 'glyco',  label: 'Glycogen',      start: 4,  end: 12, msg: 'Stored carbs powering your fast' },
  { id: 'trans',  label: 'Transition',    start: 12, end: 16, msg: 'Stored energy usage increasing' },
  { id: 'stored', label: 'Stored Energy', start: 16, end: 24, msg: 'High fat utilization phase' },
];
function energyPhaseForHours(h) {
  let s = ENERGY_PHASES[0];
  for (const x of ENERGY_PHASES) if (h >= x.start) s = x;
  return s;
}
function nextEnergyPhase(h) {
  return ENERGY_PHASES.find((x) => x.start > h) || null;
}

// ─────────────────────────────────────────────────────────────
// Plans — common IF schedules
// ─────────────────────────────────────────────────────────────
const PLANS = [
  { id: '14:10', label: '14:10', fast: 14, sub: 'Starter — gentle' },
  { id: '16:8',  label: '16:8',  fast: 16, sub: 'Most common' },
  { id: '18:6',  label: '18:6',  fast: 18, sub: 'Intermediate' },
  { id: '20:4',  label: '20:4',  fast: 20, sub: 'Warrior' },
  { id: '23:1',  label: '23:1',  fast: 23, sub: 'OMAD' },
];

// ─────────────────────────────────────────────────────────────
// Shared app state — one source of truth, shared across all
// artboards via context.
// ─────────────────────────────────────────────────────────────
const FastCtx = React.createContext(null);

function FastProvider({ children, demo = {} }) {
  // Demo time: by default, we pretend "now" is fixed at a recent moment so
  // every artboard shows the same time. Demo speed multiplies elapsed time
  // for "cycle through stages" tweak.
  const [tick, setTick] = React.useState(0);
  React.useEffect(() => {
    const t = setInterval(() => setTick((x) => x + 1), 1000);
    return () => clearInterval(t);
  }, []);

  // Backed by useState so start/stop is interactive.
  // Default: fasting started 14h 23m ago — puts the user mid-fat-burn.
  const [fastStartMs, setFastStartMs] = React.useState(() => Date.now() - (14 * 3600 + 23 * 60) * 1000);
  const [isFasting, setIsFasting] = React.useState(true);
  const [plan, setPlan] = React.useState('16:8');
  const [units, setUnits] = React.useState('imperial'); // 'metric' | 'imperial'
  const [reminderTime, setReminderTime] = React.useState('07:30');
  const [fastStartTime, setFastStartTime] = React.useState('20:00');
  const [fastingReminder, setFastingReminder] = React.useState(true);
  const [weightReminder, setWeightReminder] = React.useState(true);

  // Water tracking
  const [waterGoal, setWaterGoal] = React.useState(2500); // ml
  // Today's log: array of { time: Date, ml: number }
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

  // Demo speed multiplier (1 = real, 60 = 1s ≈ 1m, etc).
  const [speed, setSpeed] = React.useState(demo.speed ?? 1);

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

  // Mock history — fasting hours by day, weight by day, water ml by day (last 14 days).
  // Locked so charts are stable across renders.
  const history = React.useMemo(() => {
    const days = 14;
    const out = [];
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    // Deterministic pseudo-random
    let seed = 42;
    const rnd = () => { seed = (seed * 9301 + 49297) % 233280; return seed / 233280; };
    for (let i = days - 1; i >= 0; i--) {
      const d = new Date(today.getTime() - i * 86400000);
      const baseFast = 15 + Math.sin(i / 2) * 2 + rnd() * 1.5;
      const baseWeight = 178.4 - i * 0.18 + (rnd() - 0.5) * 1.2;
      const baseWater = 2000 + rnd() * 1000; // 2000-3000ml
      out.push({
        date: d,
        fastHours: Math.max(0, baseFast),
        weight: baseWeight,
        waterMl: Math.round(baseWater),
      });
    }
    return out;
  }, []);

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
    startFast, endFast, resetFast,
    speed, setSpeed,
    history,
  };

  return <FastCtx.Provider value={api}>{children}</FastCtx.Provider>;
}

const useFast = () => React.useContext(FastCtx);

// ─────────────────────────────────────────────────────────────
// Icons — minimal stroke set, currentColor
// ─────────────────────────────────────────────────────────────
const Icon = {
  Home: (p) => (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...p}>
      <path d="M3 11l9-7 9 7v9a2 2 0 01-2 2h-4v-7h-6v7H5a2 2 0 01-2-2v-9z"/>
    </svg>
  ),
  Scale: (p) => (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...p}>
      <rect x="3" y="4" width="18" height="16" rx="3"/>
      <path d="M8 9h8M10 14l2-3 2 3"/>
    </svg>
  ),
  Chart: (p) => (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...p}>
      <path d="M3 20V6M3 20h18M7 16l4-5 3 3 5-7"/>
    </svg>
  ),
  History: (p) => (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...p}>
      <path d="M3 12a9 9 0 109-9 9 9 0 00-7 3.3M3 4v4h4"/>
      <path d="M12 7v5l3 2"/>
    </svg>
  ),
  Settings: (p) => (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...p}>
      <circle cx="12" cy="12" r="3"/>
      <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 11-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 11-4 0v-.09a1.65 1.65 0 00-1-1.51 1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 11-2.83-2.83l.06-.06a1.65 1.65 0 00.33-1.82 1.65 1.65 0 00-1.51-1H3a2 2 0 110-4h.09a1.65 1.65 0 001.51-1 1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 112.83-2.83l.06.06a1.65 1.65 0 001.82.33h0a1.65 1.65 0 001-1.51V3a2 2 0 114 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 112.83 2.83l-.06.06a1.65 1.65 0 00-.33 1.82v0a1.65 1.65 0 001.51 1H21a2 2 0 110 4h-.09a1.65 1.65 0 00-1.51 1z"/>
    </svg>
  ),
  Plus: (p) => (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" {...p}>
      <path d="M12 5v14M5 12h14"/>
    </svg>
  ),
  Minus: (p) => (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" {...p}>
      <path d="M5 12h14"/>
    </svg>
  ),
  Bell: (p) => (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...p}>
      <path d="M6 8a6 6 0 1112 0c0 7 3 9 3 9H3s3-2 3-9M10 21a2 2 0 004 0"/>
    </svg>
  ),
  Back: (p) => (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...p}>
      <path d="M15 18l-6-6 6-6"/>
    </svg>
  ),
  Check: (p) => (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" {...p}>
      <path d="M5 12l4.5 4.5L19 7"/>
    </svg>
  ),
  Chevron: (p) => (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" {...p}>
      <path d="M9 6l6 6-6 6"/>
    </svg>
  ),
  Flame: (p) => (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" {...p}>
      <path d="M12 2c0 4-3 5-3 9a3 3 0 006 0c0-2-1-3-1-5 2 1 4 3 4 7a6 6 0 11-12 0c0-5 6-7 6-11z"/>
    </svg>
  ),
  Drop: (p) => (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" {...p}>
      <path d="M12 3l6 9a6 6 0 11-12 0l6-9z"/>
    </svg>
  ),
  Water: (p) => (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" strokeLinecap="round" {...p}>
      <path d="M12 2.5c4 5 6.5 8.2 6.5 11.5a6.5 6.5 0 11-13 0c0-3.3 2.5-6.5 6.5-11.5z"/>
      <path d="M9.5 14a3 3 0 002.5 2.5" opacity="0.5"/>
    </svg>
  ),
  Food: (p) => (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...p}>
      <path d="M4 4v8a4 4 0 008 0V4M6 4v6M10 4v6M18 4c-2 0-3 3-3 7v3h6V4c-3 0-3 0-3 0zM18 14v6"/>
    </svg>
  ),
  Stop: (p) => (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" {...p}>
      <rect x="6" y="6" width="12" height="12" rx="2"/>
    </svg>
  ),
  Play: (p) => (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" {...p}>
      <path d="M7 5l12 7-12 7V5z"/>
    </svg>
  ),
  Clock: (p) => (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...p}>
      <circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>
    </svg>
  ),
  Share: (p) => (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" {...p}>
      <path d="M12 3v13"/><path d="M8 7l4-4 4 4"/><path d="M5 13v6a2 2 0 002 2h10a2 2 0 002-2v-6"/>
    </svg>
  ),
};

// ─────────────────────────────────────────────────────────────
// Format helpers
// ─────────────────────────────────────────────────────────────
function fmtDuration(ms) {
  const totalSec = Math.max(0, Math.floor(ms / 1000));
  const h = Math.floor(totalSec / 3600);
  const m = Math.floor((totalSec % 3600) / 60);
  const s = totalSec % 60;
  return { h, m, s, hh: String(h).padStart(2, '0'), mm: String(m).padStart(2, '0'), ss: String(s).padStart(2, '0') };
}
function fmtTime(date) {
  return date.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
}
function addHoursToTime(hhmm, hours) {
  // hhmm: "HH:MM" 24h; returns "HH:MM" 24h, wrapping past midnight.
  const [h, m] = hhmm.split(':').map(Number);
  const total = (h * 60 + m + hours * 60 + 24 * 60) % (24 * 60);
  const oh = Math.floor(total / 60);
  const om = Math.round(total % 60);
  return String(oh).padStart(2, '0') + ':' + String(om).padStart(2, '0');
}
function diffHoursTime(start, end) {
  // Hours between two "HH:MM"s, assuming end follows start (wraps midnight).
  const [sh, sm] = start.split(':').map(Number);
  const [eh, em] = end.split(':').map(Number);
  let total = (eh * 60 + em) - (sh * 60 + sm);
  if (total <= 0) total += 24 * 60;
  return total / 60;
}
function fmtDate(date, opts = { weekday: 'short', month: 'short', day: 'numeric' }) {
  return date.toLocaleDateString([], opts);
}
function lbToKg(lb) { return lb * 0.45359237; }
function fmtWeight(lb, units) {
  if (units === 'metric') return { val: lbToKg(lb).toFixed(1), unit: 'kg' };
  return { val: lb.toFixed(1), unit: 'lb' };
}

// Water. We store everything in ml internally and convert for display.
function mlToOz(ml) { return ml / 29.5735; }
function ozToMl(oz) { return oz * 29.5735; }
function fmtWater(ml, units) {
  if (units === 'metric') return { val: Math.round(ml).toString(), unit: 'ml' };
  return { val: mlToOz(ml).toFixed(1), unit: 'fl oz' };
}
// Preset quick-add volumes (ml + label + size hint for visual)
const WATER_PRESETS = [
  { ml: 250, label: 'Glass',  hint: 'sm' },
  { ml: 350, label: 'Cup',    hint: 'md' },
  { ml: 500, label: 'Bottle', hint: 'lg' },
  { ml: 750, label: 'Flask',  hint: 'xl' },
];

// ─────────────────────────────────────────────────────────────
// Header / bottom nav UI components
// ─────────────────────────────────────────────────────────────
function FastHeader({ title, left, right, children }) {
  return (
    <div className="fast-header">
      <div style={{ width: 40, display: 'flex', justifyContent: 'flex-start' }}>{left}</div>
      <div className="fast-header-title">{title || children}</div>
      <div style={{ width: 40, display: 'flex', justifyContent: 'flex-end' }}>{right}</div>
    </div>
  );
}

function BottomNav({ active = 'home', onChange }) {
  const items = [
    { id: 'home', label: 'Today', icon: Icon.Home },
    { id: 'weight', label: 'Weight', icon: Icon.Scale },
    { id: 'water', label: 'Water', icon: Icon.Water },
    { id: 'progress', label: 'Progress', icon: Icon.Chart },
  ];
  return (
    <nav className="fast-nav">
      {items.map((it) => {
        const I = it.icon;
        return (
          <button key={it.id} className={'fast-nav-item' + (active === it.id ? ' active' : '')}
            onClick={() => onChange && onChange(it.id)}>
            <div className="fast-nav-icon"><I /></div>
            <span>{it.label}</span>
          </button>
        );
      })}
    </nav>
  );
}

// ─────────────────────────────────────────────────────────────
// Progress ring — used on the home screen
// ─────────────────────────────────────────────────────────────
function ProgressRing({ size = 240, stroke = 14, progress = 0, color = 'var(--primary)', track = 'var(--border)', children, dashed = false }) {
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  return (
    <div style={{ position: 'relative', width: size, height: size }}>
      <svg width={size} height={size} style={{ transform: 'rotate(-90deg)' }}>
        <circle cx={size / 2} cy={size / 2} r={r} stroke={track} strokeWidth={stroke} fill="none"
          strokeDasharray={dashed ? '2 6' : undefined} />
        <circle cx={size / 2} cy={size / 2} r={r} stroke={color} strokeWidth={stroke} fill="none"
          strokeLinecap="round" strokeDasharray={c} strokeDashoffset={c * (1 - progress)} />
      </svg>
      <div style={{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
        {children}
      </div>
    </div>
  );
}

// Stage progress dots — small linear indicator
function StageDots({ stages, currentIdx }) {
  return (
    <div style={{ display: 'flex', gap: 4, width: '100%' }}>
      {stages.map((s, i) => (
        <div key={s.id} style={{
          flex: s.id === 'autophagy' ? 1.5 : 1,
          height: 4,
          borderRadius: 2,
          background: i <= currentIdx ? 'var(--primary)' : 'var(--border)',
          transition: 'background .2s',
        }} />
      ))}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Energy phase card (home screen) — the active energy-utilization
// phase as a focus panel: tinted header + message, a 4-segment
// ribbon, and a "next phase" countdown. Tappable to open Stages.
// ─────────────────────────────────────────────────────────────
function EnergyPhaseCard({ onOpen }) {
  const f = useFast();
  const h = f.elapsedH;
  const act = stageForHours(h);
  const nxt = nextStageForHours(h);
  const svar = (id) => `var(--stage-${id})`;
  const c = svar(act.id);
  const clamp01 = (x) => Math.max(0, Math.min(1, x));
  // Stage arcs across the 24h dial (autophagy begins at the 24h rim).
  const segs = STAGES.filter((s) => s.start < 24).map((s, i, arr) => ({ ...s, end: arr[i + 1] ? arr[i + 1].start : 24 }));
  const fmtHM = (hh) => {
    const H = Math.floor(hh);
    const M = Math.round((hh - H) * 60);
    return M === 60 ? `${H + 1}h 00m` : `${H}h ${String(M).padStart(2, '0')}m`;
  };
  const Tag = onOpen ? 'button' : 'div';
  return (
    <Tag className="card" onClick={onOpen || undefined}
      style={{ width: '100%', textAlign: 'left', padding: 0, overflow: 'hidden', display: 'block' }}>
      {/* hero */}
      <div style={{ padding: '18px 18px 16px', background: `color-mix(in srgb, ${c} 12%, var(--card))` }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ width: 8, height: 8, borderRadius: 4, background: c }} />
            <span className="h-eyebrow" style={{ color: 'var(--ink-2)' }}>{act.name}</span>
          </div>
          {onOpen && <Icon.Chevron style={{ color: 'var(--muted)' }} />}
        </div>
        <div style={{ fontSize: 21, fontWeight: 500, letterSpacing: '-0.02em', marginTop: 8, lineHeight: 1.2 }}>
          {STAGE_MSG[act.id]}
        </div>
      </div>
      {/* ribbon */}
      <div style={{ padding: '14px 18px 16px' }}>
        <div style={{ display: 'flex', gap: 3, height: 8 }}>
          {segs.map((s) => {
            const sc = svar(s.id);
            const fill = clamp01((h - s.start) / (s.end - s.start));
            return (
              <div key={s.id} style={{ flexGrow: s.end - s.start, position: 'relative', borderRadius: 4, overflow: 'hidden', background: `color-mix(in oklab, ${sc} 18%, transparent)` }}>
                <div style={{ position: 'absolute', inset: 0, width: (fill * 100) + '%', background: sc, borderRadius: 4, transition: 'width .4s ease' }} />
              </div>
            );
          })}
        </div>
        <div style={{ marginTop: 14, paddingTop: 12, borderTop: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span className="caption">
            {nxt
              ? <>Next: <span style={{ color: 'var(--ink-2)', fontWeight: 500 }}>{nxt.name}</span></>
              : <span style={{ color: 'var(--ink-2)', fontWeight: 500 }}>Deepest stage reached</span>}
          </span>
          {nxt && <span className="mono tnum" style={{ fontSize: 13, fontWeight: 500, color: c, whiteSpace: 'nowrap' }}>in {fmtHM(nxt.start - h)}</span>}
        </div>
      </div>
    </Tag>
  );
}

// ─────────────────────────────────────────────────────────────
// Stages preview card (home, not fasting) — tinted teaser that
// opens the metabolic stages page on tap. Mirrors the fasting card.
// ─────────────────────────────────────────────────────────────
function StagesPreviewCard({ onOpen }) {
  const svar = (id) => `var(--stage-${id})`;
  const segs = STAGES.filter((s) => s.start < 24).map((s, i, arr) => ({ ...s, end: arr[i + 1] ? arr[i + 1].start : 24 }));
  return (
    <button className="card" onClick={onOpen || undefined}
      style={{ width: '100%', textAlign: 'left', padding: 0, overflow: 'hidden', display: 'block' }}>
      <div style={{ padding: '16px 18px 14px', background: 'color-mix(in srgb, var(--primary) 10%, var(--card))' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <span className="h-eyebrow" style={{ color: 'var(--ink-2)' }}>Metabolic stages</span>
          <Icon.Chevron style={{ color: 'var(--muted)' }} />
        </div>
        <div style={{ fontSize: 18, fontWeight: 500, letterSpacing: '-0.01em', marginTop: 6 }}>Preview the journey ahead</div>
      </div>
      <div style={{ padding: '14px 18px 16px' }}>
        <div style={{ display: 'flex', gap: 3, height: 8 }}>
          {segs.map((s) => (
            <div key={s.id} style={{ flexGrow: s.end - s.start, height: '100%', borderRadius: 4, background: svar(s.id), opacity: 0.9 }} />
          ))}
        </div>
        <div className="caption" style={{ marginTop: 10 }}>Eight stages, from fed to deep ketosis &amp; autophagy.</div>
      </div>
    </button>
  );
}

// ─────────────────────────────────────────────────────────────
// Brand glyph — tiny ring + dot, for share/footer contexts
// ─────────────────────────────────────────────────────────────
function RingGlyph({ size = 15, ring = 'var(--primary)', dot = 'var(--accent)' }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" style={{ display: 'block' }}>
      <circle cx="12" cy="12" r="8.5" fill="none" stroke={ring} strokeWidth="3" />
      <circle cx="12" cy="3.5" r="2.4" fill={dot} />
    </svg>
  );
}

// ─────────────────────────────────────────────────────────────
// Last-fast recap card (home, not-fasting) — a shareable summary:
// duration, goal-met, the energy phases reached, start→end times,
// and a subtle brand footer so a screenshot stands on its own.
// ─────────────────────────────────────────────────────────────
function LastFastCard({ onShare, fast, edit }) {
  const f = useFast();
  const lf = fast || f.lastFast || {
    startMs: (() => { const e = new Date(); e.setHours(11, 47, 0, 0); return e.getTime() - 16.2 * 3600000; })(),
    endMs: (() => { const e = new Date(); e.setHours(11, 47, 0, 0); return e.getTime(); })(),
    durationH: 16.2,
    goalH: (f.planObj && f.planObj.fast) || 16,
    planLabel: (f.planObj && f.planObj.label) || '16:8',
  };

  const start = new Date(lf.startMs);
  const end = new Date(lf.endMs);
  const durH = lf.durationH;
  const H = Math.floor(durH);
  const M = Math.round((durH - H) * 60);
  const pct = Math.round((durH / lf.goalH) * 100);
  const goalMet = durH >= lf.goalH;
  const reached = stageForHours(durH);
  const scvar = (id) => `var(--stage-${id})`;
  const clamp01 = (x) => Math.max(0, Math.min(1, x));
  // Stage arcs across the 24h dial (autophagy begins at the 24h rim).
  const segs = STAGES.filter((s) => s.start < 24).map((s, i, arr) => ({ ...s, end: arr[i + 1] ? arr[i + 1].start : 24 }));

  const now = new Date();
  const dayLabel = end.toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric' });

  return (
    <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
      {/* header band — bold, share-ready */}
      <div style={{ position: 'relative', overflow: 'hidden', padding: '15px 18px 18px', background: 'linear-gradient(135deg, #2a4d3e 0%, #3d6b56 46%, #c46a45 92%, #e08a4f 120%)' }}>
        {/* brand ring motif */}
        <svg aria-hidden="true" viewBox="0 0 100 100" style={{ position: 'absolute', top: -26, right: -22, width: 132, height: 132, opacity: 0.18, pointerEvents: 'none' }}>
          <circle cx="50" cy="50" r="46" fill="none" stroke="#fdfbf7" strokeWidth="0.8" />
          <circle cx="50" cy="50" r="34" fill="none" stroke="#fdfbf7" strokeWidth="0.8" />
          <circle cx="50" cy="50" r="22" fill="none" stroke="#fdfbf7" strokeWidth="0.8" />
          {Array.from({ length: 24 }).map((_, i) => {
            const a = (i / 24) * Math.PI * 2 - Math.PI / 2;
            const isC = i % 6 === 0;
            const inner = 46, outer = inner + (isC ? 5 : 2.6);
            return <line key={i} x1={50 + Math.cos(a) * inner} y1={50 + Math.sin(a) * inner} x2={50 + Math.cos(a) * outer} y2={50 + Math.sin(a) * outer} stroke="#fdfbf7" strokeWidth={isC ? 1.1 : 0.6} strokeLinecap="round" />;
          })}
        </svg>

        {/* brand lockup + share */}
        <div style={{ position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 7 }}>
            <RingGlyph size={16} ring="rgba(253,251,247,0.95)" dot="#f1b79f" />
            <span style={{ fontSize: 13, fontWeight: 600, letterSpacing: '-0.01em', color: '#fdfbf7' }}>Fast</span>
          </div>
          <button className="fast-iconbtn" style={{ width: 32, height: 32, color: '#fdfbf7', background: 'rgba(255,255,255,0.16)' }}
            onClick={onShare || undefined}><Icon.Share style={{ width: 16, height: 16 }} /></button>
        </div>

        {/* date + duration */}
        <div style={{ position: 'relative', marginTop: 15 }}>
          <div style={{ fontSize: 10.5, fontWeight: 600, letterSpacing: '0.14em', textTransform: 'uppercase', color: 'rgba(253,251,247,0.72)' }}>{dayLabel}</div>
          <div className="mono" style={{ fontSize: 46, fontWeight: 400, letterSpacing: '-0.03em', lineHeight: 1, marginTop: 8, color: '#fdfbf7' }}>
            {H}<span style={{ fontSize: 23, color: 'rgba(253,251,247,0.65)' }}>h</span> {String(M).padStart(2, '0')}<span style={{ fontSize: 23, color: 'rgba(253,251,247,0.65)' }}>m</span>
          </div>
        </div>

        {/* goal chip */}
        <div style={{ position: 'relative', marginTop: 14 }}>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, padding: '5px 11px', borderRadius: 999, background: 'rgba(255,255,255,0.17)', color: '#fdfbf7', fontSize: 12, fontWeight: 500 }}>
            {goalMet && <Icon.Check style={{ width: 14, height: 14 }} />}
            {lf.planLabel} · {goalMet ? 'Goal reached' : pct + '% of goal'}
          </span>
        </div>
      </div>

      {/* body */}
      <div style={{ padding: '14px 18px 16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
          <span className="h-eyebrow">Stage reached</span>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 12, fontWeight: 500, whiteSpace: 'nowrap' }}>
            <span style={{ width: 7, height: 7, borderRadius: 4, background: scvar(reached.id) }} />
            {reached.name}
          </span>
        </div>
        <div style={{ display: 'flex', gap: 3, height: 8 }}>
          {segs.map((s) => {
            const c = scvar(s.id);
            const fill = clamp01((durH - s.start) / (s.end - s.start));
            return (
              <div key={s.id} style={{ flexGrow: s.end - s.start, position: 'relative', borderRadius: 4, overflow: 'hidden', background: `color-mix(in oklab, ${c} 18%, transparent)` }}>
                <div style={{ position: 'absolute', inset: 0, width: (fill * 100) + '%', background: c, borderRadius: 4 }} />
              </div>
            );
          })}
        </div>

        <div style={{ marginTop: 14, paddingTop: 12, borderTop: '1px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <div className="caption">Started</div>
            {edit ? (
              <label style={{ position: 'relative', display: 'inline-flex', alignItems: 'center', gap: 4, marginTop: 2, cursor: 'pointer' }}>
                <span className="tnum" style={{ fontSize: 14, fontWeight: 500 }}>{fmtTime(start)}</span>
                <Icon.Chevron style={{ width: 12, height: 12, color: 'var(--muted)', transform: 'rotate(90deg)' }} />
                <input type="time" value={edit.start} onChange={(e) => e.target.value && edit.onStart(e.target.value)}
                  style={{ position: 'absolute', inset: 0, width: '100%', opacity: 0, cursor: 'pointer' }} />
              </label>
            ) : (
              <div className="tnum" style={{ fontSize: 14, fontWeight: 500, marginTop: 2 }}>{fmtTime(start)}</div>
            )}
          </div>
          <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, color: 'var(--subtle)' }}>
            <div style={{ height: 1, background: 'var(--border)', flex: 1, maxWidth: 40 }} />
            <Icon.Chevron style={{ width: 13, height: 13 }} />
            <div style={{ height: 1, background: 'var(--border)', flex: 1, maxWidth: 40 }} />
          </div>
          <div style={{ textAlign: 'right' }}>
            <div className="caption">Ended</div>
            {edit ? (
              <label style={{ position: 'relative', display: 'inline-flex', alignItems: 'center', gap: 4, marginTop: 2, cursor: 'pointer' }}>
                <span className="tnum" style={{ fontSize: 14, fontWeight: 500 }}>{fmtTime(end)}</span>
                <Icon.Chevron style={{ width: 12, height: 12, color: 'var(--muted)', transform: 'rotate(90deg)' }} />
                <input type="time" value={edit.end} onChange={(e) => e.target.value && edit.onEnd(e.target.value)}
                  style={{ position: 'absolute', inset: 0, width: '100%', opacity: 0, cursor: 'pointer' }} />
              </label>
            ) : (
              <div className="tnum" style={{ fontSize: 14, fontWeight: 500, marginTop: 2 }}>{fmtTime(end)}</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Weight trend card (weight screen) — weekly low–high range bands
// with the weekly-average trend line. Distinct from the Progress
// page's dual-axis line chart.
// ─────────────────────────────────────────────────────────────
function WeightTrendCard({ weeks = 8 }) {
  const f = useFast();
  const units = f.units;
  const toDisp = (x) => units === 'metric' ? x * 0.45359237 : x;
  const unit = units === 'metric' ? 'kg' : 'lb';
  const hist = f.history.slice(-weeks * 7).map((d) => ({ date: d.date, weight: toDisp(d.weight) }));
  const last = hist[hist.length - 1].weight;
  const change = last - hist[0].weight;

  const smooth = (pts) => {
    if (pts.length < 2) return pts.length ? `M ${pts[0].x} ${pts[0].y}` : '';
    const d = [`M ${pts[0].x} ${pts[0].y}`];
    for (let i = 0; i < pts.length - 1; i++) {
      const p0 = pts[i - 1] || pts[i], p1 = pts[i], p2 = pts[i + 1], p3 = pts[i + 2] || p2;
      const c1x = p1.x + (p2.x - p0.x) / 6, c1y = p1.y + (p2.y - p0.y) / 6;
      const c2x = p2.x - (p3.x - p1.x) / 6, c2y = p2.y - (p3.y - p1.y) / 6;
      d.push(`C ${c1x.toFixed(2)} ${c1y.toFixed(2)} ${c2x.toFixed(2)} ${c2y.toFixed(2)} ${p2.x.toFixed(2)} ${p2.y.toFixed(2)}`);
    }
    return d.join(' ');
  };
  const fmtDay = (dt) => dt.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });

  const W = 336, H = 180, PAD = { t: 12, r: 12, b: 22, l: 30 };
  const iW = W - PAD.l - PAD.r, iH = H - PAD.t - PAD.b;
  const weekData = [];
  for (let i = 0; i < hist.length; i += 7) {
    const chunk = hist.slice(i, i + 7);
    if (!chunk.length) continue;
    const ws = chunk.map((d) => d.weight);
    weekData.push({ min: Math.min(...ws), max: Math.max(...ws), avg: ws.reduce((a, b) => a + b, 0) / ws.length, date: chunk[0].date });
  }
  const allLo = Math.min(...weekData.map((w) => w.min)) - 1;
  const allHi = Math.max(...weekData.map((w) => w.max)) + 1;
  const yAt = (v) => PAD.t + (1 - (v - allLo) / (allHi - allLo)) * iH;
  const n = weekData.length;
  const slot = iW / n;
  const bw = Math.min(16, slot * 0.5);
  const cx = (i) => PAD.l + slot * (i + 0.5);
  const avgPts = weekData.map((w, i) => ({ x: cx(i), y: yAt(w.avg) }));
  const ticks = 3;
  const tickVals = Array.from({ length: ticks + 1 }, (_, i) => allLo + (allHi - allLo) * (i / ticks));

  return (
    <div className="card" style={{ padding: 18 }}>
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
        <div>
          <div className="h-eyebrow">Weight trend</div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, marginTop: 6 }}>
            <span className="h-display tnum" style={{ fontSize: 30 }}>{last.toFixed(1)}</span>
            <span style={{ fontSize: 15, color: 'var(--muted)' }}>{unit}</span>
          </div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div className="caption">{weeks}-week change</div>
          <div className="tnum" style={{ fontSize: 15, fontWeight: 500, marginTop: 4, color: change < 0 ? 'var(--primary)' : 'var(--accent)' }}>
            {change < 0 ? '−' : '+'}{Math.abs(change).toFixed(1)} {unit}
          </div>
        </div>
      </div>
      <div style={{ marginTop: 14 }}>
        <svg width="100%" viewBox={`0 0 ${W} ${H}`} style={{ display: 'block' }}>
          {tickVals.map((v, i) => (
            <g key={i}>
              <line x1={PAD.l} x2={W - PAD.r} y1={yAt(v)} y2={yAt(v)} stroke="var(--border)" strokeWidth="1" strokeDasharray={i === ticks ? '0' : '2 4'} />
              <text x={PAD.l - 6} y={yAt(v) + 3} fontSize="9" fill="var(--muted)" textAnchor="end" fontFamily="Geist Mono">{v.toFixed(0)}</text>
            </g>
          ))}
          {weekData.map((w, i) => (
            <rect key={i} x={cx(i) - bw / 2} y={yAt(w.max)} width={bw} height={Math.max(2, yAt(w.min) - yAt(w.max))} rx={bw / 2} fill="var(--primary)" opacity="0.16" />
          ))}
          <path d={smooth(avgPts)} fill="none" stroke="var(--primary)" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
          {avgPts.map((p, i) => <circle key={i} cx={p.x} cy={p.y} r="3.2" fill="var(--card)" stroke="var(--primary)" strokeWidth="2" />)}
          {weekData.map((w, i) => (i % 2 === 0 || n <= 6) ? <text key={i} x={cx(i)} y={H - 6} fontSize="9" fill="var(--muted)" textAnchor="middle" fontFamily="Geist Mono">{fmtDay(w.date)}</text> : null)}
        </svg>
      </div>
      <div style={{ marginTop: 8, display: 'flex', alignItems: 'center', gap: 16 }}>
        <span className="caption" style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 12, height: 10, borderRadius: 3, background: 'var(--primary)', opacity: 0.16 }} /> Weekly range
        </span>
        <span className="caption" style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 16, height: 2, borderRadius: 2, background: 'var(--primary)' }} /> Average
        </span>
      </div>
    </div>
  );
}

Object.assign(window, {
  STAGES, PLANS, stageForHours,
  ENERGY_PHASES, energyPhaseForHours, nextEnergyPhase, EnergyPhaseCard,
  STAGE_MSG, nextStageForHours, StagesPreviewCard,
  RingGlyph, LastFastCard, WeightTrendCard,
  FastCtx, FastProvider, useFast,
  Icon, fmtDuration, fmtTime, fmtDate, lbToKg, fmtWeight, mlToOz, ozToMl, fmtWater, WATER_PRESETS, addHoursToTime, diffHoursTime,
  FastHeader, BottomNav, ProgressRing, StageDots,
});
