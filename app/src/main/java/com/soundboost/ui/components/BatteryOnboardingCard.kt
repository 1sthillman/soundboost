package com.soundboost.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.soundboost.R
import com.soundboost.ui.theme.ThemeColors

/**
 * Modern bottom sheet style battery onboarding card
 * Positioned at bottom of screen with glassmorphic design
 */
@Composable
fun BatteryOnboardingCard(
    themeColors: ThemeColors,
    isVisible: Boolean,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp), // Yanlardan biraz boşluk
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = themeColors.surface.copy(alpha = 0.98f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 16.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp), // Daha kompakt padding
                verticalArrangement = Arrangement.spacedBy(14.dp) // Daha sıkı spacing
            ) {
                // Header with Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        themeColors.accent1.copy(alpha = 0.2f),
                                        themeColors.accent1.copy(alpha = 0.1f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = themeColors.accent1,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.battery_onboarding_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            ),
                            color = themeColors.onSurface
                        )
                        Text(
                            text = stringResource(R.string.battery_onboarding_subtitle),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp
                            ),
                            color = themeColors.onSurfaceVariant
                        )
                    }
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = themeColors.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                
                // Key Points - Compact horizontal chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BatteryFeatureChip(
                        icon = Icons.Default.CheckCircle,
                        text = stringResource(R.string.battery_onboarding_chip1),
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                    BatteryFeatureChip(
                        icon = Icons.Default.Speed,
                        text = stringResource(R.string.battery_onboarding_chip2),
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Action Buttons - Modern
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Dismiss Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = themeColors.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.battery_onboarding_dismiss),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Setup Button - Primary
                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f).height(48.dp),
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
                            text = stringResource(R.string.battery_onboarding_setup),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryFeatureChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    themeColors: ThemeColors,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.accent1.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = themeColors.onSurface,
                maxLines = 2,
                lineHeight = 15.sp
            )
        }
    }
}
