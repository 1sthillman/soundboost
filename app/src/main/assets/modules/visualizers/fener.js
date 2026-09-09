// fener Visualizer — "Son Işık"

/* =====================================================================
   SON IŞIK — dünyanın ucunda bir kayalık, üzerinde yalnız bir fener.
   Her gece bekçi ışığı döndürür; o ışık sisli denizde kaybolmuş
   gemilere yol gösterir. Bas frekanslar dalgaları kabartır, tizler
   yıldızları ve denizdeki ışıltıları uyandırır; ve zaman zaman,
   süzülen ışık huzmesi bir gemiye değdiğinde, gemi üç kez
   yanıt verir: bir sinyal, bir selam, bir "buradayım".
   ===================================================================== */

const PALETTE = {
  night: {
    skyTop: '#030810', skyMid: '#0b2033', skyBottom: '#17374f',
    waterTop: '#0d2738', waterBottom: '#03090f',
    body: '#e6edf3', bodyGlow: '#b9d1e6', bodyCore: '#fdfbf0',
    silhouette: '#02060b', rock: '#0a141e', tower: '#d8d3c4', towerBand: '#8e2f2f',
    reflect: '#d9b46a', starColor: '#eaf1ff', beam: '#ffe9b0', lamp: '#ffd98a'
  },
  day: {
    skyTop: '#a4cbda', skyMid: '#ecf4ef', skyBottom: '#fce3bc',
    waterTop: '#aad9d3', waterBottom: '#417f88',
    body: '#fff3d0', bodyGlow: '#ffdf9e', bodyCore: '#fffaf0',
    silhouette: '#16262b', rock: '#3d575d', tower: '#f3ead8', towerBand: '#b0552f',
    reflect: '#c98b2e', starColor: '#ffffff', beam: '#fff3cf', lamp: '#ffd27a'
  }
};

const fenerState = {
  stars: [],
  craters: [
    { dx: -0.30, dy: -0.20, r: 0.16 }, { dx: 0.25, dy: 0.15, r: 0.12 },
    { dx: 0.05, dy: 0.32, r: 0.09 }, { dx: -0.15, dy: 0.25, r: 0.07 }
  ],
  shipP: -0.25,
  shipDir: 1,
  shipLights: 0,
  nextSignalIn: 12,
  signalPhase: null,
  beamAngle: 0,
  glints: []
};

window.fenerState = fenerState;

function hexToRgbObj(h) {
  h = h.replace('#', '');
  return { r: parseInt(h.substring(0, 2), 16), g: parseInt(h.substring(2, 4), 16), b: parseInt(h.substring(4, 6), 16) };
}

function lerpRgb(h1, h2, tt) {
  const c1 = hexToRgbObj(h1), c2 = hexToRgbObj(h2);
  return { r: c1.r + (c2.r - c1.r) * tt, g: c1.g + (c2.g - c1.g) * tt, b: c1.b + (c2.b - c1.b) * tt };
}

function palFener(key, a, modeMix) {
  const c = lerpRgb(PALETTE.night[key], PALETTE.day[key], modeMix);
  if (a === undefined) return `rgb(${c.r | 0},${c.g | 0},${c.b | 0})`;
  return `rgba(${c.r | 0},${c.g | 0},${c.b | 0},${Math.max(0, a)})`;
}

function initStars() {
  fenerState.stars = [];
  for (let i = 0; i < 110; i++) {
    fenerState.stars.push({
      x: Math.random(), y: Math.random() * 0.75, r: 0.5 + Math.random() * 1.3,
      phase: Math.random() * Math.PI * 2, speed: 0.3 + Math.random() * 1.1,
      baseA: 0.4 + Math.random() * 0.6
    });
  }
}

initStars();

const WAVE_LAYERS = [
  { ampScale: 0.34, freq: 10, speed: 0.55, key: 'waterBottom', baseFactor: 0.05 },
  { ampScale: 0.20, freq: 16, speed: 0.8, key: 'waterTop', baseFactor: 0.02 }
];

function waveY(p, e, layer, horizonY) {
  const amp = (6 + e.bass * 26) * layer.ampScale * 3.4;
  const baseY = horizonY + (H - horizonY) * (0.05 + layer.baseFactor);
  return baseY + Math.sin(p * layer.freq + t * layer.speed) * amp + Math.sin(p * layer.freq * 2.3 - t * layer.speed * 1.6) * amp * 0.35;
}

function drawWaveLayer(layer, e, horizonY, modeMix) {
  ctx.beginPath();
  ctx.moveTo(0, H + 4);
  const steps = 40;
  for (let i = 0; i <= steps; i++) {
    const p = i / steps;
    ctx.lineTo(p * W, waveY(p, e, layer, horizonY));
  }
  ctx.lineTo(W, H + 4);
  ctx.closePath();
  ctx.fillStyle = palFener(layer.key, 0.92, modeMix);
  ctx.fill();
}

function shipPos(e, horizonY) {
  const layer = WAVE_LAYERS[1];
  const x = fenerState.shipP * W;
  const y = waveY(fenerState.shipP, e, layer, horizonY) - H * 0.004;
  return { x, y };
}

function drawShip(pos, e, modeMix) {
  const s = Math.min(W, H) * 0.0016;
  ctx.save();
  ctx.translate(pos.x, pos.y);
  ctx.scale(s, s);
  const sil = palFener('silhouette', 1, modeMix);
  ctx.fillStyle = sil;
  ctx.beginPath();
  ctx.moveTo(-70, 0);
  ctx.lineTo(70, 0);
  ctx.lineTo(56, -16);
  ctx.lineTo(-56, -16);
  ctx.closePath();
  ctx.fill();
  ctx.fillRect(-18, -30, 36, 14);
  ctx.fillRect(6, -44, 10, 14);
  ctx.beginPath();
  ctx.arc(-8, -34, 4, 0, 7);
  ctx.fill();
  const glow = 0.25 + fenerState.shipLights * 0.75;
  ctx.fillStyle = palFener('lamp', glow, modeMix);
  for (let i = 0; i < 3; i++) {
    ctx.beginPath();
    ctx.arc(-10 + i * 10, -26, 2.4, 0, 7);
    ctx.fill();
  }
  ctx.strokeStyle = sil;
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.moveTo(0, -30);
  ctx.lineTo(0, -58);
  ctx.stroke();
  ctx.fillStyle = palFener('lamp', glow, modeMix);
  ctx.beginPath();
  ctx.arc(0, -58, 2.8, 0, 7);
  ctx.fill();
  ctx.restore();
  ctx.fillStyle = palFener('starColor', 0.05 + 0.04 * Math.sin(t * 0.7), modeMix);
  ctx.beginPath();
  ctx.ellipse(pos.x + 12 + Math.sin(t * 0.4) * 4, pos.y - H * 0.09, 14, 5, -0.25, 0, 7);
  ctx.fill();
}

function lighthouseGeom(horizonY) {
  const baseX = W * 0.76;
  const baseY = horizonY + (H - horizonY) * 0.04;
  const scale = Math.min(W, H) * 0.0011;
  return { baseX, baseY, scale };
}

function drawLighthouse(geom, e, beamAngle, horizonY, modeMix) {
  const { baseX, baseY, scale } = geom;
  const sil = palFener('silhouette', 1, modeMix);
  ctx.save();
  ctx.translate(baseX, baseY);
  ctx.scale(scale, scale);
  ctx.fillStyle = palFener('rock', 1, modeMix);
  ctx.beginPath();
  ctx.moveTo(-150, 40);
  ctx.lineTo(-110, -30);
  ctx.lineTo(-60, -52);
  ctx.lineTo(-10, -38);
  ctx.lineTo(40, -58);
  ctx.lineTo(90, -30);
  ctx.lineTo(140, 10);
  ctx.lineTo(150, 40);
  ctx.closePath();
  ctx.fill();
  ctx.beginPath();
  ctx.moveTo(-150, 40);
  ctx.lineTo(-120, 8);
  ctx.lineTo(-40, 18);
  ctx.lineTo(30, 6);
  ctx.lineTo(110, 16);
  ctx.lineTo(150, 40);
  ctx.closePath();
  ctx.fill();
  const tw = 34, th = 150, tx = 0, ty = -52;
  ctx.beginPath();
  ctx.moveTo(tx - tw, ty);
  ctx.lineTo(tx - tw * 0.62, ty - th);
  ctx.lineTo(tx + tw * 0.62, ty - th);
  ctx.lineTo(tx + tw, ty);
  ctx.closePath();
  ctx.fillStyle = palFener('tower', 1, modeMix);
  ctx.fill();
  ctx.save();
  ctx.clip();
  ctx.fillStyle = palFener('towerBand', 0.92, modeMix);
  for (let i = 0; i < 4; i++) {
    const yy = ty - th * (0.16 + i * 0.24);
    ctx.fillRect(tx - tw * 1.4, yy, tw * 2.8, th * 0.10);
  }
  ctx.restore();
  ctx.fillStyle = palFener('silhouette', 1, modeMix);
  ctx.fillRect(tx - tw * 0.8, ty - th - 8, tw * 1.6, 6);
  ctx.fillStyle = palFener('lamp', 0.9, modeMix);
  ctx.beginPath();
  ctx.arc(tx, ty - th - 20, 11, 0, 7);
  ctx.fill();
  ctx.fillStyle = palFener('silhouette', 1, modeMix);
  ctx.beginPath();
  ctx.moveTo(tx - 14, ty - th - 30);
  ctx.lineTo(tx + 14, ty - th - 30);
  ctx.lineTo(tx, ty - th - 44);
  ctx.closePath();
  ctx.fill();
  ctx.restore();
  return { x: baseX, y: baseY + (ty - th - 20) * scale };
}

function drawBeam(lamp, angle, intensity, modeMix) {
  if (intensity <= 0.01) return;
  const len = W * 1.1;
  const spread = 0.10;
  ctx.save();
  ctx.globalCompositeOperation = 'lighter';
  for (const dir of [1, -1]) {
    const a = angle * dir;
    const str = dir === 1 ? intensity : intensity * 0.22;
    const g = ctx.createLinearGradient(lamp.x, lamp.y, lamp.x + Math.cos(a) * len, lamp.y + Math.sin(a) * len);
    g.addColorStop(0, palFener('beam', 0.30 * str, modeMix));
    g.addColorStop(0.5, palFener('beam', 0.10 * str, modeMix));
    g.addColorStop(1, palFener('beam', 0, modeMix));
    ctx.fillStyle = g;
    ctx.beginPath();
    ctx.moveTo(lamp.x, lamp.y);
    ctx.lineTo(lamp.x + Math.cos(a - spread) * len, lamp.y + Math.sin(a - spread) * len);
    ctx.lineTo(lamp.x + Math.cos(a + spread) * len, lamp.y + Math.sin(a + spread) * len);
    ctx.closePath();
    ctx.fill();
  }
  ctx.restore();
}

function angDiff(a, b) {
  let d = a - b;
  while (d > Math.PI) d -= Math.PI * 2;
  while (d < -Math.PI) d += Math.PI * 2;
  return d;
}

function updateStory(dt, e, horizonY, lamp) {
  if (!playing) {
    fenerState.shipLights += (0.15 - fenerState.shipLights) * Math.min(1, dt * 2);
    return;
  }
  fenerState.shipP += fenerState.shipDir * dt * 0.0085;
  if (fenerState.shipP > 1.25) {
    fenerState.shipP = -0.25;
    fenerState.shipDir = 1;
  }
  if (fenerState.shipP < -0.30) {
    fenerState.shipP = 1.25;
    fenerState.shipDir = -1;
  }
  const ship = shipPos(e, horizonY);
  const beamA = fenerState.beamAngle;
  const toShip = Math.atan2(ship.y - lamp.y, ship.x - lamp.x);
  const d = Math.abs(angDiff(beamA, toShip));
  const lit = Math.max(0, 1 - d / 0.16);
  fenerState.shipLights += (0.15 + lit * 0.85 - fenerState.shipLights) * Math.min(1, dt * 5);
  if (fenerState.signalPhase === null) {
    fenerState.nextSignalIn -= dt;
    if (fenerState.nextSignalIn <= 0 && lit > 0.7) {
      fenerState.signalPhase = { t: 0, flashes: 0 };
    }
  } else {
    fenerState.signalPhase.t += dt;
    const FLASH_GAP = 0.42;
    if (fenerState.signalPhase.flashes < 3 && fenerState.signalPhase.t > fenerState.signalPhase.flashes * FLASH_GAP) {
      fenerState.signalPhase.flashes++;
      burst(ship.x, ship.y - 8, 12, '--a1', 1.5, 30, { gravity: 0, drag: 0.94, r0: 0.7, r1: 1.8 });
    }
    if (fenerState.signalPhase.t > 1.8) {
      fenerState.signalPhase = null;
      fenerState.nextSignalIn = 14 + Math.random() * 18;
    }
  }
}

function updateGlints(e, horizonY, dt) {
  const spawnChance = (0.12 + e.treble * 0.55) * (playing ? 1 : 0.15);
  if (Math.random() < spawnChance) {
    let p = Math.random();
    if (Math.random() < 0.25) p = 0.45 + (Math.random() - 0.5) * 0.12;
    const y = waveY(p, e, WAVE_LAYERS[1], horizonY) - Math.random() * 3;
    fenerState.glints.push({ x: p * W, y, life: 1, size: 0.6 + Math.random() * 1.5, decay: 0.8 + Math.random() * 1.3 });
  }
  fenerState.glints.forEach(g => g.life -= dt * g.decay);
  fenerState.glints = fenerState.glints.filter(g => g.life > 0);
}

function drawGlints(modeMix) {
  fenerState.glints.forEach(g => {
    ctx.beginPath();
    ctx.arc(g.x, g.y, g.size * Math.max(0, g.life), 0, 7);
    ctx.fillStyle = palFener('reflect', Math.max(0, g.life) * 0.85, modeMix);
    ctx.fill();
  });
}

function drawReflection(bodyX, horizonY, e, modeMix) {
  const rows = 26;
  const bandBase = W * 0.1;
  for (let i = 0; i < rows; i++) {
    const p = i / rows;
    const y = horizonY + 6 + p * (H - horizonY) * 0.85;
    const fade = 1 - p * 0.85;
    const jitter = (Math.pow(Math.random(), 2) - 0.3) * (12 + e.treble * 38) * fade;
    const w = (bandBase * (1 - p * 0.55) + jitter) * (0.6 + e.energy * 0.6);
    const cx = bodyX + Math.sin(p * 22 + t * 1.3) * (4 + (1 - p) * 10);
    const h = 1.3 + Math.random() * 2 * fade;
    ctx.fillStyle = palFener('reflect', (0.1 + Math.random() * 0.2) * fade * (0.5 + e.treble * 0.8), modeMix);
    ctx.fillRect(cx - w / 2, y, Math.max(0, w), h);
  }
}


function drawFener(bars, wave, e) {
  const modeMix = phone.dataset.mode === 'light' ? 1 : 0;
  const horizonY = H * 0.60;
  
  // Sky
  const skyGrad = ctx.createLinearGradient(0, 0, 0, horizonY);
  skyGrad.addColorStop(0, palFener('skyTop', undefined, modeMix));
  skyGrad.addColorStop(0.55, palFener('skyMid', undefined, modeMix));
  skyGrad.addColorStop(1, palFener('skyBottom', undefined, modeMix));
  ctx.fillStyle = skyGrad;
  ctx.fillRect(0, 0, W, horizonY + 2);
  
  // Stars
  const starAlpha = Math.max(0, 1 - modeMix * 1.7);
  if (starAlpha > 0.01) {
    fenerState.stars.forEach(s => {
      const tw = 0.5 + 0.5 * Math.sin(t * s.speed + s.phase);
      ctx.beginPath();
      ctx.arc(s.x * W, s.y * horizonY * 0.9, s.r * (0.6 + tw * 0.6), 0, 7);
      ctx.fillStyle = palFener('starColor', starAlpha * (0.35 + tw * 0.65) * s.baseA, modeMix);
      ctx.fill();
    });
  }
  
  // Moon / Sun
  const bodyX = W * 0.20 + Math.sin(t * 0.015) * W * 0.015;
  const bodyY = horizonY * 0.24 + Math.cos(t * 0.011) * 7;
  const bodyR = Math.min(W, horizonY) * 0.10 + e.bass * 6;
  const glow = ctx.createRadialGradient(bodyX, bodyY, bodyR * 0.15, bodyX, bodyY, bodyR * 6);
  glow.addColorStop(0, palFener('bodyGlow', 0.5 + e.energy * 0.22, modeMix));
  glow.addColorStop(0.35, palFener('bodyGlow', 0.13, modeMix));
  glow.addColorStop(1, palFener('bodyGlow', 0, modeMix));
  ctx.fillStyle = glow;
  ctx.beginPath();
  ctx.arc(bodyX, bodyY, bodyR * 6, 0, 7);
  ctx.fill();
  
  // Sun rays (light mode)
  if (modeMix > 0.15) {
    ctx.save();
    ctx.globalAlpha = Math.min(1, (modeMix - 0.15) / 0.85) * 0.22;
    ctx.strokeStyle = palFener('bodyGlow', undefined, modeMix);
    ctx.lineWidth = 1;
    for (let i = 0; i < 10; i++) {
      const ang = (i / 10) * Math.PI * 2 + t * 0.04;
      ctx.beginPath();
      ctx.moveTo(bodyX + Math.cos(ang) * bodyR * 1.3, bodyY + Math.sin(ang) * bodyR * 1.3);
      ctx.lineTo(bodyX + Math.cos(ang) * bodyR * 3.4, bodyY + Math.sin(ang) * bodyR * 3.4);
      ctx.stroke();
    }
    ctx.restore();
  }
  
  // Body
  const bodyGrad = ctx.createRadialGradient(bodyX - bodyR * 0.25, bodyY - bodyR * 0.25, bodyR * 0.1, bodyX, bodyY, bodyR);
  bodyGrad.addColorStop(0, palFener('bodyCore', undefined, modeMix));
  bodyGrad.addColorStop(1, palFener('body', undefined, modeMix));
  ctx.fillStyle = bodyGrad;
  ctx.beginPath();
  ctx.arc(bodyX, bodyY, bodyR, 0, 7);
  ctx.fill();
  
  // Craters (dark mode)
  if (modeMix < 0.7) {
    ctx.globalAlpha = (1 - modeMix) * 0.16;
    ctx.fillStyle = palFener('skyMid', undefined, modeMix);
    fenerState.craters.forEach(c => {
      ctx.beginPath();
      ctx.arc(bodyX + c.dx * bodyR, bodyY + c.dy * bodyR, c.r * bodyR, 0, 7);
      ctx.fill();
    });
    ctx.globalAlpha = 1;
  }
  
  // Fog at horizon
  const fogGrad = ctx.createLinearGradient(0, horizonY - 36, 0, horizonY + 22);
  fogGrad.addColorStop(0, palFener('skyBottom', 0, modeMix));
  fogGrad.addColorStop(0.5, palFener('skyBottom', 0.3, modeMix));
  fogGrad.addColorStop(1, palFener('waterTop', 0, modeMix));
  ctx.fillStyle = fogGrad;
  ctx.fillRect(0, horizonY - 36, W, 58);
  
  // Water
  const waterGrad = ctx.createLinearGradient(0, horizonY, 0, H);
  waterGrad.addColorStop(0, palFener('waterTop', undefined, modeMix));
  waterGrad.addColorStop(1, palFener('waterBottom', undefined, modeMix));
  ctx.fillStyle = waterGrad;
  ctx.fillRect(0, horizonY - 2, W, H - horizonY + 2);
  
  // Reflection
  drawReflection(bodyX, horizonY, e, modeMix);
  
  // Update beam rotation
  fenerState.beamAngle = t * 0.30;
  
  // Lighthouse with beam (behind ship)
  const geom = lighthouseGeom(horizonY);
  const beamPulse = playing ? (0.75 + e.energy * 0.5 + (e.beat ? 0.5 : 0)) : 0.35;
  const lamp = drawLighthouse(geom, e, fenerState.beamAngle, horizonY, modeMix);
  drawBeam(lamp, fenerState.beamAngle, beamPulse * (0.35 + 0.65 * (1 - modeMix * 0.4)), modeMix);
  
  // Update story
  const dt = 0.016;
  updateStory(dt, e, horizonY, lamp);
  updateGlints(e, horizonY, dt);
  
  // Ship
  const ship = shipPos(e, horizonY);
  drawShip(ship, e, modeMix);
  
  // Waves and glints (in front of ship)
  drawWaveLayer(WAVE_LAYERS[0], e, horizonY, modeMix);
  drawGlints(modeMix);
  drawWaveLayer(WAVE_LAYERS[1], e, horizonY, modeMix);
  
  drawSparks(true);
}

// Register visualizer
RENDERERS['fener'] = drawFener;
