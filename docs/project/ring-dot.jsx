// ring-dot.jsx — Fast App Icon, final spec sheet (24-hour variant)

const T = {
  paper:    '#f6f3ee',
  paper2:   '#f7e7df',
  ink:      '#14130f',
  ink2:     '#3a3a36',
  muted:    '#6f6a60',
  primary:  '#2a4d3e',
  accent:   '#d97757',
  cream:    '#fdfbf7',
  border:   '#e1dccf',
};

const CX = 512, CY = 512;
const R = 320;
const RING_W = 80;
const PROGRESS = 0.70;

// ─────────────────────────────────────────────────────────────
// The Mark — the only icon component we render from here on
// ─────────────────────────────────────────────────────────────
function FastIcon({
  bg = T.paper,
  ring = T.primary,
  dot = T.accent,
  showConstruction = false,
}) {
  const innerR = R + RING_W/2 + 16;
  const dotAngle = -Math.PI/2 + PROGRESS * 2 * Math.PI;
  const dotX = CX + Math.cos(dotAngle) * R;
  const dotY = CY + Math.sin(dotAngle) * R;

  return (
    <svg viewBox="0 0 1024 1024" width="100%" height="100%" style={{ display: 'block', background: bg }}>
      {/* 24 hour ticks */}
      <g opacity="0.45">
        {Array.from({ length: 24 }).map((_, i) => {
          const a = (i/24) * Math.PI * 2 - Math.PI/2;
          const isCard = i % 6 === 0;
          const outer = innerR + (isCard ? 28 : 14);
          return (
            <line key={i}
              x1={CX + Math.cos(a) * innerR} y1={CY + Math.sin(a) * innerR}
              x2={CX + Math.cos(a) * outer}  y2={CY + Math.sin(a) * outer}
              stroke={ring}
              strokeWidth={isCard ? 9 : 4}
              strokeLinecap="round"/>
          );
        })}
      </g>

      {/* Construction overlay */}
      {showConstruction && (
        <g style={{ pointerEvents: 'none' }}>
          {/* Center crosshair */}
          <line x1={CX - 480} x2={CX + 480} y1={CY} y2={CY} stroke="#d97757" strokeWidth="1" strokeDasharray="6 6" opacity="0.5"/>
          <line x1={CX} x2={CX} y1={CY - 480} y2={CY + 480} stroke="#d97757" strokeWidth="1" strokeDasharray="6 6" opacity="0.5"/>
          {/* Ring radius */}
          <circle cx={CX} cy={CY} r={R} fill="none" stroke="#d97757" strokeWidth="1" strokeDasharray="4 4" opacity="0.7"/>
          <line x1={CX} x2={CX + R} y1={CY} y2={CY} stroke="#d97757" strokeWidth="2" opacity="0.8"/>
          <text x={CX + R/2} y={CY - 14} textAnchor="middle" fontFamily="Geist Mono" fontSize="22" fill="#d97757">r = 320</text>
          {/* Dot position arc */}
          <text x={dotX - 80} y={dotY + 110} fontFamily="Geist Mono" fontSize="20" fill="#d97757">70% · ø116</text>
          {/* Stroke width annotation */}
          <line x1={CX} y1={CY - R - RING_W/2} x2={CX} y2={CY - R + RING_W/2}
            stroke="#d97757" strokeWidth="2"/>
          <text x={CX + 14} y={CY - R + 8} fontFamily="Geist Mono" fontSize="20" fill="#d97757">80</text>
        </g>
      )}

      {/* Ring */}
      <circle cx={CX} cy={CY} r={R}
        fill="none" stroke={ring} strokeWidth={RING_W}/>

      {/* Dot */}
      <circle cx={dotX} cy={dotY} r={58} fill={dot}/>
    </svg>
  );
}

// ─────────────────────────────────────────────────────────────
// Squircle with shadow — for spec previews
// ─────────────────────────────────────────────────────────────
function Squircle({ size, light = true, flat = false, children }) {
  return (
    <div style={{
      width: size, height: size,
      borderRadius: '22.37%',
      overflow: 'hidden',
      flexShrink: 0,
      boxShadow: flat
        ? '0 1px 3px rgba(20,19,15,0.08), 0 0 0 1px rgba(20,19,15,0.06)'
        : '0 18px 36px -16px rgba(20,19,15,0.30), 0 4px 10px -3px rgba(20,19,15,0.18), 0 0 0 1px rgba(20,19,15,0.04)',
    }}>
      {children}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Hero card
// ─────────────────────────────────────────────────────────────
function HeroCard() {
  return (
    <div className="spec-card" style={{ width: 980, height: 620 }}>
      <div className="spec-header">
        <div>
          <div className="eyebrow">FINAL · 24-HOUR VARIANT</div>
          <div className="title">Fast — App Icon</div>
        </div>
        <div className="mono" style={{ fontSize: 11, color: 'var(--muted)', letterSpacing: '0.12em', textAlign: 'right' }}>
          <div>v1.0 · 26.05.26</div>
          <div style={{ marginTop: 4 }}>1024 × 1024 · SVG</div>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 1, gap: 64 }}>
        <Squircle size={380}><FastIcon/></Squircle>
        <div style={{ maxWidth: 320 }}>
          <div className="eyebrow">The mark</div>
          <p style={{ fontSize: 14, lineHeight: 1.55, color: 'var(--ink-2)', marginTop: 10 }}>
            A 24-hour dial. The forest-green ring frames a full day; the terracotta dot marks elapsed fasting time. Tick marks every hour, with cardinal points at 00 / 06 / 12 / 18 emphasised.
          </p>
          <div style={{ marginTop: 22, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <Stat k="Ring radius" v="320"/>
            <Stat k="Stroke" v="80"/>
            <Stat k="Tick count" v="24"/>
            <Stat k="Dot radius" v="58"/>
          </div>
        </div>
      </div>

      <div className="spec-footer">
        <span className="mono">EXPORT</span>
        <a href="exports/Fast Icon - Light.svg" download>↓ Light SVG</a>
        <a href="exports/Fast Icon - Dark.svg" download>↓ Dark SVG</a>
      </div>
    </div>
  );
}

function Stat({ k, v }) {
  return (
    <div style={{ borderTop: '1px solid var(--border)', paddingTop: 8 }}>
      <div className="mono" style={{ fontSize: 10, color: 'var(--muted)', letterSpacing: '0.12em', textTransform: 'uppercase' }}>{k}</div>
      <div className="mono" style={{ fontSize: 18, marginTop: 2, fontWeight: 500 }}>{v}</div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Light + Dark pair
// ─────────────────────────────────────────────────────────────
function PairCard() {
  return (
    <div className="spec-card" style={{ width: 700, height: 420 }}>
      <div className="spec-header">
        <div>
          <div className="eyebrow">SYSTEM</div>
          <div className="title">Light + Dark</div>
        </div>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 1, gap: 48 }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 14 }}>
          <Squircle size={220}><FastIcon/></Squircle>
          <div className="mono" style={{ fontSize: 10, letterSpacing: '0.16em', color: 'var(--muted)' }}>LIGHT · DEFAULT</div>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 14 }}>
          <Squircle size={220}><FastIcon bg={T.ink} ring={T.cream}/></Squircle>
          <div className="mono" style={{ fontSize: 10, letterSpacing: '0.16em', color: 'var(--muted)' }}>DARK</div>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Size matrix
// ─────────────────────────────────────────────────────────────
const SIZES = [
  { px: 180, use: 'iPhone @3x' },
  { px: 152, use: 'iPad @2x' },
  { px: 120, use: 'iPhone @2x' },
  { px: 87,  use: 'Settings @3x' },
  { px: 80,  use: 'Spotlight @2x' },
  { px: 60,  use: 'Notification' },
  { px: 40,  use: 'Spotlight' },
  { px: 29,  use: 'Settings' },
];

function SizeMatrixCard() {
  return (
    <div className="spec-card" style={{ width: 980, height: 380 }}>
      <div className="spec-header">
        <div>
          <div className="eyebrow">SIZES · IOS</div>
          <div className="title">From 180 to 29</div>
        </div>
        <div className="mono" style={{ fontSize: 11, color: 'var(--muted)' }}>All exported from the 1024 master</div>
      </div>
      <div style={{
        display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between',
        flex: 1, padding: '0 12px',
      }}>
        {SIZES.map(s => (
          <div key={s.px} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10 }}>
            <Squircle size={s.px} flat={s.px < 40}>
              <FastIcon/>
            </Squircle>
            <div style={{ textAlign: 'center' }}>
              <div className="mono" style={{ fontSize: 12, fontWeight: 500 }}>{s.px}</div>
              <div className="mono" style={{ fontSize: 9, color: 'var(--muted)', letterSpacing: '0.08em', marginTop: 2 }}>{s.use}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Construction grid card
// ─────────────────────────────────────────────────────────────
function ConstructionCard() {
  return (
    <div className="spec-card" style={{ width: 540, height: 620 }}>
      <div className="spec-header">
        <div>
          <div className="eyebrow">CONSTRUCTION</div>
          <div className="title">Geometry</div>
        </div>
      </div>
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{
          width: 400, height: 400,
          borderRadius: '22.37%',
          overflow: 'hidden',
          boxShadow: '0 1px 3px rgba(20,19,15,0.08), 0 0 0 1px rgba(20,19,15,0.06)',
        }}>
          <FastIcon showConstruction/>
        </div>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginTop: 20 }}>
        <Stat k="Canvas" v="1024²"/>
        <Stat k="Ring r" v="320"/>
        <Stat k="Stroke" v="80"/>
        <Stat k="Dot ø" v="116"/>
        <Stat k="Tick inset" v="56"/>
        <Stat k="Dot at" v="70%"/>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Color tokens card
// ─────────────────────────────────────────────────────────────
function ColorsCard() {
  const colors = [
    { name: 'Paper',  hex: '#f6f3ee', use: 'Light bg' },
    { name: 'Ink',    hex: '#14130f', use: 'Dark bg' },
    { name: 'Forest', hex: '#2a4d3e', use: 'Ring (light)' },
    { name: 'Cream',  hex: '#fdfbf7', use: 'Ring (dark)' },
    { name: 'Terra',  hex: '#d97757', use: 'Dot' },
  ];
  return (
    <div className="spec-card" style={{ width: 420, height: 620 }}>
      <div className="spec-header">
        <div>
          <div className="eyebrow">PALETTE</div>
          <div className="title">Colors</div>
        </div>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 4 }}>
        {colors.map(c => (
          <div key={c.name} style={{
            display: 'flex', alignItems: 'center', gap: 14,
            padding: '12px 14px',
            border: '1px solid var(--border)',
            borderRadius: 12,
          }}>
            <div style={{
              width: 48, height: 48, borderRadius: 10,
              background: c.hex, border: '1px solid rgba(20,19,15,0.08)',
            }}/>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 500 }}>{c.name}</div>
              <div className="mono" style={{ fontSize: 11, color: 'var(--muted)', marginTop: 2 }}>{c.hex.toUpperCase()}</div>
            </div>
            <div className="mono" style={{ fontSize: 10, color: 'var(--muted)', letterSpacing: '0.1em', textTransform: 'uppercase' }}>{c.use}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// In-context — home screens
// ─────────────────────────────────────────────────────────────
function HomeScreenPreview({ icon, wallpaper = 'cool', label = 'Light wallpaper' }) {
  const grads = {
    cool: 'linear-gradient(160deg, #4a5e6a, #2a3a44 60%, #1b2730)',
    warm: 'linear-gradient(160deg, #d9a98a, #b76b56 60%, #6a3b2e)',
    light: 'linear-gradient(160deg, #f0e9dc, #c8b89a 70%, #9c8669)',
    dark: 'linear-gradient(160deg, #1a1916, #0a0907)',
  };
  const placeholders = [
    '#a78bfa', '#fbbf24', '#34d399', '#60a5fa',
    '#f87171', '#a3a3a3', '#fb923c', '#22d3ee',
    null,
    '#facc15', '#10b981', '#6366f1',
    '#ec4899', '#94a3b8', '#84cc16', '#0ea5e9',
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 14 }}>
      <div style={{
        width: 380, height: 680,
        borderRadius: 52,
        background: grads[wallpaper],
        padding: 22,
        position: 'relative',
        boxShadow: '0 24px 50px -16px rgba(20,19,15,0.45), 0 0 0 1px rgba(20,19,15,0.06)',
        overflow: 'hidden',
      }}>
        <div style={{
          color: '#fff', fontSize: 13, fontWeight: 600, fontFamily: 'Geist',
          display: 'flex', justifyContent: 'space-between', padding: '6px 14px 22px',
          opacity: wallpaper === 'light' ? 0.85 : 1,
        }}>
          <span>9:41</span><span style={{ opacity: 0.85 }}>•••</span>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 22, padding: '8px 2px' }}>
          {placeholders.map((color, i) => (
            <div key={i} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
              <div style={{
                width: 64, height: 64,
                borderRadius: '22.37%',
                overflow: 'hidden',
                boxShadow: '0 2px 6px rgba(0,0,0,0.25)',
                background: color || 'transparent',
                border: color ? '1px solid rgba(255,255,255,0.08)' : 'none',
              }}>
                {color === null && icon}
              </div>
              <div style={{
                fontSize: 10, color: wallpaper === 'light' ? '#1c1b17' : '#fff',
                fontFamily: 'Geist', fontWeight: 500,
                textShadow: wallpaper === 'light' ? 'none' : '0 1px 2px rgba(0,0,0,0.4)',
              }}>
                {color === null ? 'Fast' : ''}
              </div>
            </div>
          ))}
        </div>
        <div style={{
          position: 'absolute', left: 22, right: 22, bottom: 32,
          background: wallpaper === 'light' ? 'rgba(255,255,255,0.5)' : 'rgba(255,255,255,0.18)',
          backdropFilter: 'blur(20px)',
          borderRadius: 28,
          padding: 12,
          display: 'flex', justifyContent: 'space-around',
        }}>
          {['#3b82f6', '#22c55e', '#ef4444', '#a855f7'].map((c, i) => (
            <div key={i} style={{
              width: 52, height: 52,
              borderRadius: '22.37%',
              background: c,
              boxShadow: '0 2px 6px rgba(0,0,0,0.25)',
            }}/>
          ))}
        </div>
      </div>
      <div className="mono" style={{ fontSize: 11, color: 'var(--muted)', letterSpacing: '0.12em' }}>{label.toUpperCase()}</div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// App
// ─────────────────────────────────────────────────────────────
function App() {
  return (
    <DesignCanvas title="Fast — App Icon · Final Spec" subtitle="24-hour variant, locked. All sizes derived from the 1024 master.">

      <DCSection id="hero" title="The mark">
        <DCArtboard id="hero" label="Final" width={980} height={620}>
          <HeroCard/>
        </DCArtboard>
        <DCArtboard id="pair" label="Light + Dark" width={700} height={420}>
          <PairCard/>
        </DCArtboard>
      </DCSection>

      <DCSection id="sizes" title="Sizes">
        <DCArtboard id="size-matrix" label="iOS size matrix" width={980} height={380}>
          <SizeMatrixCard/>
        </DCArtboard>
      </DCSection>

      <DCSection id="spec" title="Spec">
        <DCArtboard id="construction" label="Construction" width={540} height={620}>
          <ConstructionCard/>
        </DCArtboard>
        <DCArtboard id="colors" label="Palette" width={420} height={620}>
          <ColorsCard/>
        </DCArtboard>
      </DCSection>

      <DCSection id="context" title="In context">
        <DCArtboard id="home-cool" label="Cool wallpaper" width={380} height={720}>
          <HomeScreenPreview icon={<FastIcon/>} wallpaper="cool" label="Cool · default"/>
        </DCArtboard>
        <DCArtboard id="home-warm" label="Warm wallpaper" width={380} height={720}>
          <HomeScreenPreview icon={<FastIcon/>} wallpaper="warm" label="Warm"/>
        </DCArtboard>
        <DCArtboard id="home-light" label="Light wallpaper" width={380} height={720}>
          <HomeScreenPreview icon={<FastIcon/>} wallpaper="light" label="Light"/>
        </DCArtboard>
        <DCArtboard id="home-dark" label="Dark wallpaper (dark icon)" width={380} height={720}>
          <HomeScreenPreview icon={<FastIcon bg={T.ink} ring={T.cream}/>} wallpaper="dark" label="Dark mode"/>
        </DCArtboard>
      </DCSection>

    </DesignCanvas>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App/>);
