# Görev: Google Play Console uyarılarını projeyi bozmadan, güvenli şekilde düzeltmek

## Bağlam
Bu proje Google Play'de yayında olan bir Android "ses yükseltici" (volume booster) uygulaması. Yayındaki sürüm: versionCode 37, versionName 1.4.7. Uygulama canlıda çalıştığı için EN ÖNEMLİ öncelik hiçbir şeyi bozmamak.

Google Play Console 3 öneri gönderdi:

1) "Uçtan uca ekran tüm kullanıcılara gösterilmeyebilir": SDK 35'i hedefleyen uygulamalar Android 15+ cihazlarda varsayılan olarak edge-to-edge gösterilir. Uygulamanın window insets'i doğru işlemesi ve edge-to-edge testi yapılması gerekiyor. Kotlin için `enableEdgeToEdge()`, Java için `EdgeToEdge.enable()` öneriliyor.

2) "Uygulamanız, uçtan uca ekran için desteği sonlandırılmış API'ler veya parametreler kullanıyor": Kullanılan deprecated API'ler: `android.view.Window.setStatusBarColor` ve `android.view.Window.setNavigationBarColor`. Play Console bunların başladığı yer olarak `androidx.core.graphics.drawable.IconCompat.toIcon` gösteriyor. Bu bir kütüphane sınıfı olduğu için çağrı bizim kodumuzdan değil, bir bağımlılıktan gelebilir. Ayrıca R8/obfuscation yüzünden bu sınıf adı yanıltıcı da olabilir. Bu yüzden hem kendi kodumuzu hem bağımlılıkları incelemelisin.

3) "R8 optimizasyonu ile uygulamanızın belleğini ve performansını artırın": R8 yapılandırması bellek kullanımını artırıyor ve performansı düşürüyor olabilir. Önerilen çözüm: Android Gradle Plugin'i (AGP) 9.0 veya üstüne yükseltmek.

## ALTIN KURALLAR (her zaman geçerli)
1. Hiçbir dosyayı, klasörü, sınıfı, fonksiyonu, resource'u, izni (permission), servisi veya receiver'ı SİLME. Silmen gerektiğini düşünürsen DUR ve bana sor. Kullanımdan kaldırılmış bir çağrıyı yenisiyle DEĞİŞTİRMEK serbest, ama neyi neyle değiştirdiğini raporla.
2. Ses yükseltme mantığına (AudioEffect/LoudnessEnhancer, AudioManager, foreground service, bildirimler, ses işleme vb.) KESİNLİKLE dokunma. Bu görev sadece sistem çubukları (status bar / navigation bar), ekran kenarı boşlukları (insets) ve Gradle/R8 yapılandırmasıyla ilgili.
3. applicationId, namespace, signing config, versionCode, versionName, minSdk, targetSdk, compileSdk değerlerini ben açıkça söylemeden DEĞİŞTİRME. Bir değişiklik gerekiyorsa gerekçesiyle bana sor.
4. Yeni kütüphane ekleme ve mevcut kütüphane sürümlerini yükseltme işlerini önce bana öner (hangi kütüphane, eski sürüm → yeni sürüm, riskleri), onayımı aldıktan sonra yap.
5. Büyük refactor yapma. Minimum değişiklik (minimum diff) kuralı geçerli. Kod stilini koru, ilgisiz dosyalarda formatlama/import düzenlemesi yapma.
6. Git kuralları:
   - Başlamadan önce `git status` çalıştır. Commit edilmemiş değişiklik varsa DUR ve bana söyle. Git yoksa da DUR ve bana söyle.
   - Her aşama için ayrı branch aç, küçük ve anlamlı commit'ler at.
   - Geri dönüşü zor komutları ASLA çalıştırma: `git reset --hard`, `git clean`, `git push --force`, `rm -rf`, `git checkout -- .` gibi.
7. Emin olmadığın bir şeyi tahmin etme, bana sor. İnternet erişimin varsa resmi dokümanlara (developer.android.com) bak ve sürüm bilgilerini oradan doğrula.
8. Her aşamadan sonra kısa rapor ver: hangi dosyalar değişti (dosya + neden), neyi bilerek değiştirmedin, ben nasıl test edebilirim.
9. Bir build hatasını çözmek için ilgisiz dosyaları değiştirmeye, kuralları kapatmaya veya yapılandırmayı silmeye başlama. Hata beklenmedikse DUR ve hatayı bana raporla.

## AŞAMA 0: Sadece inceleme (HİÇBİR DOSYAYI DEĞİŞTİRME)
Şunları incele ve rapor et:
- Proje yapısı: modüller, build dosyaları Kotlin DSL mi Groovy mi, version catalog (libs.versions.toml) var mı.
- Sürümler: AGP, Gradle wrapper, Kotlin, JDK; compileSdk / targetSdk / minSdk.
- UI teknolojisi: Jetpack Compose mu, XML/View mu, karışık mı. Tüm Activity, Dialog, BottomSheet, PopupWindow, splash ve tema dosyalarının listesi.
- Şu ifadelerin projedeki TÜM kullanımları (dosya:satır ile): `setStatusBarColor`, `setNavigationBarColor`, `statusBarColor`, `navigationBarColor`, `windowTranslucentStatus`, `windowTranslucentNavigation`, `FLAG_TRANSLUCENT_STATUS`, `FLAG_TRANSLUCENT_NAVIGATION`, `FLAG_LAYOUT_NO_LIMITS`, `SYSTEM_UI_FLAG_`, `systemUiVisibility`, `fitsSystemWindows`, `setDecorFitsSystemWindows`, `enableEdgeToEdge`, `windowOptOutEdgeToEdgeEnforcement`, `WindowInsets`.
- Bağımlılıklar ve sürümleri: androidx.core, androidx.activity, appcompat, material, Compose BOM, reklam/analitik/üçüncü taraf SDK'lar. (`IconCompat` androidx.core içindedir. Deprecated çağrılar kodumuzda yoksa kaynak büyük olasılıkla bir kütüphanedir. `./gradlew :app:dependencies` ile ilgili kütüphaneleri tespit etmeye çalış.)
- Release yapılandırması: isMinifyEnabled / minifyEnabled, shrinkResources, proguardFiles (hangi default dosya: `proguard-android.txt` mi `proguard-android-optimize.txt` mi), proguard-rules.pro içeriği (özellikle `-dontoptimize` var mı), gradle.properties içindeki android.* / R8 ile ilgili satırlar, kotlin-android plugin kullanımı, kapt/ksp kullanımı, uygulanan tüm Gradle plugin'leri (google-services, crashlytics, hilt vb.).
- Baseline: Hiçbir şeyi değiştirmeden `./gradlew assembleDebug` çalışıyor mu? (Release build imza gerektiriyorsa atla ve bana söyle.)

Raporu bitirince DUR ve ONAYIMI BEKLE. Onay almadan Aşama 1'e geçme.

## AŞAMA 1: Edge-to-edge + deprecated API'ler (Sorun 1 ve 2)
Branch: `fix/edge-to-edge`

a) Kendi kodumuzdaki deprecated çağrılar:
- `setStatusBarColor` / `setNavigationBarColor` Android 15+'ta artık etkili değil. Çağrıları yeni yaklaşımla değiştir: renkli görünüm isteniyorsa o rengi header/toolbar/kök arka planına ver ve sistem çubuğu alanını inset ile doldur.
- Status/navigation bar ikon renkleri (açık/koyu) için `WindowCompat.getInsetsController(window, window.decorView)` üzerinden `isAppearanceLightStatusBars` / `isAppearanceLightNavigationBars` kullan. Eski `SYSTEM_UI_FLAG_LIGHT_STATUS_BAR` gibi bayrakları buna çevir.
- XML temalarındaki `android:statusBarColor` / `android:navigationBarColor` öğelerini de raporla. Bunlar Android 15+'ta etkisizdir, bana sormadan silme.

b) Edge-to-edge desteği:
- Her Activity'de `enableEdgeToEdge()` çağır: `super.onCreate()` ÇAĞRISINDAN ÖNCE, `setContent` / `setContentView`'dan önce. Bunun için androidx.activity en az 1.8.0 olmalı. Sürüm düşükse güncellemeyi bana öner.
- Compose ise: Scaffold `innerPadding` değerlerinin içeriğe uygulandığını doğrula. Gerekli yerlerde `WindowInsets.safeDrawing` / `systemBars` / `ime` kullan. Material3 TopAppBar/NavigationBar insets'i kendisi işler, bunu varsayma, doğrula.
- XML/View ise: kök view'a `ViewCompat.setOnApplyWindowInsetsListener` ile `WindowInsetsCompat.Type.systemBars()` ve `displayCutout()` için padding/margin uygula. Altta kalan buton, slider, reklam banner'ı gibi öğelerin navigation bar'ın altında kalmadığından emin ol. Kaydırılabilir listelerde `clipToPadding=false` kullan.
- Tüm Dialog, BottomSheet, PopupWindow, splash ekranı ve diğer tüm Activity'leri tek tek kontrol et. Sadece ana ekranı yapıp bırakma.
- `windowOptOutEdgeToEdgeEnforcement` ile sorunu GEÇİŞTİRME. Bu geçici bir yöntem ve Android 16 / targetSdk 36'da çalışmıyor.

c) Kaynak kütüphaneyse:
- `setStatusBarColor/setNavigationBarColor` çağrısı kodumuzda yoksa ve bir bağımlılıktan geliyorsa, o kütüphanenin güncel stabil sürümünde düzeltilip düzeltilmediğini araştır. Önerini bana sun (kütüphane, eski → yeni sürüm, riskler) ve onay bekle. Kütüphane kodunu kopyalayıp değiştirme.

d) Doğrulama:
- `./gradlew assembleDebug` başarılı olmalı. Lint/testler varsa çalıştır, sonuçları raporla.
- Bana şu manuel test listesini ver: Android 15 ve 16 emülatör/cihazda, hem gesture hem 3-button navigation, açık/koyu tema, dikey/yatay, çentikli ekran. Her ekranda üst/alt öğeler çubukların altında kalmamalı.

Raporla ve ONAY BEKLE.

## AŞAMA 2: R8 optimizasyonu (Sorun 3)
Aşama 1 commit'lenmiş ve ben test edip onayladıktan sonra başla.

2a) Güvenli ilk adım. Branch: `chore/r8-optimize`
- Release build type'ta hangi default proguard dosyası kullanılıyor kontrol et. `proguard-android.txt` kullanılıyorsa `proguard-android-optimize.txt` ile değiştirmeyi öner. Bu, R8 optimizasyonlarını gerçekten açar, bu yüzden aşağıdaki release testi şart.
- `isMinifyEnabled` şu an false ise, benim onayım olmadan true yapma.
- proguard-rules.pro veya gradle.properties içinde optimizasyonu kapatan satırlar (ör. `-dontoptimize`) varsa bana göster, silme.

2b) AGP 9.x yükseltmesi. Ayrı branch: `chore/agp-9`
- Önce SADECE PLAN çıkar, uygulama. Plan şunları içersin: mevcut AGP → hedef AGP (güncel stabil 9.x, sürümü doğrula), gerekli Gradle sürümü (AGP 9.0 için en az 9.1 bekleniyor, doğrula), JDK gereksinimi (17+ bekleniyor, doğrula), compileSdk gereksinimi varsa, ve aşağıdaki riskler:
  - AGP 9'da Kotlin desteği built-in. `org.jetbrains.kotlin.android` plugin'i kaldırılmalı.
  - kapt kullanılıyorsa KSP'ye veya `com.android.legacy-kapt`'a geçiş gerekebilir.
  - `proguard-android.txt` artık desteklenmiyor (kullanılıyorsa build hata verir).
  - Üçüncü taraf Gradle plugin'lerinin AGP 9 uyumluluğu (google-services, crashlytics, hilt vb.).
  - Kaldırılan/değişen DSL ve gradle.properties flag'leri.
- Planı bana sun ve ONAYIMI BEKLE. Alternatif olarak Android Studio'daki AGP Upgrade Assistant (Tools > AGP Upgrade Assistant) kullanılmasını da değerlendir ve öner.
- Onaydan sonra adım adım uygula, her adımdan sonra build al. Beklenmedik hata alırsan DUR ve raporla.

2c) R8 sonrası release testi:
- `./gradlew assembleRelease` (veya `bundleRelease`) başarılı olmalı ve mapping dosyası oluşmalı. İmza gerekiyorsa bana söyle.
- Bana release APK'yı test etmem için kontrol listesi ver: ses yükseltme, foreground service/bildirim, ayarlar, reklamlar, uygulama yeniden başlatma vb.
- R8 yüzünden bir sorun çıkarsa optimizasyonu GLOBAL olarak kapatma. Spesifik `-keep` kuralı ekle ve nedenini açıkla.

## AŞAMA 3: Son rapor
- Değişen tüm dosyaların listesi ve her değişikliğin nedeni.
- 3 sorunun her biri için durum: çözüldü / kısmen / onay bekliyor.
- Play Console'a yüklemeden önce benim yapmam gerekenler (versionCode artırma, internal testing vb.).

## ŞİMDİ YAPACAĞIN TEK ŞEY
Sadece AŞAMA 0'ı yap (hiçbir dosyayı değiştirmeden) ve raporla. Onayımı almadan Aşama 1'e geçme.