// col Visualizer

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