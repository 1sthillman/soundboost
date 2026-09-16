package com.soundboost.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
                .wrapContentHeight() // YENİ: Auto height - tüm content'e göre
                .heightIn(max = 700.dp) // Max height ile güvenli alan
                .navigationBarsPadding()
                .imePadding(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = themeColors.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 24.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title with Icon - Daha modern ve kompakt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        themeColors.accent1.copy(alpha = 0.2f),
                                        themeColors.accent1.copy(alpha = 0.05f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = themeColors.accent1
                        )
                    }
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.battery_dialog_title),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            ),
                            color = themeColors.onSurface,
                            maxLines = 2
                        )
                        Text(
                            text = stringResource(R.string.battery_dialog_subtitle),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            ),
                            color = themeColors.accent1,
                            maxLines = 2
                        )
                    }
                }
                
                // Description - Daha kısa ve net
                Text(
                    text = stringResource(R.string.battery_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = themeColors.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Steps - Daha kompakt tasarım
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
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Device Specific Tips - Collapsible modern card
                var showTips by remember { mutableStateOf(false) }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTips = !showTips },
                    colors = CardDefaults.cardColors(
                        containerColor = themeColors.accent1.copy(alpha = 0.08f)
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = themeColors.accent1,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.battery_dialog_device_specific),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = themeColors.onSurface
                                )
                            }
                            Icon(
                                imageVector = if (showTips) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = themeColors.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        androidx.compose.animation.AnimatedVisibility(visible = showTips) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
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
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Buttons - Modern horizontal layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = themeColors.onSurfaceVariant
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            themeColors.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.battery_dialog_cancel),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Open Settings Button - Primary
                    Button(
                        onClick = {
                            onOpenSettings()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
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
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.surfaceElevated
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Step Number Badge - Daha modern
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                themeColors.accent1,
                                themeColors.accent1.copy(alpha = 0.8f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    ),
                    color = Color.White
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = themeColors.accent1,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = themeColors.onSurface,
                        maxLines = 2
                    )
                }
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = themeColors.onSurfaceVariant,
                    maxLines = 3
                )
            }
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
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(5.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(themeColors.accent1)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = device,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = themeColors.accent1
            )
            Text(
                text = tip,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                color = themeColors.onSurfaceVariant
            )
        }
    }
}
