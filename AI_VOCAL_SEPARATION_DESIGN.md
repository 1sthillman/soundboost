# AI-Based Real-Time Vocal Separation - Sistem Tasarımı

## 🎯 Proje Hedefi

**MediaProjection API + Spleeter/Demucs TFLite** kullanarak gerçek zamanlı vokal/müzik ayrımı yapmak.

---

## 📋 Genel Mimari

```
[Müzik Uygulaması] (YouTube, Amazon Music, vb.)
        ↓
[MediaProjection API] - Ses yakalama izni
        ↓
[AudioPlaybackCapture] - USAGE_MEDIA akışını yakala
        ↓
[Circular Buffer] - 4096 sample chunks (93ms @ 44.1kHz)
        ↓
[TFLite Inference Thread] - Spleeter 2-stems model
        ↓  ↓
    [Vocal]  [Music]
        ↓
[Volume Mixer] - Kullanıcı kontrolü
        ↓
[AudioTrack Output] - Karışım çıkışı
        ↓
[Kulaklık/Hoparlör]
```

---

## 🚧 Bilinen Kısıtlamalar

### ✅ Çalışan Uygulamalar:
- YouTube (app)
- YouTube Music
- Amazon Music
- Deezer
- Apple Music
- Poweramp
- Oyunlar
- Çoğu video oynatıcı

### ❌ Çalışmayan Uygulamalar (DRM Korumalı):
- **Spotify** (ALLOW_CAPTURE_BY_NONE policy)
- Google Chrome (bazı siteler)
- SoundCloud
- Netflix (video ses)

**Neden?** Bu uygulamalar `AudioAttributes.setAllowedCapturePolicy(ALLOW_CAPTURE_BY_NONE)` kullanıyor - DRM/lisans anlaşmaları yüzünden.

---

## 🏗️ Implementasyon Mimarisi

### Phase 1: MediaProjection Setup (Android 10+)

#### 1.1 Permission Flow

```kotlin
// VocalSeparationActivity.kt
class VocalSeparationActivity : ComponentActivity() {
    private val mediaProjectionManager by lazy {
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    
    private val requestMediaProjection = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val mediaProjection = mediaProjectionManager.getMediaProjection(
                result.resultCode,
                result.data!!
            )
            startAudioCapture(mediaProjection)
        }
    }
    
    fun requestAudioCapturePermission() {
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        requestMediaProjection.launch(intent)
    }
}
```

#### 1.2 AudioPlaybackCapture Configuration

```kotlin
// MediaAudioCapture.kt
@RequiresApi(Build.VERSION_CODES.Q)
class MediaAudioCapture(private val mediaProjection: MediaProjection) {
    
    private var audioRecord: AudioRecord? = null
    
    fun startCapture(onAudioDataCallback: (FloatArray) -> Unit) {
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()
        
        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(44100)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .build()
        
        audioRecord = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(config)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(BUFFER_SIZE)
            .build()
        
        audioRecord?.startRecording()
        
        // Read thread
        Thread {
            val buffer = ShortArray(CHUNK_SIZE)
            while (isCapturing) {
                val read = audioRecord?.read(buffer, 0, CHUNK_SIZE) ?: 0
                if (read > 0) {
                    val floatBuffer = buffer.map { it / 32768f }.toFloatArray()
                    onAudioDataCallback(floatBuffer)
                }
            }
        }.start()
    }
    
    companion object {
        const val SAMPLE_RATE = 44100
        const val CHUNK_SIZE = 4096  // 93ms latency
        const val BUFFER_SIZE = CHUNK_SIZE * 4
    }
}
```

---

### Phase 2: TFLite Model Integration

#### 2.1 Model Selection

| Model | Size | Latency | Quality | Recommendation |
|-------|------|---------|---------|----------------|
| **Spleeter 2-stems** | 40MB | ~200ms | Good | ✅ Başlangıç |
| Demucs Hybrid | 150MB | ~800ms | Excellent | ❌ Çok yavaş |
| Custom Lightweight | 15MB | ~100ms | Fair | 🔄 Gelecek |

**Seçim: Spleeter 2-stems (vocals + accompaniment)**

#### 2.2 Model Download & Conversion

```bash
# Python tarafında - model hazırlama
pip install spleeter tensorflow

# Export to SavedModel
spleeter separate -p spleeter:2stems -o output audio.mp3

# Convert to TFLite
python convert_to_tflite.py \
    --model spleeter_2stems \
    --output spleeter_2stems_float16.tflite \
    --quantize float16
```

#### 2.3 TFLite Inference Engine

```kotlin
// VocalSeparationEngine.kt
class VocalSeparationEngine(context: Context) {
    
    private var interpreter: Interpreter? = null
    private val inputBuffer = FloatArray(CHUNK_SIZE * 2)  // Stereo
    private val vocalOutput = FloatArray(CHUNK_SIZE * 2)
    private val musicOutput = FloatArray(CHUNK_SIZE * 2)
    
    init {
        loadModel(context)
    }
    
    private fun loadModel(context: Context) {
        val modelFile = context.assets.open("spleeter_2stems_float16.tflite")
            .use { it.readBytes() }
        
        val options = Interpreter.Options().apply {
            // GPU delegate for speed
            addDelegate(GpuDelegate())
            setNumThreads(4)
        }
        
        interpreter = Interpreter(ByteBuffer.wrap(modelFile), options)
        
        Log.d(TAG, "Model loaded: ${interpreter?.inputTensorCount} inputs, " +
                   "${interpreter?.outputTensorCount} outputs")
    }
    
    fun separate(audioChunk: FloatArray): SeparatedAudio {
        // STFT preprocessing
        val spectrogram = stft(audioChunk)
        
        // Model inference
        val inputs = arrayOf(spectrogram)
        val outputs = HashMap<Int, Any>().apply {
            put(0, vocalOutput)
            put(1, musicOutput)
        }
        
        interpreter?.runForMultipleInputsOutputs(inputs, outputs)
        
        // iSTFT postprocessing
        val vocalAudio = istft(vocalOutput)
        val musicAudio = istft(musicOutput)
        
        return SeparatedAudio(vocalAudio, musicAudio)
    }
    
    private fun stft(audio: FloatArray): Array<FloatArray> {
        // Short-Time Fourier Transform
        // Use JTransforms library or custom implementation
        val fft = FloatFFT_1D(CHUNK_SIZE)
        // ... implementation
        return arrayOf()  // [frequency_bins, time_frames]
    }
    
    private fun istft(spectrogram: FloatArray): FloatArray {
        // Inverse STFT
        // ... implementation
        return FloatArray(CHUNK_SIZE * 2)
    }
    
    companion object {
        const val CHUNK_SIZE = 4096
        const val TAG = "VocalSeparationEngine"
    }
}

data class SeparatedAudio(
    val vocals: FloatArray,
    val music: FloatArray
)
```

---

### Phase 3: Real-Time Processing Pipeline

#### 3.1 Circular Buffer System

```kotlin
// AudioCircularBuffer.kt
class AudioCircularBuffer(private val capacity: Int) {
    
    private val buffer = FloatArray(capacity)
    private var writePos = 0
    private var readPos = 0
    private var availableData = 0
    
    @Synchronized
    fun write(data: FloatArray): Int {
        val toWrite = minOf(data.size, capacity - availableData)
        
        for (i in 0 until toWrite) {
            buffer[writePos] = data[i]
            writePos = (writePos + 1) % capacity
        }
        
        availableData += toWrite
        return toWrite
    }
    
    @Synchronized
    fun read(output: FloatArray): Int {
        val toRead = minOf(output.size, availableData)
        
        for (i in 0 until toRead) {
            output[i] = buffer[readPos]
            readPos = (readPos + 1) % capacity
        }
        
        availableData -= toRead
        return toRead
    }
    
    fun available() = availableData
}
```

#### 3.2 Processing Pipeline Manager

```kotlin
// RealTimeVocalSeparation.kt
class RealTimeVocalSeparation(
    private val context: Context,
    private val mediaProjection: MediaProjection
) {
    
    private val audioCapture = MediaAudioCapture(mediaProjection)
    private val separationEngine = VocalSeparationEngine(context)
    private val inputBuffer = AudioCircularBuffer(BUFFER_SIZE)
    private val vocalBuffer = AudioCircularBuffer(BUFFER_SIZE)
    private val musicBuffer = AudioCircularBuffer(BUFFER_SIZE)
    
    private var vocalVolume = 1.0f
    private var musicVolume = 1.0f
    
    private val processingThread = Thread {
        val chunk = FloatArray(CHUNK_SIZE * 2)
        
        while (isRunning) {
            // Wait for enough data
            if (inputBuffer.available() >= chunk.size) {
                inputBuffer.read(chunk)
                
                // AI inference (blocking ~200ms)
                val separated = separationEngine.separate(chunk)
                
                // Write to output buffers
                vocalBuffer.write(separated.vocals)
                musicBuffer.write(separated.music)
                
                Log.d(TAG, "Processed chunk, latency: ${System.currentTimeMillis() - chunkTimestamp}ms")
            } else {
                Thread.sleep(10)  // Wait for more data
            }
        }
    }
    
    private val playbackThread = Thread {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .build()
            )
            .setBufferSizeInBytes(BUFFER_SIZE * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        
        audioTrack.play()
        
        val vocalChunk = FloatArray(PLAYBACK_CHUNK)
        val musicChunk = FloatArray(PLAYBACK_CHUNK)
        val mixedChunk = FloatArray(PLAYBACK_CHUNK)
        
        while (isRunning) {
            val vocalRead = vocalBuffer.read(vocalChunk)
            val musicRead = musicBuffer.read(musicChunk)
            
            if (vocalRead > 0 && musicRead > 0) {
                // Mix with volume control
                for (i in 0 until vocalRead) {
                    mixedChunk[i] = (vocalChunk[i] * vocalVolume + 
                                     musicChunk[i] * musicVolume).coerceIn(-1f, 1f)
                }
                
                audioTrack.write(mixedChunk, 0, vocalRead, AudioTrack.WRITE_BLOCKING)
            } else {
                Thread.sleep(5)
            }
        }
        
        audioTrack.stop()
        audioTrack.release()
    }
    
    fun start() {
        isRunning = true
        
        // Start capture
        audioCapture.startCapture { audioData ->
            inputBuffer.write(audioData)
            chunkTimestamp = System.currentTimeMillis()
        }
        
        // Start processing
        processingThread.start()
        playbackThread.start()
    }
    
    fun setVocalVolume(volume: Float) {
        vocalVolume = volume.coerceIn(0f, 2f)
    }
    
    fun setMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0f, 2f)
    }
    
    fun stop() {
        isRunning = false
        audioCapture.stop()
        separationEngine.release()
    }
    
    companion object {
        const val CHUNK_SIZE = 4096
        const val BUFFER_SIZE = CHUNK_SIZE * 8  // 8 chunks buffering
        const val PLAYBACK_CHUNK = 2048
        const val TAG = "RealTimeVocalSeparation"
        
        private var isRunning = false
        private var chunkTimestamp = 0L
    }
}
```

---

### Phase 4: UI Integration (Ayrı Sayfa)

#### 4.1 Navigation Setup

```kotlin
// MainActivity.kt - Equalizer'dan erişim
composable("equalizer") {
    EqualizerScreen(
        // ... existing params
        onNavigateToAISeparation = {
            navController.navigate("ai_vocal_separation")
        }
    )
}

composable("ai_vocal_separation") {
    AIVocalSeparationScreen(
        onBack = { navController.popBackStack() }
    )
}
```

#### 4.2 AI Separation Screen

```kotlin
// AIVocalSeparationScreen.kt
@Composable
fun AIVocalSeparationScreen(
    viewModel: AIVocalSeparationViewModel = viewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Vocal Separation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            StatusCard(
                isCapturing = uiState.isCapturing,
                latency = uiState.latency,
                supportedApp = uiState.detectedApp
            )
            
            // Start/Stop Button
            Button(
                onClick = {
                    if (uiState.isCapturing) {
                        viewModel.stopCapture()
                    } else {
                        viewModel.requestPermissionAndStart()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isCapturing) "Stop Separation" else "Start Separation")
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Vocal Volume Control
            VolumeControl(
                label = "Vocal Volume",
                value = uiState.vocalVolume,
                onValueChange = viewModel::setVocalVolume
            )
            
            // Music Volume Control
            VolumeControl(
                label = "Music Volume",
                value = uiState.musicVolume,
                onValueChange = viewModel::setMusicVolume
            )
            
            // Latency Info
            InfoCard(
                title = "Performance",
                info = """
                    Processing Latency: ${uiState.latency}ms
                    Buffer Fill: ${uiState.bufferFill}%
                    Dropped Frames: ${uiState.droppedFrames}
                """.trimIndent()
            )
            
            // Compatibility Info
            CompatibilityCard()
        }
    }
}

@Composable
private fun StatusCard(
    isCapturing: Boolean,
    latency: Int,
    supportedApp: String?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                if (isCapturing) "Active" else "Inactive",
                style = MaterialTheme.typography.titleLarge,
                color = if (isCapturing) Color.Green else Color.Gray
            )
            
            if (supportedApp != null) {
                Text("Detected: $supportedApp", style = MaterialTheme.typography.bodyMedium)
            }
            
            if (isCapturing) {
                Text("Latency: ${latency}ms", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CompatibilityCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Supported Apps", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "✅ YouTube, Amazon Music, Apple Music, Deezer\n" +
                "❌ Spotify (DRM protected)",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
```

---

### Phase 5: ViewModel & State Management

```kotlin
// AIVocalSeparationViewModel.kt
class AIVocalSeparationViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(AIVocalSeparationState())
    val uiState: StateFlow<AIVocalSeparationState> = _uiState.asStateFlow()
    
    private var mediaProjection: MediaProjection? = null
    private var separation: RealTimeVocalSeparation? = null
    
    fun requestPermissionAndStart() {
        // Trigger MediaProjection permission request
        // This will be handled in Activity
    }
    
    fun startCapture(mediaProjection: MediaProjection) {
        this.mediaProjection = mediaProjection
        
        try {
            separation = RealTimeVocalSeparation(getApplication(), mediaProjection)
            separation?.start()
            
            _uiState.update { it.copy(isCapturing = true) }
            
            // Start latency monitoring
            monitorPerformance()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start capture", e)
            _uiState.update { it.copy(error = e.message) }
        }
    }
    
    fun stopCapture() {
        separation?.stop()
        separation = null
        mediaProjection?.stop()
        mediaProjection = null
        
        _uiState.update { it.copy(isCapturing = false) }
    }
    
    fun setVocalVolume(volume: Float) {
        separation?.setVocalVolume(volume)
        _uiState.update { it.copy(vocalVolume = volume) }
    }
    
    fun setMusicVolume(volume: Float) {
        separation?.setMusicVolume(volume)
        _uiState.update { it.copy(musicVolume = volume) }
    }
    
    private fun monitorPerformance() {
        viewModelScope.launch {
            while (_uiState.value.isCapturing) {
                // Update performance metrics
                _uiState.update {
                    it.copy(
                        latency = calculateLatency(),
                        bufferFill = calculateBufferFill()
                    )
                }
                delay(1000)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopCapture()
    }
    
    companion object {
        const val TAG = "AIVocalSeparationVM"
    }
}

data class AIVocalSeparationState(
    val isCapturing: Boolean = false,
    val vocalVolume: Float = 1.0f,
    val musicVolume: Float = 1.0f,
    val latency: Int = 0,
    val bufferFill: Int = 0,
    val droppedFrames: Int = 0,
    val detectedApp: String? = null,
    val error: String? = null
)
```

---

## 📦 Dependencies

```gradle
dependencies {
    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    
    // Audio processing
    implementation("com.github.wendykierp:JTransforms:3.1")  // FFT
    
    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
}
```

---

## 🎯 Performance Targets

| Metric | Target | Acceptable | Unacceptable |
|--------|--------|------------|--------------|
| **Latency** | <250ms | 250-500ms | >500ms |
| **CPU Usage** | <40% | 40-70% | >70% |
| **Battery** | -15%/hr | -15-25%/hr | >25%/hr |
| **Memory** | <200MB | 200-350MB | >350MB |

---

## 🧪 Testing Strategy

### Test 1: Compatibility Check
```kotlin
fun testAppCompatibility() {
    val apps = listOf(
        "com.google.android.youtube",
        "com.spotify.music",
        "com.amazon.mp3"
    )
    
    apps.forEach { packageName ->
        val result = tryCapture(packageName)
        Log.d("Test", "$packageName: ${if (result) "✅" else "❌"}")
    }
}
```

### Test 2: Latency Measurement
```kotlin
fun measureLatency(): Long {
    val startTime = System.nanoTime()
    // Input audio chunk
    val separated = engine.separate(chunk)
    val endTime = System.nanoTime()
    return (endTime - startTime) / 1_000_000  // ms
}
```

### Test 3: Quality Assessment
- SISDR (Scale-Invariant Signal-to-Distortion Ratio)
- Subjective listening tests
- A/B comparison

---

## 🚀 Rollout Plan

### Phase 1 (Week 1-2): Proof of Concept
- ✅ MediaProjection capture working
- ✅ TFLite model loaded
- ✅ Basic separation (offline)

### Phase 2 (Week 3-4): Real-Time Pipeline
- ✅ Circular buffer system
- ✅ Multi-threading
- ✅ Audio playback

### Phase 3 (Week 5-6): UI Integration
- ✅ Separate screen from equalizer
- ✅ Volume controls
- ✅ Performance monitoring

### Phase 4 (Week 7-8): Optimization
- GPU acceleration
- Latency reduction
- Battery optimization

### Phase 5 (Week 9-10): Beta Testing
- Internal testing
- User feedback
- Bug fixes

---

## 📝 User Guide (App içi)

```markdown
# AI Vocal Separation (BETA)

## What it does
Separates vocals and music in real-time from any playing app.

## How to use
1. Tap "Start Separation"
2. Grant screen recording permission
3. Play music in YouTube/Amazon Music
4. Adjust vocal/music volume sliders

## Supported Apps
✅ YouTube, YouTube Music
✅ Amazon Music, Apple Music
✅ Deezer, Poweramp
✅ Most games

❌ Spotify (DRM protected)
❌ Chrome (some sites)
❌ Netflix

## Performance
- Latency: 200-400ms
- CPU: Moderate usage
- Battery: Higher drain during use

## Limitations
- Requires Android 10+
- Works best with headphones
- Some apps block audio capture
```

---

## 🔒 Privacy & Security

- ✅ No audio recording to disk
- ✅ All processing on-device
- ✅ No network transmission
- ✅ Permission required per session
- ✅ Clear user consent UI

---

## 📊 Success Metrics

- **Technical**: <300ms latency, <50% CPU
- **User**: 70%+ satisfaction rate
- **Adoption**: 10%+ users try feature
- **Retention**: 40%+ continue using

---

Hazır mısınız? Bu tasarımı adım adım implement edebiliriz! 🚀
