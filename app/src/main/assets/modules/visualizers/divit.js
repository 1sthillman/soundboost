// divit Visualizer

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