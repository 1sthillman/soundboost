package com.soundboost.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.soundboost.MainActivity
import com.soundboost.R
import com.soundboost.data.BoostPreferences
import com.soundboost.service.BoostForegroundService
import kotlinx.coroutines.flow.first

/**
 * Premium Rezonans Widget (v1.4.0)
 * Modern Glance-based home screen widget
 * 
 * Features:
 * - Real-time boost status
 * - Quick ON/OFF toggle
 * - Volume adjustment (+/- buttons)
 * - Material 3 design
 */
class RezonansGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RezonansWidget()
}

class RezonansWidget : GlanceAppWidget() {
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            RezonansWidgetContent(context)
        }
    }
    
    @Composable
    fun RezonansWidgetContent(context: Context) {
        val prefs = BoostPreferences(context)
        
        // Read current state from preferences
        val currentSettings = currentState<androidx.datastore.preferences.core.Preferences>()
        val isEnabled = currentSettings[booleanPreferencesKey("is_boost_enabled")] ?: false
        val volumePercent = currentSettings[intPreferencesKey("master_gain_percent")] ?: 150
        
        GlanceTheme {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(R.drawable.widget_bg_active))
                    .padding(16.dp)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    // App Name
                    Text(
                        text = "Sound'ST Boost",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color.White)
                        )
                    )
                    
                    Spacer(GlanceModifier.height(8.dp))
                    
                    // Status & Volume Display
                    if (isEnabled) {
                        Text(
                            text = "$volumePercent%",
                            style = TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFFFFB74D))
                            )
                        )
                        
                        Text(
                            text = "ACTIVE",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = ColorProvider(Color(0xFF4CAF50))
                            )
                        )
                    } else {
                        Text(
                            text = "OFF",
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color.White.copy(alpha = 0.5f))
                            )
                        )
                    }
                    
                    Spacer(GlanceModifier.height(16.dp))
                    
                    // Control Buttons
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        // Decrease Volume
                        if (isEnabled) {
                            WidgetButton(
                                text = "−",
                                onClick = actionSendWidgetCommand(context, "DECREASE_VOLUME")
                            )
                            
                            Spacer(GlanceModifier.width(8.dp))
                        }
                        
                        // Toggle Button
                        WidgetButton(
                            text = if (isEnabled) "OFF" else "ON",
                            onClick = actionSendWidgetCommand(
                                context,
                                if (isEnabled) "STOP_BOOST" else "START_BOOST"
                            ),
                            primary = true
                        )
                        
                        // Increase Volume
                        if (isEnabled) {
                            Spacer(GlanceModifier.width(8.dp))
                            
                            WidgetButton(
                                text = "+",
                                onClick = actionSendWidgetCommand(context, "INCREASE_VOLUME")
                            )
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    fun WidgetButton(
        text: String,
        onClick: androidx.glance.action.Action,
        primary: Boolean = false
    ) {
        Box(
            modifier = GlanceModifier
                .width(60.dp)
                .height(40.dp)
                .background(
                    if (primary)
                        ImageProvider(R.drawable.widget_button_active)
                    else
                        ImageProvider(R.drawable.widget_button_inactive)
                )
                .clickable(onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color.White),
                    textAlign = TextAlign.Center
                )
            )
        }
    }
    
    fun actionSendWidgetCommand(context: Context, action: String): androidx.glance.action.Action {
        val intent = Intent(context, BoostForegroundService::class.java).apply {
            this.action = action
        }
        return actionStartService(intent)
    }
}

/**
 * Widget Update Worker
 * Called when boost state changes
 */
suspend fun updateWidgets(context: Context) {
    try {
        val glanceId = GlanceAppWidgetManager(context)
            .getGlanceIds(RezonansWidget::class.java)
        
        glanceId.forEach { id ->
            RezonansWidget().update(context, id)
        }
    } catch (e: Exception) {
        android.util.Log.e("Widget", "Failed to update widgets", e)
    }
}
