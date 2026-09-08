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