package com.example.aiassistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg           = Color(0xFF101013)
val Panel        = Color(0xFF18181C)
val BubbleUser   = Color(0xFF26262C)
val Accent       = Color(0xFF7AA2FF)
val TextPrimary  = Color(0xFFE9E9EC)
val TextSecondary= Color(0xFF9A9AA2)

private val DarkColors = darkColorScheme(
    primary = Accent,
    background = Bg,
    surface = Panel,
    surfaceVariant = BubbleUser,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
