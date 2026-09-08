// mycel Visualizer

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

// Register visualizer
RENDERERS['mycel'] = drawMycel;