package com.soundboost.sync

import kotlinx.serialization.Serializable

/**
 * DJ EFFECT SYSTEM - Synchronized sound effects for Party Mode
 * 
 * CRITICAL REQUIREMENTS:
 * - Ultra-low latency broadcast (20ms max)
 * - Synchronized playback across ALL devices
 * - No interference with music playback
 * - Works even without music playing
 */

@Serializable
data class DJEffect(
    val id: String,
    val displayName: String,
    val fileName: String,
    val category: EffectCategory,
    val color: String // Hex color for button
)

enum class EffectCategory {
    DROP,      // Bass drops, drops
    RISER,     // Build-ups, risers
    IMPACT,    // Horns, impacts
    VOCAL,     // Voice samples
    MEME,      // Fun/meme sounds
    WEAPON     // Gunshots, etc
}

/**
 * Predefined DJ effects from assets
 */
object DJEffects {
    val all = listOf(
        // DROPS
        DJEffect("drop_bass", "BASS DROP", "drop-bass-boosted.mp3", EffectCategory.DROP, "#FF2D55"),
        DJEffect("bass_drop1", "DROP", "bassdrop.mp3", EffectCategory.DROP, "#FF1744"),
        DJEffect("drop_the_bass", "HEAVY DROP", "drop-the-bass_2.mp3", EffectCategory.DROP, "#D50000"),
        DJEffect("bass_edit", "BASS EDIT", "bass-drop-edit_mixdown.mp3", EffectCategory.DROP, "#C62828"),
        
        // RISERS
        DJEffect("build_up", "BUILD UP", "build-up.mp3", EffectCategory.RISER, "#00E5FF"),
        DJEffect("flashback", "FLASHBACK", "flashback-riser.mp3", EffectCategory.RISER, "#00B8D4"),
        DJEffect("sweep", "SWEEP", "130bpm-indo-remix-sweep-1.mp3", EffectCategory.RISER, "#0091EA"),
        
        // IMPACTS
        DJEffect("air_horn", "AIR HORN", "air-horn-sound-effect.mp3", EffectCategory.IMPACT, "#FFC400"),
        DJEffect("mlg_horn", "MLG", "mlg-horns-sound-effect.mp3", EffectCategory.IMPACT, "#FF6F00"),
        
        // VOCALS
        DJEffect("oh_shit", "OH SHIT", "oh-shit_4.mp3", EffectCategory.VOCAL, "#B388FF"),
        DJEffect("bruh", "BRUH", "bruh-echoed.mp3", EffectCategory.VOCAL, "#9C27B0"),
        DJEffect("sonic_woo", "WOO", "sonic-unleashed-woo.mp3", EffectCategory.VOCAL, "#E040FB"),
        DJEffect("helluva", "HELLUVA", "helluva-way-to-die.mp3", EffectCategory.VOCAL, "#7B1FA2"),
        
        // MEMES
        DJEffect("rickroll", "RICKROLL", "rick-rolled-meme-aetrim1602054550919.mp3", EffectCategory.MEME, "#FF6E40"),
        DJEffect("hayriabideneme", "SPECIAL", "hayriabiyayndeneme2.mp3", EffectCategory.MEME, "#FF3D00"),
        DJEffect("sans_titre", "MYSTERY", "sans-titre_4.mp3", EffectCategory.MEME, "#DD2C00"),
        
        // WEAPONS
        DJEffect("deagle", "DEAGLE", "desert-eagle-mw2-2009-gunshot.mp3", EffectCategory.WEAPON, "#78909C")
    )
    
    fun getById(id: String): DJEffect? = all.find { it.id == id }
    
    fun getByCategory(category: EffectCategory): List<DJEffect> = 
        all.filter { it.category == category }
}
