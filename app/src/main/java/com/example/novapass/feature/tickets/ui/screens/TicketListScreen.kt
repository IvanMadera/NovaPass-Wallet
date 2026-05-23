package com.example.novapass.feature.tickets.ui.screens

import android.app.Activity
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Matrix as AndroidMatrix
import android.graphics.SweepGradient as AndroidSweepGradient
import android.graphics.Paint as AndroidPaint
import androidx.compose.ui.graphics.toArgb
import com.example.novapass.core.design.theme.*
import com.example.novapass.domain.model.Ticket
import com.example.novapass.feature.tickets.presentation.TicketViewModel
import com.example.novapass.feature.tickets.ui.components.AddTicketBottomSheet
import com.example.novapass.feature.tickets.ui.components.EmptyStateView
import com.example.novapass.feature.tickets.ui.components.NovaBackground
import com.example.novapass.feature.tickets.ui.components.NovaModalBackground
import com.example.novapass.feature.tickets.ui.components.DeveloperInfoDialog
import com.example.novapass.feature.tickets.ui.util.parseEventDate
import com.example.novapass.feature.tickets.ui.util.formatEventDate
import com.example.novapass.feature.tickets.ui.components.TicketItem
import com.example.novapass.feature.tickets.ui.components.TicketViewerDialog

// ─────────────────────────────────────────────────────────────────────────
// TicketListScreen — pantalla principal (scaffold + lista de boletos)
// La lógica de formulario vive en AddTicketBottomSheet + TicketViewModel.
// ─────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketListScreen(viewModel: TicketViewModel) {
    val context = LocalContext.current
    val view    = LocalView.current

    // Forzar iconos claros en la barra de navegación (MIUI fix)
    SideEffect {
        val window = (context as? Activity)?.window
        if (window != null) {
            androidx.core.view.WindowCompat
                .getInsetsController(window, view)
                .isAppearanceLightNavigationBars = false
        }
    }

    val tickets     by viewModel.tickets.collectAsState()
    val formState   by viewModel.formState.collectAsState()
    val isShowingArchived by viewModel.isShowingArchived.collectAsState()
    var showEasterEgg by remember { mutableStateOf(false) }
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    // UI-only state
    var showBottomSheet        by remember { mutableStateOf(false) }
    var searchQuery            by remember { mutableStateOf("") }
    var ticketToDelete         by remember { mutableStateOf<Ticket?>(null) }
    var selectedTicketForView  by remember { mutableStateOf<Ticket?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope      = rememberCoroutineScope()

    val filteredTickets = remember(searchQuery, tickets) {
        val list = if (searchQuery.isEmpty()) tickets
                   else tickets.filter { it.name.contains(searchQuery, ignoreCase = true) }
        list.sortedWith { t1, t2 ->
            val d1 = parseEventDate(t1.eventDate)
            val d2 = parseEventDate(t2.eventDate)
            when {
                d1 == null && d2 == null -> 0
                d1 == null -> 1
                d2 == null -> -1
                else -> d2.compareTo(d1) // Orden descendente (más reciente primero)
            }
        }
    }

    // FAB interaction
    val fabInteractionSource = remember { MutableInteractionSource() }
    val fabPressed by fabInteractionSource.collectIsPressedAsState()
    val fabScale   by animateFloatAsState(if (fabPressed) 0.97f else 1f, label = "fabScale")

    Scaffold(
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .padding(NovaSpacing.md)
                    .size(64.dp)
                    .graphicsLayer { scaleX = fabScale; scaleY = fabScale }
                    .drawBehind {
                        drawCircle(
                            brush  = Brush.radialGradient(
                                colors = listOf(NovaColors.GoldPrimary.copy(alpha = 0.2f), NovaColors.Transparent),
                                center = center, radius = size.width * 0.8f
                            ),
                            radius = size.width * 0.8f, center = center
                        )
                    }
                    .background(brush = NovaBrushes.GoldGradient, shape = CircleShape)
                    .clickable(interactionSource = fabInteractionSource, indication = LocalIndication.current) {
                        viewModel.resetForm()
                        showBottomSheet = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar boleto", modifier = Modifier.size(32.dp), tint = NovaColors.BackgroundPrimary)
            }
        },
        containerColor = NovaColors.Transparent
    ) { innerPadding ->

        // ── Fondo con glows Unificado ──────────────────────────────────────
        NovaBackground(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                // ── Brand Header ───────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(NovaSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "logoBorder")
                    val borderAngle by infiniteTransition.animateFloat(
                        initialValue = 0f, targetValue = 360f,
                        animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing)),
                        label = "borderRotation"
                    )
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .graphicsLayer {
                                shadowElevation = 12.dp.toPx()
                                shape = RoundedCornerShape(12.dp)
                                clip = false
                                ambientShadowColor = Color(0xFF000000)
                                spotShadowColor = Color(0xFF000000)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .drawBehind {
                                    drawIntoCanvas { canvas: Canvas ->
                                        val paint = AndroidPaint().apply {
                                            isAntiAlias = true
                                            style       = AndroidPaint.Style.STROKE
                                            strokeWidth = 2.dp.toPx()
                                            val colors  = intArrayOf(NovaColors.GoldPrimary.toArgb(), NovaColors.GreenPrimary.toArgb(), NovaColors.GoldPrimary.toArgb())
                                            val shader  = AndroidSweepGradient(size.width / 2f, size.height / 2f, colors, null)
                                            val matrix  = AndroidMatrix()
                                            matrix.postRotate(borderAngle, size.width / 2f, size.height / 2f)
                                            shader.setLocalMatrix(matrix)
                                            setShader(shader)
                                        }
                                        canvas.nativeCanvas.drawRoundRect(android.graphics.RectF(0f, 0f, size.width, size.height), 12.dp.toPx(), 12.dp.toPx(), paint)
                                    }
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .background(NovaColors.GreenBlack)
                                .border(1.dp, NovaColors.GoldPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastTapTime < 500) {
                                        tapCount++
                                        if (tapCount >= 3) {
                                            showEasterEgg = true
                                            tapCount = 0
                                        }
                                    } else {
                                        tapCount = 1
                                    }
                                    lastTapTime = currentTime
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                NovaColors.White.copy(alpha = 0.04f),
                                                NovaColors.Transparent
                                            )
                                        )
                                    )
                            )
                            Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = NovaColors.GoldPrimary, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(NovaSpacing.md))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastTapTime < 500) {
                                    tapCount++
                                    if (tapCount >= 3) {
                                        showEasterEgg = true
                                        tapCount = 0
                                    }
                                } else {
                                    tapCount = 1
                                }
                                lastTapTime = currentTime
                            }
                    ) {
                        AnimatedContent(targetState = isShowingArchived, label = "titleAnim") { archived ->
                            Text(
                                if (archived) "NovaPass Archivo" else "NovaPass Wallet", 
                                style = MaterialTheme.typography.headlineMedium, 
                                color = NovaColors.White
                            )
                        }
                        Text("${tickets.size} boletos", style = MaterialTheme.typography.bodyMedium, color = NovaColors.TextSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .graphicsLayer {
                                shadowElevation = 6.dp.toPx()
                                shape = RoundedCornerShape(12.dp)
                                clip = false
                                ambientShadowColor = Color(0xFF000000)
                                spotShadowColor = Color(0xFF000000)
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(NovaColors.GreenBlack)
                            .border(
                                1.dp,
                                if (isShowingArchived) NovaColors.GoldPrimary.copy(alpha = 0.70f)
                                else NovaColors.GoldPrimary.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.toggleArchiveView() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            NovaColors.White.copy(alpha = 0.04f),
                                            NovaColors.Transparent
                                        )
                                    )
                                )
                        )
                        Icon(
                            if (isShowingArchived) Icons.Default.Inventory else Icons.Default.Inventory2,
                            contentDescription = "Archivar",
                            tint = NovaColors.GoldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // ── Search Bar ─────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .graphicsLayer {
                            shadowElevation = 8.dp.toPx()
                            shape = RoundedCornerShape(16.dp)
                            clip = false
                            ambientShadowColor = Color(0xFF000000)
                            spotShadowColor = Color(0xFF000000)
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(NovaColors.GreenDark)
                        .border(1.dp, NovaColors.GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    OutlinedTextField(
                        value = searchQuery, onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar boletos...", color = NovaColors.TextSecondary, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)) },
                        leadingIcon  = { Icon(Icons.Default.Search, contentDescription = null, tint = NovaColors.GoldPrimary) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor   = NovaColors.Transparent, 
                            unfocusedContainerColor = NovaColors.Transparent,
                            focusedBorderColor      = NovaColors.Transparent, 
                            unfocusedBorderColor    = NovaColors.Transparent,
                            cursorColor             = NovaColors.GoldPrimary
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = NovaColors.White, fontSize = 14.sp)
                    )
                }

                // ── Lista / Empty State con Transición ────────────────────
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = filteredTickets.isEmpty(),
                        transitionSpec = {
                            fadeIn(animationSpec = tween(400)) togetherWith 
                            fadeOut(animationSpec = tween(300))
                        },
                        label = "listTransition"
                    ) { isEmpty ->
                        if (isEmpty) {
                            EmptyStateView(
                                isSearch = searchQuery.isNotEmpty(),
                                isArchive = isShowingArchived
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = Brush.verticalGradient(colors = listOf(NovaColors.Black, NovaColors.Transparent), endY = 28.dp.toPx()),
                                            blendMode = BlendMode.DstOut
                                        )
                                    },
                                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
                            ) {
                                items(filteredTickets, key = { it.id }) { ticket ->
                                    TicketItem(
                                        ticket  = ticket,
                                        onClick  = { selectedTicketForView = ticket },
                                        onDelete = { ticketToDelete = ticket },
                                        onArchive = { viewModel.archiveTicket(ticket, archive = !isShowingArchived) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Bottom Sheet ───────────────────────────────────────────────────
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { 
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) showBottomSheet = false
                    }
                },
                sheetState       = sheetState,
                containerColor   = NovaColors.GreenBlack,
                scrimColor       = NovaColors.Scrim,
                dragHandle       = null,
                tonalElevation   = 0.dp,
                shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                AddTicketBottomSheet(
                    viewModel  = viewModel,
                    formState  = formState,
                    onDismiss  = { 
                    scope.launch { 
                        sheetState.hide() 
                    }.invokeOnCompletion {
                        if (!sheetState.isVisible) showBottomSheet = false
                    }
                }
                )
            }
        }

        // ── Visor PDF ──────────────────────────────────────────────────────────
        selectedTicketForView?.let { ticket ->
            TicketViewerDialog(ticket = ticket, onDismiss = { selectedTicketForView = null })
        }

        // ── Confirmación Eliminar (Versión Premium Continua) ─────────────
        ticketToDelete?.let { ticket ->
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { ticketToDelete = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                // Fondo oscuro unificado (Scrim)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NovaColors.Scrim)
                        .clickable(onClick = { ticketToDelete = null }, indication = null, interactionSource = remember { MutableInteractionSource() }),
                    contentAlignment = Alignment.Center
                ) {
                    val dialogScale by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                        label = "dialogScale"
                    )
                    
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
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 24.dp, vertical = 36.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NovaColors.GoldPrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                        .border(1.5.dp, NovaColors.GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = NovaColors.GoldPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                Text(
                                    "¿Eliminar boleto?",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = NovaColors.White,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    "Esta acción no se puede deshacer. El boleto será eliminado permanentemente de tu wallet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NovaColors.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = NovaSpacing.sm)
                                )
                                
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md)
                                ) {
                                    OutlinedButton(
                                        onClick = { ticketToDelete = null },
                                        modifier = Modifier.weight(1f).height(54.dp),
                                        border   = BorderStroke(1.dp, NovaColors.White.copy(alpha = 0.1f)),
                                        shape    = RoundedCornerShape(16.dp),
                                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = NovaColors.White)
                                    ) {
                                        Text("Cancelar", style = MaterialTheme.typography.labelLarge)
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(NovaBrushes.GoldGradient)
                                            .clickable { viewModel.removeTicket(ticket); ticketToDelete = null },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Eliminar", color = NovaColors.BackgroundPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } // NovaModalBackground
                    }
                }
            }
        }

        if (showEasterEgg) {
            DeveloperInfoDialog(
                onDismiss = { showEasterEgg = false }
            )
        }
    }
}

