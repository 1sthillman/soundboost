package com.soundboost.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

// Colors matching the HTML design
private val ColorBg = Color(0xFF0B0D16)
private val ColorCard = Color(0x3812152)
private val ColorInk = Color(0xFFF4F5FA)
private val ColorInkDim = Color(0xFF9AA0B4)
private val ColorAccent1 = Color(0xFF7C5CFF)
private val ColorAccent2 = Color(0xFF22D3C9)
private val ColorAccent3 = Color(0xFFFF6FA5)
private val ColorStarOff = Color(0xFF383C52)

data class RateTranslation(
    val title: String,
    val subtitle: String,
    val rateStoreBtn: String,
    val later: String,
    val placeholder: String,
    val submit: String,
    val thanksTitleHigh: String,
    val thanksTextHigh: String,
    val thanksTitleLow: String,
    val thanksTextLow: String,
    val done: String
)

private val translations = mapOf(
    "en" to RateTranslation(
        title = "Enjoying Sound Boost?",
        subtitle = "Tell us how we're doing — it only takes a few seconds.",
        rateStoreBtn = "Rate us on the Store",
        later = "Maybe later",
        placeholder = "What could we improve?",
        submit = "Send feedback",
        thanksTitleHigh = "Thank you! 🎉",
        thanksTextHigh = "Thanks for the love — taking you to the Store now.",
        thanksTitleLow = "Thanks for your feedback",
        thanksTextLow = "We read every note and use it to make Sound Boost better.",
        done = "Close"
    ),
    "tr" to RateTranslation(
        title = "Sound Boost'u beğendin mi?",
        subtitle = "Deneyimini birkaç saniyede puanla, bize çok yardımcı olur.",
        rateStoreBtn = "Mağazada Değerlendir",
        later = "Daha sonra",
        placeholder = "Neyi geliştirebiliriz?",
        submit = "Gönder",
        thanksTitleHigh = "Teşekkürler! 🎉",
        thanksTextHigh = "Seni mağazaya yönlendiriyoruz.",
        thanksTitleLow = "Geri bildirimin için teşekkürler",
        thanksTextLow = "Her notu okuyor ve Sound Boost'u geliştirmek için kullanıyoruz.",
        done = "Kapat"
    ),
    "de" to RateTranslation(
        title = "Gefällt dir Sound Boost?",
        subtitle = "Bewerte deine Erfahrung in wenigen Sekunden.",
        rateStoreBtn = "Im Store bewerten",
        later = "Vielleicht später",
        placeholder = "Was können wir verbessern?",
        submit = "Feedback senden",
        thanksTitleHigh = "Danke! 🎉",
        thanksTextHigh = "Wir leiten dich jetzt zum Store weiter.",
        thanksTitleLow = "Danke für dein Feedback",
        thanksTextLow = "Wir lesen jede Rückmeldung, um Sound Boost besser zu machen.",
        done = "Schließen"
    ),
    "fr" to RateTranslation(
        title = "Sound Boost vous plaît ?",
        subtitle = "Évaluez votre expérience en quelques secondes.",
        rateStoreBtn = "Noter sur le Store",
        later = "Plus tard",
        placeholder = "Que pourrions-nous améliorer ?",
        submit = "Envoyer",
        thanksTitleHigh = "Merci ! 🎉",
        thanksTextHigh = "Nous vous redirigeons vers le Store.",
        thanksTitleLow = "Merci pour votre retour",
        thanksTextLow = "Nous lisons chaque message pour améliorer Sound Boost.",
        done = "Fermer"
    ),
    "es" to RateTranslation(
        title = "¿Te gusta Sound Boost?",
        subtitle = "Califica tu experiencia, solo toma unos segundos.",
        rateStoreBtn = "Calificar en la Store",
        later = "Más tarde",
        placeholder = "¿Qué podríamos mejorar?",
        submit = "Enviar",
        thanksTitleHigh = "¡Gracias! 🎉",
        thanksTextHigh = "Te llevamos a la Store ahora.",
        thanksTitleLow = "Gracias por tu opinión",
        thanksTextLow = "Leemos cada comentario para mejorar Sound Boost.",
        done = "Cerrar"
    )
)

@Composable
fun RateAppDialog(
    themeColors: com.soundboost.ui.theme.ThemeColors,
    onDismiss: () -> Unit,
    onRated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var currentLang by remember {
        mutableStateOf(
            Locale.getDefault().language.lowercase().let { 
                if (translations.containsKey(it)) it else "en" 
            }
        )
    }
    var rating by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    var showThanks by remember { mutableStateOf(false) }
    var animatedRating by remember { mutableIntStateOf(0) }
    
    val trans = translations[currentLang] ?: translations["en"]!!
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x8C040508))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xEB1C1E2E)
                ),
                border = BorderStroke(1.dp, Color(0x14FFFFFF))
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .animateContentSize()
                ) {
                    // Top bar with language selector and close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Language selector with better contrast
                        Box(
                            modifier = Modifier
                                .background(
                                    Color(0x18FFFFFF),
                                    RoundedCornerShape(10.dp)
                                )
                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
                                .padding(horizontal = 11.dp, vertical = 7.dp)
                        ) {
                            LanguageDropdown(
                                currentLang = currentLang,
                                onLanguageChange = { currentLang = it }
                            )
                        }
                        
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(0x0FFFFFFF), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = ColorInkDim,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(14.dp))
                    
                    if (!showThanks) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .align(Alignment.CenterHorizontally)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(ColorAccent1, ColorAccent3)
                                    ),
                                    RoundedCornerShape(20.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // Title
                        Text(
                            text = trans.title,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorInk,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(Modifier.height(6.dp))
                        
                        // Subtitle
                        Text(
                            text = trans.subtitle,
                            fontSize = 13.5.sp,
                            color = ColorInkDim,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        )
                        
                        Spacer(Modifier.height(22.dp))
                        
                        // Stars
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            (1..5).forEach { index ->
                                val scale by animateFloatAsState(
                                    targetValue = if (animatedRating >= index) 1.2f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ), label = ""
                                )
                                
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "$index stars",
                                    tint = if (rating >= index) Color(0xFFFFD166) else ColorStarOff,
                                    modifier = Modifier
                                        .padding(3.dp)
                                        .size(38.dp)
                                        .scale(scale)
                                        .clickable {
                                            rating = index
                                            animatedRating = index
                                        }
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(22.dp))
                        
                        // Feedback field (only for low ratings)
                        if (rating in 1..3) {
                            OutlinedTextField(
                                value = feedback,
                                onValueChange = { feedback = it },
                                placeholder = { 
                                    Text(trans.placeholder, color = ColorInkDim) 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(78.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = ColorInk,
                                    unfocusedTextColor = ColorInk,
                                    focusedContainerColor = Color(0x0DFFFFFF),
                                    unfocusedContainerColor = Color(0x0DFFFFFF),
                                    focusedBorderColor = ColorAccent2,
                                    unfocusedBorderColor = Color(0x1AFFFFFF)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            Spacer(Modifier.height(14.dp))
                        }
                        
                        // Primary button
                        Button(
                            onClick = {
                                if (rating >= 4) {
                                    // High rating - go to Play Store
                                    scope.launch {
                                        showThanks = true
                                        delay(1500)
                                        openPlayStore(context)
                                        delay(500)
                                        onRated()
                                        onDismiss()
                                    }
                                } else if (rating > 0) {
                                    // Low rating - send feedback via email
                                    scope.launch {
                                        sendFeedbackEmail(context, rating, feedback, currentLang)
                                        showThanks = true
                                        delay(2000)
                                        onDismiss()
                                    }
                                }
                            },
                            enabled = rating > 0,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color(0x66383C52)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (rating > 0) {
                                            Brush.linearGradient(
                                                colors = listOf(ColorAccent1, ColorAccent3)
                                            )
                                        } else {
                                            Brush.linearGradient(
                                                colors = listOf(ColorStarOff, ColorStarOff)
                                            )
                                        },
                                        RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (rating >= 4) trans.rateStoreBtn else trans.submit,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = Color.White
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(10.dp))
                        
                        // Later button (only)
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = trans.later,
                                color = ColorInkDim,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Thanks stage
                        val scale by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ), label = ""
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .scale(scale)
                                .align(Alignment.CenterHorizontally)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(ColorAccent2, ColorAccent1)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        
                        Spacer(Modifier.height(14.dp))
                        
                        Text(
                            text = if (rating >= 4) trans.thanksTitleHigh else trans.thanksTitleLow,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorInk,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Text(
                            text = if (rating >= 4) trans.thanksTextHigh else trans.thanksTextLow,
                            fontSize = 14.5.sp,
                            color = ColorInkDim,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(Modifier.height(10.dp))
                        
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = trans.done,
                                color = ColorInkDim,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageDropdown(
    currentLang: String,
    onLanguageChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val languages = mapOf(
        "en" to "🇬🇧 English",
        "tr" to "🇹🇷 Türkçe",
        "de" to "🇩🇪 Deutsch",
        "fr" to "🇫🇷 Français",
        "es" to "🇪🇸 Español"
    )
    
    Box {
        Text(
            text = languages[currentLang] ?: "🇬🇧 English",
            color = ColorInk,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable { expanded = true }
                .padding(end = 18.dp)
        )
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xEB1C1E2E))
        ) {
            languages.forEach { (code, name) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            name,
                            color = ColorInk,
                            fontSize = 13.sp,
                            fontWeight = if (code == currentLang) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onLanguageChange(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun openPlayStore(context: Context) {
    val packageName = context.packageName
    try {
        // Try opening in Play Store app first
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Fallback to browser
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

private fun sendFeedbackEmail(context: Context, rating: Int, feedback: String, language: String) {
    android.util.Log.d("RateAppDialog", "📧 Sending email - Rating: $rating, Feedback length: ${feedback.length}, Content: '$feedback'")
    
    val trans = translations[language] ?: translations["en"]!!
    
    // Email subject based on language
    val subject = when (language) {
        "tr" -> "Sound Boost Geri Bildirimi - $rating Yıldız"
        "de" -> "Sound Boost Feedback - $rating Sterne"
        "fr" -> "Retour Sound Boost - $rating Étoiles"
        "es" -> "Opinión Sound Boost - $rating Estrellas"
        else -> "Sound Boost Feedback - $rating Stars"
    }
    
    // Email body with device info
    val deviceInfo = """
        
        
        ---
        Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
        Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})
        App Version: ${try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (e: Exception) { "Unknown" }}
    """.trimIndent()
    
    val body = """
        Rating: ${"⭐".repeat(rating)}
        
        Feedback:
        $feedback
        $deviceInfo
    """.trimIndent()
    
    android.util.Log.d("RateAppDialog", "📧 Email body:\n$body")
    
    try {
        // Use ACTION_SEND (not ACTION_SENDTO) for better Gmail compatibility with extras
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"  // Force email apps only
            putExtra(Intent.EXTRA_EMAIL, arrayOf("adistoww@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        // Try to open Gmail directly if available
        try {
            val gmailIntent = Intent(emailIntent).apply {
                setPackage("com.google.android.gm")
            }
            context.startActivity(gmailIntent)
            android.util.Log.d("RateAppDialog", "✅ Opened Gmail app with message")
        } catch (e: ActivityNotFoundException) {
            // Fallback to email chooser
            val chooserTitle = when (language) {
                "tr" -> "E-posta gönder"
                "de" -> "E-Mail senden"
                "fr" -> "Envoyer un e-mail"
                "es" -> "Enviar correo"
                else -> "Send email"
            }
            context.startActivity(Intent.createChooser(emailIntent, chooserTitle))
            android.util.Log.d("RateAppDialog", "✅ Opened email chooser")
        }
    } catch (e: Exception) {
        android.util.Log.e("RateAppDialog", "❌ Failed to send email: ${e.message}")
    }
}
