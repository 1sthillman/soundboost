Raporun için teşekkürler. AŞAMA 1'e geçmene ONAY veriyorum (branch: fix/edge-to-edge), şu düzeltmelerle:

DÜZELTMELER
- "Kotlin plugin çakışması YOK" tespiti YANLIŞ. AGP 9'da Kotlin desteği built-in; `org.jetbrains.kotlin.android` plugin'i AGP 9'da kaldırılmak zorunda. Bunu Aşama 2'deki AGP 9 planına yaz. Aynı planda `android.suppressUnsupportedCompileSdk=36` satırını da değerlendir (AGP 8.7.3 compileSdk 36'yı resmen desteklemiyor olabilir; resmi dokümandan doğrula).
- Rapordaki "güncel/en güncel" işaretlerine (Compose BOM, core-ktx vb.) güvenmiyorum. Bu görevde HİÇBİR bağımlılığı güncelleme (core-splashscreen dahil).
- "IconCompat.toIcon kaynağı accompanist olabilir" bir varsayım, doğrulanmadı. Play Console'daki sınıf adı yanıltıcı olabilir. Asıl şüpheli MainActivity'deki çağrılar. Varsayımı kesin bilgi gibi raporlama.

AŞAMA 1 TALİMATLARI
1. MainActivity: `enableEdgeToEdge()` ekle (super.onCreate'tan önce). Deprecated `window.statusBarColor` / `navigationBarColor` bloğunu kaldır.
2. Görünüm aynı kalmalı. Eski kod API 35 altında navigationBarColor'ı themeColors.background yapıyordu. Artık uygulamanın arka planı (kök Surface/Box background) sistem çubuklarının arkasına kadar uzanmalı, sadece Scaffold padding içinde kalmamalı. Bunu doğrula.
3. İkon renklerini ayarlayan mevcut `WindowCompat.getInsetsController` kodunu (satır ~199) ve `setDecorFitsSystemWindows(false)` satırını SİLME. Uygulamanın kendi teması (sistem temasından bağımsız olabilir) açık/koyu iken ikon renklerinin doğru kaldığını ve enableEdgeToEdge'in bunları ezmediğini kontrol et. Gerekirse SystemBarStyle ile uygulamanın temasını geçir.
4. accompanist-systemuicontroller: önce kodda `com.google.accompanist.systemuicontroller` / `rememberSystemUiController` kullanımını ara ve bana raporla. Kullanım YOKSA bağımlılığı kaldırmayı ayrı bir commit olarak yap. Kullanım VARSA dokunma, yerlerini göster, onayımı bekle.
5. themes.xml: `android:statusBarColor` satırını kaldırabilirsin. `windowBackground`a dokunma. Eski Android'de (API 30-34) açılışta renk sıçraması olup olmadığını test listesine ekle.
6. Insets: ana ekran, alt kısımdaki slider/butonlar/reklam alanı, klavye (IME) olan alanlar ve tüm Dialog'lar çubukların altında kalmamalı. ShareDialog'un Compose mu platform AlertDialog mu olduğunu belirt. Sorunlu yer yoksa gereksiz değişiklik yapma.
7. `./gradlew assembleDebug` ve varsa lint/test çalıştır. Sonra bana şunları ver: değişen dosyalar, diff özeti, manuel test listesi.

AŞAMA 2'ye henüz GEÇME. Aşama 1 raporunu verip dur.