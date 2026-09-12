# Çoklu Ses Akışı Sistemi - Kullanım Kılavuzu

## 🎯 Özellik

Sound'ST Boost artık **tüm ses türlerine** efekt uygular:

### ✅ Desteklenen Ses Türleri

1. **STREAM_MUSIC** - Müzik, video, YouTube, Spotify
2. **STREAM_VOICE_CALL** - Telefon görüşmeleri (ahize ve hoparlör)
3. **STREAM_RING** - Zil sesleri
4. **STREAM_NOTIFICATION** - Bildirim sesleri
5. **STREAM_ALARM** - Alarm sesleri
6. **Oyun Sesleri** - Oyunlar genellikle STREAM_MUSIC kullanır

## 🔧 Teknik Detaylar

### Multi-Stream Audio Manager

`MultiStreamAudioManager` sınıfı, Android'in farklı ses akışlarına ayrı ayrı efekt uygular:

```kotlin
// Her ses akışı için ayrı efekt instance'ları
- Session 0 (GLOBAL) - Tüm sesler
- Session 3 (MUSIC) - Müzik, video, oyun
- Session 0 (VOICE_CALL) - Telefon görüşmesi
- Session 5 (NOTIFICATION) - Bildirimler
- Session 4 (ALARM) - Alarmlar
```

### Otomatik Akış Tespiti

Sistem otomatik olarak:
- Telefon görüşmelerini tespit eder (`AudioManager.MODE_IN_CALL`)
- Oyun seslerini düşük latency audio özelliğinden tanır
- Her akışa aynı efekt ayarlarını uygular

## 📊 Efekt Uygulama Önceliği

1. **10-Band Parametric EQ** (en yüksek öncelik)
2. **Vocal/Music Balance**
3. **3-Band Simple EQ** (fallback)

## 🎮 Oyun Sesi Desteği

Oyunlar genellikle `STREAM_MUSIC` kullanır ve global session (0) üzerinden yakalanır.

**Not:** Bazı oyunlar kendi audio session'larını oluşturabilir. Bu durumda:
- Global session (0) çoğu oyunu yakalar
- Multi-stream sistemi otomatik olarak tüm aktif akışlara efekt uygular

## 📞 Telefon Görüşmesi Desteği

`STREAM_VOICE_CALL` akışına efekt uygulanır:
- Ahize sesi
- Hoparlör sesi
- Bluetooth kulaklık

**Güvenlik:** Görüşme kalitesini korumak için otomatik dengeleme yapılabilir.

## 🔄 Fallback Mekanizması

Eğer multi-stream sistemi başarısız olursa:
1. Otomatik olarak eski sisteme geçiş yapılır
2. Session 0 (global) üzerinden tüm seslere efekt uygulanır
3. Kullanıcı deneyimi kesintisiz devam eder

## 🚀 Kullanım

Hiçbir ek ayar gerekmez! Sistem otomatik olarak:
1. Serviste başlatılır (`BoostForegroundService.onCreate()`)
2. Tüm ses akışlarına efekt ekler
3. Her efekt değişikliğinde tüm akışları günceller

## 📝 Log Mesajları

Service başlatıldığında:
```
🚀 Service onCreate - Initializing audio systems...
✅ LoudnessEnhancer eklendi: MUSIC/VIDEO/GAME (session: 3)
✅ LoudnessEnhancer eklendi: VOICE_CALL (session: 0)
✅ Multi-stream sistem aktif: 5 akış
```

Efekt uygulandığında:
```
🔊 Tüm akışlara gain uygulanıyor: percent=150, mB=750, enabled=true
✅ Gain uygulandı: session 0, gain=750mB
✅ Gain uygulandı: session 3, gain=750mB
📊 Toplam 5 / 5 akışa gain uygulandı
```

## ⚠️ Sınırlamalar

1. **Cihaz Desteği:** Bazı cihazlar belirli stream'lerde efekt uygulamayı desteklemeyebilir
2. **OEM Kısıtlamaları:** Bazı üreticiler (Samsung, Xiaomi) özel ses HAL'leri kullanabilir
3. **Android Sürümü:** Android 6.0+ gereklidir

## 🔍 Hata Ayıklama

Logları kontrol etmek için:
```bash
adb logcat | grep "BoostService\|MultiStreamAudio"
```

Aktif akışları görmek için:
```kotlin
multiStreamManager?.logActiveStreams()
```

## 💡 Gelecek İyileştirmeler

- [ ] Akış başına farklı efekt profilleri
- [ ] Otomatik görüşme modu (telefonda bass azalt)
- [ ] Oyun modu tespiti ve optimizasyonu
- [ ] Uygulama bazlı efekt yönetimi
