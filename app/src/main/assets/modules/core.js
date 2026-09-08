let currentLang = 'tr';

const canvas = document.getElementById('canvas');
const ctx = canvas.getContext('2d');
const phone = document.getElementById('phone');
const stageEl = document.getElementById('stage');
const trackTitle = document.getElementById('trackTitle');
const trackSub = document.getElementById('trackSub');
const stageHint = document.getElementById('stageHint');
const boostEl = document.getElementById('boost');
const sensEl = document.getElementById('sens');
const boostVal = document.getElementById('boostVal');
const sensVal = document.getElementById('sensVal');
const playBtn = document.getElementById('playBtn');
const rippleEl = document.getElementById('ripple');
const clockEl = document.getElementById('clock');
const meterBars = Array.from(document.querySelectorAll('#meter i'));

let currentTheme = 'mehtap';

/* ============ CANVAS SIZING ============ */
let W,H,DPR;
function resize(){
  DPR = Math.min(2, window.devicePixelRatio || 1);
  const r = canvas.getBoundingClientRect();
  W = r.width; H = r.height;
  canvas.width = Math.round(W*DPR); canvas.height = Math.round(H*DPR);
  ctx.setTransform(DPR,0,0,DPR,0,0);
}
window.addEventListener('resize', resize);

let cssCache = {};
function css(v){ return cssCache[v] || (cssCache[v] = getComputedStyle(phone).getPropertyValue(v).trim()); }
function alpha(colorVar, a){
  const hex = css(colorVar);
  const aa = Math.max(0,Math.min(1,a));
  const h = Math.round(aa*255).toString(16).padStart(2,'0');
  return hex.length===7 ? hex+h : hex;
}

/* ============ CLOCK ============ */
function tickClock(){
  const d = new Date();
  clockEl.textContent = d.toLocaleTimeString('tr-TR',{hour:'2-digit',minute:'2-digit'});
}
tickClock(); setInterval(tickClock, 15000);

/* ============ SCREEN SHAKE (used by Muson lightning & Nova shockwaves) ============ */
let shakeTimer = null;
function shakeStage(intensity){
  stageEl.style.transform = `translate(${(Math.random()-0.5)*intensity}px, ${(Math.random()-0.5)*intensity}px)`;
  clearTimeout(shakeTimer);
  shakeTimer = setTimeout(()=>{ stageEl.style.transform=''; }, 85);
}

/* ============ DARK/LIGHT MODE TOGGLE ============ */
const modeToggle = document.getElementById('modeToggle');
const modeThumb = document.getElementById('modeThumb');

const MOON_ICON = '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M21 12.4A9 9 0 1111.6 3a7 7 0 009.4 9.4z"/></svg>';
const SUN_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="4" fill="currentColor" stroke="none"/><path d="M12 2v3M12 19v3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M2 12h3M19 12h3M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1"/></svg>';

function setMode(mode){
  phone.dataset.mode = mode;
  cssCache = {};  // Clear CSS cache to reload colors for canvas
  modeThumb.innerHTML = mode==='dark' ? MOON_ICON : SUN_ICON;
  modeToggle.setAttribute('aria-label', mode==='dark' ? 'Karanlık mod' : 'Aydınlık mod');
  
  // Notify Android
  try{
    if(typeof AndroidBridge !== 'undefined') {
      AndroidBridge.onModeChanged(mode);
    }
  }catch(e){
    console.log('AndroidBridge not available for mode change');
  }
}

modeToggle.addEventListener('click', ()=>{
  setMode(phone.dataset.mode === 'dark' ? 'light' : 'dark');
});

// Android can set mode externally
window.setModeFromAndroid = function(mode){
  if(mode==='dark' || mode==='light') setMode(mode);
};

// Initialize with dark mode
setMode('dark');

/* ============ THEME SWITCHING ============ */
function setTheme(name){
  currentTheme = name;
  phone.dataset.theme = name;
  cssCache = {};
  document.querySelectorAll('.chip').forEach(c=>c.classList.toggle('active', c.dataset.theme===name));
  const meta = i18n[currentLang].themes[name];
  trackTitle.style.opacity = 0;
  setTimeout(()=>{
    trackTitle.textContent = meta.title;
    trackSub.textContent = meta.sub;
    trackTitle.style.opacity = 1;
  }, 140);
  
  // Don't call Android bridge - let Android control themes
  
  // reset every theme's persistent particle/state buffers
  sparks = []; inkSplats = []; sumiDust = null;
  auroraStars = null; auroraOrbPulse = 0;
  novaStars = null; novaParticles = null; novaShock = [];
  mycelBranches = null; mycelPulses = []; mycelSpores = []; mycelGrowStart = 0;
  glassBlobs = null; glassSparkles = [];
  reefParticles = null; reefTentaclePhase = 0; reefCompanionPhase = 1.7;
  stormBolts = []; rainDrops = null; cloudSeed = null; fogSeed = null;
  // New themes
  murekkepBlob = null; murekkepDrops = [];
  colDuneSeed = null; colParticles = []; colSunPulse = 0;
  inkVeins = []; inkRipples = []; inkBlobT = 0;
}
document.getElementById('themes').addEventListener('click', e=>{
  const btn = e.target.closest('.chip');
  if(!btn) return;
  const themeName = btn.dataset.theme;
  setTheme(themeName);
  // Notify Android
  if(typeof AndroidBridge !== 'undefined') {
    AndroidBridge.changeTheme(themeName);
  }
});
