============================================================
AI VOCAL SEPARATOR MODEL - INSTALLATION GUIDE
============================================================

CRITICAL: TensorFlow Lite model file required for AI-powered
vocal/music separation.

MODEL SPECIFICATIONS:
====================
- File name: vocal_separator.tflite
- Location: app/src/main/assets/vocal_separator.tflite
- Recommended size: 15-30MB (quantized INT8 or FP16)
- Architecture: Open-Unmix, Spleeter, or similar
- Input: Audio waveform (PCM float32, normalized -1 to 1)
- Output: 2 stems (vocals, music/instrumental)

WHERE TO GET THE MODEL:
=======================
Option 1: Open-Unmix TFLite
- Download pre-converted TFLite model
- GitHub: sigsep/open-unmix-pytorch
- Convert to TFLite using ai-audio-startkit

Option 2: Spleeter TFLite
- Pre-trained 2-stems model (vocals/accompaniment)
- Convert from Spleeter checkpoint to TFLite
- Use TensorFlow Model Optimization Toolkit

Option 3: Custom Training
- Train your own model on MUSDB18 dataset
- Export to TFLite with quantization
- Optimize for mobile (INT8 recommended)

INSTALLATION STEPS:
===================
1. Download or train the model file
2. Rename to: vocal_separator.tflite
3. Place in: app/src/main/assets/
4. Rebuild the app
5. The TFLiteStemSeparator will automatically load it

MODEL REQUIREMENTS:
===================
- Quantized (INT8 or FP16) for mobile performance
- Input tensor: [1, audio_length] float32
- Output tensors: [1, audio_length] x2 (vocals, music)
- GPU delegate compatible
- Size: <50MB for app store compliance

FALLBACK BEHAVIOR:
==================
If model file is missing:
- App continues to work normally
- Frequency-based vocal separation used as fallback
- DJ controls still functional
- Download/playback synchronization works perfectly

PERFORMANCE TARGETS:
====================
- Processing time: 20-40 seconds for 4-minute track
- Memory usage: <100MB during processing
- GPU acceleration: 10x faster than CPU
- Background processing: UI never freezes

TESTING:
========
1. Check logs for: "🤖 Initializing TensorFlow Lite model..."
2. If model loaded: "✅ Model loaded with GPU acceleration"
3. If model missing: "⚠️ Fallback to frequency-based separation"
4. Monitor separation progress in DJ console UI

OPTIMIZATION TIPS:
==================
- Use INT8 quantization for best speed/size balance
- Enable GPU delegate (already configured)
- Chunk processing (2-second blocks) prevents OOM
- Cache stems after first separation (instant reuse)

For more information, see:
- TFLiteStemSeparator.kt (implementation)
- AI_VOCAL_SEPARATION_IMPLEMENTATION_STATUS.md (docs)

============================================================
