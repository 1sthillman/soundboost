# awwardstheme.html - Dosya Yapısı ve Bakım Kılavuzu

## 📁 Dosya Boyutu
- **2517 satır** - Bu büyük dosya 10 farklı tema visualizer'ı içerir

## 🎨 Dosya İçeriği (Satır Numaraları)

### 1. HTML HEAD & CSS (1-320)
- Meta taglar ve font yüklemeleri
- CSS değişkenleri ve tema renkleri
- Responsive layout stilleri
- UI component stilleri

### 2. HTML BODY - UI Structure (321-410)
- Topbar (logo, saat, meter)
- Canvas sahne alanı
- Track bilgisi ve mod toggle
- Transport kontrolları (play button, sliders)
- Tema seçici chipsleri
- Alt navigasyon

### 3. JAVASCRIPT - Core Setup (411-610)
- Tema metadata ve i18n çevirileri (7 dil: TR, EN, DE, FR, ES, RU, AR, JA, ZH, KO)
- Canvas initialization
- Clock ve UI utilities
- Mode toggle (dark/light)
- Theme switching logic

### 4. AUDIO ENGINE (611-1000)
- Real audio data from Android integration
- Mock audio engine (Web Audio API)
- Audio analysis (FFT, spectral flux, beat detection)
- Synthesizer voices and effects

### 5. VISUALIZER UTILITIES (1001-1128)
- `getBars()` - Frequency bar data
- `getWave()` - Waveform data  
- `analyze()` - Band energy analysis
- `burst()` & `drawSparks()` - Particle system
- `updateMeter()` - Level meter animation

### 6. VISUALIZERS - Draw Functions

#### 6.1 Sumi-e Visualizer (1129-1235)
```javascript
let inkSplats = [];
let sumiDust = null;
function drawSumi(bars, wave, e) { ... }
```
- Japanese ink wash painting style
- Ensō circle with brush strokes
- Ink splatters on beat

#### 6.2 Aurora Visualizer (1236-1328)
```javascript
let auroraStars = null;
let auroraOrbPulse = 0;
function drawAurora(bars, wave, e) { ... }
```
- Northern lights ribbons
- Starfield background
- Silk-like light bands

#### 6.3 Nova Visualizer (1329-1469)
```javascript
let novaStars = null;
let novaParticles = null;
let novaShock = [];
function drawNova(bars, wave, e) { ... }
```
- Expanding plasma core
- Shockwave rings
- Particle explosions

#### 6.4 Mycelium Visualizer (1470-1557)
```javascript
let mycelBranches = null;
let mycelPulses = [];
let mycelSpores = [];
let mycelGrowStart = 0;
function drawMycel(bars, wave, e) { ... }
```
- Growing root network
- Glowing pulses
- Spore particles

#### 6.5 Reef Visualizer (1558-1652)
```javascript
let reefParticles = null;
let reefTentaclePhase = 0;
let reefCompanionPhase = 1.7;
function drawJelly(...) { ... }
function drawReef(bars, wave, e) { ... }
```
- Bioluminescent jellyfish
- Deep ocean particles
- Tentacle animations

#### 6.6 Monsoon Visualizer (1653-1773)
```javascript
let stormBolts = [];
let rainDrops = null;
let cloudSeed = null;
let fogSeed = null;
let splashes = [];
function drawMonsoon(bars, wave, e) { ... }
```
- Lightning bolts
- Rain simulation
- Cloud layers

#### 6.7 Mürekkep Visualizer (1774-1838)
```javascript
let murekkepBlob = null;
let murekkepDrops = [];
function drawMurekkep(bars, wave, e) { ... }
```
- Ink blooming in water
- Golden kintsugi veins
- Organic fluid simulation

#### 6.8 Çöl (Desert) Visualizer (1839-1895)
```javascript
let colDuneSeed = null;
let colParticles = [];
let colSunPulse = 0;
function drawCol(bars, wave, e) { ... }
```
- Sand dunes
- Heat shimmer
- Desert sun

#### 6.9 Divit (Inkwell) Visualizer (1896-2013)
```javascript
let inkVeins = [];
let inkRipples = [];
let inkBlobT = 0;
function drawDivit(bars, wave, e) { ... }
```
- Ink drop in water
- Golden capillaries
- Ripple waves

#### 6.10 Mehtap (Moonlight) Visualizer (2014-2370)
```javascript
window.mehtapState = { ... }
function drawMehtap(bars, wave, e) { ... }
```
- Moon/sun with day/night transition
- Water waves with reflection
- Fishing boat with rigging
- Fish catching animation (bite → pull → leap → reel → release)
- Glints and foam effects

### 7. MAIN ANIMATION LOOP (2371-2450)
```javascript
function frame() {
  // Get audio data
  // Run analysis
  // Call active visualizer
  // Apply glow effects
  requestAnimationFrame(frame);
}
```

### 8. ANDROID BRIDGE API (2451-2517)
```javascript
window.setBoostState(isEnabled)
window.setVolumeFromAndroid(value)
window.setSensitivityFromAndroid(value)
window.setThemeFromAndroid(themeName)
window.setModeFromAndroid(mode)
window.setLanguageFromAndroid(langCode)
window.updateRealAudioData(audioDataJson)
```

## 🛠️ Bakım ve Geliştirme

### Yeni Tema Eklemek İçin:
1. CSS'e yeni tema renk değişkenlerini ekle (satır 25-62)
2. Tema chip'i HTML'e ekle (satır 402-412)
3. i18n çevirilerini ekle (satır 420-570)
4. `draw[ThemeName]` fonksiyonu yaz (satır 1129+ sonrasına ekle)
5. Main loop'ta theme switch'e ekle (satır 2390-2410)

### Visualizer Düzenlemek İçin:
- İlgili `draw` fonksiyonunu yukarıdaki satır numaralarından bul
- Değişikliği sadece o fonksiyon içinde yap
- Diğer fonksiyonları etkileme

### Audio Data Kullanımı:
```javascript
// bars: 48 frequency bands (0-1 normalized)
// wave: 128-point waveform data
// e: {
//   sub, bass, lowMid, mid, highMid, treble,
//   energy, flux, punch, beat, brightness
// }
```

## ⚠️ Önemli Notlar

1. **Dosya çok büyük ama modülerleştirme riskli** - Her visualizer birbirine bağımlı değişkenler kullanıyor
2. **Android bridge kritik** - window.* fonksiyonlarını değiştirme
3. **Canvas context global** - ctx, W, H, DPR tüm fonksiyonlar tarafından kullanılıyor
4. **State variables** - Her visualizer kendi state değişkenlerini dosya başında tanımlıyor

## 🚀 Gelecek İyileştirmeler

Eğer dosya daha da büyürse:
1. Her visualizer'ı ayrı `.js` dosyasına taşı
2. Dynamic import kullan
3. Webpack/Rollup ile bundle et

Ama şu an için bu yapı:
✅ Çalışıyor
✅ Bakımı yapılabilir
✅ Performanslı
✅ Android ile entegre
