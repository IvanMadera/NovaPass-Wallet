package com.example.novapass.feature.tickets.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.novapass.core.design.theme.NovaColors
import com.example.novapass.core.design.theme.NovaSpacing

@Composable
fun DeveloperInfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        // Fondo oscuro (Scrim) — toca fuera para cerrar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NovaColors.Scrim)
                .clickable(
                    onClick = onDismiss,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            val dialogScale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "dialogScale"
            )

            // Contenedor principal para posicionar el botón de cerrar superpuesto (ancho 80%)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.80f)
                    .graphicsLayer { scaleX = dialogScale; scaleY = dialogScale }
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, NovaColors.GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .clickable(enabled = false) { }
            ) {
                NovaModalBackground {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(NovaSpacing.sm)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = NovaColors.White.copy(alpha = 0.3f)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = NovaSpacing.lg, vertical = NovaSpacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // ── Ícono principal ──────────────────────────────
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(NovaColors.GoldPrimary.copy(alpha = 0.12f))
                                    .border(1.5.dp, NovaColors.GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = "Developer",
                                    tint = NovaColors.GoldPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(NovaSpacing.md))

                            // ── Título y versión ─────────────────────────────
                            Text(
                                text = "NovaPass Wallet",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = NovaColors.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Versión 2026.05.15",
                                style = MaterialTheme.typography.labelMedium,
                                color = NovaColors.White.copy(alpha = 0.5f)
                            )

                            Spacer(modifier = Modifier.height(NovaSpacing.md))

                            // ── Crédito del desarrollador ────────────────────
                            Text(
                                text = "Desarrollado con ❤️ por",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NovaColors.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Ivan Madera",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = NovaColors.GoldPrimary
                            )

                            Spacer(modifier = Modifier.height(NovaSpacing.lg))

                            // ── GitHub Chip ───────────────────────────────────
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(NovaColors.GoldPrimary.copy(alpha = 0.1f))
                                    .border(1.dp, NovaColors.GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/IvanMadera"))
                                        context.startActivity(intent)
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "GitHub",
                                    tint = NovaColors.GoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "GitHub / IvanMadera",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = NovaColors.GoldPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(NovaSpacing.lg))

                            // ── Frase de cierre ──────────────────────────────
                            Text(
                                text = "Diseño limpio. Código limpio.\nExperiencia premium.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NovaColors.White.copy(alpha = 0.3f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
