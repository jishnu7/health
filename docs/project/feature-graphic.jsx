// feature-graphic.jsx — Play Store Feature Graphic, 1024×500
// Three directions, each a self-contained graphic.

const FG_W = 1024;
const FG_H = 500;

const T = {
  paper:    '#f6f3ee',
  paper2:   '#f7e7df',
  ink:      '#14130f',
  ink2:     '#3a3a36',
  muted:    '#6f6a60',
  primary:  '#2a4d3e',
  primary2: '#3d6b56',
  accent:   '#d97757',
  cream:    '#fdfbf7',
  border:   '#e1dccf',
};

// ─────────────────────────────────────────────────────────────
// The Fast app icon (24-hour variant) — same as final spec
// ─────────────────────────────────────────────────────────────
function FastIcon({ bg = T.paper, ring = T.primary, dot = T.accent, size = 64 }) {
  const cx = 512, cy = 512, r = 320, ringW = 80;
  const innerR = r + ringW/2 + 16;
  const progress = 0.70;
  const a = -Math.PI/2 + progress * 2 * Math.PI;
  const dotX = cx + Math.cos(a) * r, dotY = cy + Math.sin(a) * r;
  return (
    <div style={{
      width: size, height: size,
      borderRadius: '22.37%',
      overflow: 'hidden',
      boxShadow: '0 6px 16px -6px rgba(20,19,15,0.30), 0 0 0 1px rgba(20,19,15,0.05)',
      flexShrink: 0,
    }}>
      <svg viewBox="0 0 1024 1024" width="100%" height="100%" style={{ display: 'block', background: bg }}>
        <g opacity="0.45">
          {Array.from({ length: 24 }).map((_, i) => {
            const ta = (i/24) * Math.PI * 2 - Math.PI/2;
            const isCard = i % 6 === 0;
            const outer = innerR + (isCard ? 28 : 14);
            return (
              <line key={i}
                x1={cx + Math.cos(ta) * innerR} y1={cy + Math.sin(ta) * innerR}
                x2={cx + Math.cos(ta) * outer}  y2={cy + Math.sin(ta) * outer}
                stroke={ring}
                strokeWidth={isCard ? 9 : 4}
                strokeLinecap="round"/>
            );
          })}
        </g>
        <circle cx={cx} cy={cy} r={r} fill="none" stroke={ring} strokeWidth={ringW}/>
        <circle cx={dotX} cy={dotY} r={58} fill={dot}/>
      </svg>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Mini home screen for inside the phone (compact, 200×~390)
// ─────────────────────────────────────────────────────────────
function MiniHome({ dark = false }) {
  const bg = dark ? '#1c1b17' : T.paper;
  const ink = dark ? T.cream : T.ink;
  const muted = dark ? '#8a857c' : T.muted;
  const border = dark ? '#2d2a23' : T.border;
  const card = dark ? '#24221d' : '#ffffff';
  const primary = dark ? '#7dd3a8' : T.primary;
  const primarySoft = dark ? '#1f2a23' : '#e7eee8';

  // Ring
  const ringR = 70, ringW = 8, ringProg = 0.68;
  const cx = 100, cy = 130;
  const arcLen = 2 * Math.PI * ringR;
  const off = arcLen * (1 - ringProg);

  return (
    <div style={{ background: bg, color: ink, height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Status bar */}
      <div style={{
        height: 24, padding: '8px 16px 0',
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        fontSize: 9, fontWeight: 600, fontFamily: 'Geist',
      }}>
        <span className="mono tnum">9:41</span>
        <span style={{ opacity: 0.85, fontSize: 8 }}>• • •</span>
      </div>

      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 16px 0' }}>
        <div style={{ fontSize: 10, fontWeight: 600 }}>Fast</div>
        <div className="mono" style={{ fontSize: 7, color: muted, letterSpacing: '0.12em', textTransform: 'uppercase' }}>Day 14</div>
      </div>

      {/* Stage chip */}
      <div style={{ display: 'flex', justifyContent: 'center', marginTop: 14 }}>
        <div style={{
          display: 'inline-flex', alignItems: 'center', gap: 5,
          padding: '3px 8px', borderRadius: 999,
          background: primarySoft, color: primary,
          fontSize: 8, fontWeight: 500, letterSpacing: '0.04em',
        }}>
          <span style={{ width: 4, height: 4, borderRadius: 2, background: 'currentColor' }}/>
          KETOSIS
        </div>
      </div>

      {/* Ring */}
      <div style={{ position: 'relative', height: 160, display: 'flex', justifyContent: 'center', marginTop: 6 }}>
        <svg width={160} height={160} viewBox="0 0 200 200">
          <circle cx={cx} cy="100" r={ringR} fill="none" stroke={border} strokeWidth={ringW}/>
          <circle cx={cx} cy="100" r={ringR} fill="none" stroke={primary} strokeWidth={ringW}
            strokeLinecap="round"
            strokeDasharray={arcLen}
            strokeDashoffset={off}
            transform={`rotate(-90 ${cx} 100)`}/>
        </svg>
        <div style={{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', paddingTop: 4 }}>
          <div className="mono" style={{ fontSize: 7, color: muted, letterSpacing: '0.12em', textTransform: 'uppercase' }}>ELAPSED</div>
          <div className="mono tnum" style={{ fontSize: 28, fontWeight: 400, letterSpacing: '-0.04em', lineHeight: 1, marginTop: 3, color: ink }}>13:37</div>
          <div className="mono tnum" style={{ fontSize: 8, color: muted, marginTop: 3 }}>:24</div>
        </div>
      </div>

      {/* Plan / end */}
      <div style={{ padding: '6px 16px', display: 'flex', justifyContent: 'space-between' }}>
        <div>
          <div className="mono" style={{ fontSize: 7, color: muted, letterSpacing: '0.12em', textTransform: 'uppercase' }}>Plan</div>
          <div style={{ fontSize: 9, fontWeight: 500, marginTop: 2 }}>16:8 LEAN</div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div className="mono" style={{ fontSize: 7, color: muted, letterSpacing: '0.12em', textTransform: 'uppercase' }}>Ends</div>
          <div className="mono tnum" style={{ fontSize: 9, fontWeight: 500, marginTop: 2 }}>11:47 PM</div>
        </div>
      </div>

      {/* End fast button */}
      <div style={{ padding: '8px 16px 0' }}>
        <div style={{
          height: 28, borderRadius: 999,
          background: primary, color: dark ? T.ink : T.cream,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 9, fontWeight: 500,
        }}>
          End fast
        </div>
      </div>

      {/* Mini stats */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 4, padding: '8px 16px 0' }}>
        {[
          { l: 'Streak', v: '14d' },
          { l: 'Avg',    v: '15h' },
          { l: 'Water',  v: '1.6L' },
        ].map(s => (
          <div key={s.l} style={{ background: card, border: `1px solid ${border}`, borderRadius: 6, padding: '5px 6px' }}>
            <div className="mono" style={{ fontSize: 6, color: muted, letterSpacing: '0.12em', textTransform: 'uppercase' }}>{s.l}</div>
            <div className="mono tnum" style={{ fontSize: 11, fontWeight: 500, marginTop: 1 }}>{s.v}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

function Phone({ children, dark = false }) {
  return (
    <div className="fg-phone">
      <div className="notch"/>
      <div className="screen" style={{ background: dark ? '#1c1b17' : T.paper }}>
        {children}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// DIRECTION 1 — Editorial cream split
// ─────────────────────────────────────────────────────────────
function FG_Editorial() {
  return (
    <div className="fg" style={{ background: T.paper, display: 'flex' }}>
      {/* Subtle grid texture */}
      <svg style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', opacity: 0.5, pointerEvents: 'none' }}>
        <defs>
          <pattern id="fg1grid" x="0" y="0" width="64" height="64" patternUnits="userSpaceOnUse">
            <path d="M 64 0 L 0 0 0 64" fill="none" stroke="var(--border)" strokeWidth="0.4"/>
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill="url(#fg1grid)"/>
      </svg>

      {/* LEFT — text column */}
      <div style={{
        flex: 1, position: 'relative', zIndex: 1,
        padding: '52px 0 44px 64px',
        display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <FastIcon size={52}/>
          <div>
            <div style={{ fontSize: 16, fontWeight: 600, letterSpacing: '-0.01em', lineHeight: 1 }}>Fast</div>
            <div className="mono" style={{ fontSize: 9, letterSpacing: '0.18em', color: 'var(--muted)', textTransform: 'uppercase', marginTop: 4 }}>Intermittent fasting</div>
          </div>
        </div>

        <div>
          <h1 style={{
            fontSize: 64, fontWeight: 500,
            letterSpacing: '-0.04em', lineHeight: 0.95,
            margin: 0, color: 'var(--ink)',
          }}>
            Fasting,<br/>
            <span style={{ color: 'var(--primary)' }}>finally</span> measurable.
          </h1>
          <p style={{
            fontSize: 14, lineHeight: 1.45, color: 'var(--ink-2)',
            maxWidth: 380, margin: '14px 0 0',
          }}>
            A quiet timer and a stage-by-stage view of what's happening inside — from glycogen burn to autophagy.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 18 }}>
          <div style={{
            height: 40, padding: '0 20px', borderRadius: 999,
            background: 'var(--primary)', color: 'var(--paper)',
            display: 'inline-flex', alignItems: 'center', gap: 8,
            fontSize: 13, fontWeight: 500,
          }}>
            Start a 16:8 fast
            <span style={{ fontSize: 14 }}>→</span>
          </div>
        </div>
      </div>

      {/* RIGHT — phone */}
      <div style={{
        width: 440, position: 'relative',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>
        {/* Decorative concentric rings */}
        <svg width="440" height="500" style={{ position: 'absolute', right: -80, top: 0 }}>
          <circle cx="280" cy="250" r="220" fill="none" stroke="var(--border)" strokeWidth="1"/>
          <circle cx="280" cy="250" r="170" fill="none" stroke="var(--border)" strokeWidth="1"/>
          <circle cx="280" cy="250" r="120" fill="none" stroke="var(--border)" strokeWidth="1"/>
        </svg>
        <div style={{ transform: 'rotate(-3deg)', position: 'relative', zIndex: 1 }}>
          <Phone>
            <MiniHome/>
          </Phone>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// DIRECTION 2 — Forest immersive
// ─────────────────────────────────────────────────────────────
function FG_Forest() {
  return (
    <div className="fg" style={{ background: T.primary, color: T.cream }}>
      {/* Decorative giant ring overlay */}
      <svg style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', pointerEvents: 'none' }}>
        <circle cx="200" cy="-50" r="380" fill="none" stroke="rgba(253,251,247,0.10)" strokeWidth="1"/>
        <circle cx="200" cy="-50" r="300" fill="none" stroke="rgba(253,251,247,0.10)" strokeWidth="1"/>
        <circle cx="200" cy="-50" r="220" fill="none" stroke="rgba(253,251,247,0.10)" strokeWidth="1"/>
        {/* Hour ticks for the big ring */}
        <g opacity="0.35">
          {Array.from({ length: 24 }).map((_, i) => {
            const a = (i/24) * Math.PI * 2 - Math.PI/2;
            const inner = 460, outer = inner + (i % 6 === 0 ? 18 : 10);
            return (
              <line key={i}
                x1={200 + Math.cos(a) * inner} y1={-50 + Math.sin(a) * inner}
                x2={200 + Math.cos(a) * outer} y2={-50 + Math.sin(a) * outer}
                stroke="var(--cream)" strokeWidth={i % 6 === 0 ? 3 : 1.5} strokeLinecap="round"/>
            );
          })}
        </g>
      </svg>

      {/* Top brand */}
      <div style={{ position: 'absolute', top: 36, left: 64, display: 'flex', alignItems: 'center', gap: 14, zIndex: 2 }}>
        <FastIcon bg={T.primary} ring={T.cream} size={48}/>
        <div>
          <div style={{ fontSize: 16, fontWeight: 600, color: T.cream, lineHeight: 1 }}>Fast</div>
          <div className="mono" style={{ fontSize: 9, letterSpacing: '0.18em', color: 'rgba(253,251,247,0.55)', textTransform: 'uppercase', marginTop: 4 }}>Track every hour</div>
        </div>
      </div>

      {/* Headline */}
      <div style={{ position: 'absolute', left: 64, top: 168, maxWidth: 520, zIndex: 2 }}>
        <h1 style={{
          fontSize: 76, fontWeight: 500,
          letterSpacing: '-0.045em', lineHeight: 0.92,
          margin: 0, color: T.cream,
        }}>
          Time becomes<br/>
          <span style={{ fontStyle: 'italic', fontWeight: 400, color: T.accent }}>a ritual.</span>
        </h1>
        <p style={{
          fontSize: 14, lineHeight: 1.5, margin: '20px 0 0',
          color: 'rgba(253,251,247,0.75)', maxWidth: 380,
        }}>
          Time your fast. Trust your body. Watch the stages unfold — fed, glycogen, ketosis, autophagy.
        </p>
      </div>

      {/* Stages strip at bottom */}
      <div style={{ position: 'absolute', left: 64, bottom: 40, display: 'flex', gap: 24, zIndex: 2 }}>
        {[
          { h: '0–4', n: 'Fed' },
          { h: '4–12', n: 'Glycogen' },
          { h: '12–18', n: 'Ketosis', live: true },
          { h: '18–24', n: 'Fat burn' },
        ].map(s => (
          <div key={s.n} style={{ minWidth: 64 }}>
            <div style={{ height: 3, borderRadius: 2, background: s.live ? T.accent : 'rgba(253,251,247,0.20)', marginBottom: 6 }}/>
            <div className="mono" style={{ fontSize: 8, letterSpacing: '0.10em', color: 'rgba(253,251,247,0.55)' }}>{s.h}H</div>
            <div style={{ fontSize: 11, fontWeight: 500, marginTop: 2, color: s.live ? T.accent : T.cream }}>{s.n}</div>
          </div>
        ))}
      </div>

      {/* Phone right */}
      <div style={{ position: 'absolute', right: 72, top: 45, zIndex: 3 }}>
        <div style={{ transform: 'rotate(3deg)' }}>
          <Phone>
            <MiniHome/>
          </Phone>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// DIRECTION 3 — Dark typographic
// ─────────────────────────────────────────────────────────────
function FG_Typographic() {
  return (
    <div className="fg" style={{ background: T.ink, color: T.cream }}>
      {/* Faint vertical grid */}
      <svg style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', pointerEvents: 'none' }}>
        {Array.from({ length: 12 }).map((_, i) => (
          <line key={i} x1={(i+1) * FG_W/12} x2={(i+1) * FG_W/12} y1="0" y2={FG_H}
            stroke={T.cream} strokeOpacity="0.04"/>
        ))}
        <line x1="48" x2={FG_W - 48} y1="60" y2="60" stroke={T.cream} strokeOpacity="0.10"/>
        <line x1="48" x2={FG_W - 48} y1={FG_H - 60} y2={FG_H - 60} stroke={T.cream} strokeOpacity="0.10"/>
      </svg>

      {/* Top meta */}
      <div style={{
        position: 'absolute', top: 32, left: 48, right: 48,
        display: 'flex', justifyContent: 'space-between', alignItems: 'center', zIndex: 3,
        fontFamily: 'Geist Mono, monospace', fontSize: 10,
        letterSpacing: '0.18em', textTransform: 'uppercase', color: 'rgba(246,243,238,0.55)',
      }}>
        <span style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <FastIcon bg={T.ink} ring={T.cream} size={28}/>
          <span style={{ color: T.cream, letterSpacing: '0.04em', fontSize: 12, fontWeight: 600, fontFamily: 'Geist' }}>Fast</span>
        </span>
        <span style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span style={{ width: 6, height: 6, borderRadius: 3, background: '#7dd3a8' }}/>
          <span style={{ color: '#7dd3a8' }}>Ketosis · 13:37</span>
        </span>
      </div>

      {/* Giant 16:8 */}
      <div style={{
        position: 'absolute', left: 48, top: 70, zIndex: 1,
        fontFamily: 'Geist Mono, monospace',
        fontSize: 340, fontWeight: 300,
        letterSpacing: '-0.06em', lineHeight: 0.85,
        color: T.cream,
      }}>
        <span>16</span>
        <span style={{ color: T.accent }}>:</span>
        <span>8</span>
      </div>

      {/* Tagline */}
      <div style={{ position: 'absolute', left: 48, bottom: 36, maxWidth: 460, zIndex: 2 }}>
        <h2 style={{ fontSize: 22, fontWeight: 500, letterSpacing: '-0.02em', lineHeight: 1.1, margin: 0 }}>
          Two numbers. One quieter day.
        </h2>
        <p style={{ fontSize: 12, lineHeight: 1.5, color: 'rgba(246,243,238,0.65)', margin: '6px 0 0', maxWidth: 380 }}>
          Eat in 8. Rest in 16. Fast learns your rhythm and gets out of the way — until the next ritual.
        </p>
      </div>

      {/* Phone right */}
      <div style={{ position: 'absolute', right: 60, top: 48, zIndex: 4 }}>
        <Phone dark>
          <MiniHome dark/>
        </Phone>
      </div>

      {/* CTA bottom-right */}
      <div style={{
        position: 'absolute', right: 60, bottom: 36, zIndex: 5,
        display: 'flex', alignItems: 'center', gap: 12,
      }}>
        <div style={{
          height: 36, padding: '0 18px', borderRadius: 999,
          background: T.accent, color: T.ink,
          display: 'inline-flex', alignItems: 'center', gap: 8,
          fontSize: 12, fontWeight: 600,
        }}>
          Download
          <span style={{ fontSize: 13 }}>→</span>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// App
// ─────────────────────────────────────────────────────────────
function App() {
  return (
    <DesignCanvas
      title="Fast — Play Store Feature Graphic"
      subtitle="Three directions, each exactly 1024 × 500. Click an artboard to view fullscreen and screenshot.">

      <DCSection id="fgs" title="Feature graphics" subtitle="1024 × 500 · Play Store">
        <DCArtboard id="editorial" label="01 · Editorial cream" width={FG_W} height={FG_H}>
          <FG_Editorial/>
        </DCArtboard>
        <DCArtboard id="forest" label="02 · Forest immersive" width={FG_W} height={FG_H}>
          <FG_Forest/>
        </DCArtboard>
        <DCArtboard id="dark" label="03 · Dark typographic" width={FG_W} height={FG_H}>
          <FG_Typographic/>
        </DCArtboard>
      </DCSection>

    </DesignCanvas>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App/>);
