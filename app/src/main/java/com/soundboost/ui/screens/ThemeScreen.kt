package com.soundboost.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundboost.R
import com.soundboost.data.BoostSettings
import com.soundboost.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    state: BoostSettings,
    onThemeChanged: (AppTheme) -> Unit,
    onColorAccentChanged: (ColorAccent) -> Unit,
    onBack: () -> Unit
) {
    val themeColors = getThemeColors(state.theme, state.colorAccent)
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.themes),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            
            item {
                Text(
                    "THEMES",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onSurface.copy(alpha = 0.6f)
                )
            }
            
            items(AppTheme.values().toList()) { theme ->
                ThemeCard(
                    theme = theme,
                    isSelected = state.theme == theme,
                    currentAccent = state.colorAccent,
                    onClick = { onThemeChanged(theme) }
                )
            }
            
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "ACCENT COLORS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onSurface.copy(alpha = 0.6f)
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ColorAccent.values().forEach { accent ->
                        ColorCircle(
                            accent = accent,
                            isSelected = state.colorAccent == accent,
                            onClick = { onColorAccentChanged(accent) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun ThemeCard(
    theme: AppTheme,
    isSelected: Boolean,
    currentAccent: ColorAccent,
    onClick: () -> Unit
) {
    val colors = getThemeColors(theme, currentAccent)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.accent1)
                )
                
                Text(
                    getThemeName(theme),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.accent1
                )
            }
        }
    }
}

@Composable
fun ColorCircle(
    accent: ColorAccent,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = when (accent) {
        ColorAccent.CYAN -> Color(0xFF00F5FF)
        ColorAccent.PINK -> Color(0xFFFF1493)
        ColorAccent.ORANGE -> Color(0xFFFF6B35)
        ColorAccent.GREEN -> Color(0xFF00FF88)
        ColorAccent.PURPLE -> Color(0xFFAB47BC)
    }
    
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun getThemeName(theme: AppTheme): String {
    return when (theme) {
        AppTheme.NEON_DARK -> stringResource(R.string.theme_neon_dark)
        AppTheme.OCEAN_BLUE -> stringResource(R.string.theme_ocean_blue)
        AppTheme.SUNSET_ORANGE -> stringResource(R.string.theme_sunset_orange)
        AppTheme.FOREST_GREEN -> stringResource(R.string.theme_forest_green)
        AppTheme.ROYAL_PURPLE -> stringResource(R.string.theme_royal_purple)
    }
}
