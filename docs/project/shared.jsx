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

  // Mock history — fasting hours by day, weight by day (last 14 days).
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
      out.push({
        date: d,
        fastHours: Math.max(0, baseFast),
        weight: baseWeight,
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

Object.assign(window, {
  STAGES, PLANS, stageForHours,
  FastCtx, FastProvider, useFast,
  Icon, fmtDuration, fmtTime, fmtDate, lbToKg, fmtWeight, addHoursToTime, diffHoursTime,
  FastHeader, BottomNav, ProgressRing, StageDots,
});
