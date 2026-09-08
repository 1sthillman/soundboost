// aurora Visualizer

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

// Register visualizer
RENDERERS['aurora'] = drawAurora;