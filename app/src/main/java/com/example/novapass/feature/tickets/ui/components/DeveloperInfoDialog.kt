package com.example.novapass.feature.tickets.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
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
                Box(modifier = Modifier.background(NovaColors.GreenBlack)) {
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
                                .padding(horizontal = 24.dp, vertical = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // ── Ícono principal ──────────────────────────────
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(NovaColors.GoldPrimary.copy(alpha = 0.10f))
                                    .border(1.5.dp, NovaColors.GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "Developer",
                                    tint = NovaColors.GoldPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // ── Título y versión ─────────────────────────────
                            Text(
                                text = "Ivan Madera",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 26.sp
                                ),
                                color = NovaColors.GoldPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Desarrollador de NovaPass Wallet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NovaColors.White.copy(alpha = 0.72f),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Hecho con",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NovaColors.White.copy(alpha = 0.48f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = NovaColors.GoldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Text(
                                text = "Version 2026.06.07",
                                style = MaterialTheme.typography.labelMedium,
                                color = NovaColors.White.copy(alpha = 0.42f),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(26.dp))

                            // ── GitHub Chip ───────────────────────────────────
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(NovaColors.GoldPrimary.copy(alpha = 0.06f))
                                    .border(1.dp, NovaColors.GoldPrimary.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/IvanMadera"))
                                        context.startActivity(intent)
                                    }
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
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
                                    text = "github.com/IvanMadera",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = NovaColors.GoldPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // ── Frase de cierre ──────────────────────────────
                            Text(
                                text = "Codigo, diseno y experiencia\ncuidados al detalle.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NovaColors.White.copy(alpha = 0.36f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
