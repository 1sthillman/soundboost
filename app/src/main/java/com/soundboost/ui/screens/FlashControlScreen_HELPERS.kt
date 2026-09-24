package com.soundboost.ui.screens

import androidx.compose.ui.graphics.Color

/**
 * Color luminance helper extension
 */
fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
