package com.example.novapass.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
    cornerRadius: Dp = 20.dp,
    showBorder: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(NovaColors.GlassLight)
            .then(
                if (showBorder) Modifier.border(
                    width = 1.dp,
                    color = NovaColors.BorderSubtle,
                    shape = RoundedCornerShape(cornerRadius)
                )
                else Modifier
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

    // Seguimiento dinámico de alturas para el dibujo del borde
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    var midHeightPx by remember { mutableFloatStateOf(0f) }

    GlassCard(
        showBorder = false, // Desactivamos el borde automático para dibujarlo manualmente
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NovaSpacing.md, vertical = NovaSpacing.sm)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                scaleX = scale
                scaleY = scale
            }
            .drawWithContent {
                drawContent()
                
                // Dibujo del Borde Manual Premium
                val notchRadius = 12.dp.toPx()
                val cornerR = 20.dp.toPx()
                val notchCenterY = headerHeightPx + (midHeightPx / 2f)
                
                val ticketPath = Path().apply {
                    // 1. Inicio: arriba a la izquierda (después de la curva)
                    moveTo(cornerR, 0f)
                    
                    // 2. Borde superior
                    lineTo(size.width - cornerR, 0f)
                    
                    // 3. Esquina superior derecha
                    arcTo(Rect(size.width - cornerR * 2, 0f, size.width, cornerR * 2), 270f, 90f, false)
                    
                    // 4. Lado derecho hasta la muesca
                    lineTo(size.width, notchCenterY - notchRadius)
                    
                    // 5. Muesca derecha (arco invertido)
                    arcTo(Rect(size.width - notchRadius, notchCenterY - notchRadius, size.width + notchRadius, notchCenterY + notchRadius), 270f, -180f, false)
                    
                    // 6. Lado derecho hasta el fondo
                    lineTo(size.width, size.height - cornerR)
                    
                    // 7. Esquina inferior derecha
                    arcTo(Rect(size.width - cornerR * 2, size.height - cornerR * 2, size.width, size.height), 0f, 90f, false)
                    
                    // 8. Borde inferior
                    lineTo(cornerR, size.height)
                    
                    // 9. Esquina inferior izquierda
                    arcTo(Rect(0f, size.height - cornerR * 2, cornerR * 2, size.height), 90f, 90f, false)
                    
                    // 10. Lado izquierdo hasta la muesca
                    lineTo(0f, notchCenterY + notchRadius)
                    
                    // 11. Muesca izquierda (arco invertido)
                    arcTo(Rect(-notchRadius, notchCenterY - notchRadius, notchRadius, notchCenterY + notchRadius), 90f, -180f, false)
                    
                    // 12. Lado izquierdo hasta el tope
                    lineTo(0f, cornerR)
                    
                    // 13. Esquina superior izquierda
                    arcTo(Rect(0f, 0f, cornerR * 2, cornerR * 2), 180f, 90f, false)
                    
                    close()
                }
                
                drawPath(
                    path = ticketPath,
                    color = NovaColors.BorderSubtle,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current) { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header con glow sutil
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { headerHeightPx = it.height.toFloat() }
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
                    .onSizeChanged { midHeightPx = it.height.toFloat() }
            ) {
                val halfH = size.height / 2f
                val notchR = 12.dp.toPx()
                
                // 1. Limpiar el fondo del "carril" de perforación
                drawRect(
                    color = NovaColors.GoldPrimary.copy(alpha = 0.08f),
                    size = Size(size.width, halfH)
                )

                // 2. Limpiar las muescas circulares
                drawCircle(
                    color = Color.Black,
                    radius = notchR,
                    center = Offset(0f, halfH),
                    blendMode = BlendMode.Clear
                )
                drawCircle(
                    color = Color.Black,
                    radius = notchR,
                    center = Offset(size.width, halfH),
                    blendMode = BlendMode.Clear
                )
                
                // 3. Línea punteada premium
                drawLine(
                    color = NovaColors.BorderSubtle,
                    start = Offset(notchR + 8.dp.toPx(), halfH),
                    end = Offset(size.width - notchR - 8.dp.toPx(), halfH),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaColors.TextSecondary
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
