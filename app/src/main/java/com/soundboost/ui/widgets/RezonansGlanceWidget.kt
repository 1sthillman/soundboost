package com.soundboost.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.soundboost.MainActivity
import com.soundboost.data.BoostPreferences
import com.soundboost.ui.theme.AppTheme
import kotlinx.coroutines.flow.first

/**
 * Premium Rezonans Glance Widget
 * Compose-based interactive widget
 */
class RezonansGlanceWidget : GlanceAppWidget() {
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = BoostPreferences(context)
        val settings = prefs.settings.first()
        
        provideContent {
            RezonansWidgetContent(
                volumePercent = settings.masterGainPercent,
                sensitivity = settings.sensitivity,
                isActive = settings.isBoostEnabled,
                theme = settings.theme
            )
        }
    }
}

@Composable
fun RezonansWidgetContent(
    volumePercent: Int,
    sensitivity: Int,
    isActive: Boolean,
    theme: AppTheme
) {
    val themeColors = getWidgetThemeColors(theme)
    
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(themeColors.background))
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Brand
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "re",
                    style = TextStyle(
                        color = ColorProvider(Color(themeColors.onSurface)),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "zo",
                    style = TextStyle(
                        color = ColorProvider(Color(themeColors.accent1)),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "nans",
                    style = TextStyle(
                        color = ColorProvider(Color(themeColors.onSurfaceVariant)),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            Spacer(modifier = GlanceModifier.height(16.dp))
            
            // Volume Display
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$volumePercent%",
                    style = TextStyle(
                        color = ColorProvider(Color(if (isActive) themeColors.accent1 else themeColors.onSurfaceVariant)),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Spacer(modifier = GlanceModifier.height(8.dp))
                
                Text(
                    text = if (isActive) "ACTIVE" else "TAP TO OPEN",
                    style = TextStyle(
                        color = ColorProvider(Color(themeColors.onSurfaceVariant)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            Spacer(modifier = GlanceModifier.height(16.dp))
            
            // Theme Info
            Column(
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                Text(
                    text = getThemeName(theme),
                    style = TextStyle(
                        color = ColorProvider(Color(themeColors.onSurface)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Spacer(modifier = GlanceModifier.height(4.dp))
                
                Text(
                    text = getThemeDescription(theme),
                    style = TextStyle(
                        color = ColorProvider(Color(themeColors.onSurfaceVariant)),
                        fontSize = 11.sp
                    )
                )
            }
            
            Spacer(modifier = GlanceModifier.height(12.dp))
            
            // Volume Control
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Yükseltme",
                    style = TextStyle(
                        color = ColorProvider(Color(themeColors.onSurfaceVariant)),
                        fontSize = 11.sp
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                
                Text(
                    text = "$volumePercent",
                    style = TextStyle(
                        color = ColorProvider(Color(themeColors.onSurface)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            // Sensitivity Control
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hassasiyet",
                    style = TextStyle(
                        color = ColorProvider(Color(themeColors.onSurfaceVariant)),
                        fontSize = 11.sp
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                
                Text(
                    text = "$sensitivity",
                    style = TextStyle(
                        color = ColorProvider(Color(themeColors.onSurface)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

private data class WidgetThemeColors(
    val background: Long,
    val surface: Long,
    val onSurface: Long,
    val accent1: Long,
    val accent2: Long,
    val onSurfaceVariant: Long
)

private fun getWidgetThemeColors(theme: AppTheme): WidgetThemeColors {
    return when (theme) {
        AppTheme.MEHTAP -> WidgetThemeColors(0xFF040910, 0xFF0c1e30, 0xFFeef2f5, 0xFFf2b155, 0xFF4a6fa5, 0xFFb3bcc5)
        AppTheme.SUMI -> WidgetThemeColors(0xFF080705, 0xFF141210, 0xFFf1ece0, 0xFFc1442c, 0xFFe9e2d0, 0xFFa89988)
        AppTheme.AURORA -> WidgetThemeColors(0xFF02040a, 0xFF061024, 0xFFeef5ff, 0xFF33e6a8, 0xFF8a6bff, 0xFFa6c5e0)
        AppTheme.FENER -> WidgetThemeColors(0xFF040910, 0xFF0c1e30, 0xFFeef2f5, 0xFFd9b46a, 0xFF4a6fa5, 0xFFb3bcc5)
        AppTheme.ORMAN -> WidgetThemeColors(0xFF020a16, 0xFF0a2036, 0xFFe8f1fa, 0xFFffb35c, 0xFF6d87a0, 0xFFa3b8cc)
        AppTheme.EYES -> WidgetThemeColors(0xFF020a10, 0xFF04141e, 0xFFdff6ff, 0xFF2fd9c9, 0xFF0e5f7a, 0xFF9fd9e8)
        AppTheme.MYCEL -> WidgetThemeColors(0xFF020705, 0xFF06140c, 0xFFeafff0, 0xFF6dffb0, 0xFFffd58a, 0xFFb0e6c0)
        AppTheme.REEF -> WidgetThemeColors(0xFF010a10, 0xFF03151f, 0xFFd7fbff, 0xFF12e0bd, 0xFFff6bcf, 0xFF9fd9e8)
        AppTheme.MONSOON -> WidgetThemeColors(0xFF05070c, 0xFF0e1420, 0xFFe9eef7, 0xFF9cc2ff, 0xFFb48bff, 0xFFb5c5dd)
        AppTheme.MUREKKEP -> WidgetThemeColors(0xFF0a0806, 0xFF17120d, 0xFFf5ecd9, 0xFFc9a227, 0xFF8b1e3f, 0xFFc9b89a)
        AppTheme.COL -> WidgetThemeColors(0xFF0a0704, 0xFF1a120a, 0xFFffe9cf, 0xFFffb454, 0xFFff7a3d, 0xFFd9c4a8)
        AppTheme.DIVIT -> WidgetThemeColors(0xFF0a0d16, 0xFF141a2c, 0xFFf2ede0, 0xFFc9a35c, 0xFF8f98c9, 0xFFc1bcb0)
    }
}

private fun getThemeName(theme: AppTheme): String {
    return when (theme) {
        AppTheme.MEHTAP -> "Ay Balıkçısı"
        AppTheme.SUMI -> "Mürekkep Nefesi"
        AppTheme.AURORA -> "Kutup Şafağı"
        AppTheme.FENER -> "Fenerin Türküsü"
        AppTheme.ORMAN -> "Göl Kenarında Ateş"
        AppTheme.EYES -> "Derin Bakış"
        AppTheme.MYCEL -> "Yeraltı Fısıltısı"
        AppTheme.REEF -> "Derin Işıltı"
        AppTheme.MONSOON -> "Fırtına Öncesi"
        AppTheme.MUREKKEP -> "Mürekkep"
        AppTheme.COL -> "Çöl"
        AppTheme.DIVIT -> "Divit"
    }
}

private fun getThemeDescription(theme: AppTheme): String {
    return when (theme) {
        AppTheme.MEHTAP -> "Durgun suda sabırla bekleyen"
        AppTheme.SUMI -> "Fırça darbeleriyle beliren tek çizgi"
        AppTheme.AURORA -> "Gökyüzünde süzülen ışık"
        AppTheme.FENER -> "Sisli denize yol gösteren bekçi"
        AppTheme.ORMAN -> "Közlenen ateş ve göl"
        AppTheme.EYES -> "Okyanusun derinliklerinde"
        AppTheme.MYCEL -> "Kökler arasında yayılan ışık"
        AppTheme.REEF -> "Karanlıkta parıldayan biyolüminesans"
        AppTheme.MONSOON -> "Şimşek ve yağmurun ritmi"
        AppTheme.MUREKKEP -> "Suda açan mürekkep"
        AppTheme.COL -> "Kumların üstünde kıvrılan sıcak"
        AppTheme.DIVIT -> "Suda açan mürekkep ve altın"
    }
}

class RezonansGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RezonansGlanceWidget()
}
