# WebView Integration - awwardstheme.html

## 📱 Genel Bakış

`awwardstheme.html` artık Android uygulamasının ana sayfası olarak tamamen entegre edilmiştir. HTML, Kotlin ve JavaScript arasında iki yönlü köprü kurulmuştur.

## 🎨 Özellikler

### HTML Sayfası
- **6 Art-Directed Tema**: Sumi-e, Kutup Şafağı, Nova, Miselyum, Derin Işıltı, Muson
- **Gerçek Web Audio API**: Canlı ses analizi ve visualizasyon
- **Canvas Rendering**: Her tema için özel görsel efektler
- **Responsive Design**: Telefon ekranına özel tasarım

### Kotlin-JavaScript Köprüsü

#### JavaScript → Kotlin (HTML'den Android'e)
```javascript
// Tema değişikliği
AndroidBridge.changeTheme("sumi")

// Ses seviyesi değişikliği
AndroidBridge.onVolumeChanged(120)

// Hassasiyet değişikliği
AndroidBridge.onSensitivityChanged(75)

// Boost toggle
AndroidBridge.toggleBoost()

// Navigasyon
AndroidBridge.navigateToSettings()
AndroidBridge.navigateToEqualizer()
```

#### Kotlin → JavaScript (Android'den HTML'e)
```kotlin
// Ses seviyelerini güncelleme
webView?.evaluateJavascript(
    "if(window.updateAudioLevels) { window.updateAudioLevels([...levels]); }",
    null
)

// Boost durumu
webView?.evaluateJavascript(
    "if(window.setBoostState) { window.setBoostState(true); }",
    null
)

// Tema değiştirme
webView?.evaluateJavascript(
    "if(window.setThemeFromAndroid) { window.setThemeFromAndroid('nova'); }",
    null
)
```

## 📁 Dosya Yapısı

```
app/src/main/
├── assets/
│   └── awwardstheme.html          # Ana HTML sayfası (WebView'de yüklenir)
├── java/com/soundboost/
│   └── ui/
│       ├── components/
│       │   └── WebViewHomeScreen.kt   # WebView Composable + JavaScript Bridge
│       ├── screens/
│       │   └── RezonansHomeScreen.kt  # Eski Compose UI (yedek)
│       └── theme/
│           └── AppTheme.kt        # 6 tema renk şeması
└── MainActivity.kt                # WebView entegrasyonu
```

## 🔄 State Senkronizasyonu

### Android State → HTML
- `masterGainPercent` → boost slider value
- `sensitivity` → sens slider value
- `isBoostEnabled` → play/pause button state
- `theme` → active theme chip

### HTML State → Android
- Boost slider → `onMasterGainChanged()`
- Sens slider → `onSensitivityChanged()`
- Play button → `toggleBoost()`
- Theme chip → `onThemeChanged()`

## 🎯 Temalar

| Tema | Renk 1 | Renk 2 | Açıklama |
|------|--------|--------|----------|
| **Sumi** | `#c1442c` | `#e9e2d0` | Japon mürekkep sanatı |
| **Aurora** | `#4fd8b0` | `#8a6bff` | Kutup ışıkları |
| **Nova** | `#ff3d7a` | `#ffcf6b` | Yıldız patlaması |
| **Mycel** | `#6dffb0` | `#ffd58a` | Biyolüminesans ağ |
| **Reef** | `#12e0bd` | `#ff6bcf` | Derin deniz ışıkları |
| **Monsoon** | `#9cc2ff` | `#b48bff` | Fırtına ve şimşek |

## 🚀 Kullanım

1. **WebView Yükleme**: `file:///android_asset/awwardstheme.html`
2. **JavaScript Enabled**: WebView settings'de açık
3. **DOM Storage**: Enabled (tema ve state için)
4. **JavaScript Interface**: `AndroidBridge` adıyla register edilmiş

## 🔧 Geliştirme

### HTML Güncelleme
1. `.agents/awwardstheme.html` dosyasını düzenle
2. `cp .agents/awwardstheme.html app/src/main/assets/awwardstheme.html`
3. Build ve test

### Yeni JavaScript Bridge Metodu Ekleme
1. `WebViewBridge` class'ına yeni metod ekle (@JavascriptInterface)
2. HTML'de `AndroidBridge.yeniMetod()` çağrısı ekle
3. Test

### Yeni Tema Ekleme
1. `AppTheme` enum'una ekle
2. `getThemeColors()` fonksiyonuna renk şeması ekle
3. HTML'de `THEME_META` ve CSS değişkenlerini ekle
4. `WebViewBridge`'de tema mapping'i güncelle

## ✅ Test Edilenler

- [x] Tema değiştirme (HTML → Kotlin → HTML)
- [x] Ses seviyesi değiştirme (bidirectional)
- [x] Hassasiyet değiştirme (bidirectional)
- [x] Play/Pause toggle
- [x] Navigasyon butonları (Settings, Equalizer)
- [x] Canvas visualizasyon
- [x] Responsive design

## 📱 Ekran Görüntüleri

HTML sayfası telefon mockup içinde tasarlanmıştır:
- **Notch**: Üst notch simülasyonu
- **Phone Frame**: Rounded corners ve gölgeleme
- **Screen**: 390px max-width, optimal mobil görünüm

## 🎨 Tasarım Detayları

- **Typography**: Fraunces (display), Space Grotesk (UI)
- **Animations**: Smooth transitions (1.1s)
- **Effects**: Grain texture, vignette, sheen overlay
- **Audio Meter**: 5-bar realtime visualization
- **Clock**: HH:mm format, Turkish locale

## 🔒 Güvenlik

- JavaScript enabled (gerekli)
- File access enabled (assets için)
- DOM storage enabled (state için)
- Harici URL yüklemesi yok (sadece local asset)

## 📝 Notlar

- HTML Web Audio API kullanır (gerçek ses üretimi)
- Canvas rendering her frame (~60fps)
- FFT analysis gerçek zamanlı
- Theme transitions yumuşak (1.1s ease)
- Mobile-first responsive tasarım

---

**Son Güncelleme**: 2026-09-07  
**Versiyon**: 1.1.0  
**Entegrasyon**: Tam WebView + JavaScript Bridge
