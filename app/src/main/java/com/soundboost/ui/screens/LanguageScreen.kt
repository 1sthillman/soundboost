package com.soundboost.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundboost.R
import com.soundboost.data.AppLanguage
import com.soundboost.data.BoostSettings
import com.soundboost.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    state: BoostSettings,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val themeColors = getThemeColors(state.theme, state.colorAccent)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            
            items(AppLanguage.values()) { language ->
                LanguageItem(
                    language = language,
                    isSelected = currentLanguage == language,
                    themeColors = themeColors,
                    onClick = { onLanguageSelected(language) }
                )
            }
            
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun LanguageItem(
    language: AppLanguage,
    isSelected: Boolean,
    themeColors: ThemeColors,
    onClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.98f,
        animationSpec = spring(dampingRatio = 0.7f)
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .scale(scale)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) {
            themeColors.accent1.copy(alpha = 0.15f)
        } else {
            themeColors.surface.copy(alpha = 0.3f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = language.displayName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) themeColors.accent1 else themeColors.onSurface
                )
                if (language == AppLanguage.SYSTEM) {
                    val systemLangDisplay = remember {
                        com.soundboost.data.LanguageManager.getSystemLanguageDisplayName(context)
                    }
                    Text(
                        text = "${stringResource(R.string.language_system_desc)} → $systemLangDisplay",
                        fontSize = 13.sp,
                        color = themeColors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = themeColors.accent1,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
