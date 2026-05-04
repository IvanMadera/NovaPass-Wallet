package com.example.novapass.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val NovaColorScheme = darkColorScheme(
    primary = NovaColors.GoldPrimary,
    onPrimary = NovaColors.BackgroundPrimary,
    primaryContainer = NovaColors.GoldPrimary,
    onPrimaryContainer = NovaColors.BackgroundPrimary,
    
    secondary = NovaColors.GoldPrimary,
    onSecondary = NovaColors.BackgroundPrimary,
    secondaryContainer = NovaColors.GoldPrimary,
    onSecondaryContainer = NovaColors.BackgroundPrimary,
    
    tertiary = NovaColors.GoldPrimary,
    onTertiary = NovaColors.BackgroundPrimary,
    
    background = NovaColors.BackgroundPrimary,
    onBackground = NovaColors.TextPrimary,
    
    surface = NovaColors.GlassMedium,
    onSurface = NovaColors.TextPrimary,
    surfaceVariant = NovaColors.GlassStrong,
    onSurfaceVariant = NovaColors.TextSecondary,
    surfaceTint = NovaColors.BackgroundPrimary,
    
    outline = NovaColors.BorderSubtle,
    outlineVariant = NovaColors.BorderSubtle,
    
    error = NovaColors.Error
)

@Composable
fun NovaPassTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = NovaColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}