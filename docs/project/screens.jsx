// screens.jsx — All screens of the Fast app

// ─────────────────────────────────────────────────────────────
// WATER tracking
// ─────────────────────────────────────────────────────────────
function WaterGlass({ progress, size = 200 }) {
  // SVG glass with a water fill that rises from the bottom.
  const W = size, H = size;
  const cupTopY = 30, cupBotY = H - 16;
  const cupTopW = 110, cupBotW = 90;
  const cx = W / 2;
  const fillH = (cupBotY - cupTopY) * progress;
  const fillTopY = cupBotY - fillH;
  // Lerp width at fillTopY
  const lerp = (a, b, t) => a + (b - a) * t;
  const t = (cupBotY - fillTopY) / (cupBotY - cupTopY); // 0 at bottom, 1 at top
  const widthAtFill = lerp(cupBotW, cupTopW, t);

  return (
    <svg width={W} height={H} viewBox={`0 0 ${W} ${H}`}>
      <defs>
        <clipPath id="glass-clip">
          <path d={`M ${cx - cupTopW/2} ${cupTopY} L ${cx - cupBotW/2} ${cupBotY} Q ${cx} ${cupBotY + 8} ${cx + cupBotW/2} ${cupBotY} L ${cx + cupTopW/2} ${cupTopY} Z`} />
        </clipPath>
        <linearGradient id="water-grad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="var(--primary)" stopOpacity="0.55"/>
          <stop offset="1" stopColor="var(--primary)" stopOpacity="0.85"/>
        </linearGradient>
      </defs>
      {/* Water fill (inside clip) */}
      <g clipPath="url(#glass-clip)">
        {fillH > 0 && (
          <>
            <rect x="0" y={fillTopY} width={W} height={H} fill="url(#water-grad)" />
            {/* meniscus highlight */}
            <ellipse cx={cx} cy={fillTopY} rx={widthAtFill / 2} ry="3" fill="var(--primary)" opacity="0.25" />
          </>
        )}
      </g>
      {/* Glass outline */}
      <path d={`M ${cx - cupTopW/2} ${cupTopY} L ${cx - cupBotW/2} ${cupBotY} Q ${cx} ${cupBotY + 8} ${cx + cupBotW/2} ${cupBotY} L ${cx + cupTopW/2} ${cupTopY}`}
        fill="none" stroke="var(--border)" strokeWidth="1.5" />
      {/* Rim */}
      <line x1={cx - cupTopW/2} y1={cupTopY} x2={cx + cupTopW/2} y2={cupTopY} stroke="var(--border)" strokeWidth="1.5" />
      {/* Tick marks on left side */}
      {[0.25, 0.5, 0.75].map((frac) => {
        const y = cupBotY - (cupBotY - cupTopY) * frac;
        const w = lerp(cupBotW, cupTopW, frac);
        return (
          <line key={frac} x1={cx - w/2} x2={cx - w/2 + 6} y1={y} y2={y}
            stroke="var(--subtle)" strokeWidth="1" />
        );
      })}
    </svg>
  );
}

function PresetButton({ preset, onClick, units }) {
  const w = fmtWater(preset.ml, units);
  // Vessel sizes
  const heights = { sm: 22, md: 28, lg: 36, xl: 42 };
  const widths = { sm: 14, md: 16, lg: 18, xl: 20 };
  return (
    <button onClick={onClick} style={{
      flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8,
      padding: '14px 6px', borderRadius: 14,
      background: 'var(--card)', border: '1px solid var(--border)',
      transition: 'transform .12s, background .15s',
    }}>
      <svg width="28" height={heights[preset.hint]} viewBox={`0 0 28 ${heights[preset.hint]}`}>
        <path d={`M ${14 - widths[preset.hint]/2} 4 L ${14 - widths[preset.hint]/2 - 1} ${heights[preset.hint] - 2} Q 14 ${heights[preset.hint]} ${14 + widths[preset.hint]/2 + 1} ${heights[preset.hint] - 2} L ${14 + widths[preset.hint]/2} 4 Z`}
          fill="var(--primary-soft)" stroke="var(--primary)" strokeWidth="1.2" />
      </svg>
      <div style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.04em', textTransform: 'uppercase', color: 'var(--ink-2)' }}>
        {preset.label}
      </div>
      <div className="tnum caption" style={{ fontSize: 11, color: 'var(--muted)' }}>
        {w.val} {w.unit}
      </div>
    </button>
  );
}

function WaterScreen({ onNav }) {
  const f = useFast();
  const total = fmtWater(f.waterTotal, f.units);
  const goal = fmtWater(f.waterGoal, f.units);
  const remaining = Math.max(0, f.waterGoal - f.waterTotal);
  const rem = fmtWater(remaining, f.units);
  const pct = Math.round(f.waterProgress * 100);

  const [showCustom, setShowCustom] = React.useState(false);
  const [customVal, setCustomVal] = React.useState(f.units === 'metric' ? '200' : '8');

  const handleCustom = () => {
    const n = parseFloat(customVal);
    if (!n || n <= 0) return;
    const ml = f.units === 'metric' ? n : ozToMl(n);
    f.addWater(Math.round(ml));
    setShowCustom(false);
  };

  return (
    <div className="fast-screen">
      <FastHeader title="Water" />
      <div className="fast-content" style={{ paddingTop: 6 }}>

        {/* Hero — glass + numbers */}
        <div className="card" style={{ padding: 18, display: 'flex', gap: 14, alignItems: 'center' }}>
          <WaterGlass progress={f.waterProgress} size={150} />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div className="h-eyebrow">Today</div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 4, marginTop: 4 }}>
              <span className="h-display tnum" style={{ fontSize: 38 }}>{total.val}</span>
              <span style={{ fontSize: 14, color: 'var(--muted)' }}>{total.unit}</span>
            </div>
            <div className="caption tnum" style={{ marginTop: 4 }}>
              of {goal.val} {goal.unit} · {pct}%
            </div>
            <div style={{ marginTop: 12, height: 6, background: 'var(--border-2)', borderRadius: 3, overflow: 'hidden' }}>
              <div style={{ height: '100%', width: `${pct}%`, background: 'var(--primary)', borderRadius: 3, transition: 'width .25s' }} />
            </div>
            <div className="caption" style={{ marginTop: 8 }}>
              {remaining > 0 ? `${rem.val} ${rem.unit} to go` : 'Goal reached'}
            </div>
          </div>
        </div>

        {/* Quick add */}
        <SectionLabel>Quick add</SectionLabel>
        <div style={{ display: 'flex', gap: 8 }}>
          {WATER_PRESETS.map((p) => (
            <PresetButton key={p.ml} preset={p} units={f.units} onClick={() => f.addWater(p.ml)} />
          ))}
        </div>

        <button onClick={() => setShowCustom((s) => !s)}
          style={{
            marginTop: 10, width: '100%', padding: '12px 0', borderRadius: 100,
            background: 'transparent', border: '1px dashed var(--border)',
            color: 'var(--ink-2)', fontSize: 13, fontWeight: 500,
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 6,
          }}>
          <Icon.Plus /> Custom amount
        </button>

        {showCustom && (
          <div className="card" style={{ marginTop: 10, padding: 14, display: 'flex', alignItems: 'center', gap: 10 }}>
            <input type="number" value={customVal} onChange={(e) => setCustomVal(e.target.value)}
              style={{
                flex: 1, border: 0, background: 'var(--border-2)', borderRadius: 10,
                padding: '10px 14px', fontFamily: 'Geist Mono', fontSize: 16, color: 'var(--ink)',
                outline: 'none',
              }} />
            <span style={{ fontSize: 13, color: 'var(--muted)', minWidth: 36 }}>{f.units === 'metric' ? 'ml' : 'fl oz'}</span>
            <button className="btn btn-primary" style={{ height: 40, padding: '0 16px', fontSize: 13 }} onClick={handleCustom}>Add</button>
          </div>
        )}

        {/* Today's log */}
        <SectionLabel>Today's log</SectionLabel>
        {f.waterLog.length === 0 ? (
          <div className="card" style={{ padding: 18, textAlign: 'center', color: 'var(--muted)', fontSize: 13 }}>
            No drinks logged yet. Tap a preset above.
          </div>
        ) : (
          <div className="card" style={{ padding: '0 16px' }}>
            {[...f.waterLog].reverse().map((entry, ri) => {
              const i = f.waterLog.length - 1 - ri;
              const w = fmtWater(entry.ml, f.units);
              return (
                <div key={i} className="fast-row" style={{
                  padding: '14px 0',
                  borderBottom: ri === f.waterLog.length - 1 ? 0 : '1px solid var(--border)',
                }}>
                  <div style={{ width: 36, height: 36, borderRadius: 10, background: 'var(--primary-soft)', color: 'var(--primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                    <Icon.Water />
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div className="fast-row-label tnum">{w.val} {w.unit}</div>
                    <div className="fast-row-sub tnum">{fmtTime(entry.time)}</div>
                  </div>
                  <button onClick={() => f.removeWaterAt(i)} className="fast-iconbtn"
                    style={{ width: 32, height: 32, color: 'var(--muted)' }}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round">
                      <path d="M6 6l12 12M6 18L18 6"/>
                    </svg>
                  </button>
                </div>
              );
            })}
          </div>
        )}

      </div>
      <BottomNav active="water" onChange={onNav} />
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// HOME — fasting active or inactive
// ─────────────────────────────────────────────────────────────
function HomeScreen({ onNav, onShowStages }) {
  const f = useFast();
  const d = fmtDuration(f.elapsedMs);
  const remaining = Math.max(0, f.goalH * 3600000 - f.elapsedMs);
  const dr = fmtDuration(remaining);
  const startedAt = new Date(f.fastStartMs);
  const goalAt = new Date(f.fastStartMs + f.goalH * 3600000);

  // Scheduled fast window (from the start time set in settings).
  const hhmmToDate = (s) => { const [hh, mm] = s.split(':').map(Number); const dt = new Date(); dt.setHours(hh, mm, 0, 0); return dt; };
  const planStartClock = fmtTime(hhmmToDate(f.fastStartTime));
  const planGoalClock = fmtTime(hhmmToDate(addHoursToTime(f.fastStartTime, f.goalH)));

  if (!f.isFasting) {
    // No completed fast yet — ready-to-start state.
    if (!f.lastFast) {
      return (
        <div className="fast-screen">
          <FastHeader title="Fast" right={<button className="fast-iconbtn" onClick={() => onNav && onNav('settings')}><Icon.Settings /></button>} />
          <div className="fast-content" style={{ display: 'flex', flexDirection: 'column', gap: 16, paddingTop: 16 }}>
            <div>
              <div className="h-eyebrow" style={{ marginBottom: 8 }}>Not fasting</div>
              <h1 className="h-title">Begin your first fast.</h1>
              <p className="body" style={{ marginTop: 8 }}>
                We'll track every metabolic stage as you go — from fed to deep ketosis.
              </p>
            </div>

            <div className="card" style={{ width: '100%', padding: '30px 20px 20px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <ProgressRing size={240} stroke={12} progress={0} color="var(--primary)" dashed={true}>
                <div className="h-eyebrow">Goal</div>
                <div className="h-display" style={{ marginTop: 6, fontSize: 48 }}>{f.goalH}h</div>
                <div className="caption" style={{ marginTop: 6 }}>{f.planObj.label} Plan</div>
              </ProgressRing>

              <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', marginTop: 22, paddingTop: 18, borderTop: '1px solid var(--border)' }}>
                <div>
                  <div className="caption">Starts</div>
                  <div className="tnum" style={{ fontSize: 14, fontWeight: 500, marginTop: 2 }}>{planStartClock}</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div className="caption">Goal</div>
                  <div className="tnum" style={{ fontSize: 14, fontWeight: 500, marginTop: 2 }}>{planGoalClock}</div>
                </div>
              </div>

              <div style={{ width: '100%', marginTop: 18, paddingTop: 18, borderTop: '1px solid var(--border)' }}>
                <button className="btn btn-primary btn-full" onClick={() => f.startFast()}>
                  <Icon.Play /> Start fasting
                </button>
              </div>
            </div>

            <StagesPreviewCard onOpen={() => onShowStages && onShowStages()} />
          </div>
          <BottomNav active="home" onChange={onNav} />
        </div>
      );
    }

    // Returning — show the last-fast recap.
    return (
      <div className="fast-screen">
        <FastHeader title="Fast" right={<button className="fast-iconbtn" onClick={() => onNav && onNav('settings')}><Icon.Settings /></button>} />
        <div className="fast-content" style={{ display: 'flex', flexDirection: 'column', gap: 16, paddingTop: 16 }}>
          <div>
            <div className="h-eyebrow" style={{ marginBottom: 8 }}>Not fasting</div>
            <h1 className="h-title">Nice work.</h1>
            <p className="body" style={{ marginTop: 8 }}>
              Here's your last fast. Start another {f.planObj.label} whenever you're ready.
            </p>
          </div>

          <LastFastCard onShare={() => {}} />

          <div className="card" style={{ width: '100%', padding: 18 }}>
            <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
              <div className="h-eyebrow">Next fast</div>
              <span className="h-display tnum" style={{ fontSize: 26 }}>{f.goalH}h</span>
            </div>
            <div className="body" style={{ marginTop: 6, color: 'var(--muted)' }}>
              {f.goalH}-hour fast, {24 - f.goalH}-hour eating window.
            </div>
            <button className="btn btn-primary btn-full" style={{ marginTop: 16 }} onClick={() => f.startFast()}>
              <Icon.Play /> Start fasting now
            </button>
            <div className="caption" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, marginTop: 12 }}>
              <Icon.Clock style={{ width: 13, height: 13 }} /> Usual window · {planStartClock} – {planGoalClock}
            </div>
          </div>

          <StagesPreviewCard onOpen={() => onShowStages && onShowStages()} />
        </div>
        <BottomNav active="home" onChange={onNav} />
      </div>
    );
  }

  // Active state
  return (
    <div className="fast-screen">
      <FastHeader title="Fast" right={<button className="fast-iconbtn" onClick={() => onNav && onNav('settings')}><Icon.Settings /></button>} />
      <div className="fast-content" style={{ display: 'flex', flexDirection: 'column', gap: 16, paddingTop: 16 }}>

        <div>
          <div className="h-eyebrow" style={{ marginBottom: 8 }}>Fasting · {f.planObj.label}</div>
          <h1 className="h-title">Keep it going.</h1>
          <p className="body" style={{ marginTop: 8 }}>
            You're {Math.round(f.progress * 100)}% of the way to your {f.goalH}-hour goal — keep it steady.
          </p>
        </div>

        <div className="card" style={{ width: '100%', padding: '24px 20px 20px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <ProgressRing size={250} stroke={12} progress={f.progress}>
            <div className="h-eyebrow">Elapsed</div>
            <div className="h-display tnum" style={{ marginTop: 4 }}>
              {d.hh}<span style={{ color: 'var(--muted)' }}>:</span>{d.mm}
            </div>
            <div className="caption tnum" style={{ marginTop: 4 }}>
              {d.ss}s · {Math.round(f.progress * 100)}% of {f.goalH}h
            </div>
          </ProgressRing>

          <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', marginTop: 22, paddingTop: 18, borderTop: '1px solid var(--border)' }}>
            <div>
              <div className="caption">Started</div>
              <div style={{ fontSize: 14, fontWeight: 500, marginTop: 2 }} className="tnum">{fmtTime(startedAt)}</div>
            </div>
            <div style={{ textAlign: 'center' }}>
              <div className="caption">Remaining</div>
              <div style={{ fontSize: 14, fontWeight: 500, marginTop: 2 }} className="tnum">
                {dr.h}h {dr.mm}m
              </div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div className="caption">Goal</div>
              <div style={{ fontSize: 14, fontWeight: 500, marginTop: 2 }} className="tnum">{fmtTime(goalAt)}</div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: 10, width: '100%', marginTop: 18, paddingTop: 18, borderTop: '1px solid var(--border)' }}>
            <button className="btn btn-soft" style={{ flex: 1 }} onClick={() => f.endFast()}>
              <Icon.Stop /> End fast
            </button>
            <button className="btn btn-danger" style={{ flex: 1 }} onClick={() => f.resetFast()}>
              <Icon.Food /> I ate
            </button>
          </div>
        </div>

        <div style={{ width: '100%' }}>
          <EnergyPhaseCard onOpen={() => onShowStages && onShowStages()} />
        </div>

      </div>
      <BottomNav active="home" onChange={onNav} />
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// WEIGHT entry
// ─────────────────────────────────────────────────────────────
function WeightScreen({ onNav }) {
  const f = useFast();
  const last = f.history[f.history.length - 1].weight;
  const prev = f.history[f.history.length - 2].weight;
  const trend = last - prev;
  const [val, setVal] = React.useState(last.toFixed(1));
  const w = fmtWeight(parseFloat(val) || last, f.units);
  const trendW = fmtWeight(Math.abs(trend), f.units);

  const adjust = (delta) => {
    const cur = parseFloat(val) || last;
    setVal((cur + delta).toFixed(1));
  };

  return (
    <div className="fast-screen">
      <FastHeader title="Weight" left={<button className="fast-iconbtn" onClick={() => onNav && onNav('home')}><Icon.Back /></button>} />
      <div className="fast-content" style={{ paddingTop: 8 }}>

        <div className="card" style={{ textAlign: 'center', padding: '28px 18px' }}>
          <div className="h-eyebrow">{fmtDate(new Date(), { weekday: 'long', month: 'short', day: 'numeric' })}</div>

          <div className="fast-num" style={{ marginTop: 18 }}>
            <button className="fast-iconbtn" style={{ width: 44, height: 44, background: 'var(--border-2)' }}
              onClick={() => adjust(-0.1)}><Icon.Minus /></button>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, margin: '0 14px', minWidth: 180, justifyContent: 'center' }}>
              <span className="h-display tnum" style={{ fontSize: 64 }}>{w.val}</span>
              <span style={{ fontSize: 20, color: 'var(--muted)', fontWeight: 400 }}>{w.unit}</span>
            </div>
            <button className="fast-iconbtn" style={{ width: 44, height: 44, background: 'var(--border-2)' }}
              onClick={() => adjust(0.1)}><Icon.Plus /></button>
          </div>

          <div style={{ marginTop: 18, display: 'flex', justifyContent: 'center', gap: 8 }}>
            <div className="fast-seg">
              {[-1, -0.1, 0.1, 1].map((d) => (
                <button key={d} className="fast-seg-btn" onClick={() => adjust(d)}>
                  {d > 0 ? '+' : ''}{d}
                </button>
              ))}
            </div>
          </div>

          <div style={{ marginTop: 22, display: 'flex', justifyContent: 'space-around', borderTop: '1px solid var(--border)', paddingTop: 16 }}>
            <div>
              <div className="caption">Previous</div>
              <div style={{ fontSize: 15, fontWeight: 500, marginTop: 2 }} className="tnum">
                {fmtWeight(prev, f.units).val} {fmtWeight(prev, f.units).unit}
              </div>
            </div>
            <div>
              <div className="caption">Change</div>
              <div style={{ fontSize: 15, fontWeight: 500, marginTop: 2, color: trend < 0 ? 'var(--primary)' : 'var(--accent)' }} className="tnum">
                {trend < 0 ? '−' : '+'}{trendW.val} {trendW.unit}
              </div>
            </div>
            <div>
              <div className="caption">7-day avg</div>
              <div style={{ fontSize: 15, fontWeight: 500, marginTop: 2 }} className="tnum">
                {(() => { const avg = f.history.slice(-7).reduce((a, b) => a + b.weight, 0) / 7; const x = fmtWeight(avg, f.units); return x.val + ' ' + x.unit; })()}
              </div>
            </div>
          </div>

          <div style={{ marginTop: 20, paddingTop: 16, borderTop: '1px solid var(--border)' }}>
            <button className="btn btn-primary btn-full" onClick={() => onNav && onNav('home')}>
              <Icon.Check /> Save weight
            </button>
          </div>
        </div>

        <div style={{ marginTop: 20 }}>
          <WeightTrendCard weeks={8} />
        </div>
      </div>
      <BottomNav active="weight" onChange={onNav} />
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// PROGRESS — dual-axis chart of fasting hours + weight
// ─────────────────────────────────────────────────────────────
function ProgressChart({ data, units, dark }) {
  const W = 340, H = 220;
  const PAD = { t: 20, r: 36, b: 28, l: 32 };
  const innerW = W - PAD.l - PAD.r;
  const innerH = H - PAD.t - PAD.b;

  const weights = data.map((d) => d.weight);
  const fastHrs = data.map((d) => d.fastHours);
  // Y axis: weight (left)
  const wMin = Math.floor(Math.min(...weights) - 1);
  const wMax = Math.ceil(Math.max(...weights) + 1);
  // Y axis: fasting hours (right)
  const fMin = 0, fMax = 24;

  const xAt = (i) => PAD.l + (i / (data.length - 1)) * innerW;
  const wY = (v) => PAD.t + (1 - (v - wMin) / (wMax - wMin)) * innerH;
  const fY = (v) => PAD.t + (1 - (v - fMin) / (fMax - fMin)) * innerH;

  const weightPath = data.map((d, i) => `${i === 0 ? 'M' : 'L'} ${xAt(i)} ${wY(d.weight)}`).join(' ');
  const fastPath = data.map((d, i) => `${i === 0 ? 'M' : 'L'} ${xAt(i)} ${fY(d.fastHours)}`).join(' ');

  const lblW = (v) => units === 'metric' ? (v * 0.45359237).toFixed(0) : v.toFixed(0);

  const wTicks = 4;
  const wTickVals = Array.from({ length: wTicks + 1 }, (_, i) => wMin + (wMax - wMin) * (i / wTicks));

  const grid = 'var(--border)';
  const muted = 'var(--muted)';

  return (
    <svg width={W} height={H} style={{ display: 'block' }}>
      {/* gridlines */}
      {wTickVals.map((v, i) => (
        <g key={i}>
          <line x1={PAD.l} x2={W - PAD.r} y1={wY(v)} y2={wY(v)} stroke={grid} strokeWidth="1" strokeDasharray={i === 0 || i === wTicks ? '0' : '2 4'} />
          <text x={PAD.l - 6} y={wY(v) + 3} fontSize="10" fill={muted} textAnchor="end" fontFamily="Geist Mono">{lblW(v)}</text>
        </g>
      ))}
      {/* right axis ticks */}
      {[0, 8, 16, 24].map((v) => (
        <text key={v} x={W - PAD.r + 6} y={fY(v) + 3} fontSize="10" fill={muted} fontFamily="Geist Mono">{v}h</text>
      ))}

      {/* Fasting hours: lighter, dashed-ish + filled dots */}
      <path d={fastPath} stroke="var(--accent)" strokeWidth="1.8" fill="none" strokeLinecap="round" strokeLinejoin="round" strokeOpacity="0.85" />
      {data.map((d, i) => (
        <circle key={'f' + i} cx={xAt(i)} cy={fY(d.fastHours)} r="2.5" fill="var(--accent)" />
      ))}

      {/* Weight: primary, thicker */}
      <path d={weightPath} stroke="var(--primary)" strokeWidth="2.2" fill="none" strokeLinecap="round" strokeLinejoin="round" />
      {data.map((d, i) => (
        <circle key={'w' + i} cx={xAt(i)} cy={wY(d.weight)} r="3" fill="var(--bg)" stroke="var(--primary)" strokeWidth="2" />
      ))}

      {/* X labels — every 2 days */}
      {data.map((d, i) => (i % 2 === 0) ? (
        <text key={'x' + i} x={xAt(i)} y={H - 8} fontSize="10" fill={muted} textAnchor="middle" fontFamily="Geist Mono">
          {d.date.getDate()}
        </text>
      ) : null)}
    </svg>
  );
}

// Date input styled to match — wraps native input[type=date] for accessibility
function DateField({ label, value, min, max, onChange }) {
  return (
    <label style={{
      flex: 1,
      display: 'flex', flexDirection: 'column', gap: 4,
      padding: '8px 12px',
      borderRadius: 10,
      background: 'var(--border-2)',
      cursor: 'pointer',
    }}>
      <span className="caption" style={{ fontSize: 10, letterSpacing: '0.08em', textTransform: 'uppercase' }}>{label}</span>
      <input type="date" value={value} min={min} max={max}
        onChange={(e) => onChange(e.target.value)}
        style={{
          background: 'transparent', border: 0, padding: 0, margin: 0,
          fontFamily: 'Geist Mono, ui-monospace, monospace',
          fontSize: 13, fontWeight: 500, color: 'var(--ink)',
          fontVariantNumeric: 'tabular-nums',
          width: '100%',
        }} />
    </label>
  );
}

function ProgressScreen({ onNav }) {
  const f = useFast();
  const [range, setRange] = React.useState('14d');
  const [pickerOpen, setPickerOpen] = React.useState(false);

  // Custom range defaults
  const todayISO = React.useMemo(() => {
    const t = new Date(); t.setHours(0, 0, 0, 0);
    return t.toISOString().slice(0, 10);
  }, []);
  const fourteenAgoISO = React.useMemo(() => {
    const t = new Date(); t.setHours(0, 0, 0, 0); t.setDate(t.getDate() - 13);
    return t.toISOString().slice(0, 10);
  }, []);
  const [fromDate, setFromDate] = React.useState(fourteenAgoISO);
  const [toDate, setToDate] = React.useState(todayISO);

  // Quick selector definitions — single source of truth for labels and durations
  const QUICKS = [
    { id: '7d',  label: 'Last 7 days',  days: 7 },
    { id: '14d', label: 'Last 14 days', days: 14 },
    { id: '30d', label: 'Last 30 days', days: 30 },
    { id: '90d', label: 'Last 90 days', days: 90 },
    { id: 'tw',  label: 'This week',    days: ((new Date().getDay() + 6) % 7) + 1 },
    { id: 'tm',  label: 'This month',   days: new Date().getDate() },
    { id: 'ytd', label: 'Year to date', days: Math.floor((new Date() - new Date(new Date().getFullYear(), 0, 1)) / 86400000) + 1 },
  ];
  const currentQuick = QUICKS.find((q) => q.id === range);

  const fmtChip = (iso) => new Date(iso).toLocaleDateString([], { month: 'short', day: 'numeric' });
  const rangeLabel = range === 'custom'
    ? `${fmtChip(fromDate)} – ${fmtChip(toDate)}`
    : (currentQuick ? currentQuick.label : 'Last 14 days');

  // Compute display data
  const data = React.useMemo(() => {
    if (range === 'custom') {
      const from = new Date(fromDate); from.setHours(0, 0, 0, 0);
      const to = new Date(toDate); to.setHours(0, 0, 0, 0);
      return f.history.filter((h) => h.date >= from && h.date <= to);
    }
    const q = QUICKS.find((x) => x.id === range);
    const n = q ? q.days : 14;
    return f.history.slice(-Math.min(n, f.history.length));
  }, [range, fromDate, toDate, f.history]);

  const chartData = data.length >= 2 ? data : f.history.slice(-2);

  const pickQuick = (id) => { setRange(id); setPickerOpen(false); };

  const totalFast = data.reduce((a, b) => a + b.fastHours, 0);
  const avgFast = data.length ? totalFast / data.length : 0;
  const startW = data.length ? data[0].weight : f.history[0].weight;
  const nowW = data.length ? data[data.length - 1].weight : f.history[f.history.length - 1].weight;
  const delta = nowW - startW;
  const startWf = fmtWeight(startW, f.units);
  const nowWf = fmtWeight(nowW, f.units);
  const deltaWf = fmtWeight(Math.abs(delta), f.units);

  return (
    <div className="fast-screen">
      <FastHeader title="Progress" />
      <div className="fast-content" style={{ paddingTop: 4 }}>

        {/* Single range chip */}
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: pickerOpen ? 12 : 18 }}>
          <button onClick={() => setPickerOpen((o) => !o)}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 8,
              padding: '8px 16px', borderRadius: 100,
              background: 'var(--border-2)', color: 'var(--ink)',
              fontSize: 13, fontWeight: 500,
            }}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18M8 3v4M16 3v4"/>
            </svg>
            <span className={range === 'custom' ? 'tnum' : ''}>{rangeLabel}</span>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
              style={{ transform: pickerOpen ? 'rotate(180deg)' : 'none', transition: 'transform .15s', opacity: 0.6 }}>
              <path d="M6 9l6 6 6-6"/>
            </svg>
          </button>
        </div>

        {pickerOpen && (
          <div className="card" style={{ marginBottom: 14, padding: 16 }}>
            <div className="h-eyebrow" style={{ marginBottom: 10 }}>Quick select</div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
              {QUICKS.map((q) => {
                const on = range === q.id;
                return (
                  <button key={q.id} onClick={() => pickQuick(q.id)}
                    style={{
                      padding: '7px 12px', fontSize: 12, fontWeight: 500, borderRadius: 100,
                      background: on ? 'var(--primary)' : 'var(--border-2)',
                      color: on ? 'var(--surface)' : 'var(--ink-2)',
                    }}>{q.label}</button>
                );
              })}
            </div>

            <div className="divider" style={{ margin: '14px -16px' }} />

            <div className="h-eyebrow" style={{ marginBottom: 10 }}>Custom range</div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <DateField label="From" value={fromDate} max={toDate} onChange={(v) => { setFromDate(v); setRange('custom'); }} />
              <div style={{ color: 'var(--muted)', fontSize: 16, padding: '0 2px' }}>→</div>
              <DateField label="To" value={toDate} min={fromDate} max={todayISO} onChange={(v) => { setToDate(v); setRange('custom'); }} />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 14 }}>
              <button onClick={() => setPickerOpen(false)}
                style={{
                  padding: '8px 18px', fontSize: 12, fontWeight: 500, borderRadius: 100,
                  background: 'var(--primary)', color: 'var(--surface)',
                }}>Done</button>
            </div>
          </div>
        )}

        <div className="card" style={{ padding: '16px 12px' }}>
          {/* Legend */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0 8px 8px' }}>
            <div style={{ display: 'flex', gap: 14 }}>
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 12, color: 'var(--ink-2)' }}>
                <span style={{ width: 10, height: 2, background: 'var(--primary)', borderRadius: 1 }} />
                Weight ({f.units === 'metric' ? 'kg' : 'lb'})
              </span>
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 12, color: 'var(--ink-2)' }}>
                <span style={{ width: 10, height: 2, background: 'var(--accent)', borderRadius: 1 }} />
                Fast (h)
              </span>
            </div>
          </div>
          <ProgressChart data={chartData} units={f.units} />
        </div>

        {/* Summary stats */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginTop: 14 }}>
          <div className="card" style={{ padding: 14 }}>
            <div className="caption">Avg fast</div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 4, marginTop: 6 }}>
              <span className="tnum" style={{ fontSize: 24, fontWeight: 500 }}>{avgFast.toFixed(1)}</span>
              <span className="caption">h / day</span>
            </div>
            <div className="caption" style={{ marginTop: 4 }}>{data.length}-day average</div>
          </div>
          <div className="card" style={{ padding: 14 }}>
            <div className="caption">Weight change</div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 4, marginTop: 6 }}>
              <span className="tnum" style={{ fontSize: 24, fontWeight: 500, color: delta < 0 ? 'var(--primary)' : 'var(--accent)' }}>
                {delta < 0 ? '−' : '+'}{deltaWf.val}
              </span>
              <span className="caption">{deltaWf.unit}</span>
            </div>
            <div className="caption" style={{ marginTop: 4 }}>{startWf.val} → {nowWf.val} {nowWf.unit}</div>
          </div>
          <div className="card" style={{ padding: 14 }}>
            <div className="caption">Total fasted</div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 4, marginTop: 6 }}>
              <span className="tnum" style={{ fontSize: 24, fontWeight: 500 }}>{Math.round(totalFast)}</span>
              <span className="caption">hours</span>
            </div>
            <div className="caption" style={{ marginTop: 4 }}>Since {fmtDate(data[0] ? data[0].date : f.history[0].date)}</div>
          </div>
          <div className="card" style={{ padding: 14 }}>
            <div className="caption">Streak</div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 4, marginTop: 6 }}>
              <span className="tnum" style={{ fontSize: 24, fontWeight: 500 }}>12</span>
              <span className="caption">days</span>
            </div>
            <div className="caption" style={{ marginTop: 4 }}>Hit goal every day</div>
          </div>
        </div>

        {/* History log — merged in. Shows the selected range. */}
        <div style={{ marginTop: 22, display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', padding: '0 4px' }}>
          <div className="h-eyebrow">History</div>
          <div className="caption">{data.length} {data.length === 1 ? 'entry' : 'entries'}</div>
        </div>
        <div className="card" style={{ padding: '4px 16px', marginTop: 8 }}>
          {[...data].reverse().map((h, i, arr) => (
            <HistoryRow key={h.date.getTime()} entry={h} units={f.units} goalH={f.planObj.fast}
              isLast={i === arr.length - 1}
              onClick={() => onNav && onNav('day-detail', { dayKey: h.date.getTime() })} />
          ))}
        </div>
      </div>
      <BottomNav active="progress" onChange={onNav} />
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// HISTORY log
// ─────────────────────────────────────────────────────────────
function HistoryBar({ hours, goalH, hit }) {
  const W = 64, H = 6;
  const fillW = Math.min(1, hours / 24) * W;
  const goalX = (goalH / 24) * W;
  return (
    <svg width={W} height={H + 6} style={{ display: 'block' }}>
      {/* track */}
      <rect x="0" y="3" width={W} height={H} rx={H / 2} fill="var(--border-2)" />
      {/* fill */}
      <rect x="0" y="3" width={fillW} height={H} rx={H / 2}
        fill={hit ? 'var(--primary)' : 'var(--accent)'} />
      {/* goal tick */}
      <rect x={goalX - 0.5} y="0" width="1" height={H + 6} fill="var(--ink)" opacity="0.5" />
    </svg>
  );
}

function HistoryRow({ entry, units, goalH, isLast, onClick }) {
  const wf = fmtWeight(entry.weight, units);
  const hours = Math.floor(entry.fastHours);
  const mins = Math.round((entry.fastHours - hours) * 60);
  const hit = entry.fastHours >= goalH;
  return (
    <button onClick={onClick} className="fast-row" style={{
      borderBottom: isLast ? 0 : '1px solid var(--border)',
      width: '100%', textAlign: 'left', cursor: onClick ? 'pointer' : 'default',
      padding: '18px 0', gap: 16,
    }}>
      <div style={{ width: 44, textAlign: 'center', flexShrink: 0 }}>
        <div className="caption" style={{ fontSize: 10, letterSpacing: '0.06em' }}>{fmtDate(entry.date, { weekday: 'short' }).toUpperCase()}</div>
        <div className="tnum" style={{ fontSize: 19, fontWeight: 500, marginTop: 4 }}>{entry.date.getDate()}</div>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <div className="fast-row-label tnum">{hours}h {mins}m</div>
          {hit ? (
            <span style={{ fontSize: 10, letterSpacing: '0.06em', color: 'var(--primary)', background: 'var(--primary-soft)', padding: '2px 6px', borderRadius: 4, fontWeight: 600 }}>GOAL</span>
          ) : (
            <span style={{ fontSize: 10, letterSpacing: '0.06em', color: 'var(--accent)', background: 'var(--accent-soft)', padding: '2px 6px', borderRadius: 4, fontWeight: 600 }}>SHORT</span>
          )}
        </div>
        <div className="fast-row-sub tnum" style={{ marginTop: 4 }}>Weight {wf.val} {wf.unit}</div>
      </div>
      <div style={{ width: 70, display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4, flexShrink: 0 }}>
        <HistoryBar hours={entry.fastHours} goalH={goalH} hit={hit} />
        <div className="caption tnum" style={{ fontSize: 10 }}>goal {goalH}h</div>
      </div>
      {onClick && <Icon.Chevron style={{ color: 'var(--subtle)', marginLeft: 4, flexShrink: 0 }} />}
    </button>
  );
}

function HistoryScreen({ onNav }) {
  const f = useFast();
  const reversed = [...f.history].reverse();

  return (
    <div className="fast-screen">
      <FastHeader title="History" />
      <div className="fast-content" style={{ paddingTop: 6 }}>
        <div className="card" style={{ padding: '4px 16px' }}>
          {reversed.map((h, i) => (
            <HistoryRow key={i} entry={h} units={f.units} goalH={f.planObj.fast}
              isLast={i === reversed.length - 1}
              onClick={() => onNav && onNav('day-detail', { dayKey: h.date.getTime() })} />
          ))}
        </div>
      </div>
      <BottomNav active="progress" onChange={onNav} />
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// DAY DETAIL — opened from a history row
// Shows: hero with duration + chip, editable start/end times,
// editable weight, delete option.
// ─────────────────────────────────────────────────────────────
function DayDetailScreen({ dayKey, onBack, onNav }) {
  const f = useFast();
  // Resolve the entry by timestamp; fall back to most recent.
  const entry = React.useMemo(() => {
    if (dayKey != null) {
      const found = f.history.find((h) => h.date.getTime() === dayKey);
      if (found) return found;
    }
    return f.history[f.history.length - 1];
  }, [dayKey, f.history]);

  const goalH = f.planObj.fast;
  const dayWaterMl = entry.waterMl || 0;

  // History entries only store fastHours — fabricate a plausible start/end
  // pair anchored to the user's configured fastStartTime. Editing these is
  // local to this screen (prototype: nothing persists back to history).
  const initialStart = f.fastStartTime || '20:00';
  const initialEnd = addHoursToTime(initialStart, entry.fastHours);
  const [startTime, setStartTime] = React.useState(initialStart);
  const [endTime, setEndTime] = React.useState(initialEnd);
  const [weight, setWeight] = React.useState(entry.weight);
  const [notes, setNotes] = React.useState('');

  const duration = diffHoursTime(startTime, endTime);
  const dh = Math.floor(duration);
  const dm = Math.round((duration - dh) * 60);
  const hit = duration >= goalH;
  const wf = fmtWeight(weight, f.units);

  // Stepper deltas in user's chosen units.
  const stepUp = () => setWeight((w) => w + (f.units === 'metric' ? 0.1 / 0.45359237 : 0.1));
  const stepDown = () => setWeight((w) => w - (f.units === 'metric' ? 0.1 / 0.45359237 : 0.1));

  // Previous day for delta context
  const idx = f.history.indexOf(entry);
  const prev = idx > 0 ? f.history[idx - 1] : null;
  const delta = prev ? weight - prev.weight : 0;
  const deltaWf = fmtWeight(Math.abs(delta), f.units);

  return (
    <div className="fast-screen">
      <FastHeader
        title={entry.date.toLocaleDateString([], { weekday: 'long', month: 'short', day: 'numeric' })}
        left={<button className="fast-iconbtn" onClick={() => onBack && onBack()}><Icon.Back /></button>} />
      <div className="fast-content" style={{ paddingTop: 6 }}>

        {/* Fast recap — same card used on the home screen, with editable times */}
        {(() => {
          const endMs = (() => { const e = new Date(entry.date); const [eh, em] = endTime.split(':').map(Number); e.setHours(eh, em, 0, 0); return e.getTime(); })();
          const startMs = endMs - duration * 3600000;
          return (
            <>
              <LastFastCard
                fast={{ startMs, endMs, durationH: duration, goalH, planLabel: f.planObj.label }}
                edit={{ start: startTime, end: endTime, onStart: setStartTime, onEnd: setEndTime }}
                onShare={() => {}} />
              <button className="btn btn-soft btn-full" style={{ marginTop: 12 }}
                onClick={() => { f.resumeFast(startMs); onNav && onNav('home'); }}>
                <Icon.Play /> Resume this fast
              </button>
            </>
          );
        })()}

        {/* Weight */}
        <SectionLabel>Weight</SectionLabel>
        <div className="card" style={{ padding: '16px 16px 18px', textAlign: 'center' }}>
          <div className="fast-num">
            <button className="fast-iconbtn" style={{ width: 40, height: 40, background: 'var(--border-2)' }} onClick={stepDown}><Icon.Minus /></button>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, margin: '0 14px', minWidth: 130, justifyContent: 'center' }}>
              <span className="h-display tnum" style={{ fontSize: 40 }}>{wf.val}</span>
              <span style={{ fontSize: 16, color: 'var(--muted)' }}>{wf.unit}</span>
            </div>
            <button className="fast-iconbtn" style={{ width: 40, height: 40, background: 'var(--border-2)' }} onClick={stepUp}><Icon.Plus /></button>
          </div>
          {prev && (
            <div className="caption" style={{ marginTop: 8 }}>
              <span style={{ color: delta < 0 ? 'var(--primary)' : delta > 0 ? 'var(--accent)' : 'var(--muted)' }} className="tnum">
                {delta < 0 ? '−' : delta > 0 ? '+' : ''}{deltaWf.val} {deltaWf.unit}
              </span>
              <span style={{ color: 'var(--muted)' }}> vs. previous day</span>
            </div>
          )}
        </div>

        {/* Water */}
        <SectionLabel>Water</SectionLabel>
        <DayWaterCard ml={dayWaterMl} goalMl={f.waterGoal} units={f.units} />

        {/* Notes */}
        <SectionLabel>Notes</SectionLabel>
        <div className="card" style={{ padding: 14 }}>
          <textarea value={notes} onChange={(e) => setNotes(e.target.value)}
            placeholder="How did this day feel?"
            rows={3}
            style={{
              width: '100%', border: 0, background: 'transparent', resize: 'none',
              fontFamily: 'inherit', fontSize: 14, color: 'var(--ink)',
              outline: 'none',
            }} />
        </div>

        {/* Actions */}
        <div style={{ display: 'flex', gap: 10, marginTop: 22 }}>
          <button className="btn btn-ghost" style={{ flex: 1, height: 48, color: 'var(--accent)' }}>
            Delete entry
          </button>
          <button className="btn btn-primary" style={{ flex: 1.4, height: 48 }} onClick={() => onBack && onBack()}>
            <Icon.Check /> Save
          </button>
        </div>
        <div style={{ height: 8 }} />
      </div>
    </div>
  );
}

function DayWaterCard({ ml, goalMl, units }) {
  const total = fmtWater(ml, units);
  const goal = fmtWater(goalMl, units);
  const pct = Math.min(100, Math.round((ml / goalMl) * 100));
  const hit = ml >= goalMl;
  return (
    <div className="card" style={{ padding: 16, display: 'flex', alignItems: 'center', gap: 14 }}>
      <WaterGlass progress={Math.min(1, ml / goalMl)} size={88} />
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 4 }}>
          <span className="tnum" style={{ fontSize: 24, fontWeight: 500 }}>{total.val}</span>
          <span style={{ fontSize: 13, color: 'var(--muted)' }}>{total.unit}</span>
          {hit && (
            <span style={{ marginLeft: 8, fontSize: 10, letterSpacing: '0.06em', color: 'var(--primary)', background: 'var(--primary-soft)', padding: '2px 6px', borderRadius: 4, fontWeight: 600 }}>GOAL</span>
          )}
        </div>
        <div className="caption tnum" style={{ marginTop: 2 }}>of {goal.val} {goal.unit} · {pct}%</div>
        <div style={{ marginTop: 10, height: 5, background: 'var(--border-2)', borderRadius: 3, overflow: 'hidden' }}>
          <div style={{ height: '100%', width: `${pct}%`, background: 'var(--primary)', borderRadius: 3 }} />
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// SETTINGS
// ─────────────────────────────────────────────────────────────
function SettingsScreen({ onNav, onBack }) {
  const f = useFast();
  const [stickyNotif, setStickyNotif] = React.useState(true);

  return (
    <div className="fast-screen">
      <FastHeader title="Settings" left={<button className="fast-iconbtn" onClick={() => onBack ? onBack() : onNav && onNav('home')}><Icon.Back /></button>} />
      <div className="fast-content" style={{ paddingTop: 6 }}>

        <SectionLabel>Fasting</SectionLabel>
        <div className="card" style={{ padding: '0 16px' }}>
          <NavRow
            label="Fasting protocol"
            sub={`${f.planObj.fast}h fast · ${24 - f.planObj.fast}h eating window`}
            trailing={f.planObj.label}
            onClick={() => onNav && onNav('plan-picker')} />
          <ToggleRow
            label="Fasting reminders"
            sub="Window start and end"
            value={f.fastingReminder}
            onChange={() => f.setFastingReminder(!f.fastingReminder)} />
          <TimeRow
            label="Daily fasting start"
            sub={`Ends at ${addHoursToTime(f.fastStartTime, f.planObj.fast)} (${f.planObj.label})`}
            value={f.fastStartTime}
            onChange={f.setFastStartTime}
            dim={!f.fastingReminder} />
          <ToggleRow
            label="Sticky notification"
            sub="Show progress while fasting"
            value={stickyNotif}
            onChange={() => setStickyNotif(!stickyNotif)} />
        </div>

        <SectionLabel>Weight</SectionLabel>
        <div className="card" style={{ padding: '0 16px' }}>
          <div className="fast-row" style={{ padding: '18px 0' }}>
            <div style={{ flex: 1 }}>
              <div className="fast-row-label">Units</div>
            </div>
            <div className="fast-seg">
              <button className={'fast-seg-btn' + (f.units === 'metric' ? ' active' : '')} onClick={() => f.setUnits('metric')}>kg</button>
              <button className={'fast-seg-btn' + (f.units === 'imperial' ? ' active' : '')} onClick={() => f.setUnits('imperial')}>lb</button>
            </div>
          </div>
          <ToggleRow
            label="Daily weigh-in reminder"
            value={f.weightReminder}
            onChange={() => f.setWeightReminder(!f.weightReminder)} />
          <TimeRow
            label="Reminder time"
            value={f.reminderTime}
            onChange={f.setReminderTime}
            dim={!f.weightReminder} />
        </div>

        <SectionLabel>Water</SectionLabel>
        <div className="card" style={{ padding: '0 16px' }}>
          <WaterGoalRow
            label="Daily goal"
            value={f.waterGoal}
            onChange={f.setWaterGoal}
            units={f.units} />
        </div>

        <SectionLabel>About</SectionLabel>
        <div className="card" style={{ padding: '0 16px' }}>
          <NavRow label="Privacy policy" />
          <NavRow label="Help & support" />
          <NavRow label="Version" trailing="1.0.0" hideChevron />
        </div>
        <div style={{ height: 12 }} />
      </div>
    </div>
  );
}

function SectionLabel({ children }) {
  return <div className="h-eyebrow" style={{ margin: '22px 6px 10px' }}>{children}</div>;
}

function ToggleRow({ label, sub, value, onChange }) {
  return (
    <button className="fast-row" onClick={onChange} style={{ width: '100%', textAlign: 'left', padding: '18px 0' }}>
      <div style={{ flex: 1 }}>
        <div className="fast-row-label">{label}</div>
        {sub && <div className="fast-row-sub">{sub}</div>}
      </div>
      <div className={'fast-toggle' + (value ? ' on' : '')} />
    </button>
  );
}

function NavRow({ label, sub, trailing, onClick, hideChevron }) {
  return (
    <button className="fast-row" onClick={onClick} style={{ width: '100%', textAlign: 'left', padding: '18px 0' }}>
      <div style={{ flex: 1 }}>
        <div className="fast-row-label">{label}</div>
        {sub && <div className="fast-row-sub">{sub}</div>}
      </div>
      {trailing && <div className="caption tnum" style={{ fontSize: 14, color: 'var(--muted)', fontWeight: 500 }}>{trailing}</div>}
      {!hideChevron && <Icon.Chevron style={{ color: 'var(--subtle)', marginLeft: trailing ? 8 : 0 }} />}
    </button>
  );
}

function WaterGoalRow({ label, value, onChange, units }) {
  const PRESETS_ML = [1500, 2000, 2500, 3000, 3500, 4000];
  const cur = fmtWater(value, units);
  const cycle = () => {
    const idx = PRESETS_ML.indexOf(value);
    const next = idx === -1 ? 2500 : PRESETS_ML[(idx + 1) % PRESETS_ML.length];
    onChange(next);
  };
  return (
    <button className="fast-row" onClick={cycle} style={{ width: '100%', textAlign: 'left', padding: '18px 0' }}>
      <div style={{ flex: 1 }}>
        <div className="fast-row-label">{label}</div>
        <div className="fast-row-sub">Tap to cycle: 1.5–4 L</div>
      </div>
      <div className="tnum" style={{ fontSize: 14, fontWeight: 500, color: 'var(--muted)' }}>
        {cur.val} {cur.unit}
      </div>
    </button>
  );
}

function TimeRow({ label, sub, value, onChange, dim }) {
  // Native time input — taps open the Android time picker.
  return (
    <label className="fast-row" style={{ width: '100%', padding: '18px 0', opacity: dim ? 0.4 : 1, cursor: dim ? 'default' : 'pointer' }}>
      <div style={{ flex: 1 }}>
        <div className="fast-row-label">{label}</div>
        {sub && <div className="fast-row-sub tnum">{sub}</div>}
      </div>
      <div style={{ position: 'relative', display: 'inline-flex', alignItems: 'center', gap: 4 }}>
        <span className="tnum" style={{ fontSize: 14, color: 'var(--muted)', fontWeight: 500 }}>{value}</span>
        <Icon.Chevron style={{ color: 'var(--subtle)' }} />
        <input type="time" value={value} disabled={dim}
          onChange={(e) => e.target.value && onChange(e.target.value)}
          style={{
            position: 'absolute', inset: 0, opacity: 0, cursor: 'inherit',
            width: '100%', height: '100%',
          }} />
      </div>
    </label>
  );
}

function SimpleRow({ label, trailing }) {
  return <NavRow label={label} trailing={trailing} hideChevron={!!trailing} />;
}

// ─────────────────────────────────────────────────────────────
// STAGES detail screen

// ─────────────────────────────────────────────────────────────
// PLAN PICKER — reached from Settings → Fasting protocol
// Layout mirrors onboarding step 2 but updates the live plan.
// ─────────────────────────────────────────────────────────────
function PlanPickerScreen({ onBack }) {
  const f = useFast();
  const [selected, setSelected] = React.useState(f.plan);
  const commit = () => { f.setPlan(selected); onBack && onBack(); };
  return (
    <div className="fast-screen">
      <FastHeader title="Fasting protocol" left={<button className="fast-iconbtn" onClick={() => onBack && onBack()}><Icon.Back /></button>} />
      <div className="fast-content" style={{ paddingTop: 4 }}>
        <p className="body" style={{ marginTop: 0, marginBottom: 18 }}>
          Choose how long you want to fast each day. You can change this any time.
        </p>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {PLANS.map((p) => {
            const on = selected === p.id;
            return (
              <button key={p.id} onClick={() => setSelected(p.id)}
                style={{
                  textAlign: 'left', padding: '14px 16px', borderRadius: 14,
                  border: '1.5px solid ' + (on ? 'var(--primary)' : 'var(--border)'),
                  background: on ? 'var(--primary-soft)' : 'var(--card)',
                  display: 'flex', alignItems: 'center', gap: 12,
                }}>
                <div style={{
                  width: 56, height: 56, borderRadius: 14, background: 'var(--bg)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  border: '1px solid var(--border)',
                }}>
                  <span className="tnum" style={{ fontSize: 16, fontWeight: 600 }}>{p.label}</span>
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 15, fontWeight: 500 }}>{p.fast}h fasting</div>
                  <div className="caption" style={{ marginTop: 2 }}>{p.sub} · {24 - p.fast}h eating window</div>
                </div>
                <div style={{
                  width: 22, height: 22, borderRadius: 11,
                  border: '1.5px solid ' + (on ? 'var(--primary)' : 'var(--border)'),
                  background: on ? 'var(--primary)' : 'transparent',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--surface)',
                }}>{on && <Icon.Check />}</div>
              </button>
            );
          })}
        </div>
        <button className="btn btn-primary btn-full" style={{ marginTop: 22 }} onClick={commit}>
          <Icon.Check /> Save
        </button>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// STAGES detail screen
// ─────────────────────────────────────────────────────────────
function StagesScreen({ onNav, onBack }) {
  const f = useFast();
  return (
    <div className="fast-screen">
      <FastHeader title="Metabolic stages" left={<button className="fast-iconbtn" onClick={() => onBack ? onBack() : onNav && onNav('home')}><Icon.Back /></button>} />
      <div className="fast-content" style={{ paddingTop: 6 }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {f.stages.map((s, i) => {
            const current = i === f.stageIdx;
            const passed = i < f.stageIdx;
            return (
              <div key={s.id} className="card" style={{
                padding: 16,
                borderColor: current ? 'var(--primary)' : 'var(--border)',
                borderWidth: current ? 1.5 : 1,
                background: current ? 'var(--primary-soft)' : 'var(--card)',
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <div className="tnum" style={{
                      width: 24, height: 24, borderRadius: 12, fontSize: 11,
                      display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 600,
                      background: current ? 'var(--primary)' : passed ? 'var(--ink)' : 'var(--border)',
                      color: current ? 'var(--surface)' : passed ? 'var(--surface)' : 'var(--muted)',
                    }}>{i + 1}</div>
                    <div style={{ fontSize: 16, fontWeight: 500, letterSpacing: '-0.01em' }}>{s.name}</div>
                  </div>
                  <div className="caption tnum">{s.range}</div>
                </div>
                <div className="body" style={{ marginTop: 10, color: current ? 'var(--ink)' : 'var(--ink-2)' }}>{s.body}</div>
                <div style={{ marginTop: 10, display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                  {s.benefits.map((b) => (
                    <span key={b} style={{
                      fontSize: 11, padding: '4px 8px', borderRadius: 6,
                      background: current ? 'rgba(0,0,0,0.04)' : 'var(--border-2)',
                      color: 'var(--ink-2)',
                    }}>{b}</span>
                  ))}
                </div>
                {current && (
                  <div style={{ marginTop: 10, display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 11, fontWeight: 500, color: 'var(--primary)' }}>
                    <span className="fast-stage-dot" style={{ background: 'var(--primary)' }} />
                    You are here
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// ONBOARDING screens
// ─────────────────────────────────────────────────────────────
function OnboardWelcomeScreen() {
  return (
    <div className="fast-screen" style={{ background: 'var(--bg)' }}>
      <div style={{ flex: 1, padding: '60px 28px 28px', display: 'flex', flexDirection: 'column' }}>
        <div className="h-eyebrow">Welcome to Fast</div>
        <h1 className="h-title" style={{ marginTop: 8, fontSize: 36 }}>
          Track your fasting and your weight.
        </h1>
        <p className="body" style={{ marginTop: 12, fontSize: 15 }}>
          A quiet companion for intermittent fasting. We focus on the duration of your fast and the metabolic phases you move through.
        </p>

        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '20px 0' }}>
          <ProgressRing size={200} stroke={10} progress={0.66} color="var(--primary)">
            <div className="h-eyebrow">Stage 5</div>
            <div className="h-display" style={{ fontSize: 32, marginTop: 4 }}>14:23</div>
            <div className="caption" style={{ marginTop: 2 }}>Fat burn</div>
          </ProgressRing>
        </div>

        <div style={{ display: 'flex', gap: 6, marginBottom: 18 }}>
          <div style={{ flex: 1, height: 4, borderRadius: 2, background: 'var(--primary)' }} />
          <div style={{ flex: 1, height: 4, borderRadius: 2, background: 'var(--border)' }} />
          <div style={{ flex: 1, height: 4, borderRadius: 2, background: 'var(--border)' }} />
        </div>

        <button className="btn btn-primary btn-full">Get started <Icon.Chevron /></button>
        <button className="btn btn-ghost btn-full" style={{ marginTop: 10, border: 0 }}>I already have an account</button>
      </div>
    </div>
  );
}

function OnboardPlanScreen() {
  const [selected, setSelected] = React.useState('16:8');
  return (
    <div className="fast-screen" style={{ background: 'var(--bg)' }}>
      <FastHeader title="" left={<button className="fast-iconbtn"><Icon.Back /></button>} />
      <div style={{ flex: 1, padding: '8px 28px 28px', display: 'flex', flexDirection: 'column' }}>
        <div className="h-eyebrow">Step 2 of 3</div>
        <h1 className="h-title" style={{ marginTop: 8 }}>Choose a fasting plan.</h1>
        <p className="body" style={{ marginTop: 8 }}>You can change this any time in settings.</p>

        <div style={{ marginTop: 20, display: 'flex', flexDirection: 'column', gap: 10 }}>
          {PLANS.map((p) => {
            const on = selected === p.id;
            return (
              <button key={p.id} onClick={() => setSelected(p.id)}
                style={{
                  textAlign: 'left',
                  padding: '14px 16px',
                  borderRadius: 14,
                  border: '1.5px solid ' + (on ? 'var(--primary)' : 'var(--border)'),
                  background: on ? 'var(--primary-soft)' : 'var(--card)',
                  display: 'flex', alignItems: 'center', gap: 12,
                }}>
                <div style={{
                  width: 56, height: 56, borderRadius: 14,
                  background: 'var(--bg)',
                  display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
                  border: '1px solid var(--border)',
                }}>
                  <div className="tnum" style={{ fontSize: 16, fontWeight: 600 }}>{p.label}</div>
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 15, fontWeight: 500 }}>{p.fast}h fasting</div>
                  <div className="caption" style={{ marginTop: 2 }}>{p.sub} · {24 - p.fast}h eating window</div>
                </div>
                <div style={{
                  width: 22, height: 22, borderRadius: 11,
                  border: '1.5px solid ' + (on ? 'var(--primary)' : 'var(--border)'),
                  background: on ? 'var(--primary)' : 'transparent',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  color: 'var(--surface)',
                }}>{on && <Icon.Check />}</div>
              </button>
            );
          })}
        </div>

        <div style={{ flex: 1 }} />

        <div style={{ display: 'flex', gap: 6, margin: '18px 0' }}>
          <div style={{ flex: 1, height: 4, borderRadius: 2, background: 'var(--ink-2)' }} />
          <div style={{ flex: 1, height: 4, borderRadius: 2, background: 'var(--ink-2)' }} />
          <div style={{ flex: 1, height: 4, borderRadius: 2, background: 'var(--border)' }} />
        </div>

        <button className="btn btn-primary btn-full">Continue</button>
      </div>
    </div>
  );
}

function OnboardRemindersScreen() {
  const f = useFast();
  const [units, setUnits] = React.useState('metric');
  const [time, setTime] = React.useState('07:30');
  const [fastStart, setFastStart] = React.useState('20:00');
  const [fastingNotif, setFastingNotif] = React.useState(true);
  return (
    <div className="fast-screen" style={{ background: 'var(--bg)' }}>
      <FastHeader title="" left={<button className="fast-iconbtn"><Icon.Back /></button>} />
      <div style={{ flex: 1, padding: '8px 28px 28px', display: 'flex', flexDirection: 'column', overflowY: 'auto' }}>
        <div className="h-eyebrow">Step 3 of 3</div>
        <h1 className="h-title" style={{ marginTop: 8 }}>A few preferences.</h1>

        <div style={{ marginTop: 24 }}>
          <div className="h-eyebrow" style={{ marginBottom: 10 }}>Units</div>
          <div className="card" style={{ padding: 4, display: 'flex' }}>
            {['metric', 'imperial'].map((u) => (
              <button key={u} onClick={() => setUnits(u)}
                style={{
                  flex: 1, padding: '12px 0', borderRadius: 10,
                  background: units === u ? 'var(--primary-soft)' : 'transparent',
                  color: units === u ? 'var(--primary)' : 'var(--ink-2)',
                  fontWeight: units === u ? 600 : 500, fontSize: 14,
                }}>
                {u === 'metric' ? 'Metric (kg)' : 'Imperial (lb)'}
              </button>
            ))}
          </div>
        </div>

        <div style={{ marginTop: 20 }}>
          <div className="h-eyebrow" style={{ marginBottom: 10 }}>Fasting start</div>
          <label className="card" style={{ padding: 14, display: 'flex', alignItems: 'center', gap: 14, cursor: 'pointer', position: 'relative' }}>
            <Icon.Flame />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 500 }}>Start each day at</div>
              <div className="caption tnum" style={{ marginTop: 2 }}>
                Ends at {addHoursToTime(fastStart, f.planObj.fast)} · {f.planObj.label}
              </div>
            </div>
            <div className="tnum" style={{ fontSize: 18, fontWeight: 500 }}>{fastStart}</div>
            <input type="time" value={fastStart}
              onChange={(e) => e.target.value && setFastStart(e.target.value)}
              style={{ position: 'absolute', inset: 0, opacity: 0, cursor: 'pointer' }} />
          </label>
        </div>

        <div style={{ marginTop: 20 }}>
          <div className="h-eyebrow" style={{ marginBottom: 10 }}>Daily weigh-in</div>
          <label className="card" style={{ padding: 14, display: 'flex', alignItems: 'center', gap: 14, cursor: 'pointer', position: 'relative' }}>
            <Icon.Bell />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 500 }}>Remind me at</div>
              <div className="caption" style={{ marginTop: 2 }}>Every day, gentle nudge</div>
            </div>
            <div className="tnum" style={{ fontSize: 18, fontWeight: 500 }}>{time}</div>
            <input type="time" value={time}
              onChange={(e) => e.target.value && setTime(e.target.value)}
              style={{ position: 'absolute', inset: 0, opacity: 0, cursor: 'pointer' }} />
          </label>
        </div>

        <div style={{ marginTop: 16 }}>
          <div className="card" style={{ padding: 14, display: 'flex', alignItems: 'center', gap: 14 }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 500 }}>Fasting reminders</div>
              <div className="caption" style={{ marginTop: 2 }}>When a fast is about to start or end</div>
            </div>
            <div className={'fast-toggle' + (fastingNotif ? ' on' : '')}
              onClick={() => setFastingNotif(!fastingNotif)} />
          </div>
        </div>

        <div style={{ flex: 1, minHeight: 12 }} />

        <div style={{ display: 'flex', gap: 6, margin: '18px 0' }}>
          <div style={{ flex: 1, height: 4, borderRadius: 2, background: 'var(--ink-2)' }} />
          <div style={{ flex: 1, height: 4, borderRadius: 2, background: 'var(--ink-2)' }} />
          <div style={{ flex: 1, height: 4, borderRadius: 2, background: 'var(--ink-2)' }} />
        </div>

        <button className="btn btn-primary btn-full">Start fasting</button>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// NOTIFICATION panel preview — fake Android shade
// ─────────────────────────────────────────────────────────────
function NotificationPanelScreen() {
  const f = useFast();
  const d = fmtDuration(f.elapsedMs);
  const remaining = Math.max(0, f.goalH * 3600000 - f.elapsedMs);
  const dr = fmtDuration(remaining);

  return (
    <div style={{
      height: '100%',
      background: 'linear-gradient(180deg, rgba(20,19,15,0.92) 0%, rgba(20,19,15,0.96) 100%)',
      color: '#f6f3ee',
      display: 'flex', flexDirection: 'column',
      padding: '12px 16px',
      fontFamily: 'Geist, sans-serif',
    }}>
      {/* Status row */}
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, opacity: 0.85, padding: '4px 6px 14px' }}>
        <span className="tnum">Tue, Jan 14</span>
        <span style={{ display: 'inline-flex', gap: 8 }}>
          <span>·:·</span><span>5G</span><span>82%</span>
        </span>
      </div>

      {/* Quick toggles */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        {['WiFi', 'BT', 'DND', 'Flash'].map((t, i) => (
          <div key={t} style={{
            flex: 1, height: 56, borderRadius: 16,
            background: i === 0 ? '#7dd3a8' : 'rgba(255,255,255,0.08)',
            color: i === 0 ? '#14130f' : '#f6f3ee',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 12, fontWeight: 500,
          }}>{t}</div>
        ))}
      </div>

      {/* Brightness slider */}
      <div style={{
        height: 40, background: 'rgba(255,255,255,0.08)', borderRadius: 100,
        position: 'relative', marginBottom: 18,
      }}>
        <div style={{ position: 'absolute', left: 0, top: 0, bottom: 0, width: '60%', background: 'rgba(255,255,255,0.18)', borderRadius: 100 }} />
      </div>

      {/* Fast notification — the sticky one */}
      <div style={{
        background: '#24221d',
        borderRadius: 22,
        padding: 14,
        border: '1px solid rgba(125,211,168,0.25)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 11, opacity: 0.7, marginBottom: 10 }}>
          <div style={{
            width: 16, height: 16, borderRadius: 4,
            background: '#7dd3a8', color: '#14130f',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontWeight: 700, fontSize: 10,
          }}>F</div>
          <span style={{ fontWeight: 500 }}>FAST</span>
          <span>· now · Ongoing</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          {/* mini ring */}
          <div style={{ position: 'relative', width: 56, height: 56 }}>
            <svg width="56" height="56" style={{ transform: 'rotate(-90deg)' }}>
              <circle cx="28" cy="28" r="24" stroke="rgba(255,255,255,0.12)" strokeWidth="4" fill="none" />
              <circle cx="28" cy="28" r="24" stroke="#7dd3a8" strokeWidth="4" fill="none"
                strokeLinecap="round" strokeDasharray={2 * Math.PI * 24}
                strokeDashoffset={2 * Math.PI * 24 * (1 - f.progress)} />
            </svg>
            <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: 'Geist Mono', fontSize: 11, fontWeight: 500 }} className="tnum">
              {Math.round(f.progress * 100)}%
            </div>
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 15, fontWeight: 500 }}>Fasting · Stage {f.stageIdx + 1} {f.stage.name}</div>
            <div style={{ fontSize: 13, opacity: 0.75, marginTop: 2 }} className="tnum">
              {d.h}h {d.mm}m elapsed · {dr.h}h {dr.mm}m to go
            </div>
          </div>
        </div>
        <div style={{ display: 'flex', gap: 8, marginTop: 14 }}>
          <button style={{
            flex: 1, padding: '10px 0', borderRadius: 100,
            background: 'rgba(255,255,255,0.06)', color: '#f6f3ee',
            fontSize: 13, fontWeight: 500,
          }}>End fast</button>
          <button style={{
            flex: 1, padding: '10px 0', borderRadius: 100,
            background: 'rgba(232,113,100,0.15)', color: '#e87164',
            fontSize: 13, fontWeight: 500,
          }}>I ate</button>
        </div>
      </div>

      {/* Other notifications, faded */}
      <div style={{ marginTop: 16, padding: '0 6px', opacity: 0.55 }}>
        <div className="h-eyebrow" style={{ color: '#f6f3ee', fontSize: 11 }}>Silent</div>
      </div>
      {[
        { app: 'Calendar', when: '2h', title: 'Meeting in 30 minutes', body: 'Standup · Conference Room A' },
        { app: 'Messages', when: '3h', title: 'Alex', body: 'Are we still on for tonight?' },
      ].map((n, i) => (
        <div key={i} style={{ background: 'rgba(36,34,29,0.7)', borderRadius: 18, padding: 12, marginTop: 8, opacity: 0.7 }}>
          <div style={{ fontSize: 11, opacity: 0.7, marginBottom: 4 }}>{n.app} · {n.when}</div>
          <div style={{ fontSize: 14, fontWeight: 500 }}>{n.title}</div>
          <div style={{ fontSize: 13, opacity: 0.8, marginTop: 2 }}>{n.body}</div>
        </div>
      ))}
    </div>
  );
}

Object.assign(window, {
  HomeScreen, WeightScreen, WaterScreen, ProgressScreen, HistoryScreen, SettingsScreen,
  PlanPickerScreen, DayDetailScreen,
  StagesScreen, OnboardWelcomeScreen, OnboardPlanScreen, OnboardRemindersScreen,
  NotificationPanelScreen,
});
