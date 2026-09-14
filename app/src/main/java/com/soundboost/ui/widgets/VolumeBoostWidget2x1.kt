package com.soundboost.ui.widgets

import android.content.Context
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
 * Volume Boost Widget - 2x1 Size (Medium)
 * 
 * Features:
 * - Toggle on/off button
 * - 4 preset buttons: 60%, 100%, 150%, 200%
 * - Current status indicator
 * - Material 3 theming
 * 
 * Google Play Compliant:
 * - No data collection
 * - User-initiated actions
 */
class VolumeBoostWidget2x1 : GlanceAppWidget() {
    
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
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.Horizontal.Start,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                // Toggle button
                Box(
                    modifier = GlanceModifier
                        .size(48.dp)
                        .background(
                            if (settings.isBoostEnabled) {
                                ColorProvider(Color(0xFFFFD700))
                            } else {
                                ColorProvider(Color(0xFF333333))
                            }
                        )
                        .cornerRadius(24.dp)
                        .clickable(
                            onClick = actionStartService<BoostForegroundService>(
                                params = mapOf(
                                    "action" to if (settings.isBoostEnabled) "STOP_BOOST" else "START_BOOST"
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(
                            if (settings.isBoostEnabled) {
                                R.drawable.ic_volume_up
                            } else {
                                R.drawable.ic_volume_off
                            }
                        ),
                        contentDescription = "Toggle",
                        modifier = GlanceModifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = GlanceModifier.width(8.dp))
                
                // Preset buttons
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Horizontal.SpaceBetween,
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    PresetButton(context, "60", 60)
                    PresetButton(context, "100", 100)
                    PresetButton(context, "150", 150)
                    PresetButton(context, "200", 200)
                }
            }
        }
    }
    
    @Composable
    private fun PresetButton(context: Context, label: String, percent: Int) {
        Box(
            modifier = GlanceModifier
                .size(40.dp)
                .background(ColorProvider(Color(0xFF2A2A2A)))
                .cornerRadius(8.dp)
                .clickable(
                    onClick = actionStartService<BoostForegroundService>(
                        params = mapOf(
                            "action" to "SET_VOLUME",
                            "percent" to percent.toString()
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color.White)
                )
            )
        }
    }
}

/**
 * Widget Receiver for 2x1 widget
 */
class VolumeBoostWidget2x1Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VolumeBoostWidget2x1()
}
