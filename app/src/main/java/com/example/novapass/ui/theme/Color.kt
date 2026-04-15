package com.example.novapass.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// NovaPass Premium Design System — Color Tokens
// ============================================

// -- Primary Background --
val NovaBackground = Color(0xFF0B0F1A)      // Deep ambient navy/black

// -- Accents --
val NovaPrimary = Color(0xFFD4AF37)         // Premium Gold Accent
val NovaOnPrimary = Color(0xFF0B0F1A)       // Dark text on gold

// -- Typography Hierarchy --
val NovaTextPrimary = Color.White.copy(alpha = 0.90f)
val NovaTextSecondary = Color.White.copy(alpha = 0.60f)
val NovaTextTertiary = Color.White.copy(alpha = 0.40f)

// (Legacy text colors mapping)
val NovaOnBackground = NovaTextPrimary
val NovaOnSurface = NovaTextSecondary

// -- Glassmorphism Tokens --
val NovaGlassCard = Color.White.copy(alpha = 0.05f)        // 5% - 8% for surface cards
val NovaGlassSheet = Color.White.copy(alpha = 0.12f)       // 10% - 12% for elevated bottom sheet
val NovaGlassInputDefault = Color.White.copy(alpha = 0.06f) // Inputs unfocused
val NovaGlassInputFocused = Color.White.copy(alpha = 0.10f) // Inputs focused
val NovaGlassBorder = Color.White.copy(alpha = 0.12f)      // Edge highlights

// -- Functional Colors --
val NovaSurface = NovaGlassCard             // Mapping Material surface
val NovaSurfaceVariant = NovaGlassSheet
val NovaInputBackground = NovaGlassInputDefault
val NovaError = Color(0xFFCF6679)           // Error state

// -- Glow/Ambient Colors --
val NovaGlowGreen = Color(0xFF2DCA8C)       // Very subtle glow accent
val NovaGlowGold = Color(0xFFD4AF37)        // Gold radial glow
val NovaGlowBlue = Color(0xFF1D3A5F)        // Background depth glow