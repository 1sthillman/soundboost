package com.soundboost.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soundboost.R
import com.soundboost.data.BoostSettings
import com.soundboost.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    state: BoostSettings,
    onBack: () -> Unit
) {
    val themeColors = getThemeColors(state.theme, state.colorAccent)
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.help_and_faq),
                        fontWeight = FontWeight.Black,
                        fontSize = TextStyles.titleMedium,
                        letterSpacing = TextStyles.spacingWide
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
                .padding(horizontal = Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Spacer(Modifier.height(Spacing.xs))
            
            FAQItem(
                themeColors = themeColors,
                question = stringResource(R.string.faq_how_to_use),
                answer = stringResource(R.string.faq_how_to_use_answer)
            )
            
            FAQItem(
                themeColors = themeColors,
                question = stringResource(R.string.faq_not_working),
                answer = stringResource(R.string.faq_not_working_answer)
            )
            
            FAQItem(
                themeColors = themeColors,
                question = stringResource(R.string.faq_background_work),
                answer = stringResource(R.string.faq_background_work_answer)
            )
            
            FAQItem(
                themeColors = themeColors,
                question = stringResource(R.string.faq_battery),
                answer = stringResource(R.string.faq_battery_answer)
            )
            
            FAQItem(
                themeColors = themeColors,
                question = stringResource(R.string.faq_audio_quality),
                answer = stringResource(R.string.faq_audio_quality_answer)
            )
            
            FAQItem(
                themeColors = themeColors,
                question = stringResource(R.string.faq_device_support),
                answer = stringResource(R.string.faq_device_support_answer)
            )
            
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun FAQItem(
    themeColors: ThemeColors,
    question: String,
    answer: String
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(Corners.lg),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.surfaceElevated.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level1)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = themeColors.accent1,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        question,
                        fontSize = TextStyles.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onSurface,
                        letterSpacing = TextStyles.spacingNormal
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = themeColors.accent1
                )
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(Spacing.sm))
                    Divider(color = themeColors.outline.copy(alpha = 0.3f))
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        answer,
                        fontSize = TextStyles.bodyMedium,
                        color = themeColors.onSurfaceVariant,
                        lineHeight = TextStyles.bodyLarge
                    )
                }
            }
        }
    }
}
