# 16 KB Page Size Uyumluluk Güncellemesi

## Tarih: 17 Eylül 2026

## Yapılan Değişiklikler

### 1. Android Gradle Plugin (AGP) Güncellemesi
- **Önceki Versiyon:** 8.1.4
- **Yeni Versiyon:** 8.7.3
- **Sebep:** 16 KB page size desteği için minimum AGP 8.5.1 gerekli

### 2. Gradle Wrapper Güncellemesi
- **Önceki Versiyon:** 8.0
- **Yeni Versiyon:** 8.9
- **Sebep:** AGP 8.7.3 için Gradle 8.9+ gerekli

### 3. NDK Yapılandırması
```kotlin
ndkVersion = "28.0.12674087"  // 16 KB page size support
```
NDK r28, 16 KB memory alignment'ı otomatik olarak destekler.

### 4. Kütüphane Güncellemeleri
- `androidx.core:core-ktx`: 1.13.1 → 1.15.0
- `androidx.core:core-splashscreen`: 1.0.1 → 1.2.0-alpha02

### 5. Proguard Kuralları
Kotlinx Serialization UUID uyumluluğu için kurallar eklendi:
```proguard
-dontwarn kotlin.uuid.**
-keep class kotlin.uuid.** { *; }
```

### 6. Gradle Properties
Zaten mevcut olan flag korundu:
```properties
android.experimental.enable16KbPageSize=true
```

## Build Sonuçları

### AAB Dosyası
- **Konum:** `app/build/outputs/bundle/release/SoundSTBoost-v1.4.4-release.aab`
- **Boyut:** 5.98 MB
- **Versiyon:** 1.4.4 (versionCode: 34)
- **Oluşturulma:** 17 Eylül 2026, 01:37

## Google Play Console'a Yükleme

Bu AAB dosyası artık:
✅ Android 15 (API 35) ve üzeri cihazları destekliyor
✅ 16 KB bellek sayfası boyutlarıyla uyumlu
✅ Google Play Console'ın "16 KB page size" gereksinimini karşılıyor

### Yükleme Adımları:
1. Google Play Console'a giriş yapın
2. **Üretim > Sürüm oluştur** veya **Test > Kapalı Test > Sürüm oluştur**'a gidin
3. `SoundSTBoost-v1.4.4-release.aab` dosyasını yükleyin
4. Sürüm notlarını ekleyin
5. İncelemeye gönderin

## Teknik Detaylar

### AGP 8.7.3'ün 16 KB Desteği
- Yerel kütüphaneler (.so) otomatik olarak 16 KB hizalamasıyla derlenir
- NDK r28 ile birlikte ELF segmentleri `-Wl,-z,max-page-size=16384` flag'i ile derlenir
- Android 15+ cihazlarda performans optimizasyonu sağlar

### Uyumluluk
- **Minimum SDK:** 24 (Android 7.0)
- **Target SDK:** 36 (Android 15)
- **Compile SDK:** 36

## Referanslar
- [Android 16 KB Page Size Guide](https://developer.android.com/guide/practices/page-sizes)
- [AGP 8.7 Release Notes](https://developer.android.com/build/releases/gradle-plugin)
- [NDK r28 Release Notes](https://developer.android.com/ndk/downloads/revision_history)
