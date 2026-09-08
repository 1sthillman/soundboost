// murekkep Visualizer

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

// Register visualizer
RENDERERS['murekkep'] = drawMurekkep;