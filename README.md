# Sound'ST Boost 🎧⚡

Android için neon/DJ temalı, gerçek ses efektleriyle çalışan bir **ses yükseltici**
uygulaması. Ana ses gücü, bas boost, 3D alan (virtualizer) ve 3 bantlı basit
ekolayzer içerir; boost, arka planda (ekran kapalıyken veya başka bir uygulama
kullanırken) da devam eder.

## İçindekiler

1. [Uygulama ne yapıyor, ne yapmıyor (dürüst özet)](#1-uygulama-ne-yapıyor-ne-yapmıyor-dürüst-özet)
2. [Gereksinimler](#2-gereksinimler)
3. [Projeyi Android Studio'da açma](#3-projeyi-android-studioda-açma)
4. [Derleme ve çalıştırma](#4-derleme-ve-çalıştırma)
5. [Proje mimarisi](#5-proje-mimarisi)
6. [İzinler — neden isteniyor](#6-i̇zinler--neden-isteniyor)
7. [Play Store'a yayınlama adımları](#7-play-storea-yayınlama-adımları)
8. [Sorun giderme](#8-sorun-giderme)
9. [Geliştirme fikirleri](#9-geliştirme-fikirleri)

---

## 1) Uygulama ne yapıyor, ne yapmıyor (dürüst özet)

Bunu en başta açıkça yazmak önemli, çünkü Play Store'da yayınlanacak bir ürün
için gerçekçi beklenti önemli:

- Uygulama, Android'in resmi `android.media.audiofx` API'lerini (LoudnessEnhancer,
  BassBoost, Virtualizer, Equalizer) cihazın **global ses çıkışına (session 0)**
  bağlayarak çalışır. Bu, gerçek dünyada yayınlanmış birçok ses yükseltici
  uygulamanın kullandığı, kanıtlanmış bir tekniktir.
- **"Tüm telefonlarda birebir aynı güçte çalışır" diye bir garanti yoktur** —
  hiçbir ses yükseltici uygulama (Play Store'daki hiçbiri dahil) bunu garanti
  edemez, çünkü ses donanımı (DAC, amplifikatör) ve OEM'in ses HAL kısıtlamaları
  cihazdan cihaza değişir. Uygulama bunu telafi etmek için:
  - Her efekti ayrı ayrı `try/catch` ile dener, desteklenmeyen bir efekt sessizce
    devre dışı kalır, **uygulama asla çökmez**.
  - Kazanç 20dB (%200) ile sınırlandırılmıştır — bunun üzerinde ciddi ses
    bozulması ve hoparlör hasarı riski başlar, bu yüzden bilinçli bir üst sınır
    konuldu.
- Uygulama ayrıca donanım ses seviyesini (medya/alarm/çağrı) tek dokunuşla
  maksimuma çıkaran bir kısayol da içerir.

## 2) Gereksinimler

- **Android Studio**: Ladybug (2024.2) veya daha yeni bir sürüm önerilir.
- **JDK 17** (Android Studio genelde kendi JBR'ını kullanır, ayrıca kurmanıza
  gerek yok).
- Test için: Android 7.0 (API 24) veya üstü bir cihaz/emülatör.
- Play Store'a yükleme için: Android 15 (API 35) veya üstü hedefleme şu an
  zorunlu; **31 Ağustos 2026'dan itibaren yeni uygulamalar için Android 16
  (API 36) zorunlu hale geliyor** — proje zaten `compileSdk`/`targetSdk = 36`
  olarak ayarlandı, bu yüzden ek işlem gerekmiyor.

## 3) Projeyi Android Studio'da açma

1. `SoundSTBoost` klasörünü bilgisayarınıza indirin/kopyalayın.
2. Android Studio'yu açın → **File → Open** → `SoundSTBoost` klasörünü seçin.
3. Projede **Gradle wrapper jar dosyası dahil değildir** (bu ortamda internet
   erişimi kısıtlı olduğu için ikili dosya indirilemedi). Android Studio projeyi
   açtığınızda bunu otomatik algılayıp "Gradle wrapper eksik, oluşturulsun mu?"
   şeklinde soracak ya da kendi yerleşik Gradle sürümüyle senkronize edecektir.
   Eğer sormazsa: **Terminal'den** `gradle wrapper --gradle-version 8.11.1`
   komutunu çalıştırmanız yeterlidir (bilgisayarınızda Gradle kuruluysa) — ya da
   Android Studio'nun *Sync Project with Gradle Files* (fil ikonu) düğmesine
   basmanız genelde yeterli olacaktır.
4. İlk senkronizasyon sırasında gerekli tüm bağımlılıklar (Compose, DataStore,
   Navigation vb.) `google()` ve `mavenCentral()` depolarından otomatik inecek.

## 4) Derleme ve çalıştırma

- **Debug çalıştırma**: Üstteki yeşil ▶️ (Run) düğmesine basmanız yeterli.
- **İmzalı sürüm (release) oluşturma**: Aşağıdaki "Play Store'a yayınlama"
  bölümüne bakın.
- Uygulamayı ilk açtığınızda Android 13+ cihazlarda bildirim izni istenecek —
  bu, boost aktifken kalıcı durum bildirimini gösterebilmek için gerekli.

## 5) Proje mimarisi

```
app/src/main/java/com/stdev/soundstboost/
├── MainActivity.kt            → Compose host, navigasyon, bildirim izni
├── MainViewModel.kt           → Tüm ayarların tek doğruluk kaynağı (state)
├── SoundBoostApplication.kt   → Bildirim kanalı oluşturma
├── audio/
│   ├── AudioEffectsManager.kt → LoudnessEnhancer/BassBoost/Virtualizer/EQ motoru
│   └── SystemVolumeController.kt → Donanım ses seviyesi kontrolü
├── data/
│   └── BoostPreferences.kt    → DataStore ile kalıcı ayar saklama
├── service/
│   ├── BoostForegroundService.kt → Arka planda çalışan foreground servis
│   └── BootReceiver.kt        → Yeniden başlatma sonrası otomatik devam
└── ui/
    ├── theme/                 → Neon renk paleti, tipografi
    ├── components/            → NeonSlider, BoostDial, EqualizerVisualizer, vb.
    └── screens/                → HomeScreen (ana panel), SettingsScreen
```

**Akış**: Kullanıcı bir slider'ı hareket ettirdiğinde `MainViewModel` state'i
anında günceller (akıcı sürükleme için), arka planda `DataStore`'a kaydeder ve
eğer boost aktifse çalışan servise canlı güncelleme gönderir. Güç düğmesine
basıldığında `BoostForegroundService` başlatılır/durdurulur; servis, efektleri
`AudioEffectsManager` üzerinden global ses oturumuna uygular.

## 6) İzinler — neden isteniyor

Sadece **gerçekten gerekli olan** izinler istenir; mikrofon/ses kaydı izni
**kasıtlı olarak eklenmedi** (ekolayzer görselleştirmesi tamamen dekoratif
animasyondur, gerçek ses verisi okumaz) — bu hem gizliliği korur hem de Play
Store incelemesini kolaylaştırır.

| İzin | Neden | Kullanıcıya sorulur mu? |
|---|---|---|
| `MODIFY_AUDIO_SETTINGS` | Ses efektlerini uygulamak için | Hayır (normal izin) |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Arka planda boost'u sürdürmek için | Hayır (manifest izni) |
| `POST_NOTIFICATIONS` | Boost aktifken kalıcı bildirim göstermek için | Evet (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | "Yeniden başlayınca otomatik başlat" ayarı açıksa | Hayır (manifest izni) |

## 7) Play Store'a yayınlama adımları

1. **İmzalama anahtarı oluşturun**: Android Studio → *Build → Generate Signed
   App Bundle/APK* → *Android App Bundle* seçin → yeni bir keystore oluşturun
   (şifreyi ve dosyayı güvenli bir yerde saklayın, kaybederseniz uygulamayı bir
   daha güncelleyemezsiniz).
2. **`applicationId`'yi kontrol edin**: Şu an `com.stdev.soundstboost` olarak
   ayarlı. Play Console'da başka biri bu ismi almadıysa değiştirmenize gerek
   yok; isterseniz kendi geliştirici imzanıza göre değiştirebilirsiniz
   (`app/build.gradle.kts` içinde `applicationId`).
3. **Release .aab dosyasını üretin** (yukarıdaki sihirbazla) — Play Console
   yalnızca **App Bundle (.aab)** formatını kabul eder.
4. **Play Console'da yeni uygulama oluşturun** (developer.android.com/console,
   tek seferlik kayıt ücreti gerekir).
5. **Store listing (mağaza sayfası) için gerekenler**:
   - Uygulama ikonu: proje kökünde `play_store_icon_512.png` (512×512) zaten
     hazır — bunu doğrudan kullanabilirsiniz.
   - *Feature graphic* (1024×500): Bu depoda hazır değil, Play Console'un
     kendi aracıyla ya da bir tasarım aracıyla (Canva vb.) neon temaya uygun
     bir banner hazırlamanız gerekir.
   - En az 2 ekran görüntüsü: Uygulamayı bir cihaz/emülatörde çalıştırıp ekran
     görüntüsü alabilirsiniz.
   - Kısa açıklama (80 karakter) ve tam açıklama metni.
6. **Gizlilik politikası URL'si zorunludur** — Play Console, foreground servis
   ve bildirim kullanan her uygulamadan bunu ister. Bu depodaki
   `PRIVACY_POLICY_TEMPLATE.md` dosyasını doldurup kendi web sitenizde
   (veya ücretsiz bir GitHub Pages sayfasında) yayınlayıp linkini girin.
7. **Data safety (veri güvenliği) formu**: Uygulama hiçbir kişisel veri
   toplamadığı, sunucuya göndermediği için bu formda "Veri toplanmıyor"
   seçeneğini işaretleyebilirsiniz (DataStore'daki ayarlar sadece cihazda
   yerel olarak tutulur).
8. **İçerik derecelendirmesi anketini** doldurun (bu tür bir uygulama için
   genelde "Herkes" / 3+ çıkar).
9. Sürümü **Test (Internal testing)** kanalına yükleyip birkaç cihazda
   deneyip sorun yoksa **Production**'a terfi ettirin.

## 8) Sorun giderme

- **"Gradle sync failed" / wrapper bulunamadı** → Adım 3'teki notu uygulayın;
  alternatif olarak Android Studio'da *File → Settings → Build Tools → Gradle*
  kısmından "Use Gradle from: 'gradle-wrapper.properties' file" yerine
  geçici olarak yerel bir Gradle kurulumu seçebilirsiniz.
- **Emülatörde ses efekti duyulmuyor** → Emülatörlerin sanal ses sürücüleri
  bazı `audiofx` efektlerini desteklemez; gerçek bir cihazda test etmeniz
  önerilir.
- **Bildirim görünmüyor (Android 13+)** → Ayarlar → Uygulamalar → Sound'ST
  Boost → Bildirimler'den izni manuel açtığınızdan emin olun.

## 9) Geliştirme fikirleri

- Gerçek ses tepkili görselleştirme (bunun için `RECORD_AUDIO` izni ve
  `Visualizer` API'si gerekir — bilinçli olarak bu sürümde eklenmedi).
- Uygulama başına farklı boost profilleri.
- Ekran kilit widget'ı / hızlı ayarlar (Quick Settings) döşeme.
- Çoklu dil desteği (`values-en` klasörü hazır, çeviri eklemeniz yeterli).

---

Kolay gelsin! 🎛️
#   s o u n d b o o s t  
 