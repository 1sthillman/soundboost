package com.soundboost.ui.components

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
import androidx.compose.ui.window.Dialog
import com.soundboost.R

@Composable
fun FirstTimeInstructionsDialog(
    onDismiss: () -> Unit,
    accentColor: Color,
    backgroundColor: Color,
    surfaceColor: Color,
    onSurfaceColor: Color
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = surfaceColor
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "👋 ${stringResource(R.string.welcome_title)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Instructions
                InstructionItem(
                    icon = "🎯",
                    title = stringResource(R.string.instruction_1_title),
                    description = stringResource(R.string.instruction_1_desc),
                    accentColor = accentColor,
                    onSurfaceColor = onSurfaceColor
                )
                
                InstructionItem(
                    icon = "👆",
                    title = stringResource(R.string.instruction_2_title),
                    description = stringResource(R.string.instruction_2_desc),
                    accentColor = accentColor,
                    onSurfaceColor = onSurfaceColor
                )
                
                InstructionItem(
                    icon = "🎨",
                    title = stringResource(R.string.instruction_3_title),
                    description = stringResource(R.string.instruction_3_desc),
                    accentColor = accentColor,
                    onSurfaceColor = onSurfaceColor
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor
                    )
                ) {
                    Text(
                        text = stringResource(R.string.got_it),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionItem(
    icon: String,
    title: String,
    description: String,
    accentColor: Color,
    onSurfaceColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            fontSize = 28.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = onSurfaceColor.copy(alpha = 0.7f),
                lineHeight = 18.sp
            )
        }
    }
}
