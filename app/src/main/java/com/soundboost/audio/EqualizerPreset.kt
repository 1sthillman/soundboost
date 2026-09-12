package com.soundboost.audio

/**
 * 10-Band Parametric Equalizer Preset
 * Professional frequency response modeling
 */
data class EqualizerPreset(
    val name: String,
    val nameKey: String,  // For i18n
    // 10 bands: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
    val band31Hz: Float = 0f,
    val band62Hz: Float = 0f,
    val band125Hz: Float = 0f,
    val band250Hz: Float = 0f,
    val band500Hz: Float = 0f,
    val band1kHz: Float = 0f,
    val band2kHz: Float = 0f,
    val band4kHz: Float = 0f,
    val band8kHz: Float = 0f,
    val band16kHz: Float = 0f,
    val isCustom: Boolean = false
) {
    fun toBandArray(): FloatArray {
        return floatArrayOf(
            band31Hz, band62Hz, band125Hz, band250Hz, band500Hz,
            band1kHz, band2kHz, band4kHz, band8kHz, band16kHz
        )
    }
    
    companion object {
        // Frequency labels for UI
        val BAND_FREQUENCIES = arrayOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
        val BAND_FREQUENCIES_HZ = arrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
        
        // Create from array
        fun fromBandArray(name: String, nameKey: String, bands: FloatArray, isCustom: Boolean = false): EqualizerPreset {
            return EqualizerPreset(
                name = name,
                nameKey = nameKey,
                band31Hz = bands.getOrElse(0) { 0f },
                band62Hz = bands.getOrElse(1) { 0f },
                band125Hz = bands.getOrElse(2) { 0f },
                band250Hz = bands.getOrElse(3) { 0f },
                band500Hz = bands.getOrElse(4) { 0f },
                band1kHz = bands.getOrElse(5) { 0f },
                band2kHz = bands.getOrElse(6) { 0f },
                band4kHz = bands.getOrElse(7) { 0f },
                band8kHz = bands.getOrElse(8) { 0f },
                band16kHz = bands.getOrElse(9) { 0f },
                isCustom = isCustom
            )
        }
        
        /**
         * PROFESSIONAL PRESETS - 30+ Studio-Quality EQ Curves
         */
        
        // === GENRE PRESETS ===
        val FLAT = EqualizerPreset("Flat", "eq_flat", 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        
        val BASS_BOOST = EqualizerPreset(
            "Bass Boost", "eq_bass_boost",
            band31Hz = 8f, band62Hz = 6f, band125Hz = 4f, band250Hz = 2f, band500Hz = 0f,
            band1kHz = 0f, band2kHz = 0f, band4kHz = 0f, band8kHz = 0f, band16kHz = 0f
        )
        
        val TREBLE_BOOST = EqualizerPreset(
            "Treble Boost", "eq_treble_boost",
            band31Hz = 0f, band62Hz = 0f, band125Hz = 0f, band250Hz = 0f, band500Hz = 0f,
            band1kHz = 0f, band2kHz = 2f, band4kHz = 4f, band8kHz = 6f, band16kHz = 8f
        )
        
        val ROCK = EqualizerPreset(
            "Rock", "eq_rock",
            band31Hz = 5f, band62Hz = 3f, band125Hz = -2f, band250Hz = -3f, band500Hz = -1f,
            band1kHz = 2f, band2kHz = 4f, band4kHz = 5f, band8kHz = 5f, band16kHz = 5f
        )
        
        val POP = EqualizerPreset(
            "Pop", "eq_pop",
            band31Hz = -1f, band62Hz = 0f, band125Hz = 2f, band250Hz = 4f, band500Hz = 4f,
            band1kHz = 3f, band2kHz = 0f, band4kHz = -1f, band8kHz = -1f, band16kHz = -1f
        )
        
        val JAZZ = EqualizerPreset(
            "Jazz", "eq_jazz",
            band31Hz = 3f, band62Hz = 2f, band125Hz = 1f, band250Hz = 2f, band500Hz = -1f,
            band1kHz = -1f, band2kHz = 0f, band4kHz = 2f, band8kHz = 3f, band16kHz = 4f
        )
        
        val CLASSICAL = EqualizerPreset(
            "Classical", "eq_classical",
            band31Hz = 3f, band62Hz = 2f, band125Hz = -1f, band250Hz = -2f, band500Hz = -2f,
            band1kHz = 0f, band2kHz = 2f, band4kHz = 3f, band8kHz = 4f, band16kHz = 4f
        )
        
        val ELECTRONIC = EqualizerPreset(
            "Electronic", "eq_electronic",
            band31Hz = 6f, band62Hz = 5f, band125Hz = 2f, band250Hz = 0f, band500Hz = -2f,
            band1kHz = 1f, band2kHz = 0f, band4kHz = 2f, band8kHz = 5f, band16kHz = 6f
        )
        
        val HIP_HOP = EqualizerPreset(
            "Hip-Hop", "eq_hip_hop",
            band31Hz = 7f, band62Hz = 5f, band125Hz = 3f, band250Hz = 2f, band500Hz = -1f,
            band1kHz = -1f, band2kHz = 0f, band4kHz = 1f, band8kHz = 2f, band16kHz = 3f
        )
        
        val RNB = EqualizerPreset(
            "R&B", "eq_rnb",
            band31Hz = 4f, band62Hz = 6f, band125Hz = 5f, band250Hz = 2f, band500Hz = -1f,
            band1kHz = -2f, band2kHz = -1f, band4kHz = 2f, band8kHz = 3f, band16kHz = 4f
        )
        
        val METAL = EqualizerPreset(
            "Heavy Metal", "eq_metal",
            band31Hz = 6f, band62Hz = 4f, band125Hz = 2f, band250Hz = 3f, band500Hz = 1f,
            band1kHz = -2f, band2kHz = 1f, band4kHz = 5f, band8kHz = 6f, band16kHz = 6f
        )
        
        val COUNTRY = EqualizerPreset(
            "Country", "eq_country",
            band31Hz = 2f, band62Hz = 1f, band125Hz = 0f, band250Hz = 1f, band500Hz = 2f,
            band1kHz = 3f, band2kHz = 3f, band4kHz = 2f, band8kHz = 1f, band16kHz = 0f
        )
        
        val LATIN = EqualizerPreset(
            "Latin", "eq_latin",
            band31Hz = 5f, band62Hz = 4f, band125Hz = 2f, band250Hz = 0f, band500Hz = -1f,
            band1kHz = -1f, band2kHz = 2f, band4kHz = 4f, band8kHz = 5f, band16kHz = 5f
        )
        
        val BLUES = EqualizerPreset(
            "Blues", "eq_blues",
            band31Hz = 3f, band62Hz = 2f, band125Hz = 0f, band250Hz = -1f, band500Hz = 1f,
            band1kHz = 2f, band2kHz = 3f, band4kHz = 3f, band8kHz = 2f, band16kHz = 1f
        )
        
        val REGGAE = EqualizerPreset(
            "Reggae", "eq_reggae",
            band31Hz = 6f, band62Hz = 4f, band125Hz = 1f, band250Hz = -2f, band500Hz = -1f,
            band1kHz = 0f, band2kHz = -1f, band4kHz = 1f, band8kHz = 4f, band16kHz = 5f
        )
        
        val DANCE = EqualizerPreset(
            "Dance", "eq_dance",
            band31Hz = 6f, band62Hz = 5f, band125Hz = 3f, band250Hz = 1f, band500Hz = 0f,
            band1kHz = 2f, band2kHz = 3f, band4kHz = 4f, band8kHz = 4f, band16kHz = 3f
        )
        
        // === VOCAL ENHANCEMENT ===
        val VOCAL_BOOST = EqualizerPreset(
            "Vocal Boost", "eq_vocal_boost",
            band31Hz = -2f, band62Hz = -1f, band125Hz = 0f, band250Hz = 1f, band500Hz = 3f,
            band1kHz = 5f, band2kHz = 5f, band4kHz = 4f, band8kHz = 2f, band16kHz = 0f
        )
        
        // === ACOUSTIC ===
        val ACOUSTIC = EqualizerPreset(
            "Acoustic", "eq_acoustic",
            band31Hz = 4f, band62Hz = 3f, band125Hz = 2f, band250Hz = 1f, band500Hz = 2f,
            band1kHz = 3f, band2kHz = 3f, band4kHz = 2f, band8kHz = 2f, band16kHz = 1f
        )
        
        val PIANO = EqualizerPreset(
            "Piano", "eq_piano",
            band31Hz = 2f, band62Hz = 1f, band125Hz = 0f, band250Hz = 2f, band500Hz = 3f,
            band1kHz = 3f, band2kHz = 4f, band4kHz = 5f, band8kHz = 4f, band16kHz = 3f
        )
        
        // === ROOM SIMULATION ===
        val SMALL_ROOM = EqualizerPreset(
            "Small Room", "eq_small_room",
            band31Hz = -2f, band62Hz = -1f, band125Hz = 1f, band250Hz = 2f, band500Hz = 2f,
            band1kHz = 1f, band2kHz = 0f, band4kHz = -1f, band8kHz = -1f, band16kHz = -2f
        )
        
        val MEDIUM_ROOM = EqualizerPreset(
            "Medium Room", "eq_medium_room",
            band31Hz = -1f, band62Hz = 0f, band125Hz = 1f, band250Hz = 2f, band500Hz = 1f,
            band1kHz = 0f, band2kHz = -1f, band4kHz = -1f, band8kHz = 0f, band16kHz = 0f
        )
        
        val LARGE_HALL = EqualizerPreset(
            "Large Hall", "eq_large_hall",
            band31Hz = 2f, band62Hz = 2f, band125Hz = 1f, band250Hz = 0f, band500Hz = -1f,
            band1kHz = -2f, band2kHz = -2f, band4kHz = -1f, band8kHz = 0f, band16kHz = 1f
        )
        
        // === LISTENING ENVIRONMENT ===
        val HEADPHONES = EqualizerPreset(
            "Headphones", "eq_headphones",
            band31Hz = 3f, band62Hz = 5f, band125Hz = 3f, band250Hz = 1f, band500Hz = -1f,
            band1kHz = -2f, band2kHz = 1f, band4kHz = 3f, band8kHz = 5f, band16kHz = 6f
        )
        
        val SPEAKERS = EqualizerPreset(
            "Speakers", "eq_speakers",
            band31Hz = 4f, band62Hz = 3f, band125Hz = 1f, band250Hz = 0f, band500Hz = 1f,
            band1kHz = 2f, band2kHz = 2f, band4kHz = 1f, band8kHz = 0f, band16kHz = -1f
        )
        
        // === SPECIAL ===
        val DEEP_BASS = EqualizerPreset(
            "Deep Bass", "eq_deep_bass",
            band31Hz = 10f, band62Hz = 8f, band125Hz = 6f, band250Hz = 4f, band500Hz = 2f,
            band1kHz = 0f, band2kHz = -1f, band4kHz = -2f, band8kHz = -2f, band16kHz = -2f
        )
        
        val LIVE = EqualizerPreset(
            "Live", "eq_live",
            band31Hz = 2f, band62Hz = 1f, band125Hz = 2f, band250Hz = 3f, band500Hz = 3f,
            band1kHz = 3f, band2kHz = 3f, band4kHz = 2f, band8kHz = 2f, band16kHz = 2f
        )
        
        val VINYL = EqualizerPreset(
            "Vinyl", "eq_vinyl",
            band31Hz = 1f, band62Hz = 2f, band125Hz = 1f, band250Hz = 0f, band500Hz = 1f,
            band1kHz = 2f, band2kHz = 1f, band4kHz = -1f, band8kHz = -2f, band16kHz = -3f
        )
        
        val SOFT = EqualizerPreset(
            "Soft", "eq_soft",
            band31Hz = 2f, band62Hz = 1f, band125Hz = 0f, band250Hz = -1f, band500Hz = 0f,
            band1kHz = 1f, band2kHz = 2f, band4kHz = 3f, band8kHz = 4f, band16kHz = 5f
        )
        
        // ALL PRESETS
        val ALL_PRESETS = listOf(
            FLAT,
            BASS_BOOST, TREBLE_BOOST, DEEP_BASS,
            ROCK, POP, JAZZ, CLASSICAL, ELECTRONIC, HIP_HOP, RNB, METAL,
            COUNTRY, LATIN, BLUES, REGGAE, DANCE,
            VOCAL_BOOST, ACOUSTIC, PIANO,
            SMALL_ROOM, MEDIUM_ROOM, LARGE_HALL,
            HEADPHONES, SPEAKERS,
            LIVE, VINYL, SOFT
        )
    }
}
