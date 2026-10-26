package com.edan.kesef.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** כל הצבעים של האפליקציה במקום אחד. רוצה לשנות מראה? משנים כאן. */
object K {
    val Bg1 = Color(0xFF0F1226)
    val Bg2 = Color(0xFF1B1F3B)
    val Card = Color(0xFF232849)
    val Track = Color(0xFF2E3458)
    val Text = Color(0xFFF1F5F9)
    val Muted = Color(0xFF94A3B8)
    val Income = Color(0xFF34D399)
    val Expense = Color(0xFFFB7185)
    val Accent = Color(0xFFA78BFA)
    val Cyan = Color(0xFF22D3EE)
}

private val colors = darkColorScheme(
    primary = K.Income,
    onPrimary = Color(0xFF052E1C),
    secondary = K.Accent,
    background = K.Bg1,
    onBackground = K.Text,
    surface = K.Card,
    onSurface = K.Text,
    surfaceVariant = K.Track,
    onSurfaceVariant = K.Muted,
    surfaceContainer = K.Card,
    surfaceContainerHigh = K.Card,
    surfaceContainerHighest = K.Track,
    outline = K.Track,
    error = K.Expense,
)

@Composable
fun KesefTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}
