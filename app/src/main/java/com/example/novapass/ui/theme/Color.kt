package com.example.novapass.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// NovaPass Premium Design System — NovaColors System
// ============================================================

object NovaColors {

    // 🔳 Base
    val BackgroundPrimary = Color(0xFF0A0D14)
    val BackgroundSecondary = Color(0xFF0F1422)

    // 🧊 Glass
    val GlassLight = Color.White.copy(alpha = 0.06f)
    val GlassMedium = Color.White.copy(alpha = 0.10f)
    val GlassStrong = Color.White.copy(alpha = 0.14f)

    // 🟡 Dorado (Primary Accent)
    val GoldPrimary = Color(0xFFD4AF37)
    val GoldSoft = Color(0xFFD4AF37).copy(alpha = 0.55f)
    val GoldGlow = Color(0xFFD4AF37).copy(alpha = 0.22f)

    // 🟢 Verde oscuro con tendencia a cyan (NO chillón)
    val GreenPrimary = Color(0xFF1FAF9A)
    val GreenSoft = Color(0xFF1FAF9A).copy(alpha = 0.45f)
    val GreenGlow = Color(0xFF1FAF9A).copy(alpha = 0.18f)

    // ✏️ Texto
    val TextPrimary = Color.White.copy(alpha = 0.9f)
    val TextSecondary = Color.White.copy(alpha = 0.6f)

    // 🔲 Bordes
    val BorderSubtle = Color.White.copy(alpha = 0.08f)
    val BorderAccent = GoldSoft
}

// -- Legacy Mappings (Redirected to NovaColors) --
val NovaBackground = NovaColors.BackgroundPrimary
val NovaPrimary = NovaColors.GoldPrimary
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
val NovaError = Color(0xFFEF4444)

// -- Ambient Glow Assets --
val NovaGlowGreen = NovaColors.GreenGlow
val NovaGlowGold = NovaColors.GoldGlow
val NovaGlowBlue = Color(0xFF1FAF9A).copy(alpha = 0.18f) // Redirected to Green glow to match identity