// mehtap Visualizer


function drawMehtap(bars, wave, e) {
  const st = window.mehtapState;
  const horizonY = H * 0.6;
  const isDark = phone.dataset.mode === 'dark';
  const modeMix = isDark ? 0 : 1;
  
  // Palette helper
  const pal = (key, a) => {
    const PALETTE = {
      night:{skyTop:'#03070d',skyMid:'#0a1b2c',skyBottom:'#123049',waterTop:'#0d2436',waterBottom:'#030a12',body:'#dfe8ee',bodyGlow:'#bfd4e6',bodyCore:'#fbf8ee',silhouette:'#03070c',reflect:'#d9b46a',starColor:'#eaf1ff'},
      day:{skyTop:'#bfe0e8',skyMid:'#eaf3ee',skyBottom:'#fbe6c4',waterTop:'#bfe4df',waterBottom:'#4c8a92',body:'#fff2cf',bodyGlow:'#ffdf9e',bodyCore:'#fffaf0',silhouette:'#16262b',reflect:'#c98b2e',starColor:'#ffffff'}
    };
    const hex = (h) => ({r:parseInt(h.substring(1,3),16),g:parseInt(h.substring(3,5),16),b:parseInt(h.substring(5,7),16)});
    const c1 = hex(PALETTE.night[key]), c2 = hex(PALETTE.day[key]);
    const c = {r:c1.r+(c2.r-c1.r)*modeMix, g:c1.g+(c2.g-c1.g)*modeMix, b:c1.b+(c2.b-c1.b)*modeMix};
    return a===undefined ? `rgb(${c.r|0},${c.g|0},${c.b|0})` : `rgba(${c.r|0},${c.g|0},${c.b|0},${Math.max(0,a)})`;
  };
  
  // Sky
  const skyGrad = ctx.createLinearGradient(0,0,0,horizonY);
  skyGrad.addColorStop(0,pal('skyTop'));
  skyGrad.addColorStop(0.55,pal('skyMid'));
  skyGrad.addColorStop(1,pal('skyBottom'));
  ctx.fillStyle = skyGrad;
  ctx.fillRect(0,0,W,horizonY+2);
  
  // Stars
  const starAlpha = Math.max(0,1-modeMix*1.7);
  if(starAlpha>0.01){
    st.stars.forEach(s=>{
      const tw = 0.5+0.5*Math.sin(t*s.speed+s.phase);
      ctx.beginPath();
      ctx.arc(s.x*W,s.y*horizonY*0.9,s.r*(0.6+tw*0.6),0,7);
      ctx.fillStyle = pal('starColor',starAlpha*(0.35+tw*0.65)*s.baseA);
      ctx.fill();
    });
  }
  
  // Moon/Sun
  const bodyX = W*0.52+Math.sin(t*0.015)*W*0.02;
  const bodyY = horizonY*0.26+Math.cos(t*0.011)*8;
  const bodyR = Math.min(W,horizonY)*0.115+e.bass*7;
  
  // Glow
  const glow = ctx.createRadialGradient(bodyX,bodyY,bodyR*0.15,bodyX,bodyY,bodyR*6);
  glow.addColorStop(0,pal('bodyGlow',0.5+e.energy*0.22));
  glow.addColorStop(0.35,pal('bodyGlow',0.13));
  glow.addColorStop(1,pal('bodyGlow',0));
  ctx.fillStyle = glow;
  ctx.beginPath();
  ctx.arc(bodyX,bodyY,bodyR*6,0,7);
  ctx.fill();
  
  // Sun rays (light mode)
  if(modeMix>0.15){
    ctx.save();
    ctx.globalAlpha = Math.min(1,(modeMix-0.15)/0.85)*0.22;
    ctx.strokeStyle = pal('bodyGlow');
    ctx.lineWidth = 1;
    for(let i=0;i<10;i++){
      const ang = (i/10)*Math.PI*2+t*0.04;
      ctx.beginPath();
      ctx.moveTo(bodyX+Math.cos(ang)*bodyR*1.3,bodyY+Math.sin(ang)*bodyR*1.3);
      ctx.lineTo(bodyX+Math.cos(ang)*bodyR*3.4,bodyY+Math.sin(ang)*bodyR*3.4);
      ctx.stroke();
    }
    ctx.restore();
  }
  
  // Body
  const bodyGrad = ctx.createRadialGradient(bodyX-bodyR*0.25,bodyY-bodyR*0.25,bodyR*0.1,bodyX,bodyY,bodyR);
  bodyGrad.addColorStop(0,pal('bodyCore'));
  bodyGrad.addColorStop(1,pal('body'));
  ctx.fillStyle = bodyGrad;
  ctx.beginPath();
  ctx.arc(bodyX,bodyY,bodyR,0,7);
  ctx.fill();
  
  // Craters (dark mode)
  if(modeMix<0.7){
    ctx.globalAlpha = (1-modeMix)*0.16;
    ctx.fillStyle = pal('skyMid');
    st.craters.forEach(c=>{
      ctx.beginPath();
      ctx.arc(bodyX+c.dx*bodyR,bodyY+c.dy*bodyR,c.r*bodyR,0,7);
      ctx.fill();
    });
    ctx.globalAlpha = 1;
  }
  
  // Fog
  const fogGrad = ctx.createLinearGradient(0,horizonY-36,0,horizonY+22);
  fogGrad.addColorStop(0,pal('skyBottom',0));
  fogGrad.addColorStop(0.5,pal('skyBottom',0.3));
  fogGrad.addColorStop(1,pal('waterTop',0));
  ctx.fillStyle = fogGrad;
  ctx.fillRect(0,horizonY-36,W,58);
  
  // Water
  const waterGrad = ctx.createLinearGradient(0,horizonY,0,H);
  waterGrad.addColorStop(0,pal('waterTop'));
  waterGrad.addColorStop(1,pal('waterBottom'));
  ctx.fillStyle = waterGrad;
  ctx.fillRect(0,horizonY-2,W,H-horizonY+2);
  
  // Reflection
  const rows = 26, bandBase = W*0.1;
  for(let i=0;i<rows;i++){
    const p = i/rows, y = horizonY+6+p*(H-horizonY)*0.85, fade = 1-p*0.85;
    const jitter = (Math.pow(Math.random(),2)-0.3)*(12+e.treble*38)*fade;
    const w = (bandBase*(1-p*0.55)+jitter)*(0.6+e.energy*0.6);
    const cx = bodyX+Math.sin(p*22+t*1.3)*(4+(1-p)*10);
    const h = 1.3+Math.random()*2*fade;
    ctx.fillStyle = pal('reflect',(0.1+Math.random()*0.2)*fade*(0.5+e.treble*0.8));
    ctx.fillRect(cx-w/2,y,Math.max(0,w),h);
  }
  
  // Wave helper
  const waveY = (p,layer) => {
    const amp = (6+e.bass*26)*layer.ampScale*3.4;
    const baseY = horizonY+(H-horizonY)*(0.05+layer.baseFactor);
    return baseY+Math.sin(p*layer.freq+t*layer.speed)*amp+Math.sin(p*layer.freq*2.3-t*layer.speed*1.6)*amp*0.35;
  };
  
  // Wave layers
  WAVE_LAYERS.forEach(layer=>{
    ctx.beginPath();
    ctx.moveTo(0,H+4);
    for(let i=0;i<=40;i++){
      const p=i/40;
      ctx.lineTo(p*W,waveY(p,layer));
    }
    ctx.lineTo(W,H+4);
    ctx.closePath();
    ctx.fillStyle = pal(layer.key,0.92);
    ctx.fill();
  });
  
  // Glints
  const spawnChance = (0.12+e.treble*0.55)*(playing?1:0.15);
  if(Math.random()<spawnChance){
    let p = Math.random();
    if(Math.random()<0.25) p = 0.46+(Math.random()-0.5)*0.12;
    const y = waveY(p,WAVE_LAYERS[1])-Math.random()*3;
    st.glints.push({x:p*W,y,life:1,size:0.6+Math.random()*1.5,decay:0.8+Math.random()*1.3});
  }
  st.glints.forEach(g=>g.life-=0.016*0.8);
  st.glints = st.glints.filter(g=>g.life>0);
  st.glints.forEach(g=>{
    ctx.beginPath();
    ctx.arc(g.x,g.y,g.size*Math.max(0,g.life),0,7);
    ctx.fillStyle = pal('reflect',Math.max(0,g.life)*0.85);
    ctx.fill();
  });
  
  // Boat rig computation
  const boatP = 0.46;
  const waterYc = waveY(boatP,WAVE_LAYERS[1]);
  const slope = waveY(boatP+0.015,WAVE_LAYERS[1])-waveY(boatP-0.015,WAVE_LAYERS[1]);
  const rot = Math.atan2(slope,0.035*W)*0.35;
  const bx = boatP*W, by = waterYc-H*0.010, scale = Math.min(W,H)*0.0026;
  const cosR = Math.cos(rot), sinR = Math.sin(rot);
  const toWorld = (lx,ly) => ({x:bx+(lx*cosR-ly*sinR)*scale, y:by+(lx*sinR+ly*cosR)*scale});
  const hand = toWorld(20,-34);
  const rodAngle = -1.05+rot*0.5+st.catchRodBend;
  const rodLen = Math.min(W,H)*0.11;
  const rodTip = {x:hand.x+Math.cos(rodAngle)*rodLen, y:hand.y+Math.sin(rodAngle)*rodLen};
  const bobberP = boatP+0.155;
  const bobber = {x:bobberP*W, y:waveY(bobberP,WAVE_LAYERS[1])};
  
  // Fish catch system
  const dt = 0.016;
  if(!playing){
    st.catchRodBend += (0-st.catchRodBend)*Math.min(1,dt*3);
  } else {
    if(st.catchPhaseIdx===null){
      st.nextCatchIn -= dt;
      st.catchRodBend += (0-st.catchRodBend)*Math.min(1,dt*3);
      if(st.nextCatchIn<=0){
        st.catchPhaseIdx = 0;
        st.catchElapsed = 0;
        st.catchBurstFlags = {};
      }
    } else {
      st.catchElapsed += dt;
      const ph = CATCH_PHASES[st.catchPhaseIdx], p = Math.min(1,st.catchElapsed/ph.dur);
      if(ph.name==='bite'){
        st.catchRodBend = Math.sin(p*Math.PI*6)*0.045*(1-p*0.3);
        if(p>0.15&&!st.catchBurstFlags.bite){
          burst(bobber.x,bobber.y,6,css('--foam'),0.8,26,{gravity:0.05,drag:0.92,r0:0.6,r1:1.4});
          st.catchBurstFlags.bite=true;
        }
      } else if(ph.name==='pull'){
        st.catchRodBend = 0.06+p*0.30;
        if(p>0.1&&!st.catchBurstFlags.pull){
          burst(bobber.x,bobber.y,14,css('--foam'),1.6,34,{gravity:0.08,drag:0.93,r0:0.8,r1:2});
          st.catchBurstFlags.pull=true;
        }
      } else if(ph.name==='leap'){
        if(!st.catchBurstFlags.leap){
          burst(bobber.x,bobber.y,22,css('--foam'),2.4,42,{gravity:0.1,drag:0.94,r0:1,r1:2.6});
          st.catchBurstFlags.leap=true;
        }
        st.catchRodBend = 0.36-p*0.06;
        st.fishLift = p*0.5;
        st.fishAlpha = Math.min(1,p*3);
      } else if(ph.name==='reel'){
        st.catchRodBend = 0.30-p*0.24;
        st.fishLift = 0.5+p*0.5;
        st.fishAlpha = 1;
        if(p>0.85&&!st.catchBurstFlags.reel){
          burst(hand.x+10,hand.y-30,16,css('--a1'),1.4,30,{gravity:-0.02,drag:0.95,r0:0.6,r1:1.6});
          st.catchBurstFlags.reel=true;
        }
      } else if(ph.name==='release'){
        st.catchRodBend = 0.06*(1-p);
        st.fishLift = 1;
        st.fishAlpha = 1-p;
      }
      if(p>=1){
        st.catchElapsed = 0;
        st.catchPhaseIdx++;
        if(st.catchPhaseIdx>=CATCH_PHASES.length){
          st.catchPhaseIdx = null;
          st.catchRodBend = 0;
          st.fishAlpha = 0;
          st.fishLift = 0;
          st.nextCatchIn = 16+Math.random()*22;
        }
      }
    }
  }
  
  // Draw boat
  ctx.save();
  ctx.translate(bx,by);
  ctx.rotate(rot);
  ctx.scale(scale,scale);
  const sil = pal('silhouette',1);
  ctx.fillStyle = sil;
  ctx.beginPath();
  ctx.moveTo(-95,0);
  ctx.quadraticCurveTo(-70,24,0,28);
  ctx.quadraticCurveTo(70,24,95,0);
  ctx.quadraticCurveTo(55,-10,0,-11);
  ctx.quadraticCurveTo(-55,-10,-95,0);
  ctx.closePath();
  ctx.fill();
  ctx.beginPath();
  ctx.moveTo(-86,-3);
  ctx.quadraticCurveTo(0,-13,86,-3);
  ctx.strokeStyle = pal('reflect',0.45);
  ctx.lineWidth = 1.3;
  ctx.stroke();
  // Fisherman
  ctx.beginPath();
  ctx.moveTo(-16,-9);
  ctx.bezierCurveTo(-18,-32,-6,-50,5,-52);
  ctx.bezierCurveTo(16,-50,17,-35,13,-25);
  ctx.bezierCurveTo(22,-21,24,-11,19,-7);
  ctx.bezierCurveTo(6,-11,-8,-11,-16,-9);
  ctx.closePath();
  ctx.fill();
  ctx.beginPath();
  ctx.arc(7,-58,8.4,0,7);
  ctx.fill();
  ctx.beginPath();
  ctx.ellipse(7,-64,10,3.2,0,0,7);
  ctx.fill();
  ctx.restore();
  
  // Rod
  ctx.beginPath();
  ctx.moveTo(hand.x,hand.y);
  const sag = 4+Math.max(0,st.catchRodBend)*10;
  ctx.quadraticCurveTo((hand.x+rodTip.x)/2,(hand.y+rodTip.y)/2+sag*0.3,rodTip.x,rodTip.y);
  ctx.strokeStyle = sil;
  ctx.lineWidth = 1.6;
  ctx.stroke();
  
  // Fishing line
  const fishPos = st.fishAlpha>0.02 ? {x:bobber.x+(hand.x+18-bobber.x)*Math.min(1,st.fishLift), y:bobber.y+(hand.y-6-bobber.y)*Math.min(1,st.fishLift)-Math.sin(Math.min(1,st.fishLift)*Math.PI)*38} : bobber;
  const slack = st.fishAlpha>0.02 ? 2 : 10;
  ctx.beginPath();
  ctx.moveTo(rodTip.x,rodTip.y);
  ctx.quadraticCurveTo((rodTip.x+fishPos.x)/2,(rodTip.y+fishPos.y)/2+slack,fishPos.x,fishPos.y);
  ctx.strokeStyle = pal('reflect',0.55);
  ctx.lineWidth = 0.8;
  ctx.stroke();
  
  // Fish
  if(st.fishAlpha>0.02){
    ctx.save();
    ctx.globalAlpha = Math.max(0,st.fishAlpha);
    ctx.translate(fishPos.x,fishPos.y);
    const wig = Math.sin(t*14)*0.15;
    ctx.rotate(wig-0.3);
    ctx.beginPath();
    ctx.moveTo(-9,0);
    ctx.quadraticCurveTo(-4,-5,5,0);
    ctx.quadraticCurveTo(-4,5,-9,0);
    ctx.closePath();
    ctx.fillStyle = pal('silhouette',1);
    ctx.fill();
    ctx.beginPath();
    ctx.moveTo(5,0);
    ctx.lineTo(11,-4);
    ctx.lineTo(11,4);
    ctx.closePath();
    ctx.fill();
    ctx.beginPath();
    ctx.moveTo(-2,-1.5);
    ctx.lineTo(2,-3);
    ctx.strokeStyle = pal('reflect',0.8);
    ctx.lineWidth = 0.6;
    ctx.stroke();
    ctx.restore();
  } else {
    // Bobber
    ctx.beginPath();
    ctx.arc(bobber.x,bobber.y,3+Math.sin(t*2)*0.6,0,7);
    ctx.strokeStyle = pal('reflect',0.25);
    ctx.lineWidth = 0.6;
    ctx.stroke();
  }
  
  drawSparks(true);
}

const RENDERERS = {
  mehtap: drawMehtap,
  sumi: drawSumi, aurora: drawAurora, nova: drawNova,
  mycel: drawMycel, reef: drawReef, monsoon: drawMonsoon,
  murekkep: drawMurekkep, col: drawCol, divit: drawDivit,
};

let lastResize = 0;
function render(){
  // Only resize on actual window size changes (not every frame)
  const now = Date.now();
  if(now - lastResize > 500) { // Check every 500ms max
    resize();
    lastResize = now;
  }
  
  t += 0.045;
  const sensBoost = 0.55 + (parseFloat(sensEl.value)/100)*1.1;
  const bars = getBars(sensBoost);
  const wave = getWave();
  const e = analyze(bars, sensBoost);
  updateMeter(bars);
  phone.style.setProperty('--lvl', playing || useRealAudioData ? Math.min(1, e.bass*1.3) : 0);
  const fn = RENDERERS[currentTheme] || drawSumi;
  fn(bars, wave, e);
  requestAnimationFrame(render);