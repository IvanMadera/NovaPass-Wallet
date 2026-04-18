package com.example.novapass.ui.components

import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.novapass.data.TicketEntity
import com.example.novapass.ui.theme.NovaColors
import com.example.novapass.ui.theme.NovaSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Composable
fun TicketViewerDialog(
    ticket: TicketEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uri = Uri.parse(ticket.uri)
    
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    val renderMutex = remember { Mutex() }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    
    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale == 1f) {
            offset = androidx.compose.ui.geometry.Offset.Zero
        } else {
            val maxX = (screenWidthPx * 0.85f * scale - screenWidthPx * 0.85f) / 2f
            val maxY = (screenHeightPx * 0.7f * scale - screenHeightPx * 0.7f) / 2f
            val newX = (offset.x + offsetChange.x * scale).coerceIn(-maxX, maxX)
            val newY = (offset.y + offsetChange.y * scale).coerceIn(-maxY, maxY)
            offset = androidx.compose.ui.geometry.Offset(newX, newY)
        }
    }

    DisposableEffect(uri) {
        try {
            val fd = context.contentResolver.openFileDescriptor(uri, "r")
            if (fd != null) {
                fileDescriptor = fd
                val renderer = PdfRenderer(fd)
                pdfRenderer = renderer
                pageCount = renderer.pageCount
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDispose {
            pdfRenderer?.close()
            fileDescriptor?.close()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
            // GLASS CARD CONTAINER
            GlassCard(
                showBorder = true,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // HEADER
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(NovaSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = NovaColors.GoldPrimary.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ConfirmationNumber, null, tint = NovaColors.GoldPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(NovaSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                ticket.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                "Visualización Premium",
                                style = MaterialTheme.typography.labelSmall,
                                color = NovaColors.GoldPrimary.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.5f))
                        }
                    }

                    // PDF AREA
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(NovaSpacing.md)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .transformable(state = transformableState),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pdfRenderer != null && pageCount > 0) {
                            PdfDialogImage(
                                pdfRenderer = pdfRenderer!!,
                                pageIndex = ticket.pageIndex.coerceIn(0, pageCount - 1),
                                renderMutex = renderMutex,
                                modifier = Modifier
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                            )
                        } else {
                            CircularProgressIndicator(color = NovaColors.GoldPrimary)
                        }
                        
                        // Hint de zoom
                        if (scale == 1f) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(NovaSpacing.md)
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ZoomIn, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pellizca para ampliar", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    
                    // FOOTER
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = NovaSpacing.sm, bottom = NovaSpacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Divider(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .alpha(0.08f),
                            thickness = 1.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(NovaSpacing.md))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ConfirmationNumber,
                                null,
                                tint = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(NovaSpacing.sm))
                            Text(
                                "NovaPass Secure Viewer".uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
                                    fontWeight = FontWeight.Light
                                ),
                                color = Color.White.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfDialogImage(
    pdfRenderer: PdfRenderer,
    pageIndex: Int,
    renderMutex: Mutex,
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) {
            renderMutex.withLock {
                try {
                    val page = pdfRenderer.openPage(pageIndex)
                    // Renderizamos a alta resolución para que el zoom se vea bien
                    val width = (page.width * 2.5).toInt()
                    val height = (page.height * 2.5).toInt()
                    val currentBitmap = android.graphics.Bitmap.createBitmap(
                        width, height, android.graphics.Bitmap.Config.ARGB_8888
                    )
                    currentBitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(currentBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmap = currentBitmap
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Ticket",
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        )
    } else {
        CircularProgressIndicator(color = NovaColors.GoldPrimary)
    }
}
