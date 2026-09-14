# Native Debug Symbols - Google Play Console

## ✅ YAPILDI (v1.3.7 Build 25)

### Değişiklik
`app/build.gradle.kts` dosyasına native debug symbols desteği eklendi:

```kotlin
buildTypes {
    getByName("release") {
        // ... other configs ...
        ndk {
            debugSymbolLevel = "FULL"
        }
    }
}
```

### Sonuç
- **AAB Dosyası:** `app/build/outputs/bundle/release/SoundSTBoost-v1.3.7-release.aab`
- **Boyut:** 3.73 MB
- **Native Debug Symbols:** AAB içinde gömülü (otomatik)

### Google Play Console'a Yükleme

1. **Yeni AAB'yi yükleyin:**
   - Eski AAB'yi silin
   - `SoundSTBoost-v1.3.7-release.aab` dosyasını yükleyin

2. **Semboller otomatik işlenir:**
   - Google Play Console AAB'den native sembolleri çıkarır
   - Uyarı mesajı kaybolacak
   - ANR/Crash raporlarında detaylı stack trace görünür

3. **Onay:**
   - "25 SÜRÜM KODU İÇİN 1 MESAJ" uyarısı kaybolmalı
   - "Önizle ve onayla" butonuna basabilirsiniz

### Teknik Detaylar

**debugSymbolLevel = "FULL"** ne yapar?
- Native code (C/C++) için tam debug bilgisi ekler
- `.so` kütüphane dosyalarının sembolleri dahil edilir
- Crash/ANR analizinde fonksiyon isimleri ve satır numaraları görünür

**Bu değişiklik neden gerekliydi?**
- Android uygulamaları bazen native kütüphaneler kullanır
- Google Play, crash raporlamada daha iyi analiz için sembolleri ister
- Visualizer API gibi özelliklerde native kod çağrıları olabilir

### Önemli Not
Bu değişiklik **sadece release build'de aktif**. Debug build'lerde zaten tam semboller vardır.

---

**Hazırlandı:** $(Get-Date -Format "yyyy-MM-dd HH:mm")
**Build:** v1.3.7 (25)
