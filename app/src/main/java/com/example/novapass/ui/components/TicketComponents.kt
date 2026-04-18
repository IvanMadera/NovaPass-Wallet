package com.example.novapass.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.novapass.data.TicketEntity
import com.example.novapass.ui.theme.NovaColors
import com.example.novapass.ui.theme.NovaSpacing
import java.text.SimpleDateFormat
import java.util.Locale

// ──────────────────────────────────────────────────────────
// GlassCard — base container reutilizable con borde sutil
// ──────────────────────────────────────────────────────────
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 20.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(NovaColors.GlassLight)
            .border(
                width = 1.dp,
                color = NovaColors.BorderSubtle,
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        content()
    }
}

// ──────────────────────────────────────────────────────────
// TicketItem — tarjeta premium de boleto en la lista
// ──────────────────────────────────────────────────────────
@Composable
fun TicketItem(ticket: TicketEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    val categoryIcon = when (ticket.category) {
        "Concierto" -> Icons.Default.MusicNote
        "Deportes"  -> Icons.Default.SportsBaseball
        "Cine"      -> Icons.Default.Movie
        else        -> Icons.Default.ConfirmationNumber
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, label = "ticketScale")

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NovaSpacing.md, vertical = NovaSpacing.sm)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current) { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header con glow sutil
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NovaColors.GoldPrimary.copy(alpha = 0.02f),
                                NovaColors.GoldPrimary.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .padding(NovaSpacing.md)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(NovaColors.GlassMedium, CircleShape)
                            .border(1.dp, NovaColors.GoldPrimary.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            categoryIcon,
                            contentDescription = null,
                            tint = NovaColors.GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(NovaSpacing.md))
                    Text(
                        text = ticket.name.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Eliminar",
                            tint = NovaColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Perforación con BlendMode.Clear (efecto boleto físico)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                val halfH = size.height / 2f
                val notchR = 12.dp.toPx()
                drawRect(
                    color = NovaColors.GoldPrimary.copy(alpha = 0.08f),
                    size = Size(size.width, halfH)
                )
                drawCircle(
                    color = Color.Black,
                    radius = notchR,
                    center = Offset(-notchR * 0.1f, halfH),
                    blendMode = BlendMode.Clear
                )
                drawCircle(
                    color = Color.Black,
                    radius = notchR,
                    center = Offset(size.width + notchR * 0.1f, halfH),
                    blendMode = BlendMode.Clear
                )
                drawLine(
                    color = NovaColors.BorderSubtle,
                    start = Offset(notchR + 10.dp.toPx(), halfH),
                    end = Offset(size.width - notchR - 10.dp.toPx(), halfH),
                    strokeWidth = 1f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                )
            }

            // Body: fecha, hora, ubicación
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = NovaSpacing.md, end = NovaSpacing.md, bottom = NovaSpacing.md, top = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, null, tint = NovaColors.GoldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(NovaSpacing.sm))
                    Text(
                        ticket.eventDate ?: "Fecha TBD",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaColors.TextSecondary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(NovaSpacing.sm))
                    Icon(Icons.Default.AccessTime, null, tint = NovaColors.GoldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(NovaSpacing.xs))
                    val formattedTime = remember(ticket.eventTime) {
                        try {
                            if (ticket.eventTime.isNullOrBlank()) "--:--"
                            else {
                                val sdf24 = SimpleDateFormat("HH:mm", Locale.getDefault())
                                val sdf12 = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                val date = sdf24.parse(ticket.eventTime)
                                if (date != null) sdf12.format(date) else ticket.eventTime
                            }
                        } catch (e: Exception) {
                            ticket.eventTime ?: "--:--"
                        }
                    }
                    Text(
                        formattedTime,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(NovaSpacing.sm))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Weekend, null, tint = NovaColors.GoldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(NovaSpacing.sm))
                    val locationParts = mutableListOf<String>()
                    if (!ticket.section.isNullOrBlank()) locationParts.add("Sección ${ticket.section}")
                    if (!ticket.row.isNullOrBlank()) locationParts.add("Fila ${ticket.row}")
                    if (!ticket.seat.isNullOrBlank()) locationParts.add("Asiento ${ticket.seat}")
                    val locationText = if (locationParts.isEmpty()) "Boleto Digital" else locationParts.joinToString(" | ")
                    Text(
                        locationText.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = NovaColors.TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// EmptyStateView — estado vacío de la lista
// ──────────────────────────────────────────────────────────
@Composable
fun EmptyStateView(isSearch: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(NovaSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlassCard(
            cornerRadius = 32.dp,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.Default.ConfirmationNumber,
                    contentDescription = null,
                    tint = NovaColors.GoldPrimary,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(NovaSpacing.lg))
        Text(
            text = if (isSearch) "Sin coincidencias" else "Tu wallet está vacía",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(NovaSpacing.sm))
        Text(
            text = if (isSearch)
                "No encontramos boletos que coincidan con tu búsqueda."
            else
                "Agrega tu primer boleto tocando el botón de abajo",
            style = MaterialTheme.typography.bodyMedium,
            color = NovaColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
