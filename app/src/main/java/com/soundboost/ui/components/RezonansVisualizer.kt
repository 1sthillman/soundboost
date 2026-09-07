package com.soundboost.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.soundboost.ui.theme.AppTheme

/**
 * Main Visualizer Composable - wraps the Canvas drawing functions
 */
@Composable
fun RezonansVisualizer(
    theme: AppTheme,
    audioLevels: FloatArray?,
    isActive: Boolean,
    sensitivity: Int,
    accent1: Color,
    accent2: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    // Apply sensitivity boost to audio levels (0-100 -> 0.55-1.65 multiplier matching HTML)
    val sensBoost = 0.55f + (sensitivity / 100f) * 1.1f
    val boostedLevels = audioLevels?.map { it * sensBoost }?.toFloatArray()
    
    // Dispatch to the appropriate visualizer based on theme
    when (theme) {
        AppTheme.MEHTAP -> ReefVisualizer(boostedLevels, isActive, accent1, accent2, backgroundColor, modifier)  // Water/moon theme
        AppTheme.SUMI -> SumiVisualizer(boostedLevels, isActive, accent1, accent2, backgroundColor, modifier)
        AppTheme.AURORA -> AuroraVisualizer(boostedLevels, isActive, accent1, accent2, backgroundColor, modifier)
        AppTheme.NOVA -> NovaVisualizer(boostedLevels, isActive, accent1, accent2, backgroundColor, modifier)
        AppTheme.MYCEL -> MycelVisualizer(boostedLevels, isActive, accent1, accent2, backgroundColor, modifier)
        AppTheme.REEF -> ReefVisualizer(boostedLevels, isActive, accent1, accent2, backgroundColor, modifier)
        AppTheme.MONSOON -> MonsoonVisualizer(boostedLevels, isActive, accent1, accent2, backgroundColor, modifier)
        AppTheme.MUREKKEP -> SumiVisualizer(boostedLevels, isActive, accent1, accent2, backgroundColor, modifier)  // Similar ink style
        AppTheme.COL -> NovaVisualizer(boostedLevels, isActive, accent1, accent2, backgroundColor, modifier)  // Similar heat/energy style
        AppTheme.DIVIT -> SumiVisualizer(boostedLevels, isActive, accent1, accent2, backgroundColor, modifier)  // Ink calligraphy style
    }
}
