package com.soundboost.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soundboost.R
import com.soundboost.ui.theme.*

@Composable
fun ShareDialog(
    themeColors: ThemeColors,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = themeColors.surfaceElevated,
        shape = RoundedCornerShape(Corners.xl),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    tint = themeColors.accent1,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    stringResource(R.string.share_app_title),
                    fontSize = TextStyles.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = themeColors.onSurface,
                    textAlign = TextAlign.Center,
                    letterSpacing = TextStyles.spacingWide
                )
            }
        },
        text = {
            Text(
                stringResource(R.string.share_app_message),
                fontSize = TextStyles.bodyMedium,
                color = themeColors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = TextStyles.bodyLarge
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    shareApp(context)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColors.accent1
                ),
                shape = RoundedCornerShape(Corners.md)
            ) {
                Text(
                    stringResource(R.string.share_now),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = TextStyles.spacingNormal
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = themeColors.onSurfaceVariant
                )
            ) {
                Text(
                    stringResource(R.string.cancel),
                    letterSpacing = TextStyles.spacingNormal
                )
            }
        }
    )
}

private fun shareApp(context: Context) {
    val packageName = context.packageName
    val shareText = context.getString(
        R.string.share_text,
        "https://play.google.com/store/apps/details?id=$packageName"
    )
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_via)))
}
