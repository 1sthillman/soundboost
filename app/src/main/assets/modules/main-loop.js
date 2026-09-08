/* =====================================================================
   ANALYSIS LAYER — real multi-band FFT energies, waveform, a real
   spectral-flux onset/beat detector, and a sharper "punch" transient
   signal, all pulled from the live AnalyserNode (no timers pretending
   to be music — a spike in real low-frequency energy IS the beat).
   ===================================================================== */
let t = 0;
const BINS = 48;
let fluxHistory = [];
let beatCooldown = 0;
let prevBandFrame = null;
let smoothedEnergy = 0;

function getBars(sensBoost){
  // === PRIORITY 1: Use real audio data from Android ===
  const now = Date.now();
  if(useRealAudioData && realAudioData && (now - lastRealDataTime) < REAL_DATA_TIMEOUT) {
    const realBars = realAudioData.bars || null;
    if(realBars && realBars.length === BINS) {
      // Apply sensitivity boost to real data
      return realBars.map(v => v * sensBoost);
    }
  } else if(useRealAudioData && (now - lastRealDataTime) >= REAL_DATA_TIMEOUT) {
    // Timeout - switch back to mock
    useRealAudioData = false;
    console.log('No real audio data received, switching to mock');
  }
  
  // === PRIORITY 2: Use mock audio engine (fallback) ===
  const bars = new Array(BINS).fill(0.02);
  if(!analyser){
    for(let i=0;i<BINS;i++) bars[i] = 0.035 + 0.018*Math.sin(t*0.6 + i*0.4);
    return bars;
  }
  analyser.getByteFrequencyData(freqData);
  const usable = Math.floor(freqData.length*0.8);
  for(let i=0;i<BINS;i++){
    const p = i/BINS;
    const idx = Math.floor(Math.pow(p, 1.35)*usable);
    bars[i] = Math.max(0.02, (freqData[idx]/255) * (sensBoost||1));
  }
  return bars;
}
function getWave(){
  if(!analyser){
    const arr = new Array(128).fill(128);
    for(let i=0;i<128;i++) arr[i]=128+Math.sin(t*1.3+i*0.2)*10;
    return arr;
  }
  analyser.getByteTimeDomainData(timeData);
  return timeData;
}

function bandAvg(bars, from, to){
  from = Math.max(0, from); to = Math.min(BINS, to);
  let s=0, n=0;
  for(let i=from;i<to;i++){ s+=bars[i]; n++; }
  return n ? s/n : 0;
}

function analyze(bars, sensBoost){
  // === Use real audio analysis from Android if available ===
  const now = Date.now();
  if(useRealAudioData && realAudioData && (now - lastRealDataTime) < REAL_DATA_TIMEOUT) {
    // Real data includes pre-calculated bands and beat detection
    return {
      sub: realAudioData.subBass || 0,
      bass: realAudioData.bass || 0,
      lowMid: realAudioData.lowMid || 0,
      mid: realAudioData.mid || 0,
      highMid: realAudioData.highMid || 0,
      treble: realAudioData.brilliance || 0,
      energy: realAudioData.energy || 0,
      flux: realAudioData.spectralFlux || 0,
      punch: realAudioData.energy * 0.5 || 0,
      beat: realAudioData.isBeat || false,
      brightness: (realAudioData.brilliance || 0) / Math.max(0.05, (realAudioData.bass || 0))
    };
  }
  
  // === Fallback to mock analysis ===
  const sub    = bandAvg(bars, 0, 3);
  const bass   = bandAvg(bars, 1, 7);
  const lowMid = bandAvg(bars, 7, 15);
  const mid    = bandAvg(bars, 15, 26);
  const highMid= bandAvg(bars, 26, 36);
  const treble = bandAvg(bars, 36, 46);

  const frameEnergy = (sub*1.3 + bass) / 2.3;
  smoothedEnergy += (frameEnergy - smoothedEnergy) * 0.12;

  let flux = 0;
  if(prevBandFrame){
    for(let i=0;i<BINS;i++) flux += Math.max(0, bars[i]-prevBandFrame[i]);
  }
  prevBandFrame = bars.slice();
  fluxHistory.push(flux);
  if(fluxHistory.length>30) fluxHistory.shift();
  const avgFlux = fluxHistory.reduce((a,b)=>a+b,0)/fluxHistory.length;

  const sensitivity = 0.5 + ((sensBoost-0.55)/1.1);
  const fluxThresh = avgFlux * (1.55 - sensitivity*0.4) + 0.6;

  let beat = false;
  beatCooldown = Math.max(0, beatCooldown-1);
  if(playing && beatCooldown===0 && flux > fluxThresh && frameEnergy > 0.22){
    beat = true;
    beatCooldown = 7;
  }
  const punch = Math.max(0, Math.min(1, (flux - avgFlux) / 6));
  const brightness = treble / (energyFloor(sub+bass));

  return {sub, bass, lowMid, mid, highMid, treble, energy: smoothedEnergy, flux, punch, beat, brightness};
}
function energyFloor(v){ return Math.max(0.05, v); }

function smoothPath(pts, close){
  if(pts.length<2) return;
  ctx.moveTo(pts[0].x, pts[0].y);
  for(let i=1;i<pts.length-1;i++){
    const mx = (pts[i].x+pts[i+1].x)/2, my=(pts[i].y+pts[i+1].y)/2;
    ctx.quadraticCurveTo(pts[i].x, pts[i].y, mx, my);
  }
  const last = pts[pts.length-1];
  ctx.lineTo(last.x, last.y);
  if(close) ctx.closePath();
}

let sparks = [];
function burst(cx, cy, n, colorVar, speed, life, opts){
  opts = opts||{};
  for(let i=0;i<n;i++){
    const ang = Math.random()*Math.PI*2;
    const sp = (speed||2) * (0.45+Math.random()*0.9);
    sparks.push({
      x:cx,y:cy,vx:Math.cos(ang)*sp,vy:Math.sin(ang)*sp,
      r:(opts.r0||1)+Math.random()*(opts.r1||2.2),
      life:1, decay:1/(life||40), color:colorVar,
      grav:opts.gravity||0, drag:opts.drag!==undefined?opts.drag:0.96
    });
  }
}
function drawSparks(glow){
  sparks.forEach(s=>{
    s.x+=s.vx; s.y+=s.vy; s.vy+=s.grav; s.vx*=s.drag; s.vy*=s.drag; s.life-=s.decay;
    ctx.beginPath(); ctx.arc(s.x,s.y,Math.max(0.2,s.r*s.life),0,7);
    if(glow){ ctx.shadowColor = css(s.color); ctx.shadowBlur = 8; }
    ctx.fillStyle = css(s.color); ctx.globalAlpha = Math.max(0,s.life);
    ctx.fill(); ctx.globalAlpha=1; ctx.shadowBlur=0;
  });
  sparks = sparks.filter(s=>s.life>0);
}

function updateMeter(bars){
  for(let i=0;i<5;i++){
    const v = bars[3+i*8] || 0.03;
    meterBars[i].style.height = (2+v*13).toFixed(1)+'px';
    meterBars[i].style.opacity = (0.4+v*0.6).toFixed(2);
  }
}

/* =====================================================================
   VISUALIZER RENDERERS — six distinct art-directed worlds, each with
   its own material logic, driven by the same live bars / wave / band
   energy data from the real analyser above.
   ===================================================================== */

/* ---------- SUMI-E — ink-wash calligraphy on dark paper ---------- */
let inkSplats = [];
let sumiDust = null;
function drawSumi(bars, wave, e){
  // warm dark "paper" ground with a soft radial vignette
  const g = ctx.createRadialGradient(W*0.5,H*0.4,10,W*0.5,H*0.5,W*0.9);
  g.addColorStop(0, css('--bg1')); g.addColorStop(1, css('--bg0'));
  ctx.fillStyle = g; ctx.fillRect(0,0,W,H);

  // faint fibrous paper texture lines
  ctx.strokeStyle = alpha('--ink', 0.03);
  for(let i=0;i<10;i++){
    ctx.beginPath();
    const y = (i/10)*H + Math.sin(i*3)*4;
    ctx.moveTo(0,y); ctx.bezierCurveTo(W*0.3,y+6,W*0.7,y-6,W,y);
    ctx.lineWidth=1; ctx.stroke();
  }

  // slow drifting dust motes, catching the low light like paper fibres
  if(!sumiDust){ sumiDust = []; for(let i=0;i<14;i++) sumiDust.push({x:Math.random()*W,y:Math.random()*H,r:0.4+Math.random()*0.9,p:Math.random()*7,vy:-(0.03+Math.random()*0.05)}); }
  sumiDust.forEach(d=>{ d.y+=d.vy; if(d.y<-4){d.y=H+4; d.x=Math.random()*W;} });
  sumiDust.forEach(d=>{
    ctx.beginPath(); ctx.arc(d.x,d.y,d.r,0,7);
    ctx.fillStyle = css('--ink'); ctx.globalAlpha = 0.08+0.1*Math.abs(Math.sin(t*0.8+d.p));
    ctx.fill(); ctx.globalAlpha=1;
  });

  const cx = W*0.5, cy = H*0.46, r = Math.min(W,H)*0.3;

  // Ensō — the single hand-drawn circle, brush width breathing with bass,
  // gap traditionally left unclosed. Drawn as many jittered short segments,
  // twice — a thin dry-brush ghost pass then the main wet pass — so it
  // reads as bristle and ink pooling, not a vector circle.
  const gapStart = 4.4, gapEnd = 5.0; // radians left open
  const segs = 90;
  ctx.lineCap = 'round';
  [ {off:2.4, wMul:0.5, aMul:0.35}, {off:0, wMul:1, aMul:1} ].forEach(pass=>{
    for(let i=0;i<segs;i++){
      const a0 = (i/segs)*Math.PI*2;
      const a1 = ((i+1)/segs)*Math.PI*2;
      if(a0>gapStart && a0<gapEnd) continue;
      const bandIdx = Math.floor((i/segs)*BINS);
      const wobble = (Math.sin(i*1.7+t*0.6)+Math.sin(i*0.5-t*0.3))*1.6;
      const rr = r + wobble + e.bass*10 + pass.off;
      const x0 = cx+Math.cos(a0)*rr, y0 = cy+Math.sin(a0)*rr*0.94;
      const x1 = cx+Math.cos(a1)*rr, y1 = cy+Math.sin(a1)*rr*0.94;
      const w = (2.2 + bars[bandIdx]*7 + (i%7===0? e.treble*3:0)) * pass.wMul;
      ctx.beginPath(); ctx.moveTo(x0,y0); ctx.lineTo(x1,y1);
      ctx.strokeStyle = css('--ink'); ctx.globalAlpha = (0.68 + bars[bandIdx]*0.3) * pass.aMul;
      ctx.lineWidth = w; ctx.stroke(); ctx.globalAlpha = 1;
    }
  });

  // ink bleed halo around the stroke, breathing with sub-bass
  ctx.save();
  ctx.globalCompositeOperation = 'lighter';
  const bleed = ctx.createRadialGradient(cx,cy,r*0.8,cx,cy,r*1.35+e.sub*40);
  bleed.addColorStop(0, alpha('--ink', 0));
  bleed.addColorStop(0.85, alpha('--ink', 0.03+e.sub*0.05));
  bleed.addColorStop(1, alpha('--ink', 0));
  ctx.fillStyle = bleed; ctx.beginPath(); ctx.arc(cx,cy,r*1.35+e.sub*40,0,7); ctx.fill();
  ctx.restore();

  // brush-stroke frequency marks beneath, tapered like calligraphy
  const n = 22;
  for(let i=0;i<n;i++){
    const idx = Math.floor((i/n)*BINS);
    const v = bars[idx];
    const x = W*0.12 + (i/n)*W*0.76;
    const baseY = H*0.86;
    const len = 6 + v*70;
    ctx.beginPath();
    ctx.moveTo(x, baseY);
    ctx.quadraticCurveTo(x+ (i%2?2:-2), baseY-len*0.6, x, baseY-len);
    ctx.lineWidth = 1.5 + v*4;
    ctx.strokeStyle = css('--ink'); ctx.globalAlpha = 0.28+v*0.5;
    ctx.stroke(); ctx.globalAlpha=1;
  }

  // vermillion seal, bottom right — pulses and "stamps" harder on beat
  const sealX = W*0.83, sealY = H*0.86, sealS = 15+ (e.beat?4:0);
  ctx.save();
  ctx.translate(sealX,sealY); ctx.rotate(-0.06);
  ctx.fillStyle = css('--a1'); ctx.globalAlpha = 0.88;
  ctx.fillRect(-sealS/2,-sealS/2,sealS,sealS);
  ctx.globalAlpha=1;
  ctx.fillStyle = css('--bg0');
  ctx.fillRect(-sealS*0.28,-sealS*0.28,sealS*0.56,sealS*0.56);
  ctx.restore();

  // ink splatter on beat — irregular flicked dots, not a symmetric burst
  if(e.beat){
    for(let i=0;i<9;i++){
      const ang = Math.random()*Math.PI*2;
      const dist = 14+Math.random()*40;
      inkSplats.push({x:cx+Math.cos(ang)*dist, y:cy+Math.sin(ang)*dist*0.9, r:0.6+Math.random()*2.6, life:1});
    }
  }
  inkSplats.forEach(s=>{ s.life -= 0.018; });
  inkSplats = inkSplats.filter(s=>s.life>0);
  inkSplats.forEach(s=>{
    ctx.beginPath(); ctx.arc(s.x,s.y,s.r,0,7);
    ctx.fillStyle = css('--ink'); ctx.globalAlpha = Math.max(0,s.life*0.6);
    ctx.fill(); ctx.globalAlpha=1;
  });
}

/* ---------- KUTUP ŞAFAĞI (Aurora) — silk ribbons of polar light ---------- */
let auroraStars = null;
let auroraOrbPulse = 0;
function drawAurora(bars, wave, e){
  const g = ctx.createLinearGradient(0,0,0,H);
  g.addColorStop(0, css('--bg1')); g.addColorStop(1, css('--bg0'));
  ctx.fillStyle = g; ctx.fillRect(0,0,W,H);

  if(!auroraStars){
    auroraStars = [];
    for(let i=0;i<60;i++) auroraStars.push({x:Math.random()*W, y:Math.random()*H*0.6, r:Math.random()*1.2, p:Math.random()*7});
  }
  auroraStars.forEach(s=>{
    ctx.beginPath(); ctx.arc(s.x,s.y,s.r,0,7);
    ctx.fillStyle = css('--ink'); ctx.globalAlpha = (0.22+0.4*Math.abs(Math.sin(t*1.1+s.p))) * (0.6+e.treble*1.1);
    ctx.fill(); ctx.globalAlpha=1;
  });

  // a faint moon, breathing softly with sub-bass — the scene's still point
  auroraOrbPulse += (e.sub - auroraOrbPulse) * 0.08;
  const moonX = W*0.16, moonY = H*0.2, moonR = 10 + auroraOrbPulse*6;
  ctx.save();
  ctx.globalCompositeOperation='lighter';
  const moonGlow = ctx.createRadialGradient(moonX,moonY,1,moonX,moonY,moonR*3.4);
  moonGlow.addColorStop(0, alpha('--ink',0.5));
  moonGlow.addColorStop(1, alpha('--ink',0));
  ctx.fillStyle=moonGlow; ctx.beginPath(); ctx.arc(moonX,moonY,moonR*3.4,0,7); ctx.fill();
  ctx.restore();
  ctx.beginPath(); ctx.arc(moonX,moonY,moonR,0,7);
  ctx.fillStyle = css('--ink'); ctx.globalAlpha=0.85; ctx.fill(); ctx.globalAlpha=1;

  // three silk ribbons, each a smoothed band built from two offset curves
  const ribbons = [
    {base:H*0.32, amp:34, colorA:'--a1', colorB:'--a2', speed:0.55, band:[15,26]},
    {base:H*0.46, amp:26, colorA:'--a2', colorB:'--a1', speed:0.4, band:[7,15]},
    {base:H*0.58, amp:20, colorA:'--a1', colorB:'--a2', speed:0.7, band:[26,36]},
  ];
  ctx.save();
  ctx.globalCompositeOperation = 'lighter';
  ribbons.forEach((rb, ri)=>{
    const top=[], bot=[];
    for(let i=0;i<=BINS;i++){
      const idx = Math.min(i, BINS-1);
      const bandIdx = rb.band[0] + (idx % (rb.band[1]-rb.band[0]));
      const v = bars[bandIdx];
      const x = (i/BINS)*W;
      const wave1 = Math.sin(i*0.32 + t*rb.speed + ri*2)*rb.amp;
      const wave2 = Math.sin(i*0.18 - t*rb.speed*0.6 + ri)*rb.amp*0.5;
      const y = rb.base + wave1 + wave2 - v*40;
      const thickness = 10 + v*46;
      top.push({x, y: y - thickness/2});
      bot.push({x, y: y + thickness/2});
    }
    ctx.beginPath();
    smoothPath(top);
    for(let i=bot.length-1;i>=0;i--){ ctx.lineTo(bot[i].x,bot[i].y); }
    ctx.closePath();
    const grad = ctx.createLinearGradient(0, rb.base-60, 0, rb.base+60);
    grad.addColorStop(0, alpha(rb.colorA, 0.35));
    grad.addColorStop(0.5, alpha(rb.colorB, 0.22));
    grad.addColorStop(1, alpha(rb.colorA, 0));
    ctx.fillStyle = grad;
    ctx.shadowColor = css(rb.colorA); ctx.shadowBlur = 16;
    ctx.fill();
    ctx.shadowBlur = 0;
  });
  ctx.restore();

  // shimmering crest highlight, brighter with treble
  const crest = [];
  for(let i=0;i<=BINS;i++){
    const x=(i/BINS)*W;
    const y = H*0.32 + Math.sin(i*0.32+t*0.55)*34 - bars[i]*40 - 8;
    crest.push({x,y});
  }
  ctx.beginPath(); smoothPath(crest);
  ctx.strokeStyle = css('--ink'); ctx.globalAlpha = 0.25+e.treble*0.45; ctx.lineWidth=1.2; ctx.stroke(); ctx.globalAlpha=1;

  // dark hill silhouette grounding the scene, swaying gently with bass
  const hill=[];
  for(let i=0;i<=BINS;i++){
    const x=(i/BINS)*W;
    const y = H*0.86 + Math.sin(i*0.4+t*0.2)*6 - e.bass*10;
    hill.push({x,y});
  }
  ctx.beginPath(); smoothPath(hill); ctx.lineTo(W,H); ctx.lineTo(0,H); ctx.closePath();
  ctx.fillStyle = css('--bg0'); ctx.fill();

  if(e.beat) burst(W*(0.2+Math.random()*0.6), H*0.35, 6, '--a2', 1.4, 70, {gravity:-0.01, drag:0.95});
  drawSparks(true);
}

/* ---------- NOVA — a stellar core waking with every bass hit ---------- */
let novaStars = null;
let novaParticles = null;
let novaShock = [];
function drawNova(bars, wave, e){
  const g = ctx.createRadialGradient(W*0.5,H*0.44,10,W*0.5,H*0.5,W*1.0);
  g.addColorStop(0, css('--bg1')); g.addColorStop(1, css('--bg0'));
  ctx.fillStyle = g; ctx.fillRect(0,0,W,H);

  // deep starfield, twinkling harder with treble
  if(!novaStars){
    novaStars = [];
    for(let i=0;i<70;i++) novaStars.push({x:Math.random()*W, y:Math.random()*H, r:Math.random()*1.1, p:Math.random()*7});
  }
  novaStars.forEach(s=>{
    ctx.beginPath(); ctx.arc(s.x,s.y,s.r,0,7);
    ctx.fillStyle = css('--ink'); ctx.globalAlpha = (0.16+0.5*Math.abs(Math.sin(t*1.4+s.p))) * (0.5+e.treble*1.3);
    ctx.fill(); ctx.globalAlpha = 1;
  });

  const cx = W*0.5, cy = H*0.46;
  const coreR = 18 + e.sub*44 + e.bass*20;

  // layered corona — the core's breath, driven directly by bass energy
  ctx.save();
  ctx.globalCompositeOperation = 'lighter';
  for(let i=3;i>=0;i--){
    const rr = coreR*(1.35+i*0.5) + e.bass*i*12;
    const grad = ctx.createRadialGradient(cx,cy,coreR*0.2,cx,cy,rr);
    grad.addColorStop(0, alpha('--a1', 0.2-i*0.04));
    grad.addColorStop(1, alpha('--a1', 0));
    ctx.fillStyle = grad; ctx.beginPath(); ctx.arc(cx,cy,rr,0,7); ctx.fill();
  }
  ctx.restore();

  // plasma filaments, one per frequency band, rotating slowly and flaring with amplitude
  const filCount = 16;
  ctx.save();
  ctx.globalCompositeOperation = 'lighter';
  ctx.lineCap = 'round';
  for(let i=0;i<filCount;i++){
    const bandIdx = Math.floor((i/filCount)*BINS);
    const v = bars[bandIdx];
    const baseAng = (i/filCount)*Math.PI*2 + t*0.1*(i%2===0?1:-1);
    const len = coreR*1.05 + v*95 + e.mid*26;
    const wob = Math.sin(t*1.5+i)*0.2;
    const x1 = cx+Math.cos(baseAng)*coreR*0.85;
    const y1 = cy+Math.sin(baseAng)*coreR*0.85;
    const xm = cx+Math.cos(baseAng+wob)*len*0.55;
    const ym = cy+Math.sin(baseAng+wob)*len*0.55;
    const x2 = cx+Math.cos(baseAng+wob*1.7)*len;
    const y2 = cy+Math.sin(baseAng+wob*1.7)*len;
    ctx.beginPath(); ctx.moveTo(x1,y1); ctx.quadraticCurveTo(xm,ym,x2,y2);
    ctx.strokeStyle = i%3===0 ? css('--a2') : css('--a1');
    ctx.lineWidth = 1+v*4.2;
    ctx.globalAlpha = 0.32+v*0.6;
    ctx.shadowColor = css('--a1'); ctx.shadowBlur = 5+v*11;
    ctx.stroke();
  }
  ctx.restore(); ctx.globalAlpha=1; ctx.shadowBlur=0;

  // embers orbiting the core, drifting wider as treble rises
  if(!novaParticles){
    novaParticles = [];
    for(let i=0;i<22;i++) novaParticles.push({a:Math.random()*6.28, rBase:coreR*(1.5+Math.random()*1.6), speed:(0.15+Math.random()*0.3)*(Math.random()<0.5?1:-1), r:0.8+Math.random()*1.6});
  }
  novaParticles.forEach(p=>{
    p.a += p.speed*0.02;
    const rr = p.rBase + e.treble*32;
    const x = cx+Math.cos(p.a)*rr, y = cy+Math.sin(p.a)*rr*0.82;
    ctx.beginPath(); ctx.arc(x,y,p.r,0,7);
    ctx.fillStyle = css('--a2'); ctx.shadowColor = css('--a2'); ctx.shadowBlur = 6;
    ctx.globalAlpha = 0.55+e.treble*0.45; ctx.fill(); ctx.globalAlpha=1; ctx.shadowBlur=0;
  });

  // the core itself — a small bright body of fused light
  ctx.save();
  ctx.globalCompositeOperation = 'lighter';
  const coreGrad = ctx.createRadialGradient(cx-coreR*0.25,cy-coreR*0.25,1,cx,cy,coreR);
  coreGrad.addColorStop(0, css('--ink'));
  coreGrad.addColorStop(0.4, css('--a2'));
  coreGrad.addColorStop(1, css('--a1'));
  ctx.fillStyle = coreGrad; ctx.beginPath(); ctx.arc(cx,cy,coreR,0,7); ctx.fill();
  ctx.restore();

  // a shockwave ring rings out on every real beat, expanding and fading
  if(e.beat){ novaShock.push({r:coreR, life:1}); shakeStage(2+e.punch*4); }
  novaShock.forEach(s=>{ s.r += 5+e.punch*3; s.life -= 0.028; });
  novaShock = novaShock.filter(s=>s.life>0);
  novaShock.forEach(s=>{
    ctx.beginPath(); ctx.arc(cx,cy,s.r,0,7);
    ctx.strokeStyle = css('--a1'); ctx.globalAlpha = Math.max(0,s.life*0.6); ctx.lineWidth = 2; ctx.stroke(); ctx.globalAlpha=1;
  });

  if(e.beat) burst(cx,cy,10,'--a2',2.4,45,{gravity:0,drag:0.94,r0:1,r1:2.2});
  drawSparks(true);

  // a quiet spectral horizon along the bottom edge
  const n = 28;
  for(let i=0;i<n;i++){
    const idx = Math.floor((i/n)*BINS);
    const v = bars[idx];
    const x = W*0.1+(i/n)*W*0.8;
    const baseY = H*0.93;
    const h = 4+v*34;
    ctx.beginPath(); ctx.moveTo(x,baseY); ctx.lineTo(x,baseY-h);
    ctx.strokeStyle = css('--a1'); ctx.globalAlpha = 0.16+v*0.3; ctx.lineWidth = 2; ctx.stroke(); ctx.globalAlpha=1;
  }
}

/* ---------- MİSELYUM (Mycel) — a bioluminescent network signalling in the dark ---------- */
let mycelBranches = null;
let mycelGrowStart = 0;
let mycelPulses = [];
let mycelSpores = [];
function genMycelBranches(){
  mycelBranches = [];
  const rootCount = 6;
  for(let i=0;i<rootCount;i++){
    let x = (i+0.5)/rootCount*W + (Math.random()-0.5)*26;
    let y = H+8;
    let ang = -Math.PI/2 + (Math.random()-0.5)*0.5;
    const pts = [{x,y}];
    const segs = 16+Math.floor(Math.random()*6);
    for(let s=0;s<segs;s++){
      ang += (Math.random()-0.5)*0.42;
      const len = 10+Math.random()*8;
      x += Math.cos(ang)*len; y += Math.sin(ang)*len;
      pts.push({x,y});
      if(Math.random()<0.22 && s>4 && s<segs-3){
        let bx=x, by=y, bang = ang + (Math.random()<0.5?1:-1)*(0.5+Math.random()*0.5);
        const bpts = [{x:bx,y:by}];
        const bsegs = 5+Math.floor(Math.random()*5);
        for(let k=0;k<bsegs;k++){
          bang += (Math.random()-0.5)*0.3;
          bx += Math.cos(bang)*9; by += Math.sin(bang)*9;
          bpts.push({x:bx,y:by});
        }
        mycelBranches.push({pts:bpts, bin:Math.floor(Math.random()*BINS), child:true});
      }
    }
    mycelBranches.push({pts, bin:Math.floor((i/rootCount)*BINS), child:false});
  }
  mycelGrowStart = t;
}
function drawMycel(bars, wave, e){
  const g = ctx.createRadialGradient(W*0.5,H*0.7,10,W*0.5,H*0.5,W*0.95);
  g.addColorStop(0, css('--bg1')); g.addColorStop(1, css('--bg0'));
  ctx.fillStyle = g; ctx.fillRect(0,0,W,H);

  if(!mycelBranches) genMycelBranches();

  // ambient floor haze, thickening with sub-bass
  const haze = ctx.createLinearGradient(0,H*0.68,0,H);
  haze.addColorStop(0, alpha('--a1',0));
  haze.addColorStop(1, alpha('--a1', 0.05+e.sub*0.07));
  ctx.fillStyle = haze; ctx.fillRect(0,H*0.68,W,H*0.32);

  const growProgress = Math.min(1, (t-mycelGrowStart)/2.4);

  ctx.lineCap = 'round';
  mycelBranches.forEach(br=>{
    const v = bars[br.bin]||0;
    const nShow = Math.max(2, Math.floor(br.pts.length*growProgress));
    const pts = br.pts.slice(0, nShow);
    if(pts.length<2) return;
    ctx.beginPath(); smoothPath(pts);
    ctx.strokeStyle = css('--a1');
    ctx.globalAlpha = (br.child?0.32:0.55) + v*0.35;
    ctx.lineWidth = (br.child?1:1.6) + v*2.4;
    ctx.shadowColor = css('--a1'); ctx.shadowBlur = 4+v*11;
    ctx.stroke(); ctx.shadowBlur=0; ctx.globalAlpha=1;

    if(growProgress>=1){
      const tip = pts[pts.length-1];
      const tipR = 1.4+v*3.4;
      ctx.beginPath(); ctx.arc(tip.x,tip.y,tipR,0,7);
      ctx.fillStyle = css('--a2'); ctx.shadowColor = css('--a2'); ctx.shadowBlur = 8;
      ctx.globalAlpha = 0.5+v*0.5; ctx.fill(); ctx.globalAlpha=1; ctx.shadowBlur=0;
    }
  });

  // traveling light signals, spawned along random branches on every beat
  if(e.beat && growProgress>=1){
    for(let k=0;k<2;k++){
      mycelPulses.push({bi: Math.floor(Math.random()*mycelBranches.length), progress:0, speed:0.02+Math.random()*0.015});
    }
  }
  mycelPulses.forEach(p=>{ p.progress += p.speed; });
  mycelPulses = mycelPulses.filter(p=>p.progress<1);
  mycelPulses.forEach(p=>{
    const br = mycelBranches[p.bi]; if(!br) return;
    const idx = Math.min(br.pts.length-1, Math.floor(p.progress*br.pts.length));
    const pos = br.pts[idx]; if(!pos) return;
    ctx.beginPath(); ctx.arc(pos.x,pos.y,2.4,0,7);
    ctx.fillStyle = css('--ink'); ctx.shadowColor = css('--a2'); ctx.shadowBlur = 10;
    ctx.globalAlpha = 0.9; ctx.fill(); ctx.globalAlpha=1; ctx.shadowBlur=0;
    if(p.progress>0.94){
      const tip = br.pts[br.pts.length-1];
      burst(tip.x, tip.y, 5, '--a2', 0.9, 40, {gravity:-0.02, drag:0.95, r0:0.6, r1:1.4});
    }
  });

  // drifting spores, spawn rate tied to treble
  if(Math.random() < 0.08+e.treble*0.35){
    mycelSpores.push({x:W*0.1+Math.random()*W*0.8, y:H*0.85+Math.random()*H*0.1, vy:-(0.15+Math.random()*0.3), vx:(Math.random()-0.5)*0.15, r:0.6+Math.random()*1.2, life:1, p:Math.random()*7});
  }
  mycelSpores.forEach(s=>{ s.y+=s.vy; s.x+=s.vx+Math.sin(t+s.p)*0.05; s.life -= 0.006; });
  mycelSpores = mycelSpores.filter(s=>s.life>0);
  mycelSpores.forEach(s=>{
    ctx.beginPath(); ctx.arc(s.x,s.y,s.r,0,7);
    ctx.fillStyle = css('--a2'); ctx.shadowColor = css('--a2'); ctx.shadowBlur = 5;
    ctx.globalAlpha = s.life*0.7; ctx.fill(); ctx.globalAlpha=1; ctx.shadowBlur=0;
  });

  drawSparks(true);

  // a slow breathing glow across the whole floor, driven by sub-bass
  ctx.save();
  ctx.globalCompositeOperation = 'lighter';
  const glow = ctx.createRadialGradient(W/2,H*0.95,10,W/2,H*0.95,W*0.9);
  glow.addColorStop(0, alpha('--a1', 0.05+e.sub*0.12));
  glow.addColorStop(1, alpha('--a1',0));
  ctx.fillStyle = glow; ctx.beginPath(); ctx.arc(W/2,H*0.95,W*0.9,0,7); ctx.fill();
  ctx.restore();

  if(e.beat) shakeStage(1+e.punch*1.5);
}

/* ---------- DERİN IŞILTI (Reef) — bioluminescent deep-sea jellyfish ---------- */
let reefParticles = null;
let reefTentaclePhase = 0;
let reefCompanionPhase = 1.7;
function drawJelly(cx, cy, scale, hue, bars, e, contraction){
  const bellW = (66 - contraction*10 + e.bass*10) * scale;
  const bellH = (40 + contraction*8) * scale;
  ctx.save();
  ctx.globalCompositeOperation = 'lighter';
  const bellGlow = ctx.createRadialGradient(cx,cy,4,cx,cy,bellW*1.6);
  bellGlow.addColorStop(0, alpha(hue,0.35));
  bellGlow.addColorStop(1, alpha(hue,0));
  ctx.fillStyle=bellGlow; ctx.beginPath(); ctx.arc(cx,cy,bellW*1.6,0,7); ctx.fill();
  ctx.restore();

  ctx.beginPath();
  ctx.ellipse(cx,cy,bellW,bellH,0,Math.PI,0);
  ctx.fillStyle = alpha(hue, 0.5);
  ctx.fill();
  for(let i=0;i<4;i++){
    ctx.beginPath();
    ctx.ellipse(cx,cy-i*3*scale,bellW-i*10*scale,bellH-i*4*scale,0,Math.PI,0);
    ctx.strokeStyle = alpha('--ink',0.25); ctx.lineWidth=1; ctx.stroke();
  }

  for(let i=0;i<8;i++){
    const idx = 4+i*4;
    const v = bars[idx%BINS];
    const startX = cx + (i-3.5)*(bellW*1.7/8);
    const pts=[];
    const segs=10;
    for(let s=0;s<=segs;s++){
      const frac = s/segs;
      const sway = Math.sin(reefTentaclePhase*1.4 + i*0.7 + frac*4) * (6+v*22) * frac * scale;
      pts.push({x:startX+sway, y:cy+bellH*0.3 + frac*(90+v*40)*scale});
    }
    ctx.beginPath(); smoothPath(pts);
    ctx.strokeStyle = i%2===0 ? css(hue) : css('--a2');
    ctx.globalAlpha = (0.45+v*0.4) * scale;
    ctx.lineWidth = 1.4;
    ctx.shadowColor = css(i%2===0?hue:'--a2'); ctx.shadowBlur = 4+v*8;
    ctx.stroke(); ctx.shadowBlur=0; ctx.globalAlpha=1;
  }
}
function drawReef(bars, wave, e){
  const g = ctx.createRadialGradient(W*0.5,H*0.35,10,W*0.5,H*0.5,W*0.95);
  g.addColorStop(0, css('--bg1')); g.addColorStop(1, css('--bg0'));
  ctx.fillStyle = g; ctx.fillRect(0,0,W,H);

  // faint downward light shafts
  for(let i=0;i<3;i++){
    const bx = W*(0.2+i*0.3)+Math.sin(t*0.3+i)*10;
    const grd = ctx.createLinearGradient(bx,0,bx,H);
    grd.addColorStop(0, alpha('--a1',0.08));
    grd.addColorStop(1, alpha('--a1',0));
    ctx.fillStyle=grd; ctx.fillRect(bx-24,0,48,H);
  }

  // near/far parallax caustic particles for a sense of depth
  if(!reefParticles){
    reefParticles = [];
    for(let i=0;i<50;i++){
      const far = Math.random()<0.5;
      reefParticles.push({x:Math.random()*W,y:Math.random()*H,r:(far?0.4:0.9)+Math.random()*1.2,p:Math.random()*7,vy:-(0.06+Math.random()*(far?0.1:0.22)),far});
    }
  }
  reefParticles.forEach(p=>{
    p.y += p.vy; p.x += Math.sin(t*0.4+p.p)*(p.far?0.08:0.18);
    if(p.y<-5){ p.y=H+5; p.x=Math.random()*W; }
  });
  reefParticles.forEach(p=>{
    const glow = (0.2+0.35*Math.abs(Math.sin(t*2+p.p))+e.treble*0.3) * (p.far?0.5:1);
    ctx.beginPath(); ctx.arc(p.x,p.y,p.r+e.treble*(p.far?0.5:1.2),0,7);
    ctx.fillStyle = css('--a1'); ctx.shadowColor=css('--a1'); ctx.shadowBlur = p.far?3:6; ctx.globalAlpha=glow;
    ctx.fill(); ctx.globalAlpha=1; ctx.shadowBlur=0;
  });

  reefTentaclePhase += 0.05;
  reefCompanionPhase += 0.032;

  // a smaller companion jellyfish drifting quietly in the depth
  const compX = W*(0.78 + Math.sin(reefCompanionPhase*0.4)*0.06);
  const compY = H*(0.68 + Math.sin(reefCompanionPhase*0.6)*0.04);
  drawJelly(compX, compY, 0.52, '--a2', bars, e, 0);

  // the lead jellyfish: bell contracts on beat, tentacles ripple with the mix
  const contraction = e.beat ? 1 : 0;
  drawJelly(W*0.5, H*0.36, 1, '--a1', bars, e, contraction);

  if(e.beat) burst(W*0.5, H*0.36, 10, '--a2', 1.6, 55, {gravity:-0.015, drag:0.94});
  drawSparks(true);
}

/* ---------- MUSON (Monsoon) — layered storm clouds, lightning, rain ---------- */
let stormBolts = [];
let rainDrops = null;
let cloudSeed = null;
let fogSeed = null;
let splashes = [];
function drawMonsoon(bars, wave, e){
  const g = ctx.createLinearGradient(0,0,0,H);
  g.addColorStop(0, css('--bg1')); g.addColorStop(1, css('--bg0'));
  ctx.fillStyle = g; ctx.fillRect(0,0,W,H);

  if(!cloudSeed){ cloudSeed = Array.from({length:BINS},()=>Math.random()*6.28); }
  if(!fogSeed){ fogSeed = Array.from({length:3},()=>Math.random()*6.28); }

  // drifting fog bands low on the horizon, thickening with overall energy
  fogSeed.forEach((seed,i)=>{
    const y = H*(0.66+i*0.08);
    const grd = ctx.createLinearGradient(0,y-16,0,y+16);
    grd.addColorStop(0, alpha('--ink',0));
    grd.addColorStop(0.5, alpha('--ink', 0.03+e.energy*0.05));
    grd.addColorStop(1, alpha('--ink',0));
    ctx.fillStyle=grd;
    ctx.fillRect(0,y-16,W,32);
  });

  // two parallax cloud bands, built from live bars as lumpy silhouettes
  [{y:H*0.34, amp:1, color:'--a3', op:0.9, speed:0.15},
   {y:H*0.5, amp:0.7, color:'--bg1', op:0.95, speed:0.08}].forEach((band)=>{
    const pts=[];
    for(let i=0;i<=BINS;i++){
      const idx=Math.min(i,BINS-1);
      const x=(i/BINS)*W;
      const bump = Math.sin(cloudSeed[idx]+t*band.speed+i*0.5)*10;
      const y = band.y - bars[idx]*40*band.amp - bump;
      pts.push({x,y});
    }
    ctx.beginPath(); smoothPath(pts); ctx.lineTo(W,0); ctx.lineTo(0,0); ctx.closePath();
    ctx.fillStyle = css(band.color); ctx.globalAlpha=band.op; ctx.fill(); ctx.globalAlpha=1;
  });

  // rain, density tied to overall energy, with faint ground splashes
  if(!rainDrops){ rainDrops=[]; for(let i=0;i<70;i++) rainDrops.push({x:Math.random()*W,y:Math.random()*H,vy:4+Math.random()*3,len:8+Math.random()*10}); }
  const groundY = H*0.9;
  const activeRain = Math.floor(20 + e.energy*160);
  ctx.strokeStyle = alpha('--ink', 0.22);
  ctx.lineWidth=1;
  for(let i=0;i<Math.min(rainDrops.length, activeRain);i++){
    const d = rainDrops[i];
    const wasBelow = d.y > groundY;
    d.y += d.vy; d.x -= 1.2;
    if(!wasBelow && d.y>groundY && Math.random()<0.5){
      splashes.push({x:d.x, y:groundY, life:1});
    }
    if(d.y>H){ d.y=-10; d.x=Math.random()*W; }
    ctx.beginPath(); ctx.moveTo(d.x,d.y); ctx.lineTo(d.x-3,d.y+d.len); ctx.stroke();
  }
  splashes.forEach(s=>{ s.life -= 0.09; });
  splashes = splashes.filter(s=>s.life>0);
  splashes.forEach(s=>{
    ctx.beginPath(); ctx.ellipse(s.x, s.y, (1-s.life)*5, (1-s.life)*1.6, 0, 0, 7);
    ctx.strokeStyle = alpha('--ink', s.life*0.5); ctx.lineWidth=1; ctx.stroke();
  });

  // lightning: triggered by strong punch/beat, a jagged main bolt with branches
  if((e.beat && e.bass>0.4) || e.punch>0.75){
    const startX = W*(0.2+Math.random()*0.6);
    const pts=[{x:startX,y:0}];
    let cx=startX, cy=0;
    while(cy < H*0.62){
      cx += (Math.random()-0.5)*40;
      cy += 18+Math.random()*22;
      pts.push({x:cx,y:cy});
    }
    stormBolts.push({pts, life:1, branch: Math.random()<0.7 ? Math.floor(pts.length*0.4) : -1});
    shakeStage(5 + e.punch*4);
  }
  stormBolts.forEach(b=>{ b.life -= 0.09; });
  stormBolts = stormBolts.filter(b=>b.life>0);
  stormBolts.forEach(b=>{
    ctx.save();
    ctx.strokeStyle = css('--a1');
    ctx.globalAlpha = Math.max(0,b.life);
    ctx.lineWidth = 2;
    ctx.shadowColor = css('--a1'); ctx.shadowBlur = 14;
    ctx.beginPath();
    b.pts.forEach((p,i)=> i===0?ctx.moveTo(p.x,p.y):ctx.lineTo(p.x,p.y));
    ctx.stroke();
    if(b.branch>0 && b.branch<b.pts.length){
      const bp = b.pts[b.branch];
      ctx.beginPath(); ctx.moveTo(bp.x,bp.y);
      ctx.lineTo(bp.x+40+Math.random()*20, bp.y+50);
      ctx.strokeStyle = css('--a2'); ctx.lineWidth=1.3; ctx.stroke();
    }
    ctx.restore(); ctx.globalAlpha=1; ctx.shadowBlur=0;
  });

  // flash-lit sky overlay right when a bolt spawns
  const freshBolt = stormBolts.find(b=>b.life>0.85);
  if(freshBolt){
    ctx.fillStyle = css('--a1'); ctx.globalAlpha = (freshBolt.life-0.85)*1.3;
    ctx.fillRect(0,0,W,H); ctx.globalAlpha=1;
  }

  // distant horizon glow that breathes with sub-bass
  ctx.save();
  ctx.globalCompositeOperation='lighter';
  const glow = ctx.createRadialGradient(W/2,H*0.7,10,W/2,H*0.7,W*0.7);
  glow.addColorStop(0, alpha('--a2', 0.08+e.sub*0.1));
  glow.addColorStop(1, alpha('--a2',0));
  ctx.fillStyle=glow; ctx.beginPath(); ctx.arc(W/2,H*0.7,W*0.7,0,7); ctx.fill();
  ctx.restore();
}

/* placeholders kept so any external reference to legacy theme state stays inert */
let glassBlobs = null;
let glassSparkles = [];

/* ---------- MÜREKKEP — sumi ink bloomed in water, kintsugi gold veins ---------- */
let murekkepBlob = null;
let murekkepDrops = [];
function initMurekkepBlob(){
  murekkepBlob = [];
  const n=28;
  for(let i=0;i<n;i++){
    murekkepBlob.push({ang:(i/n)*6.28, noise:Math.random()*6.28, speed:0.3+Math.random()*0.4});
  }
}
function drawMurekkep(bars, wave, e){
  const g = ctx.createRadialGradient(W*0.5,H*0.46,10,W*0.5,H*0.5,W*0.9);
  g.addColorStop(0, css('--bg1')); g.addColorStop(1, css('--bg0'));
  ctx.fillStyle=g; ctx.fillRect(0,0,W,H);
  
  if(!murekkepBlob) initMurekkepBlob();
  const cx=W*0.5, cy=H*0.46;
  
  [0.55,0.75,1].forEach((scaleF,li)=>{
    const pts = murekkepBlob.map((pt,i)=>{
      const v = bars[Math.floor((i/murekkepBlob.length)*BINS)];
      const r = (34+li*10) * scaleF + Math.sin(pt.noise + t*pt.speed)*8 + v*30;
      return {x:cx+Math.cos(pt.ang)*r, y:cy+Math.sin(pt.ang)*r*0.92};
    });
    ctx.beginPath();
    smoothPath(pts,true);
    ctx.fillStyle=css('--a2');
    ctx.globalAlpha = 0.16 + li*0.08 + e.bass*0.15;
    ctx.fill();
  });
  ctx.globalAlpha=1;
  
  ctx.save();
  for(let i=0;i<14;i++){
    const v=bars[Math.floor((i/14)*BINS)];
    const ang=(i/14)*6.28 + t*0.05;
    const len=50+v*90;
    const pts=[{x:cx,y:cy}];
    let x=cx,y=cy;
    const segs=4;
    for(let s=1;s<=segs;s++){
      x += Math.cos(ang+Math.sin(s+t)*0.15)*(len/segs);
      y += Math.sin(ang+Math.sin(s+t)*0.15)*(len/segs)*0.9;
      pts.push({x,y});
    }
    ctx.beginPath();
    smoothPath(pts,false);
    ctx.strokeStyle=css('--a1');
    ctx.globalAlpha=0.25+v*0.55;
    ctx.lineWidth=1+v*1.8;
    ctx.stroke();
  }
  ctx.restore(); ctx.globalAlpha=1;
  
  if(e.beat){
    murekkepDrops.push({r:6,life:1});
    burst(cx,cy,7,'--a1',1.1,45,{gravity:0.015,drag:0.94});
  }
  murekkepDrops.forEach(d=>{ d.r += 2.4; d.life -= 0.02; });
  murekkepDrops = murekkepDrops.filter(d=>d.life>0);
  murekkepDrops.forEach(d=>{
    ctx.beginPath(); ctx.arc(cx,cy,d.r,0,7);
    ctx.strokeStyle=css('--a1'); ctx.globalAlpha=Math.max(0,d.life*0.5); ctx.lineWidth=1.6;
    ctx.stroke();
  });
  ctx.globalAlpha=1;
  
  if(e.beat) shakeStage(0.5);
  drawSparks(true);
}

/* ---------- ÇÖL — dunes drifting in shimmering heat ---------- */
let colDuneSeed = null;
let colParticles = [];
let colSunPulse = 0;
function drawCol(bars, wave, e){
  const g = ctx.createLinearGradient(0,0,0,H);
  g.addColorStop(0, css('--bg1')); g.addColorStop(1, css('--bg0'));
  ctx.fillStyle = g; ctx.fillRect(0,0,W,H);
  
  colSunPulse += (e.sub - colSunPulse)*0.08;
  const sunX=W*0.78, sunY=H*0.28, sunR=16+colSunPulse*10;
  ctx.save(); ctx.globalCompositeOperation='lighter';
  const glow=ctx.createRadialGradient(sunX,sunY,1,sunX,sunY,sunR*4);
  glow.addColorStop(0, alpha('--a1',0.5)); glow.addColorStop(1, alpha('--a1',0));
  ctx.fillStyle=glow; ctx.beginPath(); ctx.arc(sunX,sunY,sunR*4,0,7); ctx.fill();
  ctx.restore();
  ctx.beginPath(); ctx.arc(sunX,sunY,sunR,0,7); ctx.fillStyle=css('--a2'); ctx.globalAlpha=0.9; ctx.fill(); ctx.globalAlpha=1;
  
  if(!colDuneSeed){ colDuneSeed = Array.from({length:BINS},()=>Math.random()*6.28); }
  
  [{y:H*0.6,amp:0.6,color:'--a3',op:0.9,sp:0.1},
   {y:H*0.74,amp:0.85,color:'--a1',op:0.55,sp:0.16},
   {y:H*0.88,amp:1,color:'--bg1',op:0.95,sp:0.06}].forEach(layer=>{
    const pts=[];
    for(let i=0;i<=BINS;i++){
      const idx=Math.min(i,BINS-1);
      const x=(i/BINS)*W;
      const bump=Math.sin(colDuneSeed[idx]+t*layer.sp+i*0.4)*14;
      const y=layer.y - bars[idx]*30*layer.amp - bump;
      pts.push({x,y});
    }
    ctx.beginPath(); smoothPath(pts); ctx.lineTo(W,H); ctx.lineTo(0,H); ctx.closePath();
    ctx.fillStyle=css(layer.color); ctx.globalAlpha=layer.op; ctx.fill(); ctx.globalAlpha=1;
  });
  
  ctx.strokeStyle=alpha('--ink',0.15+e.treble*0.2);
  ctx.beginPath();
  for(let x=0;x<=W;x+=6){
    const y=H*0.58+Math.sin(x*0.05+t*3)*2*e.treble*5;
    x===0?ctx.moveTo(x,y):ctx.lineTo(x,y);
  }
  ctx.lineWidth=1; ctx.stroke();
  
  if(Math.random()<0.15+e.treble*0.5){
    colParticles.push({x:-5,y:H*0.5+Math.random()*H*0.4,vx:2+Math.random()*3+e.bass*3,vy:(Math.random()-0.5)*0.3,r:0.6+Math.random()*1.2,life:1});
  }
  colParticles.forEach(p=>{ p.x+=p.vx; p.y+=p.vy; p.life-=0.01; });
  colParticles=colParticles.filter(p=>p.life>0 && p.x<W+10);
  colParticles.forEach(p=>{
    ctx.beginPath(); ctx.arc(p.x,p.y,p.r,0,7);
    ctx.fillStyle=css('--a2'); ctx.globalAlpha=p.life*0.6; ctx.fill(); ctx.globalAlpha=1;
  });
  
  if(e.beat) burst(sunX,sunY,6,'--a1',1.2,40,{gravity:0.01,drag:0.95});
  drawSparks(true);
}

/* ---------- DİVİT — inkwell drop blooming in water with golden capillaries ---------- */
let inkVeins = [];
let inkRipples = [];
let inkBlobT = 0;
function drawDivit(bars, wave, e){
  const g = ctx.createRadialGradient(W*0.5,H*0.42,10,W*0.5,H*0.56,W*1.1);
  g.addColorStop(0, css('--bg1')); g.addColorStop(1, css('--bg0'));
  ctx.fillStyle = g; ctx.fillRect(0,0,W,H);
  
  const cx = W*0.5, cy = H*0.5;
  inkBlobT += 0.01 + e.energy*0.012;
  
  function blob(ox, oy, baseR, freqStart, freqEnd, opacity, colorVar){
    const seg = 40;
    ctx.beginPath();
    for(let i=0;i<=seg;i++){
      const p = i/seg;
      const ang = p*Math.PI*2;
      const bin = freqStart + Math.floor(p*(freqEnd-freqStart));
      const v = bars[bin] || 0;
      const wob = Math.sin(ang*3 + inkBlobT*2)*0.12 + Math.sin(ang*5 - inkBlobT*1.3)*0.06;
      const r = baseR*(0.82 + wob + v*0.9);
      const x = ox + Math.cos(ang)*r;
      const y = oy + Math.sin(ang)*r*0.94;
      if(i===0) ctx.moveTo(x,y); else ctx.lineTo(x,y);
    }
    ctx.closePath();
    ctx.fillStyle = alpha(colorVar, opacity);
    ctx.fill();
  }
  
  // Main ink blob + side blobs
  ctx.save();
  ctx.filter = `blur(${7+e.mid*9}px)`;
  blob(cx, cy, 44+e.sub*68, 0, 10, 0.85, '--ink');
  blob(cx - 30 + Math.sin(t*0.3)*18, cy + 24 + Math.cos(t*0.25)*14, 24+e.lowMid*44, 8, 20, 0.5, '--ink');
  blob(cx + 34 + Math.cos(t*0.22)*16, cy - 20 + Math.sin(t*0.31)*12, 18+e.highMid*34, 18, 30, 0.4, '--accent');
  ctx.restore();
  
  // Golden capillary veins — triggered by treble
  if(inkVeins.length < 44 && Math.random() < 0.06 + e.treble*0.32){
    const ang = Math.random()*Math.PI*2;
    inkVeins.push({x:cx, y:cy, ang, len:0, maxLen: 30+Math.random()*60+e.treble*60, life:1});
  }
  inkVeins.forEach(v=>{
    v.len += 1.6 + e.treble*3;
    v.life -= 0.006;
    const x1 = v.x + Math.cos(v.ang)*v.len;
    const y1 = v.y + Math.sin(v.ang)*v.len*0.94;
    ctx.beginPath(); ctx.moveTo(v.x,v.y); ctx.lineTo(x1,y1);
    ctx.strokeStyle = alpha('--a1', Math.max(0,v.life)*0.7);
    ctx.lineWidth = 0.8;
    ctx.stroke();
  });
  inkVeins = inkVeins.filter(v=>v.life>0 && v.len<v.maxLen);
  
  // Beat ripples + sparks
  if(e.beat){
    inkRipples.push({r:8, life:1});
    burst(cx, cy, 10, '--a1', 1.3, 40, {gravity:0, drag:0.94});
    shakeStage(0.3);
  }
  inkRipples.forEach(r=>{
    r.r += 3.2+e.energy*2;
    r.life -= 0.014;
    ctx.beginPath();
    for(let i=0;i<=48;i++){
      const ang = i/48*Math.PI*2;
      const wob = Math.sin(ang*5 + t*2)*2.5;
      const x = cx+Math.cos(ang)*(r.r+wob), y = cy+Math.sin(ang)*(r.r+wob)*0.94;
      if(i===0) ctx.moveTo(x,y); else ctx.lineTo(x,y);
    }
    ctx.closePath();
    ctx.strokeStyle = alpha('--ink', Math.max(0,r.life)*0.35);
    ctx.lineWidth = 1;
    ctx.stroke();
  });
  inkRipples = inkRipples.filter(r=>r.life>0);
  
  // Frequency ring
  ctx.save();
  ctx.beginPath();
  for(let i=0;i<BINS;i++){
    const p = i/BINS;
    const ang = p*Math.PI*2 - Math.PI/2;
    const v = bars[i];
    const rr = Math.min(W,H)*0.46 + v*10;
    const x = cx+Math.cos(ang)*rr, y = cy+Math.sin(ang)*rr*0.94;
    if(i===0) ctx.moveTo(x,y); else ctx.lineTo(x,y);
  }
  ctx.closePath();
  ctx.strokeStyle = alpha('--a1', 0.32+e.treble*0.4);
  ctx.lineWidth = 1;
  ctx.stroke();
  ctx.restore();
  
  drawSparks(true);
}

// Mehtap - moonlight fisherman (sandal, balıkçı, olta, balık yakalama)
// Persistent state for fish catching
if(!window.mehtapState) {
  window.mehtapState = {
    stars: [],
    glints: [],
    catchPhaseIdx: null,
    catchElapsed: 0,
    nextCatchIn: 14,
    catchRodBend: 0,
    fishAlpha: 0,
    fishLift: 0,
    catchBurstFlags: {},
    craters: [{dx:-0.30,dy:-0.20,r:0.16},{dx:0.25,dy:0.15,r:0.12},{dx:0.05,dy:0.32,r:0.09},{dx:-0.15,dy:0.25,r:0.07}]
  };
  // Initialize stars
  for(let i=0; i<90; i++){
    window.mehtapState.stars.push({x:Math.random(),y:Math.random(),r:0.5+Math.random()*1.3,phase:Math.random()*Math.PI*2,speed:0.3+Math.random()*1.1,baseA:0.4+Math.random()*0.6});
  }
}
const CATCH_PHASES = [{name:'bite',dur:0.5},{name:'pull',dur:0.7},{name:'leap',dur:0.55},{name:'reel',dur:0.9},{name:'release',dur:0.8}];
const WAVE_LAYERS = [{ampScale:0.34,freq:10,speed:0.55,key:'waterBottom',baseFactor:0.05},{ampScale:0.20,freq:16,speed:0.8,key:'waterTop',baseFactor:0.02}];

function drawMehtap(bars, wave, e) {
  const st = window.mehtapState;
  const horizonY = H * 0.6;
  const isDark = phone.dataset.mode === 'dark';
  const modeMix = isDark ? 0 : 1;
  
  // Palette helper
  const pal = (key, a) => {
    const PALETTE = {
      night:{skyTop:'#03070d',skyMid:'#0a1b2c',skyBottom:'#123049',waterTop:'#0d2436',waterBottom:'#030a12',body:'#dfe8ee',bodyGlow:'#bfd4e6',bodyCore:'#fbf8ee',silhouette:'#03070c',reflect:'#d9b46a',starColor:'#eaf1ff'},
      day:{skyTop:'#bfe0e8',skyMid:'#eaf3ee',skyBottom:'#fbe6c4',waterTop:'#bfe4df',waterBottom:'#4c8a92',body:'#fff2cf',bodyGlow:'#ffdf9e',bodyCore:'#fffaf0',silhouette:'#16262b',reflect:'#c98b2e',starColor:'#ffffff'}
    };
    const hex = (h) => ({r:parseInt(h.substring(1,3),16),g:parseInt(h.substring(3,5),16),b:parseInt(h.substring(5,7),16)});
    const c1 = hex(PALETTE.night[key]), c2 = hex(PALETTE.day[key]);
    const c = {r:c1.r+(c2.r-c1.r)*modeMix, g:c1.g+(c2.g-c1.g)*modeMix, b:c1.b+(c2.b-c1.b)*modeMix};
    return a===undefined ? `rgb(${c.r|0},${c.g|0},${c.b|0})` : `rgba(${c.r|0},${c.g|0},${c.b|0},${Math.max(0,a)})`;
  };
  
  // Sky
  const skyGrad = ctx.createLinearGradient(0,0,0,horizonY);
  skyGrad.addColorStop(0,pal('skyTop'));
  skyGrad.addColorStop(0.55,pal('skyMid'));
  skyGrad.addColorStop(1,pal('skyBottom'));
  ctx.fillStyle = skyGrad;
  ctx.fillRect(0,0,W,horizonY+2);
  
  // Stars
  const starAlpha = Math.max(0,1-modeMix*1.7);
  if(starAlpha>0.01){
    st.stars.forEach(s=>{
      const tw = 0.5+0.5*Math.sin(t*s.speed+s.phase);
      ctx.beginPath();
      ctx.arc(s.x*W,s.y*horizonY*0.9,s.r*(0.6+tw*0.6),0,7);
      ctx.fillStyle = pal('starColor',starAlpha*(0.35+tw*0.65)*s.baseA);
      ctx.fill();
    });
  }
  
  // Moon/Sun
  const bodyX = W*0.52+Math.sin(t*0.015)*W*0.02;
  const bodyY = horizonY*0.26+Math.cos(t*0.011)*8;
  const bodyR = Math.min(W,horizonY)*0.115+e.bass*7;
  
  // Glow
  const glow = ctx.createRadialGradient(bodyX,bodyY,bodyR*0.15,bodyX,bodyY,bodyR*6);
  glow.addColorStop(0,pal('bodyGlow',0.5+e.energy*0.22));
  glow.addColorStop(0.35,pal('bodyGlow',0.13));
  glow.addColorStop(1,pal('bodyGlow',0));
  ctx.fillStyle = glow;
  ctx.beginPath();
  ctx.arc(bodyX,bodyY,bodyR*6,0,7);
  ctx.fill();
  
  // Sun rays (light mode)
  if(modeMix>0.15){
    ctx.save();
    ctx.globalAlpha = Math.min(1,(modeMix-0.15)/0.85)*0.22;
    ctx.strokeStyle = pal('bodyGlow');
    ctx.lineWidth = 1;
    for(let i=0;i<10;i++){
      const ang = (i/10)*Math.PI*2+t*0.04;
      ctx.beginPath();
      ctx.moveTo(bodyX+Math.cos(ang)*bodyR*1.3,bodyY+Math.sin(ang)*bodyR*1.3);
      ctx.lineTo(bodyX+Math.cos(ang)*bodyR*3.4,bodyY+Math.sin(ang)*bodyR*3.4);
      ctx.stroke();
    }
    ctx.restore();
  }
  
  // Body
  const bodyGrad = ctx.createRadialGradient(bodyX-bodyR*0.25,bodyY-bodyR*0.25,bodyR*0.1,bodyX,bodyY,bodyR);
  bodyGrad.addColorStop(0,pal('bodyCore'));
  bodyGrad.addColorStop(1,pal('body'));
  ctx.fillStyle = bodyGrad;
  ctx.beginPath();
  ctx.arc(bodyX,bodyY,bodyR,0,7);
  ctx.fill();
  
  // Craters (dark mode)
  if(modeMix<0.7){
    ctx.globalAlpha = (1-modeMix)*0.16;
    ctx.fillStyle = pal('skyMid');
    st.craters.forEach(c=>{
      ctx.beginPath();
      ctx.arc(bodyX+c.dx*bodyR,bodyY+c.dy*bodyR,c.r*bodyR,0,7);
      ctx.fill();
    });
    ctx.globalAlpha = 1;
  }
  
  // Fog
  const fogGrad = ctx.createLinearGradient(0,horizonY-36,0,horizonY+22);
  fogGrad.addColorStop(0,pal('skyBottom',0));
  fogGrad.addColorStop(0.5,pal('skyBottom',0.3));
  fogGrad.addColorStop(1,pal('waterTop',0));
  ctx.fillStyle = fogGrad;
  ctx.fillRect(0,horizonY-36,W,58);
  
  // Water
  const waterGrad = ctx.createLinearGradient(0,horizonY,0,H);
  waterGrad.addColorStop(0,pal('waterTop'));
  waterGrad.addColorStop(1,pal('waterBottom'));
  ctx.fillStyle = waterGrad;
  ctx.fillRect(0,horizonY-2,W,H-horizonY+2);
  
  // Reflection
  const rows = 26, bandBase = W*0.1;
  for(let i=0;i<rows;i++){
    const p = i/rows, y = horizonY+6+p*(H-horizonY)*0.85, fade = 1-p*0.85;
    const jitter = (Math.pow(Math.random(),2)-0.3)*(12+e.treble*38)*fade;
    const w = (bandBase*(1-p*0.55)+jitter)*(0.6+e.energy*0.6);
    const cx = bodyX+Math.sin(p*22+t*1.3)*(4+(1-p)*10);
    const h = 1.3+Math.random()*2*fade;
    ctx.fillStyle = pal('reflect',(0.1+Math.random()*0.2)*fade*(0.5+e.treble*0.8));
    ctx.fillRect(cx-w/2,y,Math.max(0,w),h);
  }
  
  // Wave helper
  const waveY = (p,layer) => {
    const amp = (6+e.bass*26)*layer.ampScale*3.4;
    const baseY = horizonY+(H-horizonY)*(0.05+layer.baseFactor);
    return baseY+Math.sin(p*layer.freq+t*layer.speed)*amp+Math.sin(p*layer.freq*2.3-t*layer.speed*1.6)*amp*0.35;
  };
  
  // Wave layers
  WAVE_LAYERS.forEach(layer=>{
    ctx.beginPath();
    ctx.moveTo(0,H+4);
    for(let i=0;i<=40;i++){
      const p=i/40;
      ctx.lineTo(p*W,waveY(p,layer));
    }
    ctx.lineTo(W,H+4);
    ctx.closePath();
    ctx.fillStyle = pal(layer.key,0.92);
    ctx.fill();
  });
  
  // Glints
  const spawnChance = (0.12+e.treble*0.55)*(playing?1:0.15);
  if(Math.random()<spawnChance){
    let p = Math.random();
    if(Math.random()<0.25) p = 0.46+(Math.random()-0.5)*0.12;
    const y = waveY(p,WAVE_LAYERS[1])-Math.random()*3;
    st.glints.push({x:p*W,y,life:1,size:0.6+Math.random()*1.5,decay:0.8+Math.random()*1.3});
  }
  st.glints.forEach(g=>g.life-=0.016*0.8);
  st.glints = st.glints.filter(g=>g.life>0);
  st.glints.forEach(g=>{
    ctx.beginPath();
    ctx.arc(g.x,g.y,g.size*Math.max(0,g.life),0,7);
    ctx.fillStyle = pal('reflect',Math.max(0,g.life)*0.85);
    ctx.fill();
  });
  
  // Boat rig computation
  const boatP = 0.46;
  const waterYc = waveY(boatP,WAVE_LAYERS[1]);
  const slope = waveY(boatP+0.015,WAVE_LAYERS[1])-waveY(boatP-0.015,WAVE_LAYERS[1]);
  const rot = Math.atan2(slope,0.035*W)*0.35;
  const bx = boatP*W, by = waterYc-H*0.010, scale = Math.min(W,H)*0.0026;
  const cosR = Math.cos(rot), sinR = Math.sin(rot);
  const toWorld = (lx,ly) => ({x:bx+(lx*cosR-ly*sinR)*scale, y:by+(lx*sinR+ly*cosR)*scale});
  const hand = toWorld(20,-34);
  const rodAngle = -1.05+rot*0.5+st.catchRodBend;
  const rodLen = Math.min(W,H)*0.11;
  const rodTip = {x:hand.x+Math.cos(rodAngle)*rodLen, y:hand.y+Math.sin(rodAngle)*rodLen};
  const bobberP = boatP+0.155;
  const bobber = {x:bobberP*W, y:waveY(bobberP,WAVE_LAYERS[1])};
  
  // Fish catch system
  const dt = 0.016;
  if(!playing){
    st.catchRodBend += (0-st.catchRodBend)*Math.min(1,dt*3);
  } else {
    if(st.catchPhaseIdx===null){
      st.nextCatchIn -= dt;
      st.catchRodBend += (0-st.catchRodBend)*Math.min(1,dt*3);
      if(st.nextCatchIn<=0){
        st.catchPhaseIdx = 0;
        st.catchElapsed = 0;
        st.catchBurstFlags = {};
      }
    } else {
      st.catchElapsed += dt;
      const ph = CATCH_PHASES[st.catchPhaseIdx], p = Math.min(1,st.catchElapsed/ph.dur);
      if(ph.name==='bite'){
        st.catchRodBend = Math.sin(p*Math.PI*6)*0.045*(1-p*0.3);
        if(p>0.15&&!st.catchBurstFlags.bite){
          burst(bobber.x,bobber.y,6,css('--foam'),0.8,26,{gravity:0.05,drag:0.92,r0:0.6,r1:1.4});
          st.catchBurstFlags.bite=true;
        }
      } else if(ph.name==='pull'){
        st.catchRodBend = 0.06+p*0.30;
        if(p>0.1&&!st.catchBurstFlags.pull){
          burst(bobber.x,bobber.y,14,css('--foam'),1.6,34,{gravity:0.08,drag:0.93,r0:0.8,r1:2});
          st.catchBurstFlags.pull=true;
        }
      } else if(ph.name==='leap'){
        if(!st.catchBurstFlags.leap){
          burst(bobber.x,bobber.y,22,css('--foam'),2.4,42,{gravity:0.1,drag:0.94,r0:1,r1:2.6});
          st.catchBurstFlags.leap=true;
        }
        st.catchRodBend = 0.36-p*0.06;
        st.fishLift = p*0.5;
        st.fishAlpha = Math.min(1,p*3);
      } else if(ph.name==='reel'){
        st.catchRodBend = 0.30-p*0.24;
        st.fishLift = 0.5+p*0.5;
        st.fishAlpha = 1;
        if(p>0.85&&!st.catchBurstFlags.reel){
          burst(hand.x+10,hand.y-30,16,css('--a1'),1.4,30,{gravity:-0.02,drag:0.95,r0:0.6,r1:1.6});
          st.catchBurstFlags.reel=true;
        }
      } else if(ph.name==='release'){
        st.catchRodBend = 0.06*(1-p);
        st.fishLift = 1;
        st.fishAlpha = 1-p;
      }
      if(p>=1){
        st.catchElapsed = 0;
        st.catchPhaseIdx++;
        if(st.catchPhaseIdx>=CATCH_PHASES.length){
          st.catchPhaseIdx = null;
          st.catchRodBend = 0;
          st.fishAlpha = 0;
          st.fishLift = 0;
          st.nextCatchIn = 16+Math.random()*22;
        }
      }
    }
  }
  
  // Draw boat
  ctx.save();
  ctx.translate(bx,by);
  ctx.rotate(rot);
  ctx.scale(scale,scale);
  const sil = pal('silhouette',1);
  ctx.fillStyle = sil;
  ctx.beginPath();
  ctx.moveTo(-95,0);
  ctx.quadraticCurveTo(-70,24,0,28);
  ctx.quadraticCurveTo(70,24,95,0);
  ctx.quadraticCurveTo(55,-10,0,-11);
  ctx.quadraticCurveTo(-55,-10,-95,0);
  ctx.closePath();
  ctx.fill();
  ctx.beginPath();
  ctx.moveTo(-86,-3);
  ctx.quadraticCurveTo(0,-13,86,-3);
  ctx.strokeStyle = pal('reflect',0.45);
  ctx.lineWidth = 1.3;
  ctx.stroke();
  // Fisherman
  ctx.beginPath();
  ctx.moveTo(-16,-9);
  ctx.bezierCurveTo(-18,-32,-6,-50,5,-52);
  ctx.bezierCurveTo(16,-50,17,-35,13,-25);
  ctx.bezierCurveTo(22,-21,24,-11,19,-7);
  ctx.bezierCurveTo(6,-11,-8,-11,-16,-9);
  ctx.closePath();
  ctx.fill();
  ctx.beginPath();
  ctx.arc(7,-58,8.4,0,7);
  ctx.fill();
  ctx.beginPath();
  ctx.ellipse(7,-64,10,3.2,0,0,7);
  ctx.fill();
  ctx.restore();
  
  // Rod
  ctx.beginPath();
  ctx.moveTo(hand.x,hand.y);
  const sag = 4+Math.max(0,st.catchRodBend)*10;
  ctx.quadraticCurveTo((hand.x+rodTip.x)/2,(hand.y+rodTip.y)/2+sag*0.3,rodTip.x,rodTip.y);
  ctx.strokeStyle = sil;
  ctx.lineWidth = 1.6;
  ctx.stroke();
  
  // Fishing line
  const fishPos = st.fishAlpha>0.02 ? {x:bobber.x+(hand.x+18-bobber.x)*Math.min(1,st.fishLift), y:bobber.y+(hand.y-6-bobber.y)*Math.min(1,st.fishLift)-Math.sin(Math.min(1,st.fishLift)*Math.PI)*38} : bobber;
  const slack = st.fishAlpha>0.02 ? 2 : 10;
  ctx.beginPath();
  ctx.moveTo(rodTip.x,rodTip.y);
  ctx.quadraticCurveTo((rodTip.x+fishPos.x)/2,(rodTip.y+fishPos.y)/2+slack,fishPos.x,fishPos.y);
  ctx.strokeStyle = pal('reflect',0.55);
  ctx.lineWidth = 0.8;
  ctx.stroke();
  
  // Fish
  if(st.fishAlpha>0.02){
    ctx.save();
    ctx.globalAlpha = Math.max(0,st.fishAlpha);
    ctx.translate(fishPos.x,fishPos.y);
    const wig = Math.sin(t*14)*0.15;
    ctx.rotate(wig-0.3);
    ctx.beginPath();
    ctx.moveTo(-9,0);
    ctx.quadraticCurveTo(-4,-5,5,0);
    ctx.quadraticCurveTo(-4,5,-9,0);
    ctx.closePath();
    ctx.fillStyle = pal('silhouette',1);
    ctx.fill();
    ctx.beginPath();
    ctx.moveTo(5,0);
    ctx.lineTo(11,-4);
    ctx.lineTo(11,4);
    ctx.closePath();
    ctx.fill();
    ctx.beginPath();
    ctx.moveTo(-2,-1.5);
    ctx.lineTo(2,-3);
    ctx.strokeStyle = pal('reflect',0.8);
    ctx.lineWidth = 0.6;
    ctx.stroke();
    ctx.restore();
  } else {
    // Bobber
    ctx.beginPath();
    ctx.arc(bobber.x,bobber.y,3+Math.sin(t*2)*0.6,0,7);
    ctx.strokeStyle = pal('reflect',0.25);
    ctx.lineWidth = 0.6;
    ctx.stroke();
  }
  
  drawSparks(true);
}

const RENDERERS = {
  mehtap: drawMehtap,
  sumi: drawSumi, aurora: drawAurora, nova: drawNova,
  mycel: drawMycel, reef: drawReef, monsoon: drawMonsoon,
  murekkep: drawMurekkep, col: drawCol, divit: drawDivit,
};

let lastResize = 0;
function render(){
  // Only resize on actual window size changes (not every frame)
  const now = Date.now();
  if(now - lastResize > 500) { // Check every 500ms max
    resize();
    lastResize = now;
  }
  
  t += 0.045;
  const sensBoost = 0.55 + (parseFloat(sensEl.value)/100)*1.1;
  const bars = getBars(sensBoost);
  const wave = getWave();
  const e = analyze(bars, sensBoost);
  updateMeter(bars);
  phone.style.setProperty('--lvl', playing || useRealAudioData ? Math.min(1, e.bass*1.3) : 0);
  const fn = RENDERERS[currentTheme] || drawSumi;
  fn(bars, wave, e);
  requestAnimationFrame(render);
}

setTheme('sumi');
sensVal.textContent = sensEl.value;
boostVal.textContent = boostEl.value;
resize();
updateUI(); // Initialize all i18n texts
render();

// External API for Android
window.updateAudioLevels = function(levels) {
  // Update visualization with external audio data
  if(Array.isArray(levels) && levels.length > 0) {
    // Could be used to inject real audio data from Android
  }
};

window.setBoostState = function(enabled) {
  // Update visual state only - no audio control
  // (Audio is controlled by Android, we just reflect the state)
  console.log('setBoostState called:', enabled, 'current playing:', playing);
  
  if(enabled !== playing) {
    updatePlayButtonState(enabled);
  }
};

// Map Kotlin volume value (60-200) to HTML slider value (0-100)
window.setVolumeFromKotlin = function(kotlinValue) {
  const clamped = Math.max(60, Math.min(200, kotlinValue));
  const htmlValue = Math.round((clamped - 60) / 140 * 100); // 60→0, 200→100
  if(boostEl) {
    boostEl.value = htmlValue;
    boostVal.textContent = htmlValue;
  }
  console.log('Volume from Kotlin:', kotlinValue, '→ HTML slider:', htmlValue);
};

window.setThemeFromAndroid = function(themeName) {
  if(i18n[currentLang].themes[themeName]) {
    setTheme(themeName);
  }
};

window.setLanguage = function(lang) {
  console.log('🌐 Language change requested:', lang);
  if(!i18n[lang]) {
    console.error('❌ Language not found:', lang);
    return;
  }
  
  // Validate lang has all required keys
  if(!i18n[lang].boost || !i18n[lang].themes) {
    console.error('❌ Incomplete translations for:', lang);
    return;
  }
  
  currentLang = lang;
  console.log('✅ Language set to:', lang);
  
  // Use requestAnimationFrame for smooth DOM updates
  requestAnimationFrame(() => {
    try {
      updateUI();
      console.log('✅ UI updated successfully');
    } catch(e) {
      console.error('❌ Error updating UI:', e);
    }
  });
};

function updateUI() {
  console.log('🔄 Updating UI for language:', currentLang);
  
  try {
    // 1. Update labels with safe checks
    const i18nElements = document.querySelectorAll('[data-i18n]');
    console.log('Found', i18nElements.length, 'i18n elements');
    
    i18nElements.forEach(el => {
      try {
        const key = el.dataset.i18n;
        if(key && i18n[currentLang] && i18n[currentLang][key]) {
          el.textContent = i18n[currentLang][key];
        }
      } catch(e) {
        console.warn('Failed to update element:', el, e);
      }
    });
    
    // 2. Update stage hint safely
    if(stageHint && i18n[currentLang] && i18n[currentLang].play_hint) {
      stageHint.textContent = i18n[currentLang].play_hint;
    }
    
    // 3. Update theme chips with safe checks
    const themeChips = document.querySelectorAll('[data-i18n-theme]');
    console.log('Found', themeChips.length, 'theme chips');
    
    themeChips.forEach(chip => {
      try {
        const theme = chip.dataset.i18nTheme;
        if(theme && i18n[currentLang] && i18n[currentLang].themes && i18n[currentLang].themes[theme]) {
          chip.textContent = i18n[currentLang].themes[theme].label;
        }
      } catch(e) {
        console.warn('Failed to update chip:', chip, e);
      }
    });
    
    // 4. Update current theme title/subtitle with animation
    if(currentTheme && i18n[currentLang] && i18n[currentLang].themes && i18n[currentLang].themes[currentTheme]) {
      const meta = i18n[currentLang].themes[currentTheme];
      
      // Smooth fade transition
      if(trackTitle && trackSub) {
        trackTitle.style.opacity = '0';
        trackSub.style.opacity = '0';
        
        setTimeout(() => {
          try {
            trackTitle.textContent = meta.title;
            trackSub.textContent = meta.sub;
            trackTitle.style.opacity = '1';
            trackSub.style.opacity = '1';
          } catch(e) {
            console.warn('Failed to update track info:', e);
          }
        }, 150);
      }
    }
    
    console.log('✅ UI update completed');
  } catch(e) {
    console.error('❌ Critical error in updateUI:', e);
    // Don't crash - just log and continue
  }
}

// Debug: Check if AndroidBridge is available
console.log('AndroidBridge available:', typeof AndroidBridge !== 'undefined');
if(typeof AndroidBridge !== 'undefined') {
  console.log('AndroidBridge methods:', Object.getOwnPropertyNames(Object.getPrototypeOf(AndroidBridge)));
}