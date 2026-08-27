package com.stdev.soundstboost.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stdev.soundstboost.R
import kotlinx.coroutines.delay

enum class TrainingStep {
    WELCOME,
    DOUBLE_TAP_INSTRUCTION,
    WAITING_FOR_DOUBLE_TAP,
    DRAG_INSTRUCTION,
    DRAGGING_PRACTICE,
    THEME_INSTRUCTION,
    COMPLETED
}

@Composable
fun InteractiveTrainingOverlay(
    currentStep: TrainingStep,
    onStepComplete: (TrainingStep) -> Unit,
    accentColor: Color,
    backgroundColor: Color,
    surfaceColor: Color,
    onSurfaceColor: Color,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = currentStep != TrainingStep.COMPLETED,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            when (currentStep) {
                TrainingStep.WELCOME -> WelcomeStep(accentColor, surfaceColor, onSurfaceColor) {
                    onStepComplete(TrainingStep.DOUBLE_TAP_INSTRUCTION)
                }
                TrainingStep.DOUBLE_TAP_INSTRUCTION -> DoubleTapInstruction(accentColor, surfaceColor, onSurfaceColor) {
                    onStepComplete(TrainingStep.WAITING_FOR_DOUBLE_TAP)
                }
                TrainingStep.WAITING_FOR_DOUBLE_TAP -> WaitingForAction(
                    stringResource(R.string.training_waiting_double_tap),
                    accentColor, surfaceColor, onSurfaceColor
                )
                TrainingStep.DRAG_INSTRUCTION -> DragInstruction(accentColor, surfaceColor, onSurfaceColor) {
                    onStepComplete(TrainingStep.DRAGGING_PRACTICE)
                }
                TrainingStep.DRAGGING_PRACTICE -> WaitingForAction(
                    stringResource(R.string.training_drag_to_continue),
                    accentColor, surfaceColor, onSurfaceColor
                )
                TrainingStep.THEME_INSTRUCTION -> ThemeInstruction(accentColor, surfaceColor, onSurfaceColor) {
                    onStepComplete(TrainingStep.COMPLETED)
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun WelcomeStep(
    accentColor: Color,
    surfaceColor: Color,
    onSurfaceColor: Color,
    onNext: () -> Unit
) {
    TrainingCard(accentColor, surfaceColor, onSurfaceColor) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "👋",
                fontSize = 64.sp
            )
            Text(
                text = stringResource(R.string.training_welcome_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.training_welcome_desc),
                fontSize = 16.sp,
                color = onSurfaceColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            
            Spacer(Modifier.height(8.dp))
            
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text(
                    stringResource(R.string.training_start),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun DoubleTapInstruction(
    accentColor: Color,
    surfaceColor: Color,
    onSurfaceColor: Color,
    onNext: () -> Unit
) {
    val scale by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    TrainingCard(accentColor, surfaceColor, onSurfaceColor) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "👆👆",
                fontSize = 64.sp * scale
            )
            Text(
                text = stringResource(R.string.training_double_tap_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.training_double_tap_desc),
                fontSize = 15.sp,
                color = onSurfaceColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(Modifier.height(8.dp))
            
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text(stringResource(R.string.training_understood), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DragInstruction(
    accentColor: Color,
    surfaceColor: Color,
    onSurfaceColor: Color,
    onNext: () -> Unit
) {
    TrainingCard(accentColor, surfaceColor, onSurfaceColor) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "☝️🔄",
                fontSize = 64.sp
            )
            Text(
                text = stringResource(R.string.training_drag_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.training_drag_desc),
                fontSize = 15.sp,
                color = onSurfaceColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(Modifier.height(8.dp))
            
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text(stringResource(R.string.training_try_it), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ThemeInstruction(
    accentColor: Color,
    surfaceColor: Color,
    onSurfaceColor: Color,
    onComplete: () -> Unit
) {
    TrainingCard(accentColor, surfaceColor, onSurfaceColor) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🎨",
                fontSize = 64.sp
            )
            Text(
                text = stringResource(R.string.training_theme_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.training_theme_desc),
                fontSize = 15.sp,
                color = onSurfaceColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(Modifier.height(8.dp))
            
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text(
                    stringResource(R.string.training_finish),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun WaitingForAction(
    message: String,
    accentColor: Color,
    surfaceColor: Color,
    onSurfaceColor: Color
) {
    val alpha by rememberInfiniteTransition().animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    TrainingCard(accentColor, surfaceColor, onSurfaceColor) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = accentColor.copy(alpha = alpha),
                strokeWidth = 4.dp,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = message,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TrainingCard(
    accentColor: Color,
    surfaceColor: Color,
    onSurfaceColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}
