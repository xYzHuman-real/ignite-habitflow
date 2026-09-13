package com.ignite.habitflow

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ink = Color(0xFF171717)
private val LightBackground = Color(0xFFF7F7F5)
private val LightSurface = Color.White
private val DarkBackground = Color(0xFF101010)
private val DarkSurface = Color(0xFF1A1A1A)
private val Blue = Color(0xFF2563EB)
private val Green = Color(0xFF15803D)

fun accentColor(name: String): Color = when (name) {
    "Blue" -> Blue
    "Green" -> Green
    else -> Ink
}

fun isDarkTheme(context: Context): Boolean {
    val prefs = context.getSharedPreferences("habitflow", Context.MODE_PRIVATE)
    return when (prefs.getString("theme", "System")) {
        "Dark" -> true
        "Light" -> false
        else -> (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}

@Composable
fun IgniteTheme(context: Context, content: @Composable () -> Unit) {
    val prefs = context.getSharedPreferences("habitflow", Context.MODE_PRIVATE)
    val accent = accentColor(prefs.getString("accent", "Ink") ?: "Ink")
    val dark = isDarkTheme(context)
    val scheme = if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            background = DarkBackground,
            surface = DarkSurface,
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            background = LightBackground,
            surface = LightSurface,
            onBackground = Ink,
            onSurface = Ink
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
