# SoundBoost DJ Engine — Mimari Şartname (v1)

**Okuyucu:** Bu projeyi kodlayan yapay zekâ ajanı (Claude 4.5).
**Statü:** BAĞLAYICI. "Karar Kayıtları" bölümündeki kararlar yeniden tartışılmaz. Bir kararın yanlış olduğunu düşünüyorsan kodlamaya başlamadan önce kullanıcıya gerekçeyle yaz ve onay bekle.
**Dil:** Açıklamalar Türkçe, kod/identifier/yorum satırları İngilizce.

---

## 0. ÇALIŞMA PROTOKOLÜ (önce bunu oku)

Bu bölüm, hata yapmamak için uyman gereken kurallardır.

1. **Dokümanı baştan sona oku, sonra mevcut kodu oku, sonra plan yaz.** Kod yazmadan önce, yapacağın fazın dosya listesini ve arayüzlerini 10-20 satırda kullanıcıya özetle.
2. **Küçük adımlarla ilerle.** Her adım sonunda proje **derlenmeli** ve mevcut özellikler **çalışmaya devam etmeli**. "Hepsini yeniden yaz" yasak.
3. **Eski motoru silme.** Yeni motor `USE_NATIVE_ENGINE` bayrağı arkasında gelir. Eski yol (`SynchronizedMusicPlayer`, `MusicShareManager`, `DJEffectPlayer`) Faz 2 kabul kriterleri geçilene kadar yerinde kalır.
4. **API uydurma.** Oboe, Ktor, AAudio, MediaCodec API'lerinden emin değilsen: Gradle'ın indirdiği kaynak/header'a bak, resmi dokümana bak, ya da kullanıcıya "doğrulayamadım" de. Sürüm numaralarını hafızadan yazma; güncel stabil sürümü Maven'dan doğrula.
5. **"Çalışıyor" deme, kanıtla.** Çalıştırdığın test / derleme çıktısını raporla. Gerçek cihazla test edemiyorsan açıkça "cihazda test EDİLMEDİ" yaz ve kullanıcıya adım adım test talimatı ver.
6. **Her faz sonunda rapor:** (a) değişen dosyalar, (b) kabul kriterleri tablosu (geçti/kaldı/ölçülmedi), (c) bilinen riskler, (d) sonraki adım.
7. **Dokunma listesi (kullanıcı izni olmadan değiştirme):**
   - `FlashSafetyGuard` ve flaş güvenlik sınırları (epilepsi güvenliği). Beatgrid tabanlı flaş eklenirse de **mutlaka** FlashSafetyGuard'dan geçer. Parti modunda minimum aralık 100 ms, normalde 333 ms kuralı bozulmaz.
   - `BassFlashlightSync` iç mantığı (yalnızca çağıran taraf entegre edilir).
   - `SyncForegroundService` yaşam döngüsü.
   - Mevcut JSON mesajlarının `@SerialName` değerleri (yeni mesaj eklenebilir, eskisi yeniden adlandırılmaz).
8. **Belirsizlik varsa tek, net bir soru sor** (Bölüm 15'e bak). Tahminle ilerleme.
9. **Tüm `Float`/`Double` değerleri engine'e ve ağa gitmeden önce `isFinite()` ile doğrulanır** (NaN/Infinity yasak).

---

## 1. Proje bağlamı ve mevcut durum

Android (Kotlin, Compose) parti uygulaması. Bir telefon HOST (DJ), diğerleri CLIENT. Ktor CIO WebSocket (`/sync`, port 8127) ile bağlanıyorlar; NSD ile keşif, QR yedek. Host dosyadan müzik seçer, dosya 64 KB base64 parçalarla clientlara iner, hepsi hazır olunca "Başlat" aktifleşir. Zaman senkronu `ClockSync` (NTP benzeri ping/pong, WebSocket üzerinden). Efektler `SoundPool` ile.

### Mevcut dosyaların kaderi

| Dosya | Karar |
|---|---|
| `SyncServer`, `SyncClient` | KORU, genişlet (HTTP track endpoint, UDP saat, v2 mesajlar, protokol sürüm kontrolü) |
| `SyncMessage` | GENİŞLET (Bölüm 6.4). Eski mesajlar kalır |
| `ClockSync` | Saf fonksiyonlar kalır; üstüne `ClockModel` (Bölüm 5) yazılır |
| `RoomStateManager`, `Playlist`, `RoomState` | KORU, genişlet |
| `DiscoveryManager`, `DJEffects` (kayıt listesi) | KORU |
| `DJStateManager`, `DJState`, `DJCommand` | LEGACY → `EngineController` durumu ile değişecek |
| `MusicShareManager` | REPLACE → `TrackStore` + `TrackHttp` + `CanonicalConverter` |
| `SynchronizedMusicPlayer` | LEGACY (bayrak arkasında kalır, Faz 2 sonunda silinir) |
| `DJEffectPlayer` | REPLACE → native `Sampler` |
| `SyncViewModel` (~1500 satır) | REFACTOR: sorumluluklara böl (Bölüm 10). Büyük-patlama refactor yapma, parça parça taşı |

---

## 2. Gerçekçi hedefler (söz verilebilenler / verilemeyenler)

**Hedef (dahili hoparlör veya kablolu çıkış, aynı Wi-Fi):**
- Cihazlar arası medyan ses ofseti ≤ 10 ms, p95 ≤ 20 ms.
- Host komutu → herkesin duyması: düşük gecikme modu ~60-100 ms, stabil mod ~200 ms (adaptif).
- Uzun parçada (10+ dk) drift birikmez (sürekli düzeltme).

**Garanti EDİLEMEZ (kullanıcıya dürüst söyle, UI'da gösterme/ima etme):**
- Bluetooth hoparlör/kulaklık: gecikme 150-300 ms, cihaza göre değişir, AAudio timestamp'ı güvenilmez. Çözüm: cihaz başına manuel "nudge (ms)" ayarı (+ Faz 4'te mikrofonla kalibrasyon).
- Sample-seviyesi (µs) senkron: Wi-Fi saat senkronu ile mümkün değil. Hedef milisaniye altı değil, birkaç ms.
- Wi-Fi AP izolasyonu açıksa hiçbir şey çalışmaz (NSD/QR zaten bunun için yedek).

---

## 3. KARAR KAYITLARI (tartışılmaz)

| # | Karar | Gerekçe |
|---|---|---|
| D1 | Ses motoru **Oboe (C++/NDK)**, LowLatency, mümkünse Exclusive, değilse Shared | `MediaPlayer` ters oynatma/sample-hassas seek/crossfade yapamaz |
| D2 | Her telefon **aynı dosyayı aynı motorla** çalar; host yalnızca **olay + zaman** gönderir. Ses akışı (streaming) gönderilmez | Ağ yükü düşük, senkron deterministik |
| D3 | Zaman domaini: **`System.nanoTime()`** (Kotlin) ≡ `CLOCK_MONOTONIC` (C++). `currentTimeMillis()` zamanlama için YASAK | NTP sıçraması; Oboe timestamp'ı CLOCK_MONOTONIC. (`elapsedRealtimeNanos` = BOOTTIME, karıştırma) |
| D4 | Saat senkronu: **UDP** kanal, en düşük RTT'li örnekler, **offset + skew (ppm)** doğrusal model | TCP yeniden iletimi ölçümü bozar |
| D5 | Kontrol: mevcut **WebSocket (JSON)**. Zaman-kritik olmayan veri taşımaz | Mevcut altyapı |
| D6 | Dosya aktarımı: **HTTP** `/track/{trackId}` (Range, SHA-256), paralel indirme. Base64-chunk-over-WebSocket **kaldırılır** | %33 az veri, kontrol kanalını tıkamaz |
| D7 | Şarkılar host'ta **tek kez** **kanonik PCM**'e çevrilir: **44100 Hz, stereo, int16 LE**, başlıksız; metadata manifest'te. Clientlar decode ETMEZ | Cihazlar arası decoder/padding farkı (20-30 ms sabit kayma) yok olur |
| D8 | Olay modeli: **frame'e çevrilen host-zamanlı olaylar** + **anchor (çapa) zaman çizelgesi**. Olay, audio callback içinde tam hedef sample'da uygulanır | Çıkış gecikmesi otomatik telafi, jitter yok |
| D9 | Sürekli drift düzeltme **motorun içinde** (PLL), ±%0.2 hız trim, >30 ms ise sert seek | Kristal kayması |
| D10 | Olaylar 3 sınıf: **Anchor** (geç gelirse yakala), **Trigger** (çok geç ise atla), **Loose** (son değer kazanır) | Bölüm 8.3 |
| D11 | Tüm parametre geçişleri (fader, EQ, filtre, crossfader) motorda **rampa (ramp)** komutudur; ağda 30 Hz akış yok, tek komut | Trafik ve zipper noise |
| D12 | Gerçek stem ayrıştırma (TFLite/Demucs) **Faz 4'ten önce YAPILMAZ** | Ağır, riskli; anlık kill/isolator yeterli |
| D13 | Protokol **v2**'ye geçilir; v1 ile uyumluluk YOK. Sürüm uyuşmazsa host `Error("PROTOCOL_MISMATCH")` döner, UI "uygulamayı güncelle" der | Sade kod |

---

## 4. Mimari genel bakış

```
                         HOST (DJ)                                   CLIENT
  ┌────────────────────────────────────────────┐        ┌───────────────────────────────────────┐
  │ UI (Compose)                               │        │ UI (read-only deck/mixer görünümü)    │
  │   ▼                                        │        │   ▲                                   │
  │ HostConsole (DJ komutları, quantize,       │        │ ClientEngineBridge                    │
  │   geçiş şablonları, lead-time hesabı)      │        │                                       │
  │   ▼ EngineEvent(hostTimeNs, ...)           │  WS    │   ▲ EngineEvent                       │
  │ EventScheduler  ─────────────────────────────────►  │ EventScheduler                        │
  │   (host için ClockModel = identity)        │ (JSON) │   hostNs → ClockModel → localNs       │
  │   ▼ localNs → frame (TimeAnchor)           │        │   ▼ localNs → frame (TimeAnchor)      │
  │ NativeEngine (JNI, tek thread'den yazılır) │  UDP   │ NativeEngine                          │
  │   ▼ lock-free SPSC komut kuyruğu           │ saat   │   ▼ SPSC                              │
  │ ┌─ Oboe audio callback (RT, kilitsiz) ───┐ │ ◄────► │ ┌─ Oboe audio callback ─────────────┐ │
  │ │ Deck A/B → EQ/Filter → Fader →         │ │        │ │ aynı motor, aynı kod              │ │
  │ │ Crossfader → FX → Sampler(+duck) →     │ │  HTTP  │ └───────────────────────────────────┘ │
  │ │ Master → Limiter → out                 │ │ /track │ TrackHttpClient → TrackStore → RAM    │
  │ └────────────────────────────────────────┘ │ ◄────► │                                       │
  │ TrackHttpServer + CanonicalConverter       │        │                                       │
  └────────────────────────────────────────────┘        └───────────────────────────────────────┘
```

**Üç ağ düzlemi:**
1. **Kontrol düzlemi** (WebSocket/JSON, TCP): olaylar, snapshot, manifest, hazır durumu.
2. **Zaman düzlemi** (UDP, port 8128): yalnızca saat ping/pong, ikili paket.
3. **Veri düzlemi** (HTTP, aynı Ktor sunucusu, port 8127 `/track/{id}`): şarkı dosyaları.

**Thread kuralları (ihlal = hata):**
- **Audio callback (RT):** malloc/new/delete YOK, mutex YOK, log YOK, JNI YOK, dosya/ağ I/O YOK, `std::vector::push_back` YOK. Yalnızca önceden ayrılmış bellek + atomikler.
- **Engine'e komut gönderen Kotlin tarafı TEK thread'dir** (`engineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()`). SPSC kuyruk tek üretici varsayar. Birden fazla thread'den `NativeEngine.post*` çağırmak yasak.
- **UDP saat thread'i:** ayrı, yüksek öncelikli (`Process.THREAD_PRIORITY_URGENT_AUDIO`), coroutine `delay` ile değil bloklayan `DatagramSocket.receive` ile.
- **Zamanlama için `delay()`, `Thread.sleep`, `Handler.postAtTime` KULLANMA.** Zamanlama motorun frame sayacındadır.

---

## 5. Zaman modeli

### 5.1 Saat domainleri
- `localNs` = `System.nanoTime()` (cihaz-yerel, monoton).
- `hostNs` = host cihazın `System.nanoTime()` değeri. **Host için** `hostNs == localNs` (identity model); host da olayları aynı kod yolundan geçirir ("host da bekler").
- Protokolde tüm zaman alanları **nanosaniye, hostNs domaininde** (`Long`). Epoch milisaniye yalnızca UI/log içindir.

### 5.2 `ClockModel` (Kotlin, saf mantık, Android bağımsız, unit test'li)

Model: `hostNs = (1 + skew) * localNs + b` (doğrusal). Ters: `localNs = (hostNs - b) / (1 + skew)`.

Algoritma:
1. Her ping/pong turu (t0,t1,t2,t3; hepsi nanoTime) → `rtt = (t3 - t0) - (t2 - t1)`, `offset = ((t1 - t0) + (t2 - t3)) / 2`. Örnek zamanı = `t0`'ın orta noktası (local).
2. Son 30 sn içindeki en fazla 128 örneği tut.
3. **Örnek seçimi:** `minRtt` bul; yalnızca `rtt <= minRtt * 1.5 + 0.5ms` olanları kullan (veya en düşük %25).
4. Seçilenlere **ağırlıklı en küçük kareler** (ağırlık ∝ 1/rtt) ile `offset(t) = b0 + s*(t - tRef)` doğrusu uydur. En az 8 seçili örnek yoksa yalnızca medyan offset (skew=0) kullan.
5. Sanity: `|skew| > 200 ppm` ise reddet, skew=0 kullan. Model değişimi ani sıçrama yapıyorsa (|Δoffset| > 5 ms tek adımda) yumuşat (kayan ortalama / slew).
6. Başlangıçta **burst**: ilk 2 saniyede 20 ms aralıkla 40 ping. Sonra 1 Hz. Model "hazır" bayrağı: ≥ 8 seçili örnek ve `|skew|` kararlı. **Hazır değilken olay çalıştırma, kuyruğa al.**
7. Host UDP sunucusu: aldığı pakete anında `t1` (alış) ve `t2` (gönderiş) damgalar. Damgayı mümkün olduğunca soket okuma/yazma satırına yakın al.
8. UDP 2 sn içinde hiç yanıt alamazsa: WebSocket ping/pong'a düş ve UI'da "Düşük hassasiyet" uyarısı ver (hata değil).

### 5.3 `TimeAnchor` (C++, çıkış gecikmesini telafi eden parça)

Amaç: `localNs → engine output frame`.
- Oboe stream'in `getTimestamp(CLOCK_MONOTONIC, &framePosition, &timeNanos)` değeri **ayrı bir (RT olmayan) thread'de her ~200 ms** çağrılır (callback içinde ÇAĞIRMA).
- Her başarılı çift `(frame, nanos)` bir halka tampona yazılır; son 8 çifte **en küçük kareler** doğrusu uydurulur → `nanosToFrame(t) = round(frame0 + (t - nanos0) * sampleRate / 1e9)`.
- Bu "sunulma (presented) frame'i" olduğundan, olayı bu frame'de render etmek sesin **tam o `localNs` anında hoparlörden çıkması** demektir; çıkış gecikmesi otomatik telafi olur.
- JNI: `nativeLocalNsToFrame(localNs): Long` (RT olmayan thread'den çağrılır). Seqlock/atomik çift tampon ile okunur.
- `getTimestamp` başarısızsa (ilk anlar / `Unimplemented`): yeniden dene; hiç alınamazsa yedek: `framesRendered` + `calculateLatencyMillis()` tahmini, ve UI'da "Düşük hassasiyet" uyarısı.
- **Stream yeniden başlarsa** (kulaklık çıkarıldı, `onErrorAfterClose`): `streamEpoch++`, frame sayacı sıfırlanır; Kotlin tarafı en son `StateSnapshot`'tan deck'leri **yeniden çapalar** (Bölüm 8.4).

### 5.4 Dönüşüm zinciri (Kotlin, `EventScheduler` içinde)
```kotlin
fun hostNsToFrame(hostNs: Long): Long {
    val localNs = clockModel.hostToLocal(hostNs)          // host ise identity
    return nativeEngine.localNsToFrame(localNs)
}
```
Kullanıcı "nudge" (ms) ayarı `localNs`'ye eklenir: `localNs += nudgeMs * 1_000_000`.

---

## 6. Ağ katmanları ve Protokol v2

### 6.1 Kontrol (WebSocket) — mevcut yapı korunur
- `Join.protocolVersion = 2`. Sunucu `Join` alınca sürümü **kontrol eder**; uyuşmazsa `Error("PROTOCOL_MISMATCH")` gönderip kapatır.
- **TCP_NODELAY** açık olmalı (Ktor CIO için nasıl ayarlandığını doğrula; varsayılana güvenme).
- `Frame.Text` her client için **yeniden oluşturulur** (mevcut kod doğru; koru). `async(SupervisorJob()) { }` kalıbını `async { }` yap.
- Müzik bloklarına artık bu kanalda yer yok; bu kanal küçük JSON mesajları taşır → ping/efekt gecikmesi bozulmaz.

### 6.2 Zaman (UDP, port 8128)
İkili paket (big-endian), JSON YOK:
```
offset  size  alan
0       2     magic 0x5342 ("SB")
2       1     type (1 = PING, 2 = PONG)
3       1     reserved
4       4     seq (uint32)
8       8     t0 (client nanoTime)
16      8     t1 (host nanoTime, alış)       [PONG'ta dolu]
24      8     t2 (host nanoTime, gönderiş)   [PONG'ta dolu]
```
Host: `DatagramSocket(8128)` dinler; `SyncServer.start()` ile açılır, `stop()` ile kapanır. Client host IP'sine gönderir.

### 6.3 Veri (HTTP) — `/track/{trackId}`
- Ktor eklentileri: partial-content (Range) ve auto-head-response. **Eklenti/artefakt adlarını Ktor sürümüne göre doğrula.**
- `GET /track/{trackId}` → kanonik PCM dosyası, `Range` destekli. `HEAD` → `Content-Length`, `X-Sha256`.
- Client: dosyayı **4 paralel Range** isteği ile (1-2 MB aralıklar) indirir, `RandomAccessFile`/`FileChannel` ile konumsal yazar, aralık başına yeniden dener, bitince **SHA-256** doğrular. Hatalı ise silip baştan.
- **Prefetch:** playlist'e eklenen her şarkı (yalnızca seçilen değil) arka planda indirilir. Çalan şarkı + sıradaki öncelikli.
- İlerleme `TrackReady` / `TrackProgress` mesajıyla host'a bildirilir (aşağıda).
- Host'ta eş zamanlı client başına en fazla 4 aralık; toplam bant genişliği paylaşılır.
- "Başlat" butonu, **o trackId için tüm bağlı clientlar `TrackReady`** olunca aktifleşir (mevcut `allClientsReady` mantığı trackId bazlı yapılır).

### 6.4 Yeni `SyncMessage` türleri (v2)

Mevcut sealed class'a eklenir. Zaman alanları **hostNs (Long)**. Tüm `Float` alanlar sonlu olmalı.

| Mesaj (`@SerialName`) | Yön | İçerik |
|---|---|---|
| `track_manifest` | host→all | `trackId, title, fileName, durationMs, canonicalFrames: Long, byteSize: Long, sha256, bpm: Float?, beatOffsetFrames: Long?, beatsPerBar: Int=4, key: String?, loudnessLufs: Float?, trimDb: Float?` |
| `track_ready` | client→host | `trackId, ok: Boolean, error: String?` |
| `deck_event` | host→all | `eventId: Long, hostTimeNs: Long, deck: Int(0/1), kind: DeckEventKind, trackId: String?, posFrames: Double?, tempoRatio: Double?, loopStartFrames: Double?, loopEndFrames: Double?, cueIndex: Int?` |
| `ramp_event` | host→all | `eventId, hostTimeNs, param: ParamId, deck: Int, from: Float?, to: Float, durationNs: Long, curve: RampCurve` |
| `param_batch` | host→all | `hostTimeNs, items: List<ParamValue(param, deck, value)>` (Loose; ~30 Hz'e kadar) |
| `sample_trigger` | host→all | `eventId, hostTimeNs, effectId: String, volume: Float, duckDb: Float` |
| `transition_start` | host→all | `eventId, hostTimeNs, template: String, fromDeck: Int, toDeck: Int, bars: Int, params: Map<String,Float>` (istemci şablonu yerelde açar, bkz. 9.2) |
| `state_snapshot` | host→all (1 Hz + join'de) | `seq, hostTimeNs, decks: [DeckTimeline], mixer: MixerState, bpmMaster: Float?` |

`DeckEventKind`: `LOAD, PLAY, PAUSE, STOP, SEEK, SET_TEMPO, LOOP_ON, LOOP_OFF, CUE_JUMP, BRAKE, SPINBACK`.
`RampCurve`: `LINEAR, EQUAL_POWER, EXP, SCURVE`.

**`DeckTimeline` (anchor modeli — ÇEKİRDEK KAVRAM):**
```kotlin
@Serializable data class DeckTimeline(
    val deck: Int,
    val trackId: String?,
    val playing: Boolean,
    val anchorHostNs: Long,        // bu anda...
    val anchorPosFrames: Double,   // ...pozisyon bu (kanonik 44.1k frame)
    val tempoRatio: Double,        // 1.0 nominal, negatif = ters
    val loopStartFrames: Double? = null,
    val loopEndFrames: Double? = null
)
// Konum(T) = anchorPos + tempoRatio * 44100 * (T - anchorHostNs)/1e9   (loop varsa sarmala)
```
Bir deck'in konumu **zamanın fonksiyonudur**. Late join, drift düzeltme, resync hep bu fonksiyondan hesaplanır.

---

## 7. Audio engine (C++, `app/src/main/cpp/`)

### 7.1 Tasarım ilkeleri
- **Çekirdek Android-bağımsız:** `AudioEngine::render(float* out, int frames)` cihazdan bağımsız çalışır; Oboe yalnızca ince bir sarmalayıcı. Böylece masaüstünde offline render + test yapılabilir.
- Dahili format: **float32 stereo, interleaved**, cihazın yerel örnekleme hızı (`stream->getSampleRate()`, genelde 48000). **Sample rate'i zorla sabitleme** (ek resampler gecikmesi olmasın). Hız oranı deck içinde hesaplanır: `step = tempoRatio * (44100.0 / outSampleRate) * pllTrim`.
- **Maks blok:** `kMaxBlock = 512` frame. Oboe daha büyük callback verirse içeride 512'lik parçalara böl.
- Denormal koruması: geri beslemeli yapılara (filtre/echo/reverb) ~1e-15 anti-denormal ekle veya callback thread'inde FTZ ayarla. `-ffast-math` KULLANMA.
- Oboe ayarları: `PerformanceMode::LowLatency`, `SharingMode::Exclusive` (olmazsa Shared), `Usage::Media`, `ContentType::Music`, `Format::Float`, `ChannelCount::Stereo`, `DataCallback`+`ErrorCallback`. `setBufferSizeInFrames(burst * 2)` ile başla, XRun sayısına göre ayarla. `getXRunCount()` telemetriye yazılır.
- `onErrorAfterClose` → stream yeniden aç, `streamEpoch++`, Kotlin'e `STREAM_RESTARTED` olayı yolla.

### 7.2 Dosya düzeni
```
cpp/
  CMakeLists.txt                 (C++17, arm64-v8a zorunlu, armeabi-v7a opsiyonel, Oboe via Prefab)
  engine/
    AudioEngine.{h,cpp}          // render(), komut kuyruğu boşaltma, frame sayacı
    Commands.h                   // POD komut yapıları + ParamId enum
    SpscQueue.h                  // kilitsiz tek-üretici tek-tüketici kuyruk
    Deck.{h,cpp}                 // konum (double), varispeed, loop, cue, anchor+PLL
    TrackBuffer.h                // RAM'de int16 PCM (loader thread ayırır)
    Interpolator.h               // lineer (Faz1) → Hermite/cubic (Faz2)
    Smoother.h                   // tek kutuplu + doğrusal rampa
    Eq3.{h,cpp}                  // LR4 crossover, kill destekli
    SvfFilter.{h,cpp}            // TPT SVF, LPF/HPF, rezonans
    Mixer.{h,cpp}                // kanal fader, crossfader, master
    Limiter.{h,cpp}              // lookahead brickwall
    Sampler.{h,cpp}              // 16 ses, ducking
    fx/ Echo, Reverb(Freeverb), Flanger, (Faz3: Phaser, BitCrusher, Gater, Roll)
    TimeAnchor.{h,cpp}           // getTimestamp regresyonu
    EngineEvents.h               // engine→Kotlin geri bildirim kuyruğu (TRACK_ENDED vb.)
  oboe_glue/OboeOutput.{h,cpp}   // Oboe stream + callback
  jni/EngineJni.cpp
  tests/                         // masaüstünde derlenen birim testleri
```

### 7.3 Komut kuyruğu
`Commands.h` içinde sabit boyutlu POD:
```cpp
struct EngineCommand {
  int64_t targetFrame;   // 0 veya geçmiş = hemen uygula
  uint8_t type;          // SET_ANCHOR, PLAY, PAUSE, SEEK, SET_PARAM, RAMP, TRIGGER_SAMPLE,
                         // LOAD_TRACK, LOOP, SET_TEMPO, ...
  uint8_t deck;
  uint16_t param;        // ParamId
  float   value;
  float   value2;
  double  d0, d1;        // pozisyon, tempoRatio vb.
  int64_t i0;            // süre (frame), id
  void*   ptr;           // LOAD_TRACK: TrackBuffer*
};
```
- Kuyruk kapasitesi 1024 (2'nin kuvveti). Dolarsa `post` `false` döner → Kotlin loglar + Loose komutları düşürür, Strict komutları hata sayar.
- **Callback başında** kuyruk boşaltılır, komutlar `targetFrame`'e göre sıralı sabit kapasiteli (256) bir diziye alınır.
- **Frame-hassas uygulama döngüsü:**
```cpp
void AudioEngine::process(float* out, int numFrames) {
  drainQueueIntoPending();                     // RT-safe, sabit dizi
  int done = 0;
  while (done < numFrames) {
    int64_t cur = framesRendered_ + done;
    applyAllDue(cur);                          // targetFrame <= cur olanları uygula
    int64_t next = earliestPendingFrame();     // yoksa INT64_MAX; next > cur garantili
    int chunk = (int)std::min<int64_t>({numFrames - done, next - cur, kMaxBlock});
    renderChunk(out + done * 2, chunk);        // chunk >= 1
    done += chunk;
  }
  framesRendered_ += numFrames;
}
```
- Yük bırakma: `LOAD_TRACK` ile gelen yeni `TrackBuffer*` deck'e geçirilir; eski pointer bir **retire kuyruğuna** konur, RT olmayan thread serbest bırakır (callback'te `delete` YOK).

### 7.4 Deck
- Konum `double posFrames` (kanonik 44.1k frame biriminde). Her çıkış frame'inde `pos += step`; `step` negatif olabilir (ters).
- Örnekleme: Faz 1'de lineer; Faz 2'de 4 noktalı Hermite/cubic. Sınır kontrolü: `pos < 0` veya `pos >= length` → durdur, `TRACK_ENDED` olayı gönder (ters çalarken 0'da dur).
- Durum: `playing`, `tempoRatio`, loop (in/out), 8 hot cue (frame), `loaded`.
- **Anchor + PLL (drift düzeltme, motorun içinde):**
  - Kotlin `SET_ANCHOR(deck, engineFrame, posFrames, tempoRatio)` gönderir (Bölüm 8.4).
  - Deck beklenen konumu hesaplar: `expected(F) = anchorPos + tempoRatio * (44100/outSR) * (F - anchorFrame)` (loop varsa sarmala).
  - Her blokta `err = pos - expected` (kanonik frame → saniyeye çevir).
  - `|err| <= 0.5 ms` → trim = 1.0.
  - `0.5 ms < |err| <= 30 ms` → `trim = 1 - clamp(err_sec / 2.0, -0.002, 0.002)` (kayan ortalama ile yumuşat). **İşaret:** ileride (err>0) ise yavaşlat.
  - `|err| > 30 ms` → **sert seek**, ~5 ms'lik kısa çapraz geçiş (click olmasın).
  - PLL **kapalı** olduğu durumlar: scratch/brake/spinback/serbest jog, tempo rampası sürerken (anchor yeniden kurulana dek).
- `SEEK`/`PLAY`/`CUE_JUMP` yerel konumu ölçmez; **host'un verdiği `posFrames`** değerine atar.

### 7.5 Mixer ve DSP zinciri
Sıra (deck başına): `Trim(ReplayGain/trimDb) → Eq3 → SvfFilter → ChannelFader → Crossfader gain`; sonra iki deck toplanır → `FX (send/insert)` → `Sampler` ile toplanır (ducking uygulanmış deck bus'tan sonra) → `MasterGain → Limiter → hard clip ±1.0`.

- **Eq3:** İzolatör tipi, **Linkwitz-Riley 4. derece** crossover, varsayılan 250 Hz ve 2.5 kHz. 3 bantlı yapıda düşük bant, `fc2` all-pass'inden geçirilerek **faz telafisi** yapılır (yoksa toplam düz olmaz). Bant kazancı: UI 0..1 → `0 = KILL (gain 0, -∞ dB)`, `0.5 = 0 dB`, `1.0 = +6 dB` (parçalı eşleme; kill gerçekten sıfır).
- **SvfFilter:** TPT (Zavalishin) SVF, tek knob `-1..+1`: negatif = LPF (20 kHz → ~30 Hz, üstel), 0 = bypass (geçişi çapraz karıştırarak), pozitif = HPF (~20 Hz → ~8 kHz). Rezonans sabit/ayarlanabilir (Q 0.7..4).
- **Crossfader:** eğri seçilebilir; varsayılan **equal-power:** `gainA = cos(x·π/2), gainB = sin(x·π/2)` (x∈[0,1]). Ayrıca "sharp/cut" eğrisi (scratch için).
- **Limiter:** ~1.5 ms lookahead, release 50-100 ms, tavan −0.3 dBFS. Kapatılamaz (clipping koruması).
- **Smoother:** TÜM kazanç/parametre değişimleri smoother'dan geçer (varsayılan ~10 ms; EQ/filtre katsayıları 32 frame'de bir güncellenir). Anlık atlama = zipper noise = HATA.
- **Loudness:** manifest'teki `trimDb` deck trim'ine uygulanır (şarkılar arası seviye eşitleme).

### 7.6 FX
- **Echo:** süre beat-kesri (1/8, 1/4, 3/16, 1/2, 1, ...) × `60/bpm`; feedback 0..0.9 (üst sınır sert); geri beslemede LPF (~4 kHz); isteğe bağlı ping-pong. Süre değişince **fraksiyonel gecikme interpolasyonu** (pitch kayması olmasın, çapraz geçişli iki tap veya yumuşatılmış okuma). Wet/dry smoother'lı.
- **Reverb:** Freeverb (8 comb + 4 all-pass / kanal), parametreler: room, damp, width, pre-delay, wet. Send/return.
- **Flanger/Phaser:** LFO fazı **zaman çizelgesinden türetilir** (deck konumu/beat'ten), serbest çalışan sayaçtan değil → cihazlar arası deterministik.
- **Not:** Reverb kuyruğu gibi durumlar cihazlar arasında çok küçük farklar gösterebilir; kabul edilebilir. LFO/echo süreleri deterministik olmalı.

### 7.7 Sampler
- `DJEffects.all` assetleri uygulama açılışında **RT olmayan thread'de** decode edilir (MediaCodec) → float buffer'a, motorun örnekleme hızına resample → motora `LOAD_SAMPLE`.
- 16 ses (voice), her biri: buffer pointer, konum, gain. `TRIGGER_SAMPLE` `targetFrame`'de ses başlatır (sample-hassas).
- **Ducking:** `duckDb` (örn. −6 dB) deck bus'ına envelope uygular: attack ~5 ms, hold = sample süresi, release ~300 ms. Air horn çalarken müzik hafif iner.
- Efekt `id` string'leri aynı kalır (`DJEffects.getById`); native taraf `id → sampleIndex` haritasını Kotlin'den alır.

### 7.8 JNI yüzeyi (ince tutulur)
```kotlin
object NativeEngine {
  external fun create(): Long
  external fun start(h: Long): Boolean
  external fun stop(h: Long)
  external fun destroy(h: Long)
  external fun post(h: Long, /* paketlenmiş komut alanları */ ...): Boolean   // yalnızca engineDispatcher'dan
  external fun loadTrackFile(h: Long, deck: Int, path: String, frames: Long): Boolean // RT dışı thread'de okur+post eder
  external fun loadSample(h: Long, index: Int, pcmFloat: FloatArray, sampleRate: Int)
  external fun localNsToFrame(h: Long, localNs: Long): Long
  external fun pollEvents(h: Long, out: LongArray): Int       // engine→Kotlin olayları
  external fun getStats(h: Long, out: DoubleArray)            // xruns, latency, pllTrim, anchorErr...
}
```
JNI kodunda: `GetFloatArrayElements`/`ReleaseFloatArrayElements` eşleşmesi, `jlong` handle ömrü, exception temizleme. `destroy` sonrası kullanım yasak.

---

## 8. Senkron yürütme (Kotlin tarafı)

### 8.1 `EventScheduler`
Host ve client **aynı sınıfı** kullanır. Girdi: `deck_event/ramp_event/sample_trigger/...` (hostNs içerir). Çıktı: motora `targetFrame` ile komut.
```
onEvent(e):
  if (!clockModel.ready || !timeAnchor.ready) { pendingUntilReady.add(e); return }
  frame = hostNsToFrame(e.hostTimeNs)
  cmd   = toEngineCommand(e, frame)       // olay sınıfına göre, Bölüm 8.3
  engineDispatcher.post { nativeEngine.post(cmd) }
```

### 8.2 Host akışı (örnek: Deck A PLAY)
1. `HostConsole.play(deck)`: `leadNs = leadTimeController.currentLeadNs()`; `hostT = System.nanoTime() + leadNs`; quantize açıksa `hostT` bir sonraki beat'e kenetlenir (beatgrid + mevcut deck zaman çizelgesi ile).
2. `DeckTimeline` yeni anchor ile güncellenir (`anchorHostNs = hostT`, `anchorPosFrames = ...`, `playing = true`).
3. `deck_event(PLAY, hostT, posFrames, tempoRatio)` **broadcast** edilir.
4. **Aynı event host'un kendi `EventScheduler`'ına verilir** (identity saat). Host'un sesi de aynı gecikmeyle çıkar; ekran/UI anında tepki verir.

### 8.3 Olay sınıfları ve geç-kalma davranışı (D10) — AYNEN uygula
| Sınıf | Olaylar | Zamanında | Geç gelirse |
|---|---|---|---|
| **Anchor** | PLAY, SEEK, CUE_JUMP, SET_TEMPO, LOOP_ON/OFF, BRAKE/SPINBACK başlangıcı | `targetFrame`'de uygula | **ATLAMA.** Gecikme `lateFrames` kadar ise konumu telafi et: `pos += tempoRatio*(44100/outSR)*lateFrames`, hemen uygula |
| **PAUSE/STOP** | PAUSE, STOP | `targetFrame`'de dur | Hemen dur; konumu **olayın taşıdığı `posFrames`**'e ayarla (yerel ölçümü kullanma) |
| **Ramp** | `ramp_event` | `targetFrame`'de başlat | **Rampaya ilerlemiş yerinden gir:** `progress = late/duration`; değeri `from + (to-from)*curve(progress)`, kalan süreyi uygula |
| **Trigger** | `sample_trigger`, scratch makro tetikleri | `targetFrame`'de | `late > 80 ms` ise **atla** (log yaz); aksi halde hemen çal |
| **Loose** | `param_batch` | alınca uygula | Her zaman uygula, son değer kazanır |

### 8.4 Anchor yenileme döngüsü (sürekli senkron)
Her **500 ms**'de bir, çalan her deck için client `EventScheduler`:
1. `now = System.nanoTime()`; `hostNowEq = clockModel.localToHost(now)`.
2. Host'un son `DeckTimeline`'ından `expectedPos(hostNowEq + 100ms)` hesapla.
3. `F = hostNsToFrame(hostNowEq + 100ms)`.
4. Motora `SET_ANCHOR(deck, F, expectedPos, tempoRatio)` gönder. Motor PLL'i bununla uyumu sürdürür (7.4).

Ek olarak: `state_snapshot` (1 Hz) alınınca client kendi `DeckTimeline` kopyasını günceller; stream yeniden başlama veya late join durumunda `LOAD` + `SET_ANCHOR` ile deck'ler yeniden kurulur.

### 8.5 Adaptif lead time (`LeadTimeController`)
- Girdi: her client'ın `heartbeat` içinde bildirdiği RTT ve host'un kendi UDP ölçümleri.
- `lead = clamp(p95(oneWayDelay_of_all_clients) + 25 ms, minLead, maxLead)`; `oneWay ≈ rtt/2`.
- Modlar: **Düşük gecikme** (min 60 ms, max 150 ms), **Stabil** (min 150 ms, max 400 ms). UI'da seçilebilir. Varsayılan Stabil.
- Lead, host'un kendi gecikmesini de belirler (host da bekler).

### 8.6 Late join
Yeni client: `Welcome` + `track_manifest` listesi + `state_snapshot` alır → eksik trackleri HTTP ile indirir → hazır olunca `track_ready` yollar → deck'leri snapshot'tan çapalar ve **beklenen konuma yakalayarak** katılır (Anchor sınıfı mantığı).

---

## 9. DJ özellikleri (şartname)

### 9.1 Parametre kimlikleri (`ParamId`)
Aralıklar UI ölçeğindedir; motor içi dönüşüm 7.5'tedir.

| ParamId | Deck | Aralık | Not |
|---|---|---|---|
| `DECK_TRIM_DB` | A/B | −12..+12 | manifest `trimDb` ile |
| `EQ_LOW`, `EQ_MID`, `EQ_HIGH` | A/B | 0..1 | 0 = KILL, 0.5 = 0 dB, 1 = +6 dB |
| `FILTER` | A/B | −1..+1 | 0 = bypass |
| `CHANNEL_FADER` | A/B | 0..1 | |
| `CROSSFADER` | — | 0..1 | 0 = A, 1 = B |
| `CROSSFADER_CURVE` | — | enum | `EQUAL_POWER`, `SHARP` |
| `MASTER_GAIN` | — | 0..1 | |
| `ECHO_WET`, `ECHO_FEEDBACK`, `ECHO_BEATS` | — | 0..1 / 0..0.9 / enum | |
| `REVERB_WET`, `REVERB_ROOM`, `REVERB_DAMP` | — | 0..1 | |
| `FLANGER_DEPTH`, `FLANGER_RATE`, `FLANGER_WET` | — | 0..1 | |
| `DUCK_DB` | — | −12..0 | sampler ducking |

Eski `DJState` (bass/mid/treble 0..1) → yeni `EQ_*`'a doğrudan eşlenir.

### 9.2 Geçiş şablonları (`TransitionTemplates`)
Her şablon **saf fonksiyondur**: `(fromDeck, toDeck, bpm, startHostNs, bars) → List<Event>` (ramp'lar + deck olayları). Host tek `transition_start` yollar; **her cihaz şablonu yerelde aynı fonksiyonla açar** (trafik minimal, tam deterministik). `bar` = `beatsPerBar * 60/bpm` sn. Başlangıç `startHostNs` **gelen şarkının downbeat'ine** kuantize edilir.

1. **`SMOOTH_BLEND` (varsayılan 16 bar):** B `startHostNs`'de PLAY (SYNC'li, `EQ_LOW`=0 kill). CROSSFADER 0→1 EQUAL_POWER tüm süre boyunca. Ortada (bar 8) **bass swap**: A `EQ_LOW` 0.5→0 ve B `EQ_LOW` 0→0.5, ikisi **aynı beat'te, ~1 beat'lik rampa**. Bitişte A STOP.
2. **`BASS_SWAP` (8 bar):** Yukarıdakinin kısa hali; crossfader sabit 0.5'te kalır, geçişi yalnız EQ_LOW/MID swap yapar.
3. **`FILTER_SWEEP` (8 bar):** A `FILTER` 0→+1 (HPF) EXP; B `FILTER` −1→0 (LPF açılır); fader/crossfader ortada kesişir.
4. **`ECHO_OUT` (4 bar):** `startHostNs`'de ECHO_WET↑, ECHO_FEEDBACK 0.6; 1 beat sonra A `CHANNEL_FADER`→0 (echo kuyruğu sürer); B bir bar sonra downbeat'te hard PLAY.
5. **`BACKSPIN_CUT` (1 bar):** A `SPINBACK` (tempoRatio 1→−2, ~½ beat, sonra durdur), aynı downbeat'te B PLAY.
6. **`DROP_SWAP`:** B `CUE_JUMP` (kayıtlı "drop" hot cue'su) bar sınırında + A'nın bası 1 beat kill; crossfader hard çevrilir.
Auto-DJ (Faz 3): şarkı bitiminden `bars` bar önce şablon otomatik başlatılır.

### 9.3 Beat-sync ve quantize
- **Tempo:** `tempoRatio_B = bpm_A_effective / bpm_B` (sınır ±8%, ayarlanabilir; key-lock yok, Faz 4).
- **Faz hizalama:** B'yi başlatırken `startPos_B`, B'nin beatgrid'inde A'nın o andaki beat fazıyla eşleşecek şekilde seçilir (yakın beat'e yuvarla).
- **Quantize:** cue/loop/sample/transition tetikleri bir sonraki beat'e (veya 1/2, 1/4 beat) kenetlenir. Varsayılan AÇIK.
- BPM yoksa/yanlışsa: **manuel BPM + tap tempo + beat offset kaydırma** UI'ı (analiz her zaman doğru olmaz; bu yedek zorunlu).

### 9.4 Scratch, brake, spinback
- Motor varispeed + kesirli konumla gerçek ters çalma ve scratch yapar (`tempoRatio` negatif olabilir; sıfıra yakın değerlerde `±0.01` alt sınır uygulama, kesin 0 yerine "pause" kullan).
- **Brake:** `tempoRatio` 1→0'a eğri ile (süre param), sonra pause. **Spinback:** 1→negatif, sonra durdur.
- **Scratch makroları** (`baby, chirp, transform, flare, tear`): `ScratchMacro(name, keyframes: List<(beatFrac, posOffsetFrames, crossfaderOpen)>)` tablosu, BPM'e ölçeklenir; host `deck_event`/`macro_start` ile tek komut yollar, her cihaz aynı deseni aynı anda çalar. **Trigger sınıfı** (geç ise atla).
- **Serbest scratch (Faz 3, sonra):** jog konumu ~60 Hz UDP ile akar; client 60-80 ms jitter buffer ile oynatır. İlk sürümde YAPMA; önce makrolar.

### 9.5 Cue / loop
- 8 hot cue (frame olarak, manifest/state içinde senkron), cue set/jump.
- Loop in/out, otomatik loop 1/2/4/8 beat, **loop roll** (1/8-1/2 beat, bırakınca kaldığı yerden devam).
- Loop, deck zaman çizelgesinin parçasıdır (konum hesabı loop'u sarmalar).

### 9.6 Analiz (host'ta tek sefer, kanonik PCM üzerinde, sonuç `track_manifest` ile)
- **BPM + beatgrid:** onset envelope + otokorelasyon (önce basit kendi implementasyonun). **Lisans uyarısı:** GPL/AGPL kütüphaneleri (aubio, libKeyFinder, Essentia) ticari/kapalı uygulamada sorun yaratır; kullanmadan önce kullanıcıya sor.
- **Key (Camelot):** chroma tabanlı, Faz 4.
- **Loudness:** BS.1770 benzeri (K-ağırlıklı, kapılı) → `trimDb`; ilk sürümde RMS tabanlı yaklaşık değer kabul.
- **Waveform tepe noktaları:** UI için, düşük çözünürlük.
- Analiz `Dispatchers.Default`'ta, ilerleme göstergesiyle; kullanıcıyı bloklamaz.

### 9.7 Çoklu telefona özel (Faz 3)
- **Hoparlör rolleri** (`FULL, LEFT, RIGHT, BASS, TREBLE`): rol, motorun **çıkış aşamasında** uygulanır (kanal seçimi + 120 Hz LR4 LPF/HPF). Host rolü `deck/role` mesajıyla atar.
- **Ripple/dalga:** efekt/flaş cihaz sırasına göre **sabit ms ofsetle** kaydırılır (ofset = `index * stepMs`, host'un cihaz listesi sırası `state_snapshot`'ta).
- **Beatgrid'den flaş:** her cihaz beatgrid'i bildiği için flaşı yerelde planlar → `BassSync`'in beat başına ağ mesajı **kaldırılır** (kalan: `BassFlashControl` aç/kapa). Flaş, **FlashSafetyGuard**'dan geçmeden tetiklenmez. Torch gecikmesi cihaza özel olabilir; ayrı `flashNudgeMs` ayarı.
- **Mix kaydı:** master çıkışı (limiter sonrası) dosyaya yazılır (RT thread'de değil: lock-free ring buffer + yazıcı thread).

---

## 10. Kod tabanı düzeni (Kotlin)

```
com.soundboost.engine/
  NativeEngine.kt              // JNI sarmalayıcı (yalnızca ince)
  EngineController.kt          // deck/mixer durumu (StateFlow), engineDispatcher sahibi
  EventScheduler.kt            // hostNs → frame → komut
  TimeAnchorClient.kt          // native frame eşleme sarmalayıcı
  CanonicalConverter.kt        // MediaExtractor+MediaCodec → 44.1k s16 stereo
  TrackStore.kt                // trackId → dosya, manifest, durum (HOST+CLIENT)
com.soundboost.sync/
  net/UdpClockServer.kt, UdpClockClient.kt
  net/TrackHttpServer.kt (Ktor route), TrackHttpClient.kt
  ClockModel.kt                // Bölüm 5.2 (saf)
  HostConsole.kt               // DJ komutları → olay üretimi
  LeadTimeController.kt
  TransitionTemplates.kt       // saf fonksiyonlar
  ScratchMacros.kt
  analysis/BpmAnalyzer.kt, LoudnessAnalyzer.kt
```
`SyncViewModel` yalnızca UI durumunu ve yaşam döngüsünü yönetir; iş mantığı yukarıdaki sınıflara taşınır. `TrackStore`, `sessionId → File` haritasıdır (çoklu şarkı, iki deck, prefetch için zorunlu).

**Gradle/CMake notları:** `externalNativeBuild { cmake }`, `buildFeatures { prefab = true }`, Oboe'yu Maven AAR olarak ekle (**sürümü doğrula**), `abiFilters` en az `arm64-v8a`, C++17. `-O2/-O3`; `-ffast-math` YOK.

**Güç/ağ:** Parti modunda `WifiManager.WifiLock` (API 29+ `WIFI_MODE_FULL_LOW_LATENCY`, altında `WIFI_MODE_FULL_HIGH_PERF`) + `PARTIAL_WAKE_LOCK`; Wi-Fi güç tasarrufu gecikme sıçraması yapar. Audio focus: kaybında deck'ler PAUSE, kazanımda host kararı ile devam.

---

## 11. FAZ 0 — Mevcut koddaki hatalar (önce bunları düzelt)

1. **Host senkron başlangıç bozuk:** `startSharedMusic` bekledikten sonra `startPlayback` çağırıyor, o da `prepare()` yapıyor (50-300 ms) → host geç başlar. (Yeni motorda kalıcı çözüm; geçici: player'ı indirme/hazırlık biter bitmez prepare et, başlangıçta yalnız zamanla.)
2. **Playlist çoklu şarkı desteklemiyor:** `prepareMusic` her seferinde `hostMusicFile/currentSession`'ı eziyor; `getChunk` yalnız son şarkıyı verir. → `TrackStore` (`trackId → File`).
3. **EQ/volume çift broadcast:** `DJStateManager` callback'leri hem broadcast ediyor hem `updateDJEQ` ayrıca throttle'lı broadcast yapıyor. → Tek yol (`HostConsole`).
4. **`getChunk` yanlış okuyabilir:** `skip()`+`read()` istenen kadarı garanti etmez. → `RandomAccessFile.seek` + `readFully` (HTTP'ye geçince kalkar ama geçişe kadar düzelt).
5. **`localDelayUntil` `-1` döndürüyor ama çağıran `<= 0` ile hemen tetikliyor** → "çok geç, atla" davranışı hiç çalışmıyor. Çağıranlarda `-1` ayrı ele alınmalı (Trigger sınıfı mantığı).
6. **`DJEffectPlayer`:** 30 ms erken çalıyor + 10 ms coroutine polling jitter'ı. → Native sampler ile değişecek.
7. **`broadcast` içinde `async(SupervisorJob())`** yapılandırılmış eşzamanlılığı kırıyor → `async { }`.
8. **`SyncViewModel.currentPlaylist` ve `roomState` getter'ları** her erişimde **yeni** `MutableStateFlow` + yeni `collect` coroutine'i yaratıyor (sızıntı). → `roomStateManager` değiştiğinde `flatMapLatest` ile tek, kalıcı `StateFlow` (`stateIn`).
9. **`joinRoom` her çağrıda yeni collector'lar başlatıyor** (`connectionState`, `flashEvents`, `incomingMessages`, `audioChunkEvents`); tekrar katılmada çoğalır. → Collector'ları bir `Job` grubunda tut, `leaveRoom`'da iptal et.
10. **`SyncClient`:** `launch(SupervisorJob()) { ... }` ebeveynden koparıyor (iptal edilemeyen coroutine'ler). Ebeveyn scope içinde `launch { }` kullan.
11. **`SyncServer.stop()`** `connections.clear()`'ı mutex olmadan yapıyor.
12. **Protokol sürümü kontrol edilmiyor** (`Join.protocolVersion` hiç okunmuyor).
13. **`becomeHost`:** oda adı sabit ("Rezonans Room"). Önceki oda adı `HostTransfer`/state ile taşınmalı.
14. **Log gürültüsü:** `Log.d` yoğunluğu (ping, her mesaj). Release'te kapat; sıcak yollarda (EQ 30 Hz) loglama.

---

## 12. FAZLAR ve KABUL KRİTERLERİ

### Faz 0 — Temizlik (1-2 gün)
Bölüm 11'i düzelt; `TrackStore` kur; **HTTP track endpoint + paralel Range indirme + SHA-256** (base64-chunk yolu kaldırılır); `CanonicalConverter` (host'ta tek decode → 44.1k s16).
**Kabul:** (a) 3 farklı şarkıyı host ekler, 2 client'a iner, SHA-256 doğrulanır; (b) dosya inerken ping RTT'si belirgin artmaz (kontrol/veri ayrı); (c) mevcut eski oynatma hâlâ çalışır; (d) derleme temiz.

### Faz 1 — Çekirdek motor + SENKRON KANITI (kritik kilometre taşı: M1)
Oboe stream + `AudioEngine` + 1 `Deck` + `TimeAnchor` + `SpscQueue` + `EventScheduler` + `ClockModel` + UDP saat. **Önce yalnızca bu dikey dilim**: "kanonik PCM yükle → host-zamanlı PLAY → metronom/tık parçasıyla iki telefonda ölç".
**Kabul (M1):**
- Tık parçasıyla 2-3 cihazda dış mikrofon kaydı: medyan ofset ≤ 10 ms, p95 ≤ 20 ms (dahili hoparlör).
- 10 dakika çalmada ofset artmıyor (PLL çalışıyor; debug ekranında `anchorErr` ±2 ms içinde).
- Callback'te malloc/lock/log yok (kod incelemesi + `getXRunCount() == 0` normal yükte).
- `ClockModel`, frame eşleme, anchor/PLL matematiği için birim testleri geçiyor.
**M1 geçilmeden Faz 2'ye GEÇME.**

### Faz 2 — DJ çekirdeği
İki deck, Eq3, SvfFilter, crossfader, limiter, ramp komutu, Echo/Reverb/Flanger, loop/cue/quantize, BPM+beatgrid (+manuel yedek), SYNC, geçiş şablonları 1-3, native Sampler + ducking, `param_batch`/`ramp_event`/`state_snapshot`, late join. Sonunda **`USE_NATIVE_ENGINE` varsayılan AÇIK**, eski player/efekt kodu silinir.
**Kabul:** (a) Bass swap geçişi iki cihazda tıksız ve çukursuz; (b) EQ kill gerçekten sessiz; (c) knob çevirirken zipper noise yok; (d) air horn sample'ı iki cihazda ≤ 20 ms farkla çalıyor; (e) late join 5 sn içinde ≤ 30 ms içinde katılıyor; (f) offline render testleri (EQ düz toplam, crossfader güç sabiti, limiter tavanı) geçiyor.

### Faz 3 — Şov
Scratch makroları, brake/spinback, kalan geçişler (4-6), Auto-DJ, hoparlör rolleri, ripple, beatgrid'den flaş (FlashSafetyGuard'lı), mix kaydı. Ardından (ayrı karar) serbest scratch.

### Faz 4 — İleri
Mikrofonla otomatik gecikme kalibrasyonu (chirp + korelasyon), key-lock (SoundTouch LGPL: dinamik bağlama/lisans onayı, ya da WSOLA), key tespiti/Camelot önerileri, gerçek stem ayrıştırma (ayrı karar).

---

## 13. TEST ve ÖLÇÜM

**Birim testler (Kotlin, JVM):** `ClockModel` (sentetik jitter + skew + asimetri ile; düşük RTT seçimi; skew tahmini), frame eşleme, anchor beklenen-konum hesabı (loop dahil), geçiş şablonlarının ürettiği olay listeleri (deterministik), protokol serileştirme gidiş-dönüş (NaN koruması dahil), lead-time kontrolcüsü.

**Birim testler (C++, masaüstü):** `render()` ile offline: Eq3 toplam yanıtı, kill = sessizlik, crossfader equal-power güç sabiti, limiter tavanı, ramp eğrileri, SPSC kuyruk (iki thread stres), olay sıralama + frame-hassas uygulama, PLL yakınsaması (sentetik drift ile).

**Gerçek cihaz testleri:**
- **Tık testi:** her cihaz timeline'daki aynı beat'lerde tık çalar; tek harici mikrofonla kayıt, tıklar arası ofset analizi. Uygulamada gizli "Sync Test" modu (metronom).
- **Debug paneli** (host+client): clock offset, RTT, skew (ppm), model hazır mı, anchorErr, PLL trim, XRun sayısı, çıkış gecikmesi tahmini, kuyruk doluluğu.
- Senaryolar: zayıf Wi-Fi, bir cihaz uyku/arka plan, kulaklık takıp çıkarma (stream restart), host'un uygulamadan çıkması (host transfer), 8+ cihaz.

---

## 14. YASAKLAR ve SIK YAPILAN HATALAR

- `System.currentTimeMillis()` ile olay zamanlama. → nanoTime.
- Audio callback'te: `Log`, `new/malloc`, mutex, JNI, dosya, `std::vector` büyütme, `shared_ptr` ref-count paylaşımı.
- Birden fazla thread'den `NativeEngine.post`.
- `delay()`/`Handler` ile "zamanında" ses tetikleme.
- Parametreyi anlık atlatmak (smoother/ramp'sız).
- `MediaPlayer.setPlaybackParams` ile negatif hız (güvenilmez). Yeni motor varispeed'dir.
- Clientlarda şarkıyı yeniden decode etmek (D7).
- Her beat için ağ mesajı göndermek (beatgrid yerelde).
- Mevcut `@SerialName`'leri değiştirmek; FlashSafetyGuard'ı atlamak.
- `-ffast-math`; NaN/Infinity'yi motora sokmak.
- GPL/AGPL kütüphaneyi sormadan eklemek.
- "Büyük patlama" refactor; eski motoru kanıtsız silmek.
- Test etmeden "çalışıyor" demek; sürüm numarası/API uydurmak.
- M1 geçilmeden DJ özelliklerine (Faz 2+) başlamak.

---

## 15. Kullanıcıya SORULACAK açık noktalar (tahmin etme, sor)

1. **Minimum Android sürümü (minSdk) ve hedef cihaz sınıfı?** (AAudio/Oboe low-latency ve `WIFI_MODE_FULL_LOW_LATENCY` için önemli.)
2. **Maksimum şarkı süresi/boyutu limiti?** (RAM'de tutulan kanonik PCM: ~10 MB/dk; öneri: 12 dk üstünü reddet veya uyar.)
3. **BPM/key analizi için GPL kütüphane kabul edilir mi?** (Varsayılan: kendi basit implementasyon.)
4. **Bluetooth çıkış için yalnızca manuel nudge yeterli mi?**
5. **Protokol v1 uyumluluğu gerçekten gereksiz mi?** (D13 varsayılanı: gereksiz.)
6. **Yeni telefon rolü "co-DJ" (birden fazla kişi kontrol)** istenir mi? (Şimdilik tek host; ileride `HostConsole` yetki modeli.)

---

## EK: İlk teslimat (M1) için asgari iskelet — sırayla

1. `ClockModel.kt` + birim testleri.
2. `UdpClockServer/Client` + debug panelinde offset/RTT/skew göstergesi.
3. CMake + Oboe: sessiz stream açılıp kapanıyor, callback sayacı çalışıyor.
4. `SpscQueue`, `EngineCommand`, `AudioEngine.process` döngüsü (offline testli).
5. `TimeAnchor` + `localNsToFrame` JNI.
6. `Deck` (lineer interpolasyon) + RAM'e kanonik PCM yükleme + PLAY/PAUSE/SET_ANCHOR + PLL.
7. `EventScheduler` + `deck_event(PLAY)` + `track_manifest` + `state_snapshot` (yalnız deck timeline).
8. Metronom/tık parçası ile "Sync Test" modu ve ölçüm raporu.
Her adım sonunda derle, test et, raporla.
