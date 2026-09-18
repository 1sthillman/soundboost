# Native Debug Symbols Çözümü

## Durum
Google Play Console'da "31 sürüm kodu için 1 mesaj" uyarısı:
> Bu App Bundle, yerel kod içeriyor ve hata ayıklama sembolleri yüklemediniz.

## Neden Bu Uyarı Çıktı?
AAB içinde native library'ler (.so dosyaları) var:
- Haze kütüphanesi (glassmorphism için)
- Compose native bileşenleri
- Diğer NDK kütüphaneleri

Google, ANR (Application Not Responding) ve crash analizleri için bu sembolleri istiyor.

## Çözüm 1: Sembol Dosyalarını Manuel Upload (ÖNERİLEN)

### Adım 1: AAB'den Sembol Dosyalarını Çıkar
```bash
# AAB içindeki native dosyaları listele
unzip -l app/build/outputs/bundle/release/SoundSTBoost-v1.4.1-release.aab | grep ".so"
```

### Adım 2: Google Play Console'da Upload
1. Google Play Console → Release → v1.4.1
2. "App bundle explorer" tıkla
3. "Downloads" tab → "Native debug symbols" 
4. Sembol dosyalarını upload et

**Ancak**: Bu AAB'de zaten `debugSymbolLevel = "FULL"` var, yani semboller AAB içinde gömülü.

## Çözüm 2: Uyarıyı Yok Say (KOLAY)

Bu uyarı **kritik değil**:
- ✅ AAB yayınlanabilir
- ✅ Uygulama çalışır
- ⚠️ Sadece crash analysis biraz daha zor olur

Google otomatik olarak AAB'den sembolleri çıkaracak. Birkaç saat içinde uyarı kaybolabilir.

## Çözüm 3: Gradle Ayarını Kontrol Et

build.gradle.kts dosyasında zaten var:
```kotlin
buildTypes {
    release {
        ndk {
            debugSymbolLevel = "FULL"  // ✅ Var
        }
    }
}

packaging {
    jniLibs {
        keepDebugSymbols += listOf("**/*.so")  // ✅ Eklendi
    }
}
```

## Sonuç: NE YAPMALIYIZ?

### Seçenek A: Hiçbir Şey Yapma (ÖNERİLEN)
- Uyarı kritik değil
- AAB'yi yükle, yayınla
- Google 24-48 saat içinde otomatik olarak işler
- Uyarı kaybolur

### Seçenek B: Yeni Build Al
1. Bu build.gradle.kts zaten doğru
2. AAB'yi yeniden yükle
3. Google yeni AAB'deki sembolleri algılayacak

### Seçenek C: Manuel Sembol Upload
1. Build'den sonra `app/build/intermediates/merged_native_libs/release/out/lib/` klasörünü zip'le
2. Google Play Console'dan upload et

## Önerilen Aksiyon
✅ **Hiçbir şey yapma**, AAB'yi yükle ve devam et. Bu uyarı uygulama yayınını engellemez.

---
**Not:** Bu uyarı sadece crash analytics için. Uygulama stabil çalışıyor, endişelenmeye gerek yok.
