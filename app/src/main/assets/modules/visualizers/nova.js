// nova Visualizer

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