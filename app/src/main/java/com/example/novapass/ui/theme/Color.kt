package com.example.novapass.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// NovaPass Premium Design System — NovaColors System
// Fuente única de verdad para todos los tokens de color.
// Usa siempre NovaColors.* en código nuevo.
// ============================================================

object NovaColors {

    // 🔳 Base
    val BackgroundPrimary   = Color(0xFF0A0D14)
    val BackgroundSecondary = Color(0xFF0F1422)

    // 🧊 Glass
    val GlassLight  = Color.White.copy(alpha = 0.06f)
    val GlassMedium = Color.White.copy(alpha = 0.10f)
    val GlassStrong = Color.White.copy(alpha = 0.14f)

    // 🟡 Dorado (acento principal)
    val GoldPrimary = Color(0xFFD4AF37)
    val GoldSoft    = Color(0xFFD4AF37).copy(alpha = 0.55f)
    val GoldGlow    = Color(0xFFD4AF37).copy(alpha = 0.22f)

    // 🟢 Verde/Cyan (acento secundario)
    val GreenPrimary = Color(0xFF1FAF9A)
    val GreenSoft    = Color(0xFF1FAF9A).copy(alpha = 0.45f)
    val GreenGlow    = Color(0xFF1FAF9A).copy(alpha = 0.18f)

    // ✏️ Texto
    val TextPrimary   = Color.White.copy(alpha = 0.9f)
    val TextSecondary = Color.White.copy(alpha = 0.6f)
    val TextTertiary  = Color.White.copy(alpha = 0.4f)

    // 🔲 Bordes
    val BorderSubtle = Color.White.copy(alpha = 0.08f)
    val BorderAccent = GoldSoft

    // ⚠️ Error
    val Error = Color(0xFFEF4444)
}

// ─────────────────────────────────────────────────────────────────────────
// Aliases de compatibilidad — apuntan a NovaColors para no romper
// el código existente. Migrar progresivamente a NovaColors.* directo.
// ─────────────────────────────────────────────────────────────────────────
val NovaBackground    = NovaColors.BackgroundPrimary
val NovaPrimary       = NovaColors.GoldPrimary
val NovaOnPrimary     = NovaColors.BackgroundPrimary

val NovaTextPrimary   = NovaColors.TextPrimary
val NovaTextSecondary = NovaColors.TextSecondary
val NovaTextTertiary  = NovaColors.TextTertiary

val NovaOnBackground  = NovaColors.TextPrimary
val NovaOnSurface     = NovaColors.TextSecondary

val NovaSurface         = NovaColors.GlassMedium
val NovaSurfaceVariant  = NovaColors.GlassStrong
val NovaInputBackground = NovaColors.GlassLight
val NovaGlassCard       = NovaColors.GlassLight
val NovaGlassSheet      = NovaColors.GlassStrong
val NovaGlassInputDefault  = NovaColors.GlassLight
val NovaGlassInputFocused  = NovaColors.GlassMedium
val NovaGlassBorder        = NovaColors.BorderSubtle
val NovaError              = NovaColors.Error

val NovaGlowGreen = NovaColors.GreenGlow
val NovaGlowGold  = NovaColors.GoldGlow
val NovaGlowBlue  = NovaColors.GreenGlow   // alias semántico

// ─────────────────────────────────────────────────────────────────────────
// Brushes premium reutilizables
// ─────────────────────────────────────────────────────────────────────────
object NovaBrushes {
    val GoldGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFEA9E),
            NovaColors.GoldPrimary,
            Color(0xFFB8860B),
            NovaColors.GoldPrimary
        )
    )

    val GlassTopLight = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = 0.15f), androidx.compose.ui.graphics.Color.Transparent),
        startY = 0f,
        endY   = 40f
    )
}