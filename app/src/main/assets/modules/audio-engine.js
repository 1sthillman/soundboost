/* =====================================================================
   REAL AUDIO ENGINE — a small generative ambient patch, analysed live
   via a real AnalyserNode. Nothing here is faked: the visuals below are
   driven by actual FFT / waveform data pulled from this audio graph.
   Signal path: voices -> lowpass -> bass shelf -> air shelf -> master
   -> compressor -> auto-panner -> analyser -> speakers, with a parallel
   feedback delay line fed by the arpeggio for spatial depth.
   ===================================================================== */

// === REAL AUDIO DATA FROM ANDROID ===
let useRealAudioData = false;
let realAudioData = null;
let lastRealDataTime = 0;
const REAL_DATA_TIMEOUT = 200; // ms - eğer 200ms veri gelmezse mock'a geç

// Android'den gerçek audio data'yı alacak API
window.updateRealAudioData = function(audioDataJson) {
  try {
    realAudioData = JSON.parse(audioDataJson);
    lastRealDataTime = Date.now();
    useRealAudioData = true;
  } catch(e) {
    console.error('Failed to parse audio data:', e);
  }
};

let actx, analyser, freqData, timeData;
let master, bassShelf, lowpass, airShelf, comp, panner, panLfo, panLfoGain;
let delay, delayFeedback, delayFilter, delayOut;
let lfo, lfoGain;
let padOscs = [];
let padDetuneLfos = [];
let chordIdx = 0;
let chordTimer=null, arpTimer=null, hatTimer=null, kickTimer=null;
let playing = false;

const CHORDS = [
  [73.42, 110.00, 146.83, 174.61, 220.00],
  [65.41, 98.00, 130.81, 155.56, 196.00],
  [58.27, 87.31, 116.54, 138.59, 174.61],
  [61.74, 92.50, 123.47, 146.83, 185.00],
];

function initAudio(){
  actx = new (window.AudioContext||window.webkitAudioContext)();

  analyser = actx.createAnalyser();
  analyser.fftSize = 1024;
  analyser.smoothingTimeConstant = 0.78;
  freqData = new Uint8Array(analyser.frequencyBinCount);
  timeData = new Uint8Array(analyser.fftSize);

  master = actx.createGain();
  master.gain.value = 0.0001;

  bassShelf = actx.createBiquadFilter();
  bassShelf.type = 'lowshelf';
  bassShelf.frequency.value = 220;
  bassShelf.gain.value = 0;

  lowpass = actx.createBiquadFilter();
  lowpass.type = 'lowpass';
  lowpass.frequency.value = 900;
  lowpass.Q.value = 0.6;

  airShelf = actx.createBiquadFilter();
  airShelf.type = 'highshelf';
  airShelf.frequency.value = 5200;
  airShelf.gain.value = 2.5;

  comp = actx.createDynamicsCompressor();
  comp.threshold.value = -20; comp.knee.value = 26; comp.ratio.value = 3.2;
  comp.attack.value = 0.005; comp.release.value = 0.22;

  panner = actx.createStereoPanner();
  panLfo = actx.createOscillator(); panLfo.frequency.value = 0.06;
  panLfoGain = actx.createGain(); panLfoGain.gain.value = 0.3;
  panLfo.connect(panLfoGain); panLfoGain.connect(panner.pan); panLfo.start();

  // spatial echo, fed only by the plucked arpeggio for a sense of depth
  delay = actx.createDelay(1.2); delay.delayTime.value = 0.36;
  delayFeedback = actx.createGain(); delayFeedback.gain.value = 0.36;
  delayFilter = actx.createBiquadFilter(); delayFilter.type = 'lowpass'; delayFilter.frequency.value = 2400;
  delay.connect(delayFilter); delayFilter.connect(delayFeedback); delayFeedback.connect(delay);
  delayOut = actx.createGain(); delayOut.gain.value = 0.55;
  delay.connect(delayOut);

  lowpass.connect(bassShelf);
  bassShelf.connect(airShelf);
  airShelf.connect(master);
  delayOut.connect(master);
  master.connect(comp);
  comp.connect(panner);
  panner.connect(analyser);
  analyser.connect(actx.destination);

  lfo = actx.createOscillator();
  lfo.frequency.value = 0.045;
  lfoGain = actx.createGain();
  lfoGain.gain.value = 260;
  lfo.connect(lfoGain);
  lfoGain.connect(lowpass.frequency);
  lfo.start();

  buildChordVoices(CHORDS[0]);
  applyBoost();
  applySensitivity();
}

function buildChordVoices(chord){
  padOscs.forEach(o=>{ try{o.stop();}catch(e){} });
  padDetuneLfos.forEach(o=>{ try{o.stop();}catch(e){} });
  padOscs = [];
  padDetuneLfos = [];
  chord.forEach((freq,i)=>{
    const o = actx.createOscillator();
    o.type = i===0 ? 'sine' : (i%2===0 ? 'triangle' : 'sawtooth');
    o.frequency.value = i===0 ? freq/2 : freq;
    const g = actx.createGain();
    g.gain.value = i===0 ? 0.22 : 0.045;
    o.connect(g); g.connect(lowpass);
    o.start();
    padOscs.push(o);

    if(i>0){
      // slow detune drift per voice — a gentle chorus, so the pad breathes
      const dl = actx.createOscillator(); dl.frequency.value = 0.028 + i*0.013;
      const dlg = actx.createGain(); dlg.gain.value = 3 + i*0.6;
      dl.connect(dlg); dlg.connect(o.detune); dl.start();
      padDetuneLfos.push(dl);
    }
  });

  // shimmer LFO on the echo's tone, for slow-moving spatial movement
  const shimmer = actx.createOscillator(); shimmer.frequency.value = 0.08;
  const shimmerGain = actx.createGain(); shimmerGain.gain.value = 650;
  shimmer.connect(shimmerGain); shimmerGain.connect(delayFilter.frequency);
  shimmer.start();
  padDetuneLfos.push(shimmer);
}

function advanceChord(){
  chordIdx = (chordIdx+1) % CHORDS.length;
  const chord = CHORDS[chordIdx];
  const now = actx.currentTime;
  padOscs.forEach((o,i)=>{
    if(o.frequency && chord[i] !== undefined){
      const target = i===0 ? chord[i]/2 : chord[i];
      o.frequency.linearRampToValueAtTime(target, now + 2.2);
    }
  });
}

function pluck(freq, when){
  const t0 = actx.currentTime + (when||0);
  const o = actx.createOscillator();
  o.type = 'triangle';
  o.frequency.value = freq;
  const g = actx.createGain();
  g.gain.setValueAtTime(0.0001, t0);
  g.gain.exponentialRampToValueAtTime(0.13, t0+0.015);
  g.gain.exponentialRampToValueAtTime(0.0001, t0+0.5);
  o.connect(g); g.connect(lowpass);
  const send = actx.createGain(); send.gain.value = 0.5;
  g.connect(send); send.connect(delay);
  o.start(t0); o.stop(t0+0.55);
}

function arpeggiate(){
  const chord = CHORDS[chordIdx];
  const root = chord[Math.floor(Math.random()*chord.length)];
  const notes = [root*2, root*2.5, root*3, root*2*1.5];
  notes.forEach((f,i)=> pluck(f, i*0.14));
}

function kick(){
  if(!actx) return;
  const t0 = actx.currentTime;
  const o = actx.createOscillator();
  o.type = 'sine';
  o.frequency.setValueAtTime(120, t0);
  o.frequency.exponentialRampToValueAtTime(38, t0+0.14);
  const g = actx.createGain();
  g.gain.setValueAtTime(0.0001, t0);
  g.gain.exponentialRampToValueAtTime(0.5, t0+0.008);
  g.gain.exponentialRampToValueAtTime(0.0001, t0+0.32);
  o.connect(g); g.connect(bassShelf);
  o.start(t0); o.stop(t0+0.34);
}

function hat(){
  const dur = 0.045;
  const bufferSize = Math.floor(actx.sampleRate*dur);
  const buffer = actx.createBuffer(1, bufferSize, actx.sampleRate);
  const data = buffer.getChannelData(0);
  for(let i=0;i<bufferSize;i++) data[i] = (Math.random()*2-1) * (1 - i/bufferSize);
  const src = actx.createBufferSource();
  src.buffer = buffer;
  const hp = actx.createBiquadFilter(); hp.type='highpass'; hp.frequency.value=6500;
  const g = actx.createGain(); g.gain.value = 0.10 + Math.random()*0.05;
  src.connect(hp); hp.connect(g); g.connect(bassShelf);
  src.start();
}

function applyBoost(){
  const v = parseFloat(boostEl.value);
  boostVal.textContent = v;  // Her zaman güncelle
  
  // Mock audio için (preview mode)
  if(bassShelf) {
    const db = (v-50)/50*15;
    bassShelf.gain.setTargetAtTime(db, actx.currentTime, 0.05);
  }
  
  // Bridge to Android: map 0-100 slider → 60-200 Kotlin range
  if(typeof AndroidBridge !== 'undefined') {
    const kotlinValue = Math.round(60 + (v / 100) * 140); // 0→60, 100→200
    AndroidBridge.onVolumeChanged(kotlinValue);
  }
}
function applySensitivity(){
  if(!analyser) { sensVal.textContent = sensEl.value; return; }
  const v = parseFloat(sensEl.value);
  analyser.smoothingTimeConstant = 0.88 - (v/100)*0.58;
  if(airShelf) airShelf.gain.setTargetAtTime(1.2 + (v/100)*5.5, actx.currentTime, 0.12);
  sensVal.textContent = v;
  // Bridge to Android
  if(typeof AndroidBridge !== 'undefined') {
    AndroidBridge.onSensitivityChanged(Math.floor(v));
  }
}
boostEl.addEventListener('input', applyBoost);
sensEl.addEventListener('input', applySensitivity);

async function togglePlay(){
  // Visual feedback
  rippleEl.classList.remove('go'); 
  void rippleEl.offsetWidth; 
  rippleEl.classList.add('go');
  
  // ONLY toggle Android boost - no mock audio!
  if(typeof AndroidBridge !== 'undefined') {
    AndroidBridge.toggleBoost();
    // State will be updated via window.setBoostState() from Kotlin
  } else {
    // Fallback: If no Android bridge, toggle mock audio for preview
    toggleMockAudio();
  }
}

function toggleMockAudio(){
  // This function is ONLY for preview/testing without Android
  if(!actx) initAudio();
  if(actx && actx.state === 'suspended') actx.resume();
  
  if(!playing){
    master.gain.cancelScheduledValues(actx.currentTime);
    master.gain.setTargetAtTime(0.42, actx.currentTime, 0.4);
    hatTimer = setInterval(hat, 260);
    arpTimer = setInterval(arpeggiate, 3200);
    chordTimer = setInterval(advanceChord, 7000);
    kickTimer = setInterval(kick, 1600);
    kick();
    arpeggiate();
    updatePlayButtonState(true);
  } else {
    master.gain.cancelScheduledValues(actx.currentTime);
    master.gain.setTargetAtTime(0.0001, actx.currentTime, 0.3);
    clearInterval(hatTimer); 
    clearInterval(arpTimer); 
    clearInterval(chordTimer); 
    clearInterval(kickTimer);
    updatePlayButtonState(false);
  }
}

function updatePlayButtonState(isPlaying){
  playing = isPlaying;
  if(isPlaying){
    playBtn.querySelector('svg').innerHTML = '<rect x="6" y="5" width="4" height="14"/><rect x="14" y="5" width="4" height="14"/>';
    stageHint.style.opacity = '0';
  } else {
    playBtn.querySelector('svg').innerHTML = '<path d="M8 5v14l11-7z"/>';
    stageHint.style.opacity = '0.9';
  }
}

playBtn.addEventListener('click', togglePlay);
