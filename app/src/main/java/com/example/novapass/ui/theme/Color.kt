package com.example.novapass.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// NovaPass Premium Design System — NovaColors System
// Fuente única de verdad para todos los tokens de color.
// Usa siempre NovaColors.* en código nuevo.
// ============================================================

object NovaColors {

    // 🔳 Base
    val White       = Color.White
    val Black       = Color.Black
    val Transparent = Color.Transparent

    val BackgroundPrimary   = Color(0xFF0A0D14)
    val BackgroundSecondary = Color(0xFF0F1422)
    val BackgroundAccent    = Color(0xFF1A1F35)
    val NavBar              = Color(0xFF0D1117)

    // 🧊 Glass
    val GlassLight  = White.copy(alpha = 0.06f)
    val GlassMedium = White.copy(alpha = 0.10f)
    val GlassStrong = White.copy(alpha = 0.14f)

    // 🟡 Dorado (acento principal)
    val GoldPrimary = Color(0xFFFFB700)
    val GoldBright  = Color(0xFFFFEA9E)
    val GoldDark    = Color(0xFFB8860B)

    // 🟢 Verde/Cyan (acento secundario)
    val GreenPrimary = Color(0xFF1FAF9A)
    val GreenDark    = Color(0xFF043927)
    val GreenBlack   = Color(0xFF03261B)
    val SapphireDark = Color(0xFF0D1B3E)

    // ✏️ Texto
    val TextPrimary   = White.copy(alpha = 0.9f)
    val TextSecondary = White.copy(alpha = 0.6f)

    // 🔲 Bordes
    val BorderSubtle = White.copy(alpha = 0.08f)

    // ⚠️ Error
    val Error = Color(0xFFEF4444)

    // 🌑 Overlays
    val Scrim = Black.copy(alpha = 0.6f)
}

// ─────────────────────────────────────────────────────────────────────────
// Brushes premium reutilizables
// ─────────────────────────────────────────────────────────────────────────
object NovaBrushes {
    val GoldGradient = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(
            NovaColors.GoldBright,
            NovaColors.GoldPrimary,
            NovaColors.GoldDark,
            NovaColors.GoldPrimary
        )
    )

    val GlassTopLight = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(NovaColors.White.copy(alpha = 0.15f), NovaColors.Transparent),
        startY = 0f,
        endY   = 40f
    )
}