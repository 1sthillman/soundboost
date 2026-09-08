// monsoon Visualizer

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

// Register visualizer
RENDERERS['monsoon'] = drawMonsoon;