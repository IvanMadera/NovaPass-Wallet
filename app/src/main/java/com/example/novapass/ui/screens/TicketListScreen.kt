package com.example.novapass.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.graphics.Matrix as AndroidMatrix
import android.graphics.SweepGradient as AndroidSweepGradient
import android.graphics.Paint as AndroidPaint
import androidx.compose.ui.graphics.toArgb
import com.example.novapass.TicketViewModel
import com.example.novapass.data.TicketEntity
import com.example.novapass.models.ExtractedTicketData
import com.example.novapass.ui.components.CustomInputField
import com.example.novapass.ui.components.EmptyStateView
import com.example.novapass.ui.components.TicketItem
import com.example.novapass.ui.components.TicketViewerDialog
import com.example.novapass.ui.theme.*
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────
// TicketListScreen — pantalla principal con lista de boletos y bottom sheet
// ─────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TicketListScreen(
    viewModel: TicketViewModel,
    onAddTicket: suspend (String, Uri, String, String?, String?, String?, String?, String?, String?, Int) -> Boolean
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Forzar iconos claros en la barra de navegación (MIUI/Xiaomi fix)
    SideEffect {
        val window = (context as? Activity)?.window
        if (window != null) {
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightNavigationBars = false
        }
    }

    val tickets by viewModel.tickets.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Estados del formulario
    var ticketName by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("") }
    var eventTime by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var row by remember { mutableStateOf("") }
    var seat by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Otro") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedTicketForView by remember { mutableStateOf<TicketEntity?>(null) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    var isVerifying by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var ticketToDelete by remember { mutableStateOf<TicketEntity?>(null) }
    var pendingTickets by remember { mutableStateOf<List<ExtractedTicketData>>(emptyList()) }
    var editingTicketIndex by remember { mutableStateOf<Int?>(null) }
    var showIndividualDatePicker by remember { mutableStateOf<Int?>(null) }
    var showIndividualTimePicker by remember { mutableStateOf<Int?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState(initialHour = 12, initialMinute = 0, is24Hour = false)
    val categories = listOf("Concierto", "Cine", "Deportes", "Otro")

    val filteredTickets = remember(searchQuery, tickets) {
        val sdf = java.text.SimpleDateFormat("EEEE, dd 'DE' MMMM 'DE' yyyy", java.util.Locale("es", "ES"))
        val list = if (searchQuery.isEmpty()) tickets
        else tickets.filter { it.name.contains(searchQuery, ignoreCase = true) }
        list.sortedWith { t1, t2 ->
            try {
                val d1 = sdf.parse(t1.eventDate ?: "")
                val d2 = sdf.parse(t2.eventDate ?: "")
                d1?.compareTo(d2) ?: 0
            } catch (e: Exception) { 0 }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        selectedUri = uri
        if (uri != null) {
            isVerifying = true
            coroutineScope.launch {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        selectedFileName = cursor.getString(nameIndex)
                    }
                }
                if (isValidTicket(context, uri)) {
                    val dataList = extractTicketData(context, uri)
                    pendingTickets = dataList
                    if (dataList.isNotEmpty()) {
                        if (dataList.size == 1) {
                            val data = dataList[0]
                            ticketName = data.eventName
                            selectedCategory = data.category
                            eventDate = data.date
                            eventTime = data.time
                            section = data.section
                            row = data.row
                            seat = data.seat
                            currentPageIndex = data.pageIndex
                        } else {
                            ticketName = dataList[0].eventName
                        }
                    }
                } else {
                    Toast.makeText(context, "El archivo no parece ser un boleto válido", Toast.LENGTH_LONG).show()
                    selectedUri = null
                    selectedFileName = ""
                }
                isVerifying = false
            }
        }
    }

    val fabInteractionSource = remember { MutableInteractionSource() }
    val fabPressed by fabInteractionSource.collectIsPressedAsState()
    val fabScale by animateFloatAsState(if (fabPressed) 0.97f else 1f, label = "fabScale")

    Scaffold(
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .padding(NovaSpacing.md)
                    .size(64.dp)
                    .graphicsLayer { scaleX = fabScale; scaleY = fabScale }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(NovaColors.GoldPrimary.copy(alpha = 0.2f), Color.Transparent),
                                center = center, radius = size.width * 0.8f
                            ),
                            radius = size.width * 0.8f, center = center
                        )
                    }
                    .background(
                        brush = Brush.linearGradient(listOf(Color(0xFFFFEA9E), NovaColors.GoldPrimary)),
                        shape = CircleShape
                    )
                    .clickable(interactionSource = fabInteractionSource, indication = LocalIndication.current) {
                        selectedUri = null; selectedFileName = ""; ticketName = ""; eventDate = ""
                        eventTime = ""; location = ""; section = ""; row = ""; seat = ""
                        selectedCategory = "Otro"; pendingTickets = emptyList()
                        showBottomSheet = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(32.dp), tint = NovaColors.BackgroundPrimary)
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1A1F35), NovaColors.BackgroundPrimary),
                        center = Offset(0.5f, 0f), radius = 2000f
                    )
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize().blur(140.dp)) {
                drawCircle(color = NovaColors.GoldPrimary.copy(alpha = 0.12f), radius = size.width * 0.9f, center = Offset(size.width * 0.9f, size.height * 0.1f))
                drawCircle(color = NovaColors.GreenPrimary.copy(alpha = 0.18f), radius = size.width * 0.8f, center = Offset(size.width * 0.1f, size.height * 0.85f))
            }

            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                // BRAND HEADER
                Row(
                    modifier = Modifier.fillMaxWidth().padding(NovaSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Animación de rotación infinita para el borde del logo
                    val infiniteTransition = rememberInfiniteTransition(label = "logoBorder")
                    val borderAngle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(animation = tween(3000, easing = androidx.compose.animation.core.LinearEasing)),
                        label = "borderRotation"
                    )

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .drawBehind {
                                drawIntoCanvas { canvas: androidx.compose.ui.graphics.Canvas ->
                                    val paint = AndroidPaint().apply {
                                        isAntiAlias = true
                                        style = AndroidPaint.Style.STROKE
                                        strokeWidth = 2.dp.toPx()
                                        
                                        val colors = intArrayOf(
                                            NovaColors.GoldPrimary.toArgb(),
                                            NovaColors.GreenPrimary.toArgb(),
                                            NovaColors.GoldPrimary.toArgb()
                                        )
                                        val shader = AndroidSweepGradient(
                                            size.width / 2f,
                                            size.height / 2f,
                                            colors,
                                            null
                                        )
                                        val matrix = AndroidMatrix()
                                        matrix.postRotate(borderAngle, size.width / 2f, size.height / 2f)
                                        shader.setLocalMatrix(matrix)
                                        setShader(shader)
                                    }
                                    
                                    val rect = android.graphics.RectF(0f, 0f, size.width, size.height)
                                    val radius = 12.dp.toPx()
                                    canvas.nativeCanvas.drawRoundRect(rect, radius, radius, paint)
                                }
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(NovaColors.BackgroundSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = NovaColors.GoldPrimary, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(NovaSpacing.md))
                    Column {
                        Text("NovaPass Wallet", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text("${tickets.size} boletos", style = MaterialTheme.typography.bodyMedium, color = NovaColors.TextSecondary)
                    }
                }

                // DIALOGO CONFIRMACIÓN ELIMINAR
                if (ticketToDelete != null) {
                    AlertDialog(
                        onDismissRequest = { ticketToDelete = null },
                        containerColor = NovaColors.BackgroundSecondary,
                        shape = RoundedCornerShape(24.dp),
                        title = {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Eliminar boleto", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            }
                        },
                        text = {
                            Text("Esta acción no se puede deshacer. El boleto será eliminado permanentemente.",
                                textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth())
                        },
                        confirmButton = {
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { ticketToDelete = null },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) { Text("Cancelar", style = MaterialTheme.typography.labelLarge) }
                                Button(
                                    onClick = { ticketToDelete?.let { viewModel.removeTicket(it) }; ticketToDelete = null },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NovaColors.GoldPrimary),
                                    shape = RoundedCornerShape(20.dp)
                                ) { Text("Eliminar", color = NovaColors.BackgroundPrimary, style = MaterialTheme.typography.labelLarge) }
                            }
                        }
                    )
                }

                // SEARCH BAR
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NovaInputBackground)
                        .border(1.dp, NovaGlassBorder, RoundedCornerShape(20.dp))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(1.dp)
                            .background(Brush.horizontalGradient(colors = listOf(Color.Transparent, NovaGlassBorder, Color.Transparent)))
                            .align(Alignment.TopCenter)
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar boletos...", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        singleLine = true
                    )
                }

                // LISTA O ESTADO VACÍO
                Box(modifier = Modifier.fillMaxSize()) {
                    if (filteredTickets.isEmpty()) {
                        EmptyStateView(isSearch = searchQuery.isNotEmpty())
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(colors = listOf(Color.Black, Color.Transparent), endY = 28.dp.toPx()),
                                        blendMode = BlendMode.DstOut
                                    )
                                },
                            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
                        ) {
                            items(filteredTickets) { ticket ->
                                TicketItem(ticket = ticket, onClick = { selectedTicketForView = ticket }, onDelete = { ticketToDelete = ticket })
                            }
                        }
                    }
                }
            }
        }

        // BOTTOM SHEET — Agregar Boleto
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = NovaColors.BackgroundPrimary,
                scrimColor = Color.Black.copy(alpha = 0.6f),
                dragHandle = null,
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Canvas(modifier = Modifier.matchParentSize().blur(80.dp)) {
                        drawCircle(color = NovaColors.GoldPrimary.copy(alpha = 0.10f), radius = size.width * 0.6f, center = Offset(size.width * 0.3f, size.height * 0.1f))
                        drawCircle(color = NovaColors.GreenPrimary.copy(alpha = 0.15f), radius = size.width * 0.85f, center = Offset(size.width * 0.8f, size.height * 0.4f))
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = NovaSpacing.lg),
                        contentPadding = PaddingValues(bottom = NovaSpacing.xl)
                    ) {
                        // Drag handle
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier.width(40.dp).height(4.dp).background(NovaColors.TextSecondary.copy(alpha = 0.4f), CircleShape))
                            }
                        }

                        // Header
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Detalles del Evento", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                                IconButton(
                                    onClick = { showBottomSheet = false; pendingTickets = emptyList() },
                                    modifier = Modifier.background(NovaColors.GlassMedium, CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White.copy(alpha = 0.7f))
                                }
                            }
                            Spacer(modifier = Modifier.height(NovaSpacing.lg))
                        }

                        // PDF Picker
                        item {
                            Text("Selecciona el archivo *", style = MaterialTheme.typography.labelMedium, color = NovaColors.TextSecondary)
                            Spacer(modifier = Modifier.height(NovaSpacing.sm))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selectedUri == null) NovaColors.GlassLight else NovaColors.GlassMedium)
                                    .border(1.dp, if (selectedUri == null) NovaColors.BorderSubtle else NovaColors.GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .clickable { filePickerLauncher.launch(arrayOf("application/pdf")) }
                            ) {
                                Row(modifier = Modifier.padding(NovaSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (selectedUri == null) Icons.Default.Description else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (selectedUri == null) NovaColors.TextSecondary.copy(alpha = 0.4f) else NovaColors.GoldPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(NovaSpacing.md))
                                    Text(
                                        if (selectedUri == null) "Toque para seleccionar sus boletos" else selectedFileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (selectedUri == null) NovaColors.TextSecondary else NovaColors.GoldPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(NovaSpacing.lg))
                        }

                        // Formulario: múltiples boletos o formulario individual
                        if (pendingTickets.size > 1) {
                            item {
                                Text("Se han detectado ${pendingTickets.size} boletos",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NovaPrimary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            items(pendingTickets.size) { index ->
                                val pTicket = pendingTickets[index]
                                val isEditing = editingTicketIndex == index

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = NovaSpacing.xs)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isEditing) NovaColors.GlassMedium else NovaColors.GlassLight)
                                        .border(1.dp, if (isEditing) NovaColors.GoldPrimary.copy(alpha = 0.3f) else NovaColors.BorderSubtle, RoundedCornerShape(20.dp))
                                        .clickable { editingTicketIndex = if (isEditing) null else index }
                                ) {
                                    Column(modifier = Modifier.padding(NovaSpacing.md)) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(NovaSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = if (isEditing) NovaColors.GoldPrimary else NovaColors.BackgroundSecondary,
                                                shape = CircleShape,
                                                modifier = Modifier.size(36.dp).border(1.dp, if (isEditing) Color.Transparent else NovaColors.BorderSubtle, CircleShape)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("${index + 1}", color = if (isEditing) NovaColors.BackgroundPrimary else NovaColors.GoldPrimary, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(NovaSpacing.md))
                                            Text(pTicket.eventName, modifier = Modifier.weight(1f), color = NovaColors.TextPrimary, fontWeight = FontWeight.Bold)
                                            Icon(if (isEditing) Icons.Default.KeyboardArrowUp else Icons.Default.Edit, contentDescription = null, tint = NovaColors.GoldPrimary.copy(alpha = 0.7f))
                                        }

                                        androidx.compose.animation.AnimatedVisibility(visible = isEditing) {
                                            Column {
                                                Spacer(modifier = Modifier.height(NovaSpacing.md))
                                                CustomInputField(value = pTicket.eventName, onValueChange = { newVal ->
                                                    val newList = pendingTickets.toMutableList()
                                                    newList[index] = pTicket.copy(eventName = newVal)
                                                    pendingTickets = newList
                                                }, label = "Evento", placeholder = "Nombre")
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    CustomInputField(value = pTicket.date, onValueChange = {}, label = "Fecha", placeholder = "Fecha", modifier = Modifier.weight(1.5f), readOnly = true, onClick = { showIndividualDatePicker = index })
                                                    CustomInputField(value = pTicket.time, onValueChange = {}, label = "Hora", placeholder = "Hora", modifier = Modifier.weight(1f), readOnly = true, onClick = { showIndividualTimePicker = index })
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    CustomInputField(value = pTicket.section, onValueChange = { v -> val nl = pendingTickets.toMutableList(); nl[index] = pTicket.copy(section = v); pendingTickets = nl }, label = "Sec", placeholder = "Ej: 102", modifier = Modifier.weight(1f))
                                                    CustomInputField(value = pTicket.row, onValueChange = { v -> val nl = pendingTickets.toMutableList(); nl[index] = pTicket.copy(row = v); pendingTickets = nl }, label = "Fila", placeholder = "Ej: A", modifier = Modifier.weight(1f))
                                                    CustomInputField(value = pTicket.seat, onValueChange = { v -> val nl = pendingTickets.toMutableList(); nl[index] = pTicket.copy(seat = v); pendingTickets = nl }, label = "Asiento", placeholder = "Ej: 15", modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item { Spacer(modifier = Modifier.height(24.dp)) }
                        } else {
                            item {
                                Text("Categoría", style = MaterialTheme.typography.labelMedium, color = NovaColors.TextSecondary)
                                Spacer(modifier = Modifier.height(NovaSpacing.sm))
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm)
                                ) {
                                    categories.forEach { category ->
                                        val isSelected = selectedCategory == category
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (isSelected) Brush.linearGradient(listOf(Color(0xFFFFEA9E), NovaColors.GoldPrimary)) else SolidColor(NovaColors.GlassLight))
                                                .border(1.dp, if (isSelected) Color.Transparent else NovaColors.BorderSubtle, RoundedCornerShape(20.dp))
                                                .clickable { selectedCategory = category }
                                                .padding(horizontal = NovaSpacing.md, vertical = NovaSpacing.sm)
                                        ) {
                                            Text(category, color = if (isSelected) NovaColors.BackgroundPrimary else NovaColors.TextSecondary, style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(NovaSpacing.lg))

                                CustomInputField(value = ticketName, onValueChange = { ticketName = it }, label = "Nombre del evento *", placeholder = "Ej: Coldplay World Tour")
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    CustomInputField(value = eventDate, onValueChange = {}, label = "Fecha *", placeholder = "DD/MM/AAAA", modifier = Modifier.weight(1f), readOnly = true, onClick = { showDatePicker = true })
                                    Spacer(modifier = Modifier.width(16.dp))
                                    CustomInputField(value = eventTime, onValueChange = {}, label = "Hora", placeholder = "HH:MM", modifier = Modifier.weight(1f), readOnly = true, onClick = { showTimePicker = true })
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    CustomInputField(value = section, onValueChange = { section = it }, label = "Sección", placeholder = "Ej: 102", modifier = Modifier.weight(1f))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    CustomInputField(value = row, onValueChange = { row = it }, label = "Fila", placeholder = "Ej: A", modifier = Modifier.weight(0.5f))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    CustomInputField(value = seat, onValueChange = { seat = it }, label = "Asiento", placeholder = "Ej: 15", modifier = Modifier.weight(0.5f))
                                }
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }

                        // Botón Guardar
                        item {
                            val saveInteractionSource = remember { MutableInteractionSource() }
                            val savePressed by saveInteractionSource.collectIsPressedAsState()
                            val saveScale by animateFloatAsState(if (savePressed) 0.97f else 1f, label = "saveScale")
                            val isEnabled = (ticketName.isNotBlank() || pendingTickets.isNotEmpty()) && selectedUri != null && !isVerifying

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .graphicsLayer { scaleX = if (isEnabled) saveScale else 1f; scaleY = if (isEnabled) saveScale else 1f }
                                    .clip(RoundedCornerShape(20.dp))
                                    .then(
                                        if (isEnabled) Modifier.background(Brush.linearGradient(listOf(Color(0xFFFFEA9E), NovaColors.GoldPrimary)))
                                        else Modifier.background(NovaColors.GlassLight)
                                    )
                                    .clickable(enabled = isEnabled, interactionSource = saveInteractionSource, indication = LocalIndication.current) {
                                        selectedUri?.let { uri ->
                                            coroutineScope.launch {
                                                try {
                                                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                } catch (e: Exception) { e.printStackTrace() }
                                                
                                                var addedCount = 0
                                                var skippedCount = 0

                                                if (pendingTickets.size > 1) {
                                                    pendingTickets.forEach { pTicket ->
                                                        val added = onAddTicket(pTicket.eventName, uri, pTicket.category, pTicket.date, pTicket.time, "", pTicket.section, pTicket.row, pTicket.seat, pTicket.pageIndex)
                                                        if (added) addedCount++ else skippedCount++
                                                    }
                                                } else {
                                                    val added = onAddTicket(ticketName, uri, selectedCategory, eventDate, eventTime, location, section, row, seat, currentPageIndex)
                                                    if (added) addedCount++ else skippedCount++
                                                }
                                                
                                                // Notificación de resultados
                                                when {
                                                    addedCount > 0 && skippedCount > 0 -> {
                                                        Toast.makeText(context, "$addedCount boletos guardados, $skippedCount duplicados omitidos", Toast.LENGTH_LONG).show()
                                                    }
                                                    addedCount > 0 -> {
                                                        Toast.makeText(context, if (addedCount == 1) "Boleto guardado exitosamente" else "$addedCount boletos guardados", Toast.LENGTH_SHORT).show()
                                                    }
                                                    skippedCount > 0 -> {
                                                        val msg = if (skippedCount == 1) "El boleto ya existe en tu wallet" else "$skippedCount boletos ya existen en tu wallet"
                                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                                
                                                showBottomSheet = false
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isVerifying) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                } else {
                                    Text(
                                        if (pendingTickets.size > 1) "Guardar ${pendingTickets.size} Boletos" else "Guardar Boleto",
                                        color = if (isEnabled) NovaColors.BackgroundPrimary else NovaColors.TextSecondary.copy(alpha = 0.3f),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Date Picker Dialog
        if (showDatePicker || showIndividualDatePicker != null) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false; showIndividualDatePicker = null },
                confirmButton = {
                    TextButton(onClick = {
                        val date = datePickerState.selectedDateMillis?.let {
                            val instant = Instant.ofEpochMilli(it)
                            val formatter = DateTimeFormatter.ofPattern("EEEE, dd 'DE' MMMM 'DE' yyyy", java.util.Locale("es", "ES"))
                            instant.atZone(ZoneId.systemDefault()).format(formatter).uppercase()
                        }
                        if (date != null) {
                            if (showIndividualDatePicker != null) {
                                val idx = showIndividualDatePicker!!
                                val newList = pendingTickets.toMutableList()
                                newList[idx] = newList[idx].copy(date = date)
                                pendingTickets = newList
                            } else { eventDate = date }
                        }
                        showDatePicker = false; showIndividualDatePicker = null
                    }) { Text("Aceptar", color = NovaPrimary) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false; showIndividualDatePicker = null }) {
                        Text("Cancelar", color = Color.White.copy(alpha = 0.6f))
                    }
                },
                colors = DatePickerDefaults.colors(containerColor = NovaBackground, titleContentColor = Color.White, headlineContentColor = Color.White, selectedDayContainerColor = NovaPrimary, selectedDayContentColor = Color.Black, todayContentColor = NovaPrimary, todayDateBorderColor = NovaPrimary, dayContentColor = Color.White, weekdayContentColor = Color.White.copy(alpha = 0.5f), yearContentColor = Color.White, currentYearContentColor = NovaPrimary)
            ) {
                DatePicker(state = datePickerState, colors = DatePickerDefaults.colors(containerColor = NovaBackground, titleContentColor = Color.White, headlineContentColor = Color.White, selectedDayContainerColor = NovaPrimary, selectedDayContentColor = Color.Black, todayContentColor = NovaPrimary, todayDateBorderColor = NovaPrimary, dayContentColor = Color.White, weekdayContentColor = Color.White.copy(alpha = 0.5f), yearContentColor = Color.White, currentYearContentColor = NovaPrimary))
            }
        }

        // Time Picker Dialog
        if (showTimePicker || showIndividualTimePicker != null) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showTimePicker = false; showIndividualTimePicker = null }) {
                Surface(shape = RoundedCornerShape(28.dp), color = NovaBackground, tonalElevation = 6.dp, modifier = Modifier.width(IntrinsicSize.Max).height(IntrinsicSize.Min)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Selecciona la hora", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.Start).padding(bottom = 20.dp))
                        TimePicker(state = timePickerState, colors = TimePickerDefaults.colors(clockDialColor = Color.White.copy(alpha = 0.05f), clockDialSelectedContentColor = Color.Black, clockDialUnselectedContentColor = Color.White, selectorColor = NovaPrimary, periodSelectorSelectedContainerColor = NovaPrimary, periodSelectorSelectedContentColor = Color.Black, periodSelectorUnselectedContentColor = Color.White, timeSelectorSelectedContainerColor = NovaPrimary.copy(alpha = 0.2f), timeSelectorSelectedContentColor = NovaPrimary, timeSelectorUnselectedContainerColor = Color.White.copy(alpha = 0.05f), timeSelectorUnselectedContentColor = Color.White))
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showTimePicker = false; showIndividualTimePicker = null }) { Text("Cancelar", color = Color.White.copy(alpha = 0.6f)) }
                            TextButton(onClick = {
                                val timeStr = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                                if (showIndividualTimePicker != null) {
                                    val idx = showIndividualTimePicker!!
                                    val newList = pendingTickets.toMutableList()
                                    newList[idx] = newList[idx].copy(time = timeStr)
                                    pendingTickets = newList
                                } else { eventTime = timeStr }
                                showTimePicker = false; showIndividualTimePicker = null
                            }) { Text("Aceptar", color = NovaPrimary) }
                        }
                    }
                }
            }
        }
    }

    // DIÁLOGO DE VISUALIZACIÓN PREMIUM CENTRADA
    selectedTicketForView?.let { ticket ->
        TicketViewerDialog(
            ticket = ticket,
            onDismiss = { selectedTicketForView = null }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Funciones de utilidad: validación y extracción de PDFs
// ─────────────────────────────────────────────────────────────────────────

suspend fun isValidTicket(context: Context, uri: Uri): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                stripper.startPage = 1; stripper.endPage = 2
                val text = stripper.getText(document).lowercase()
                document.close(); inputStream.close()
                val keywords = listOf("boleto", "ticket", "butaca", "fila", "sector", "zona", "acceso", "seat", "row", "section")
                for (keyword in keywords) { if (text.contains(keyword)) return@withContext true }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext false
    }
}

suspend fun extractTicketData(context: Context, uri: Uri): List<ExtractedTicketData> {
    return withContext(Dispatchers.IO) {
        val extractedTickets = mutableListOf<ExtractedTicketData>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val document = PDDocument.load(inputStream)
                val totalPages = document.numberOfPages
                val stripper = PDFTextStripper()
                for (pageIndex in 1..totalPages) {
                    stripper.startPage = pageIndex; stripper.endPage = pageIndex
                    val text = stripper.getText(document)
                    var eventName = ""; var category = "Otro"; var date = ""
                    var time = ""; var section = ""; var row = ""; var seat = ""
                    val lines = text.split("\n").map { it.trim() }

                    val vsLine = lines.find { it.contains(" VS ", ignoreCase = true) }
                    if (vsLine != null) {
                        eventName = vsLine.substringBefore(" SERIE").trim().uppercase()
                        category = "Deportes"
                    }
                    val datePattern = Regex("(?i)(lunes|martes|miércoles|jueves|viernes|sábado|domingo),\\s+\\d+\\s+DE\\s+[A-Z]+\\s+DE\\s+\\d{4}")
                    datePattern.find(text)?.let { date = it.value }
                    if (text.contains("SECCION", ignoreCase = true)) {
                        section = Regex("(?i)SECCION\\s+([A-Z0-9 ]+)").find(text)?.groupValues?.get(1)?.trim() ?: ""
                    }
                    val rowSeatMatch = Regex("(?i)FILA\\s*\\|?\\s*BUTACA\\s*\\n\\s*([A-Z0-9]+)\\s+([A-Z0-9]+)").find(text)
                    if (rowSeatMatch != null) { row = rowSeatMatch.groupValues[1]; seat = rowSeatMatch.groupValues[2] }

                    if (eventName.isBlank()) eventName = getPdfTitle(context, uri).uppercase()
                    if (eventName.isNotBlank() || section.isNotBlank() || seat.isNotBlank()) {
                        extractedTickets.add(ExtractedTicketData(eventName, category, date, time, "", section, row, seat, pageIndex - 1))
                    }
                }
                document.close(); inputStream.close()
            }
        } catch (e: Exception) { e.printStackTrace() }
        extractedTickets
    }
}

suspend fun getPdfTitle(context: Context, uri: Uri): String {
    return withContext(Dispatchers.IO) {
        var title = ""
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) title = cursor.getString(nameIndex).substringBeforeLast(".")
            }
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val document = PDDocument.load(inputStream)
                val pdfTitle = document.documentInformation?.title
                document.close(); inputStream.close()
                if (!pdfTitle.isNullOrBlank()) title = pdfTitle
            }
        } catch (e: Exception) { }
        title
    }
}
