# Google Play Release Build Instructions

## Adım 1: Keystore Oluştur (İlk Defa)

```bash
# Windows
set JAVA_HOME=C:\SoundSTBoost\jdk-11.0.25+9
"%JAVA_HOME%\bin\keytool.exe" -genkey -v -keystore soundst-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias soundst-key
```

**Sorulan Bilgiler:**
- Keystore password: `[ŞİFRENİZ - EN AZ 6 KARAKTER]`
- Key password: `[AYNI ŞİFRE VEYA FARKLI]`
- First and last name: `Sound ST Developer`
- Organizational unit: `Sound ST Team`
- Organization: `Sound ST`
- City: `Istanbul`
- State: `Turkey`
- Country code: `TR`

**ÖNEMLİ:** Keystore dosyasını ve şifrelerini GÜVENLİ bir yerde sakla!

---

## Adım 2: key.properties Dosyası Oluştur

Proje kök dizininde `key.properties` dosyası oluştur:

```properties
storePassword=YOUR_KEYSTORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=soundst-key
storeFile=soundst-release-key.jks
```

**NOT:** Bu dosyayı `.gitignore`'a ekle (zaten eklendi)

---

## Adım 3: build.gradle.kts'i Güncelle

`app/build.gradle.kts` dosyasının başına ekle:

```kotlin
import java.util.Properties
import java.io.FileInputStream

val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}
```

Ve `signingConfigs` bölümünü güncelle:

```kotlin
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

buildTypes {
    getByName("release") {
        isMinifyEnabled = true
        isShrinkResources = true
        signingConfig = signingConfigs.getByName("release")
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

---

## Adım 4: Release Build Oluştur

### AAB (Android App Bundle) - Play Store İçin Önerilen

```bash
# Windows
gradlew bundleRelease

# Çıktı:
# app\build\outputs\bundle\release\app-release.aab
```

### APK - Direkt Kurulum İçin (Opsiyonel)

```bash
# Windows
gradlew assembleRelease

# Çıktı:
# app\build\outputs\apk\release\app-release.apk
```

---

## Adım 5: Build'i Doğrula

### AAB İçin:
```bash
"%JAVA_HOME%\bin\jarsigner.exe" -verify -verbose -certs app\build\outputs\bundle\release\app-release.aab
```

### APK İçin:
```bash
"%JAVA_HOME%\bin\jarsigner.exe" -verify -verbose -certs app\build\outputs\apk\release\app-release.apk
```

**Başarılı:** "jar verified" mesajı görmeli

---

## Adım 6: Google Play Console'a Yükle

1. **Play Console'a Git:** https://play.google.com/console
2. **Yeni Uygulama Oluştur:**
   - App name: `Sound'ST Boost`
   - Default language: `Turkish` veya `English`
   - App/Game: `App`
   - Free/Paid: `Free`

3. **App Content:**
   - Privacy Policy: `[PRIVACY_POLICY.md içeriğini online'a koy]`
   - Data Safety: `Tüm "No"` (veri toplamıyoruz)
   - Content Rating: Questionnaire doldur (Everyone olmalı)
   - Target Audience: `13+` ve `Everyone`
   - News Apps: `No`
   - COVID-19 Contact Tracing: `No`
   - Data Safety Declarations: Complete

4. **Store Listing:**
   - App name: `Sound'ST Boost - Volume++`
   - Short description: `PLAY_STORE_LISTING.md`'den kopyala
   - Full description: `PLAY_STORE_LISTING.md`'den kopyala
   - App icon: `play_store_icon_512.png`
   - Feature graphic: Photoshop/Canva ile 1024x500 oluştur
   - Screenshots: Uygulamayı telefonda çalıştır ve ekran görüntüleri al

5. **Release:**
   - Production > Create new release
   - Upload: `app-release.aab`
   - Release name: `1.0.0`
   - Release notes: `PLAY_STORE_LISTING.md`'deki "What's New"
   - Review and rollout

---

## Gerekli Görseller

### ✅ Zaten Var:
- App Icon: `play_store_icon_512.png`

### 📸 Oluşturulması Gerekenler:

**1. Feature Graphic (1024 x 500 px):**
   - Uygulamanın temel özelliklerini gösteren banner
   - 5 temayı yan yana göster
   - "Sound'ST Boost" yazısı ve tagline
   
**2. Phone Screenshots (En az 2, maks 8):**
   - Ana ekran (NEON tema, boost aktif)
   - Equalizer ekranı
   - Tema seçim ekranı
   - Dil seçim ekranı
   - OCEAN tema görseli
   - SUNSET tema görseli
   - Settings ekranı
   
**3. Tablet Screenshots (Opsiyonel):**
   - Tablettte daha güzel gözüken temalar
   
**4. Promo Video (Opsiyonel ama Önerilir):**
   - 30-60 saniye
   - Tüm temaları göster
   - Sürükleme ve ayarlama göster
   - Visualizer'ın müzikle çalışmasını göster

---

## Hızlı Checklist

- [ ] Keystore oluşturuldu ve güvenli yerde saklandı
- [ ] key.properties dosyası oluşturuldu
- [ ] build.gradle.kts güncellendi
- [ ] Release AAB build alındı
- [ ] Build doğrulandı (signed)
- [ ] Privacy Policy online'a kondu
- [ ] Feature graphic oluşturuldu
- [ ] Screenshots alındı (en az 2)
- [ ] Play Console'da uygulama oluşturuldu
- [ ] Store listing dolduruldu
- [ ] Data safety declarations tamamlandı
- [ ] Content rating alındı
- [ ] AAB yüklendi ve release oluşturuldu
- [ ] Review'a gönderildi

---

## Önemli Notlar

1. **İlk Yayın:** Google review süreci 1-7 gün sürebilir
2. **Güncellemeler:** Daha hızlı onaylanır (birkaç saat)
3. **Keystore:** Kaybedersen uygulamayı güncelleyemezsin! Yedekle!
4. **Version Code:** Her güncelleme için artır
5. **Minimum SDK:** 24 (Android 7.0) - %97.5 cihaz kapsamı

---

## Faydalı Linkler

- **Play Console:** https://play.google.com/console
- **Play Store Guidelines:** https://play.google.com/about/developer-content-policy/
- **Android App Bundle:** https://developer.android.com/guide/app-bundle
- **Data Safety Form:** https://support.google.com/googleplay/android-developer/answer/10787469
