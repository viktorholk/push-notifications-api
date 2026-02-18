package com.viktorholk.apipushnotifications.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryBlue = Color(0xFF1554F0)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = PrimaryBlue,
            onPrimary = Color.White,
            primaryContainer = PrimaryBlue.copy(alpha = 0.3f), // Slightly transparent for dark mode container
            onPrimaryContainer = Color.White
        )
    } else {
        lightColorScheme(
            primary = PrimaryBlue,
            onPrimary = Color.White,
            primaryContainer = PrimaryBlue.copy(alpha = 0.1f), // Lighter for light mode container
            onPrimaryContainer = PrimaryBlue
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
