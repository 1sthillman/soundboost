# Synchronized Audio Streaming - Design Document

## Overview
Party mode özelliğine senkronize audio streaming ekleniyor. Host'un çaldığı müzik tüm client'larda ultra-low latency ile senkron çalacak.

## Architecture

### 1. Audio Capture (Host)
- **API**: MediaProjection (Android 10+) veya AudioPlaybackCapture (Android 10+)
- **Format**: PCM 16-bit, 48kHz stereo
- **Buffer**: 480 samples (10ms @ 48kHz)
- **Permission**: RECORD_AUDIO + MediaProjection consent

### 2. Audio Encoding
- **Codec**: Opus (ultra-low latency mode)
- **Bitrate**: 128kbps (music quality)
- **Frame Size**: 10ms (480 samples)
- **Latency**: ~10-20ms encoding

### 3. Network Protocol
```
UDP Broadcast Packet:
├─ Header (16 bytes)
│  ├─ Magic: 0xAB (1 byte)
│  ├─ Type: AUDIO_CHUNK (1 byte)
│  ├─ Timestamp: clock-synced millis (8 bytes)
│  ├─ Sequence: packet number (4 bytes)
│  └─ Size: payload size (2 bytes)
└─ Payload
   └─ Opus-encoded audio data
```

### 4. Clock Synchronization
- **Existing**: ClockSync.kt already implemented!
- **Usage**: Sync playback timing across devices
- **Precision**: ±10ms accuracy

### 5. Audio Playback (Client)
- **API**: AudioTrack (low-latency mode)
- **Buffer**: Jitter buffer (50-100ms)
- **Sync**: Play at clock-synced timestamp

### 6. Jitter Buffer
```
Client Jitter Buffer:
├─ Min Buffer: 50ms (2-3 packets)
├─ Max Buffer: 200ms
├─ Adaptive: Adjust based on network jitter
└─ Late Packet Handling: Interpolate or drop
```

## UI Design

### FlashControlScreen - New Section
```kotlin
// Add below Bass Flash Sync card
┌─────────────────────────────────────────┐
│  🎵 Synchronized Audio Streaming        │
├─────────────────────────────────────────┤
│  [Icon] Sync Audio                      │
│  Stream music to all connected devices  │
│  [Toggle Switch] OFF                    │
│                                         │
│  Quality: [High] [Standard] [Low]       │
│  Buffer: [50ms] [100ms] [150ms]        │
└─────────────────────────────────────────┘
```

### Host View
- **Permission Request**: MediaProjection dialog
- **Streaming Indicator**: "Streaming to 3 devices"
- **Quality Settings**: High/Standard/Low
- **Buffer Settings**: Latency vs stability

### Client View
- **Sync Status**: "Synced - 15ms latency"
- **Buffer Status**: "Buffer: 75ms"
- **Audio Quality Indicator**

## Implementation Plan

### Phase 1: Core Infrastructure (v1.5.1)
- [ ] `AudioCaptureManager.kt` - Host audio capture
- [ ] `AudioStreamEncoder.kt` - Opus encoding
- [ ] `AudioStreamDecoder.kt` - Opus decoding
- [ ] `AudioPlaybackManager.kt` - Client playback
- [ ] `JitterBuffer.kt` - Client buffer management

### Phase 2: Network Integration (v1.5.2)
- [ ] Extend `SyncMessage.kt` - Add AUDIO_CHUNK type
- [ ] Extend `SyncServer.kt` - Broadcast audio packets
- [ ] Extend `SyncClient.kt` - Receive and buffer audio

### Phase 3: UI & Controls (v1.5.3)
- [ ] Add UI toggle in `FlashControlScreen.kt`
- [ ] Quality/Buffer settings
- [ ] Permission handling in `MainActivity.kt`
- [ ] Status indicators and metrics

### Phase 4: Optimization (v1.5.4)
- [ ] Adaptive bitrate
- [ ] Packet loss recovery
- [ ] Latency optimization
- [ ] Battery optimization

## Technical Challenges

### 1. Permissions
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
```
- MediaProjection requires user consent dialog
- Cannot be auto-granted
- Must request each session

### 2. Audio Focus
- Host must handle audio focus conflicts
- Pause streaming when phone call comes
- Resume after interruption

### 3. Network Bandwidth
```
Bandwidth calculation:
- PCM: 48kHz * 16bit * 2ch = 1536 kbps
- Opus 128kbps: ~16KB/s per client
- 10 clients: 160KB/s upload
```

### 4. Latency Breakdown
```
Total latency: ~80-150ms
├─ Capture: 10ms
├─ Encode: 10-20ms
├─ Network: 20-50ms (WiFi)
├─ Decode: 10-20ms
├─ Buffer: 50-100ms
└─ Playback: <10ms
```

## Testing Strategy

### 1. Single Device Loop
- Host captures → encodes → decodes → plays back
- Verify audio quality and latency

### 2. Two Device Sync
- Host and 1 client
- Measure actual end-to-end latency
- Verify clock sync accuracy

### 3. Multi-Device (5-10)
- Stress test network capacity
- Verify all devices stay in sync
- Monitor battery consumption

### 4. Network Conditions
- Test under WiFi congestion
- Test with packet loss (1%, 5%, 10%)
- Test with variable latency

## User Experience

### Host Flow
1. Create party room
2. Enable "Sync Audio" toggle
3. Grant MediaProjection permission
4. Play music (any app)
5. All clients hear same audio synchronized

### Client Flow
1. Join party room
2. See "Sync Audio: Active" indicator
3. Audio automatically starts playing
4. Adjust local volume independently

## Performance Targets

- **Latency**: < 150ms end-to-end
- **Sync Accuracy**: ± 20ms between devices
- **Audio Quality**: Near-lossless (Opus 128kbps)
- **Battery**: < 5% drain per hour (host)
- **Network**: < 200KB/s per client

## Future Enhancements

- **Stereo Panning**: Spatial audio based on device position
- **Individual Volume**: Per-device volume control
- **Multi-Source**: Multiple hosts streaming simultaneously
- **Recording**: Save party session audio
- **Visualizer Sync**: Sync visualizer animations with audio

## Notes

- Opus codec library: `implementation("com.getkeepsafe.relinker:relinker:1.4.5")`
- MediaProjection requires Android 10+ (API 29+)
- Low-latency audio requires Android 8+ (API 26+)
- Real-world testing essential for timing tuning
