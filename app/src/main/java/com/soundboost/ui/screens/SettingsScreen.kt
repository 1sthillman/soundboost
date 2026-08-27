package com.soundboost.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundboost.R
import com.soundboost.data.BoostSettings
import com.soundboost.ui.theme.getThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: BoostSettings,
    onAutoStartToggled: (Boolean) -> Unit,
    onMaximizeVolume: () -> Unit,
    onOpenThemes: () -> Unit,
    onBack: () -> Unit
) {
    val themeColors = getThemeColors(state.theme, state.colorAccent)
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = themeColors.background
                )
            )
        },
        containerColor = themeColors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            
            // Theme Button
            Card(
                onClick = onOpenThemes,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = null,
                            tint = themeColors.accent1
                        )
                        Column {
                            Text(
                                stringResource(R.string.themes),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.onSurface
                            )
                            Text(
                                stringResource(R.string.theme_customization),
                                fontSize = 12.sp,
                                color = themeColors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            
            // Auto Start
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.auto_start),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.auto_start_desc),
                            fontSize = 12.sp,
                            color = themeColors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = state.autoStartOnBoot,
                        onCheckedChange = onAutoStartToggled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = themeColors.accent1,
                            checkedTrackColor = themeColors.accent1.copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            // Microphone Permission kaldırıldı - Artık sistem ses capture kullanıyoruz
            
            // Maximize Volume
            Button(
                onClick = onMaximizeVolume,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColors.accent1
                )
            ) {
                Text(
                    stringResource(R.string.max_volume),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // About
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        stringResource(R.string.about),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.about_desc),
                        fontSize = 12.sp,
                        color = themeColors.onSurface.copy(alpha = 0.6f),
                        lineHeight = 16.sp
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
        }
    }
}
