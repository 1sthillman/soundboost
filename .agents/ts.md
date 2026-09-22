Özellik tarafını öncelik sırasına göre, her biri için "ne, neden, nasıl (teknik yaklaşım), zorluk" mantığıyla ele alayım.

## 1. Per-App Boost Profilleri
**Ne:** Her uygulama için ayrı boost/EQ ayarı (Spotify'da farklı, oyunlarda farklı).

**Neden öncelikli:** Roadmap'te zaten var, teknik olarak `AudioEffectsManager` altyapısı hazır, sadece bir "hangi uygulama şu an ses çalıyor" katmanı ekleniyor. Kullanıcı sadakati için en yüksek etkili özelliklerden biri.

**Nasıl:**
- `AudioManager.AudioPlaybackCallback` ile hangi paketin ses çaldığını (Android 10+ `AudioPlaybackConfiguration.getClientUid()` üzerinden dolaylı) takip et. Doğrudan "hangi app çalıyor" API'si sınırlı olduğundan, pratikte kullanıcının manuel olarak "bu app için profil oluştur" demesini ve `PACKAGE_USAGE_STATS` izniyle foreground app'i algılamasını kombinleyen bir yaklaşım daha güvenilir.
- Profil verisi: `Map<packageName, BoostProfile>` → DataStore'da Proto DataStore ile (mevcut Preferences DataStore yerine, çünkü liste/map yapısı için daha temiz).
- `BoostForegroundService` içinde: foreground app değiştiğinde `AudioEffectsManager.applyProfile(profile)` çağrısı.
- UI: Settings'e "Uygulama Profilleri" listesi, her satırda app ikonu + boost/EQ mini önizleme.

**Zorluk:** Orta. En kırılgan kısmı "hangi app şu an aktif" tespiti — bunu abartmadan, kullanıcı ilk kez bir app açtığında bildirim ile "Bu uygulama için özel profil oluşturmak ister misin?" diye sorup opt-in yapmak en sağlam yol.

## 2. Quick Settings Tile
**Ne:** Bildirim çubuğundan tek dokunuşla boost aç/kapat.

**Nasıl:**
- `TileService` extend eden bir sınıf (`BoostQuickTile.kt`), manifest'e `android.service.quicksettings.TileService` intent-filter'ı ile kaydet.
- `onClick()` içinde mevcut `BoostForegroundService`'e start/stop komutu gönder, `onStartListening()` içinde tile ikonunu servisin durumuna göre güncelle (aktif/pasif).
- State senkronizasyonu için zaten var olan DataStore'daki boost-active flag'ini gözlemle.

**Zorluk:** Düşük. 1 gün civarı iş, mevcut servis mimarisine ek bir giriş noktası sadece.

## 3. Widget
**Ne:** Ana ekranda boost dial'ı veya hızlı on/off widget'ı.

**Nasıl:**
- Glance API (Jetpack Compose için modern widget toolkit) kullan — mevcut Compose kod tabanıyla en uyumlu yaklaşım, klasik RemoteViews'a göre çok daha az kod tekrarı.
- Basit versiyon: tek buton (boost aç/kapat) + o anki seviye göstergesi.
- Gelişmiş versiyon: widget üzerinde küçük bir slider (Glance'de slider desteği sınırlı, bu yüzden muhtemelen +/- butonları ile adım adım artırma daha pratik).

**Zorluk:** Orta. Glance'e hiç dokunulmadıysa öğrenme eğrisi var, ama UI tarafı zaten Compose olduğu için bileşenler tekrar kullanılabilir.

## 4. Parametrik EQ (10 bant) + Preset'ler
**Ne:** Mevcut 3 bantlı EQ'yu genişletip Rock/Elektronik/Klasik/Pop gibi hazır profiller eklemek.

**Nasıl:**
- Android'in native `Equalizer` audiofx sınıfı zaten çoğu cihazda 5 banda kadar destekliyor (`getNumberOfBands()` ile cihazdan sorgulanır) — "10 bant" iddiası donanıma göre değişeceğinden, gerçekçi olan: cihazın desteklediği bant sayısını oku, UI'ı ona göre dinamik oluştur. Sabit 10 bant vaat etmek "gerçek olmayan UI" riskine girer (repo'nun kendi anti-pattern kuralına da aykırı — "Real audio data or honest decorative animation").
- Preset'ler: her preset için `short[] bandLevels` sabit dizisi, `Equalizer.setBandLevel()` ile uygula. Bu tamamen istemci tarafında, ek izin gerektirmiyor.
- UI: EqualizerVisualizer bileşenine preset seçici chip row ekle, kullanıcı manuel sürüklediğinde preset "Custom" olarak işaretlensin.

**Zorluk:** Düşük-orta. Asıl incelik, cihazlar arası bant sayısı farkını dürüstçe yönetmek.

## 5. Bluetooth Cihaza Özel Profiller
**Ne:** Kulaklık/hoparlör değiştiğinde otomatik profil geçişi (zaten Bluetooth headphone desteği var, bunun üzerine inşa).

**Nasıl:**
- `BluetoothProfile.ServiceListener` + `AudioDeviceCallback` (API 23+) ile cihaz bağlantı/kopma event'lerini dinle.
- Cihaz MAC adresi (veya `AudioDeviceInfo.getProductName()`) anahtar olarak profil eşleştir.
- İlk bağlantıda "Bu cihaz için ayarları hatırlayalım mı?" diyalogu.

**Zorluk:** Orta. Bluetooth API'leri cihaz/OEM'e göre tutarsız davranabiliyor (repo'nun kendi troubleshooting notunda da Xiaomi/Oppo kısıtlamalarından bahsediliyor) — bu yüzden "best effort" yaklaşımı ve net hata mesajları şart.

## 6. Ayar Export/Import
**Ne:** Profilleri JSON olarak dışa/içe aktarma (cihaz değişiminde veya paylaşımda).

**Nasıl:**
- DataStore içeriğini `kotlinx.serialization` ile JSON'a çevir, `Storage Access Framework` (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`) ile dosya sistemine yaz/oku — ekstra izin gerekmiyor, SAF zaten sandbox güvenli.

**Zorluk:** Düşük. Yarım gün iş, ama kullanıcı güveni açısından değerli (veri kaybı korkusunu azaltır).

## 7. AI Vocal Separation — en riskli ama en farklılaştırıcı
Repoda tasarım + implementasyon durumu dosyaları zaten var, yani bu üzerinde düşünülmüş. Birkaç gerçekçi yol var:

- **On-device model (TensorFlow Lite / ONNX Runtime Mobile):** Spleeter benzeri hafifletilmiş bir vokal ayırma modeli cihazda çalıştırılır. Artısı: gizlilik vaadi bozulmaz (repo zaten "internet izni yok" diyor). Eksisi: gerçek zamanlı sistem sesi üzerinde (yani kullanıcının Spotify'da çaldığı şarkı üzerinde) çalışmak için önce sesi yakalamak gerekir — bu da `RECORD_AUDIO` + `MediaProjection` (Android 10+ sistem sesi yakalama API'si) gerektirir, ki bu hem izin hem Play Store politika riski (kullanıcı verisi/gizlilik açıklaması) getirir.
- **Gerçekçi kapsam:** Gerçek zamanlı tam sistem sesi ayrıştırma yerine, kullanıcının cihazına yüklediği bir ses dosyası üzerinde offline işleme (örn. "bu şarkıyı vokalsiz versiyona çevir") çok daha ulaşılabilir bir ilk adım. Bu hem teknik risk hem Play Store onay riski açısından çok daha az sorunlu.

**Zorluk:** Yüksek. Bunu MVP olarak "dosya bazlı offline işleme" ile başlatıp, tutarsa gerçek zamanlı sisteme genişletmek en mantıklı yol.

---

**Önerilen sıralama (etki/efor oranına göre):**
1. Quick Settings tile (hızlı kazanım)
2. Preset EQ'lar (dürüst bant sayısıyla)
3. Ayar export/import
4. Widget
5. Per-app profilleri
6. Bluetooth cihaz profilleri
7. AI Vocal Separation (dosya bazlı MVP)

Bunlardan birini kod seviyesinde (örneğin Quick Settings tile'ı gerçekten yazmak) birlikte ilerletmek ister misin, yoksa önce roadmap'i bir dokümana mı dökelim?