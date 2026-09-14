package com.soundboost.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.soundboost.R
import com.soundboost.ui.theme.ThemeColors

@Composable
fun BatteryOptimizationDialog(
    themeColors: ThemeColors,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = themeColors.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Title with Icon
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = themeColors.accent1
                )
                
                Text(
                    text = stringResource(R.string.battery_dialog_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = themeColors.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = stringResource(R.string.battery_dialog_subtitle),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = themeColors.accent1,
                    textAlign = TextAlign.Center
                )
                
                // Description
                Text(
                    text = stringResource(R.string.battery_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Steps
                BatteryOptimizationStep(
                    number = "1",
                    title = stringResource(R.string.battery_dialog_step1_title),
                    description = stringResource(R.string.battery_dialog_step1_desc),
                    icon = Icons.Default.Settings,
                    themeColors = themeColors
                )
                
                BatteryOptimizationStep(
                    number = "2",
                    title = stringResource(R.string.battery_dialog_step2_title),
                    description = stringResource(R.string.battery_dialog_step2_desc),
                    icon = Icons.Default.Search,
                    themeColors = themeColors
                )
                
                BatteryOptimizationStep(
                    number = "3",
                    title = stringResource(R.string.battery_dialog_step3_title),
                    description = stringResource(R.string.battery_dialog_step3_desc),
                    icon = Icons.Default.CheckCircle,
                    themeColors = themeColors
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Device Specific Tips
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = themeColors.accent1.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = themeColors.accent1,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.battery_dialog_device_specific),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = themeColors.onSurface
                            )
                        }
                        
                        DeviceTip(
                            device = "Xiaomi",
                            tip = stringResource(R.string.battery_dialog_tip_xiaomi),
                            themeColors = themeColors
                        )
                        
                        DeviceTip(
                            device = "Samsung",
                            tip = stringResource(R.string.battery_dialog_tip_samsung),
                            themeColors = themeColors
                        )
                        
                        DeviceTip(
                            device = "OnePlus",
                            tip = stringResource(R.string.battery_dialog_tip_oneplus),
                            themeColors = themeColors
                        )
                        
                        DeviceTip(
                            device = "Huawei",
                            tip = stringResource(R.string.battery_dialog_tip_huawei),
                            themeColors = themeColors
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = themeColors.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.battery_dialog_cancel),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    // Open Settings Button
                    Button(
                        onClick = {
                            onOpenSettings()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColors.accent1
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.battery_dialog_open_settings),
                            modifier = Modifier.padding(vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryOptimizationStep(
    number: String,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    themeColors: ThemeColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Step Number Badge
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(themeColors.accent1),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = themeColors.accent1,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = themeColors.onSurface
                )
            }
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun DeviceTip(
    device: String,
    tip: String,
    themeColors: ThemeColors
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall,
            color = themeColors.accent1,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = tip,
            style = MaterialTheme.typography.bodySmall,
            color = themeColors.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}
