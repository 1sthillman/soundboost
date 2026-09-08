// reef Visualizer

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

// Register visualizer
RENDERERS['reef'] = drawReef;