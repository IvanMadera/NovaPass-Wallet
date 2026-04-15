package com.example.novapass.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// NovaPass Premium Design System — NovaColors System
// ============================================================

object NovaColors {
    val BackgroundPrimary = Color(0xFF0B0F1A)
    val BackgroundSecondary = Color(0xFF121826)

    val GlassLight = Color.White.copy(alpha = 0.08f)
    val GlassMedium = Color.White.copy(alpha = 0.12f)
    val GlassStrong = Color.White.copy(alpha = 0.18f)

    val BorderSubtle = Color.White.copy(alpha = 0.10f)
    val BorderStrong = Color.White.copy(alpha = 0.20f)

    val AccentPrimary = Color(0xFF6C8CFF)    // Blue Fintech 2026
    val AccentSecondary = Color(0xFF9F7AEA)  // Purple Fintech 2026

    val Success = Color(0xFF22C55E)
    val Error = Color(0xFFEF4444)

    val TextPrimary = Color.White.copy(alpha = 0.90f)
    val TextSecondary = Color.White.copy(alpha = 0.60f)
}

// -- Legacy Mappings (Redirected to NovaColors) --
val NovaBackground = NovaColors.BackgroundPrimary
val NovaPrimary = NovaColors.AccentPrimary
val NovaOnPrimary = NovaColors.BackgroundPrimary

val NovaTextPrimary = NovaColors.TextPrimary
val NovaTextSecondary = NovaColors.TextSecondary
val NovaTextTertiary = Color.White.copy(alpha = 0.40f)

val NovaOnBackground = NovaTextPrimary
val NovaOnSurface = NovaTextSecondary

val NovaGlassCard = NovaColors.GlassLight
val NovaGlassSheet = NovaColors.GlassStrong
val NovaGlassInputDefault = NovaColors.GlassLight
val NovaGlassInputFocused = NovaColors.GlassMedium
val NovaGlassBorder = NovaColors.BorderSubtle

val NovaSurface = NovaColors.GlassMedium
val NovaSurfaceVariant = NovaColors.GlassStrong
val NovaInputBackground = NovaColors.GlassLight
val NovaError = NovaColors.Error

// -- Ambient Glow Assets (Alpha reduced as per rule) --
val NovaGlowGreen = Color(0xFF2DCA8C).copy(alpha = 0.12f)
val NovaGlowGold = Color(0xFFD4AF37).copy(alpha = 0.12f)
val NovaGlowBlue = Color(0xFF1D3A5F).copy(alpha = 0.15f)