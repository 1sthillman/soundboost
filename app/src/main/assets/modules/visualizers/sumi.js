// Sumi-e Visualizer - Japanese ink wash painting
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

// Reset function for theme switching
function resetSumiState() {
  inkSplats = [];
  sumiDust = null;
}

// Register visualizer
RENDERERS['sumi'] = drawSumi;