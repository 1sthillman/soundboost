package com.soundboost.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundboost.R
import com.soundboost.ui.theme.ThemeColors

@Composable
fun CallVolumeBoostCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    themeColors: ThemeColors
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                themeColors.accent1.copy(alpha = 0.15f)
            } else {
                themeColors.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.BluetoothAudio,
                        contentDescription = null,
                        tint = if (isEnabled) themeColors.accent1 else themeColors.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        stringResource(R.string.call_volume_boost_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = if (isEnabled) themeColors.accent1 else themeColors.onSurface
                    )
                }
                
                Text(
                    stringResource(R.string.call_volume_boost_desc),
                    fontSize = 11.sp,
                    color = themeColors.onSurface.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )
            }
            
            // Toggle Switch
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = themeColors.accent1,
                    checkedTrackColor = themeColors.accent1.copy(alpha = 0.5f)
                )
            )
        }
    }
}
