package com.soundboost.ui.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.soundboost.R
import com.soundboost.data.BoostPreferences
import com.soundboost.service.BoostForegroundService
import kotlinx.coroutines.flow.first

/**
 * Volume Boost Widget - 1x1 Size (Small)
 * 
 * Features:
 * - Toggle boost on/off with single tap
 * - Shows current boost percentage
 * - Material 3 theming
 * - No ads, no data collection
 * 
 * Google Play Compliant:
 * - Standard Android widget API
 * - User-initiated actions only
 * - No background data access
 */
class VolumeBoostWidget : GlanceAppWidget() {
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent(context)
            }
        }
    }
    
    @Composable
    private fun WidgetContent(context: Context) {
        val prefs = BoostPreferences(context)
        val settings = androidx.compose.runtime.remember { 
            kotlinx.coroutines.runBlocking { prefs.settings.first() }
        }
        
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF1E1E1E)))
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(
                    onClick = actionStartService<BoostForegroundService>(
                        params = mapOf(
                            "action" to if (settings.isBoostEnabled) "STOP_BOOST" else "START_BOOST"
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Image(
                    provider = ImageProvider(
                        if (settings.isBoostEnabled) {
                            R.drawable.ic_volume_up
                        } else {
                            R.drawable.ic_volume_off
                        }
                    ),
                    contentDescription = "Volume Boost",
                    modifier = GlanceModifier.size(32.dp)
                )
                
                Spacer(modifier = GlanceModifier.height(4.dp))
                
                // Percentage
                if (settings.isBoostEnabled) {
                    Text(
                        text = "${settings.masterGainPercent}%",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color(0xFFFFD700))
                        )
                    )
                } else {
                    Text(
                        text = "OFF",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorProvider(Color(0xFF888888))
                        )
                    )
                }
            }
        }
    }
}

/**
 * Widget Receiver for 1x1 widget
 */
class VolumeBoostWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VolumeBoostWidget()
}
