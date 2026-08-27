# ✅ Google Play Store Yükleme Kontrol Listesi

## 📦 Hazır Dosyalar

### ✅ AAB Dosyası (HAZIR!)
**Konum:** `app\build\outputs\bundle\release\SoundSTBoost-v1.0.0-release.aab`
**Boyut:** 2.4 MB
**Version Code:** 1
**Version Name:** 1.0.0

### ✅ Uygulama İkonu (HAZIR!)
**Konum:** `play_store_icon_512.png`
**Boyut:** 512 x 512 px

### ✅ Dokümantasyon (HAZIR!)
- `PRIVACY_POLICY.md` - Gizlilik politikası
- `PLAY_STORE_LISTING.md` - Mağaza açıklaması ve anahtar kelimeler
- `BUILD_RELEASE.md` - Detaylı build talimatları

---

## 🚀 Google Play Console Adımları

### 1. Play Console'a Git
https://play.google.com/console

### 2. Yeni Uygulama Oluştur
- **App name:** Sound'ST Boost
- **Default language:** Turkish veya English
- **App or Game:** App
- **Free or Paid:** Free

---

### 3. Store Listing (Mağaza Sayfası)

#### 📝 Temel Bilgiler
```
App Name: Sound'ST Boost - Volume++
Short Description (80 char): Powerful audio booster with 5 stunning themes, equalizer & bass boost effects.
```

#### 📄 Full Description
`PLAY_STORE_LISTING.md` dosyasındaki "Full Description" kısmını kopyala.

#### 🎨 Görseller

**Şu an hazır:**
- ✅ App Icon (512x512): `play_store_icon_512.png`

**Oluşturulması gerekenler:**
- ❌ Feature Graphic (1024x500): Canva veya Photoshop ile oluştur
- ❌ Phone Screenshots (minimum 2): Uygulamayı çalıştırıp ekran görüntüsü al
  - Ana ekran (NEON tema)
  - Equalizer ekranı
  - Tema seçim ekranı
  - Dil seçimi ekranı

#### 📋 Kategoriler
- **Category:** Music & Audio
- **Tags:** volume booster, equalizer, bass boost, audio effects

---

### 4. App Content (Uygulama İçeriği)

#### 🔒 Privacy Policy (ZORUNLU)
1. `PRIVACY_POLICY.md` dosyasını bir yere yükle:
   - GitHub Pages (ücretsiz)
   - Kendi web siten
   - Google Sites (ücretsiz)
2. URL'yi Play Console'a gir

**Örnek GitHub Pages URL:**
```
https://1sthillman.github.io/soundboost/privacy-policy.html
```

#### 📊 Data Safety
Tüm sorulara **"No"** cevabı ver çünkü:
- ✅ Hiçbir veri toplanmıyor
- ✅ Hiçbir veri paylaşılmıyor
- ✅ Tüm ayarlar cihazda kalıyor

#### 🎯 Target Audience and Content
- **Target age:** 13+ (veya Everyone)
- **Content rating:** Questionnaire doldur (Everyone çıkacak)
- **News app:** No
- **COVID-19 contact tracing:** No
- **Data safety declarations:** Yukarıdaki gibi "No data collected"

---

### 5. Release (Sürüm Yükleme)

#### 📤 Production Track
1. **Create new release**
2. **Upload AAB:** `app\build\outputs\bundle\release\SoundSTBoost-v1.0.0-release.aab`
3. **Release name:** 1.0.0
4. **Release notes:** 

```
🎉 İlk Sürüm!

✨ Özellikler:
• 5 benzersiz görsel tema
• Profesyonel ses efektleri (volume boost, equalizer, bass boost)
• 10 dil desteği
• Gerçek zamanlı ses görselleştirici
• Arka plan servisi
• Gizlilik odaklı (veri toplama YOK)

🎵 Sound'ST Boost ile ses deneyiminizi mükemmelleştirin!
```

5. **Review and rollout:** Release'i başlat

---

## ⚙️ İmzalı AAB İstersen (Opsiyonel)

Google Play Console zaten AAB'yi otomatik imzalar ama kendi keystore'unu kullanmak istersen:

### Adım 1: Keystore Oluştur
```bash
CREATE_KEYSTORE.bat
```

Veya manuel:
```bash
set JAVA_HOME=C:\SoundSTBoost\jdk-11.0.25+9
"%JAVA_HOME%\bin\keytool.exe" -genkey -v -keystore soundst-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias soundst-key
```

**Bilgiler:**
- Store password: (en az 6 karakter)
- Key password: (aynısı veya farklı)
- Name: Sound ST Developer
- Organization: Sound ST
- City: Istanbul
- Country: TR

### Adım 2: key.properties Oluştur
Proje kökünde `key.properties` dosyası:
```properties
storePassword=SİFRENİZ
keyPassword=SİFRENİZ
keyAlias=soundst-key
storeFile=soundst-release-key.jks
```

### Adım 3: build.gradle.kts Güncelle
```kotlin
// En üste ekle
import java.util.Properties
import java.io.FileInputStream

val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// signingConfigs içinde güncelle
signingConfigs {
    create("release") {
        if (keystoreProperties["storeFile"] != null) {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
}

// buildTypes içinde güncelle
buildTypes {
    getByName("release") {
        signingConfig = signingConfigs.getByName("release")
        // ... diğer ayarlar
    }
}
```

### Adım 4: Yeniden Build
```bash
gradlew clean bundleRelease
```

---

## ⏱️ İnceleme Süreci

- **İlk yayın:** 1-7 gün sürebilir
- **Güncellemeler:** Birkaç saat içinde onaylanır
- **Otomatik inceleme:** Genelde 24 saat içinde
- **Manuel inceleme:** Nadir, 3-7 gün

---

## 📋 Son Kontrol

- [ ] AAB dosyası hazır (`app\build\outputs\bundle\release\`)
- [ ] App icon hazır (`play_store_icon_512.png`)
- [ ] Privacy Policy online'da yayınlandı
- [ ] Feature graphic oluşturuldu (1024x500)
- [ ] En az 2 ekran görüntüsü alındı
- [ ] Play Console'da uygulama oluşturuldu
- [ ] Store listing dolduruldu
- [ ] Data safety "No data collected" işaretlendi
- [ ] Content rating questionnaire tamamlandı
- [ ] AAB yüklendi
- [ ] Release notes yazıldı
- [ ] Review'a gönderildi

---

## 🎉 Başarılı Yükleme Sonrası

1. **Uygulama URL'i:**
   ```
   https://play.google.com/store/apps/details?id=com.stdev.soundstboost
   ```

2. **Internal Testing Link:** (Google oluşturacak)
   - Test kullanıcıları ekle
   - Erken test yap
   - Sorun varsa düzelt

3. **Production'a Al:**
   - Internal test başarılı olunca
   - "Promote to production" tıkla
   - Yayınlan!

---

## 💡 İpuçları

1. **İlk sürüm için sabırlı ol** - İnceleme süreci uzun olabilir
2. **Test track kullan** - Internal/Closed testing ile önce test et
3. **Release notes'u güncel tut** - Her güncellemede ne değişti yaz
4. **Screenshots'ları güzel çek** - İlk izlenim çok önemli
5. **Keywords optimize et** - ASO (App Store Optimization) için önemli
6. **Kullanıcı yorumlarını oku** - Geri bildirimlere dikkat et

---

**Hazırsın! 🚀 Başarılar dilerim!**
