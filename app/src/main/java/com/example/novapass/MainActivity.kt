package com.example.novapass

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.novapass.data.TicketEntity
import com.example.novapass.ui.theme.*
import android.content.Intent
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import android.widget.Toast
import android.app.DatePickerDialog as LegacyDatePickerDialog
import android.app.TimePickerDialog as LegacyTimePickerDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import java.util.Calendar
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import android.graphics.pdf.PdfRenderer
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication

data class ExtractedTicketData(
    val eventName: String = "",
    val category: String = "Otro",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val section: String = "",
    val row: String = "",
    val seat: String = "",
    val pageIndex: Int = 0
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        val navBarColor = android.graphics.Color.parseColor("#0D1117")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(navBarColor)
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            NovaPassTheme {
                NovaPassApp()
            }
        }
    }
}

@Composable
fun NovaPassApp(viewModel: TicketViewModel = viewModel()) {
    val navController = rememberNavController()

    // Lógica para mostrar Selectores de Fecha/Hora (Fuera del NavHost para accesibilidad total)
    // Estos se disparan desde TicketListScreen a través de estados

    NavHost(navController = navController, startDestination = "ticketList") {
        composable("ticketList") {
            TicketListScreen(
                viewModel = viewModel,
                onTicketClick = { ticket ->
                    navController.navigate("ticketDetail/${Uri.encode(ticket.uri)}/${ticket.pageIndex}")
                },
                onAddTicket = { name, uri, category, eventDate, eventTime, location, section, row, seat, pageIndex ->
                    viewModel.addTicket(name, uri, category, eventDate, eventTime, location, section, row, seat, pageIndex)
                }
            )
        }
        composable(
            "ticketDetail/{pdfUri}/{pageIndex}",
            arguments = listOf(
                navArgument("pdfUri") { type = NavType.StringType },
                navArgument("pageIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val pdfUriString = backStackEntry.arguments?.getString("pdfUri")
            val pageIndex = backStackEntry.arguments?.getInt("pageIndex") ?: 0
            if (pdfUriString != null) {
                PdfViewerScreen(uri = pdfUriString.toUri(), pageIndex = pageIndex, onBack = { navController.popBackStack() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TicketListScreen(
    viewModel: TicketViewModel,
    onTicketClick: (TicketEntity) -> Unit,
    onAddTicket: (String, Uri, String, String?, String?, String?, String?, String?, String?, Int) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    
    // Forzar iconos claros en la barra de navegación para el tema oscuro (Quantum Sapphire)
    // Esto resuelve el problema de la barra blanca en dispositivos con capas personalizadas (Xiaomi/MIUI)
    androidx.compose.runtime.SideEffect {
        val window = (context as? Activity)?.window
        if (window != null) {
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightNavigationBars = false
        }
    }
    val tickets by viewModel.tickets.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Estados del Formulario
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
    val timePickerState = rememberTimePickerState(
        initialHour = 12,
        initialMinute = 0,
        is24Hour = false
    )

    val categories = listOf("Concierto", "Cine", "Deportes", "Otro")

    val filteredTickets = remember(searchQuery, tickets) {
        val sdf = java.text.SimpleDateFormat("EEEE, dd 'DE' MMMM 'DE' yyyy", java.util.Locale("es", "ES"))
        val list = if (searchQuery.isEmpty()) {
            tickets
        } else {
            tickets.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
        
        list.sortedWith { t1, t2 ->
            try {
                val d1 = sdf.parse(t1.eventDate ?: "")
                val d2 = sdf.parse(t2.eventDate ?: "")
                d1?.compareTo(d2) ?: 0
            } catch (e: Exception) {
                0
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        selectedUri = uri
        if (uri != null) {
            isVerifying = true
            coroutineScope.launch {
                // Obtener nombre del archivo
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        selectedFileName = cursor.getString(nameIndex)
                    }
                }
                
                // Validar si es un boleto real
                if (isValidTicket(context, uri)) {
                    // Extracción Inteligente de Múltiples Boletos
                    val dataList = extractTicketData(context, uri)
                    pendingTickets = dataList
                    
                    if (dataList.isNotEmpty()) {
                        // Si solo hay uno, rellenamos los campos individuales para edición
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
                            // Si hay varios, el nombre común del primer boleto se usa como referencia
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
            // FAB PERSONALIZADO CON SOMBRA MANUAL Y MICRO-INTERACCIÓN
            Box(
                modifier = Modifier
                    .padding(NovaSpacing.md)
                    .size(64.dp)
                    .graphicsLayer {
                        scaleX = fabScale
                        scaleY = fabScale
                    }
                    .drawBehind {
                        // Resplandor de acento sutil
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(NovaColors.AccentPrimary.copy(alpha = 0.2f), Color.Transparent),
                                center = center,
                                radius = size.width * 0.8f
                            ),
                            radius = size.width * 0.8f,
                            center = center
                        )
                    }
                    .background(
                        color = NovaColors.AccentPrimary,
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = fabInteractionSource,
                        indication = LocalIndication.current
                    ) {
                        selectedUri = null
                        selectedFileName = ""
                        ticketName = ""
                        eventDate = ""
                        eventTime = ""
                        location = ""
                        section = ""
                        row = ""
                        seat = ""
                        selectedCategory = "Otro"
                        pendingTickets = emptyList()
                        showBottomSheet = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "Add", 
                    modifier = Modifier.size(32.dp),
                    tint = NovaColors.BackgroundPrimary
                )
            }
        },
        containerColor = Color.Transparent  // Transparente para que los blobs se vean en toda la pantalla
    ) { innerPadding ->
        // Box global con blobs para cubrir TODA la pantalla (header + search + tickets)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1A1F35), NovaColors.BackgroundPrimary),
                        center = androidx.compose.ui.geometry.Offset(0.5f, 0f),
                        radius = 2000f
                    )
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize().blur(100.dp)) {
                // Glow 1: Ultra Ambient Accent (top-right)
                drawCircle(
                    color = NovaColors.AccentPrimary.copy(alpha = 0.08f),
                    radius = size.width * 0.8f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.1f)
                )
                // Glow 2: Background Depth (bottom-left)
                drawCircle(
                    color = NovaColors.AccentSecondary.copy(alpha = 0.05f),
                    radius = size.width * 0.7f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.8f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            // BRAND HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(NovaSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = NovaColors.AccentPrimary,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            tint = NovaColors.BackgroundPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(NovaSpacing.md))
                
                Column {
                    Text(
                        text = "NovaPass Wallet",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = "${tickets.size} boletos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaColors.TextSecondary
                    )
                }
            }

            // MODAL DE CONFIRMACIÓN DE ELIMINACIÓN
            if (ticketToDelete != null) {
                AlertDialog(
                    onDismissRequest = { ticketToDelete = null },
                    containerColor = NovaColors.BackgroundSecondary,
                    shape = RoundedCornerShape(24.dp),
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "Eliminar boleto",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    },
                    text = {
                        Text(
                            "Esta acción no se puede deshacer. El boleto será eliminado permanentemente.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { ticketToDelete = null },
                                modifier = Modifier.weight(1f).height(48.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("Cancelar", style = MaterialTheme.typography.labelLarge)
                            }
                            
                            Button(
                                onClick = {
                                    ticketToDelete?.let { viewModel.removeTicket(it) }
                                    ticketToDelete = null
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NovaColors.AccentPrimary),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Eliminar", color = NovaColors.BackgroundPrimary, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                )
            }

            // SEARCH BAR (GLASS STYLE - NO BLUR ON CONTENT)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(NovaInputBackground)
                    .border(1.dp, NovaGlassBorder, RoundedCornerShape(20.dp))
            ) {
                // Inner highlight overlay (top reflection)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, NovaGlassBorder, Color.Transparent)
                            )
                        )
                        .align(Alignment.TopCenter)
                )
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { 
                        Text(
                            "Buscar boletos...", 
                            color = MaterialTheme.colorScheme.onSurface, 
                            style = MaterialTheme.typography.bodyLarge 
                        ) 
                    },
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


            // LIST OR EMPTY STATE — sin Spacer para no crear una línea de corte
            Box(modifier = Modifier.fillMaxSize()) {
                if (filteredTickets.isEmpty()) {
                    EmptyStateView(isSearch = searchQuery.isNotEmpty())
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            // Fading edge con DstOut: borra el contenido de la lista en la parte
                            // superior revelando el fondo (blobs) que hay detras — sin usar color
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Black, Color.Transparent),
                                        endY = 28.dp.toPx()
                                    ),
                                    blendMode = BlendMode.DstOut
                                )
                            },
                        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
                    ) {
                        items(filteredTickets) { ticket ->
                            TicketItem(
                                ticket = ticket,
                                onClick = { onTicketClick(ticket) },
                                onDelete = { ticketToDelete = ticket }
                            )
                        }
                    }
                }
            }
        } // cierre Column principal
        } // cierre Box global blobs

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
                        // AMBIENTE QUANTUM TOTAL
                        Canvas(
                            modifier = Modifier
                                .matchParentSize()
                                .blur(80.dp)
                        ) {
                            drawCircle(
                                color = NovaGlowGreen.copy(alpha = 0.05f),
                                radius = size.width * 0.6f,
                                center = Offset(size.width * 0.3f, size.height * 0.1f)
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(NovaGlowBlue.copy(alpha = 0.10f), Color.Transparent)
                                ),
                                radius = size.width * 0.85f,
                                center = Offset(size.width * 0.8f, size.height * 0.4f)
                            )
                            drawCircle(
                                color = NovaGlowGreen.copy(alpha = 0.05f),
                                radius = size.width * 0.5f,
                                center = Offset(size.width * 0.95f, size.height * 0.85f)
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding() // Respeta el espacio de la barra de navegación de forma transparente
                                .padding(horizontal = NovaSpacing.lg),
                            contentPadding = PaddingValues(bottom = NovaSpacing.xl)
                        ) {
                            // 1. DRAG HANDLE
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(40.dp)
                                            .height(4.dp)
                                            .background(NovaColors.TextSecondary.copy(alpha = 0.4f), CircleShape)
                                    )
                                }
                            }

                            // 2. HEADER
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Detalles del Evento",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White
                                    )
                                    IconButton(
                                        onClick = { 
                                            showBottomSheet = false
                                            pendingTickets = emptyList()
                                        },
                                        modifier = Modifier.background(NovaColors.GlassMedium, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White.copy(alpha = 0.7f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(NovaSpacing.lg))
                            }

                            // 3. PDF PICKER
                            item {
                                Text("Selecciona el archivo *", style = MaterialTheme.typography.labelMedium, color = NovaColors.TextSecondary)
                                Spacer(modifier = Modifier.height(NovaSpacing.sm))
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (selectedUri == null) NovaColors.GlassLight else NovaColors.GlassMedium)
                                        .border(
                                            1.dp, 
                                            if (selectedUri == null) NovaColors.BorderSubtle else NovaColors.AccentPrimary.copy(alpha = 0.5f),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable { filePickerLauncher.launch(arrayOf("application/pdf")) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(NovaSpacing.md),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (selectedUri == null) Icons.Default.Description else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (selectedUri == null) NovaColors.TextSecondary.copy(alpha = 0.4f) else NovaColors.AccentPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(NovaSpacing.md))
                                        Text(
                                            if (selectedUri == null) "Toque para seleccionar PDF" else selectedFileName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (selectedUri == null) NovaColors.TextSecondary else NovaColors.AccentPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(NovaSpacing.lg))
                            }

                            // 4. FORM CONTENT
                            if (pendingTickets.size > 1) {
                                item {
                                    Text(
                                        "Se han detectado ${pendingTickets.size} boletos",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = NovaPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
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
                                            .border(
                                                1.dp,
                                                if (isEditing) NovaColors.AccentPrimary.copy(alpha = 0.3f) else NovaColors.BorderSubtle,
                                                RoundedCornerShape(20.dp)
                                            )
                                            .clickable { editingTicketIndex = if (isEditing) null else index }
                                    ) {
                                        Column(modifier = Modifier.padding(NovaSpacing.md)) {
                                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = if (isEditing) NovaColors.AccentPrimary else NovaColors.GlassMedium,
                                                    shape = CircleShape,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text("${index + 1}", color = if (isEditing) NovaColors.BackgroundPrimary else Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(NovaSpacing.md))
                                                Text(pTicket.eventName, modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold)
                                                Icon(if (isEditing) Icons.Default.KeyboardArrowUp else Icons.Default.Edit, contentDescription = null, tint = NovaColors.TextSecondary)
                                            }
                                            
                                            androidx.compose.animation.AnimatedVisibility(visible = isEditing) {
                                                Column {
                                                    Spacer(modifier = Modifier.height(NovaSpacing.md))
                                                    CustomInputField(
                                                        value = pTicket.eventName,
                                                        onValueChange = { newVal ->
                                                            val newList = pendingTickets.toMutableList()
                                                            newList[index] = pTicket.copy(eventName = newVal)
                                                            pendingTickets = newList
                                                        },
                                                        label = "Evento",
                                                        placeholder = "Nombre"
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        CustomInputField(value = pTicket.date, onValueChange = { }, label = "Fecha", placeholder = "Fecha", modifier = Modifier.weight(1.5f), readOnly = true, onClick = { showIndividualDatePicker = index })
                                                        CustomInputField(value = pTicket.time, onValueChange = { }, label = "Hora", placeholder = "Hora", modifier = Modifier.weight(1f), readOnly = true, onClick = { showIndividualTimePicker = index })
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        CustomInputField(value = pTicket.section, onValueChange = { v -> 
                                                            val newList = pendingTickets.toMutableList()
                                                            newList[index] = pTicket.copy(section = v)
                                                            pendingTickets = newList
                                                        }, label = "Sec", placeholder = "Sec", modifier = Modifier.weight(1f))
                                                        CustomInputField(value = pTicket.row, onValueChange = { v ->
                                                            val newList = pendingTickets.toMutableList()
                                                            newList[index] = pTicket.copy(row = v)
                                                            pendingTickets = newList
                                                        }, label = "Fila", placeholder = "Fila", modifier = Modifier.weight(1f))
                                                        CustomInputField(value = pTicket.seat, onValueChange = { v ->
                                                            val newList = pendingTickets.toMutableList()
                                                            newList[index] = pTicket.copy(seat = v)
                                                            pendingTickets = newList
                                                        }, label = "Asiento", placeholder = "Asi", modifier = Modifier.weight(1f))
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
                                                    .background(if (isSelected) NovaColors.AccentPrimary else NovaColors.GlassLight)
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) Color.Transparent else NovaColors.BorderSubtle,
                                                        RoundedCornerShape(20.dp)
                                                    )
                                                    .clickable { selectedCategory = category }
                                                    .padding(horizontal = NovaSpacing.md, vertical = NovaSpacing.sm)
                                            ) {
                                                Text(
                                                    category, 
                                                    color = if (isSelected) NovaColors.BackgroundPrimary else NovaColors.TextSecondary,
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(NovaSpacing.lg))

                                    CustomInputField(
                                        value = ticketName,
                                        onValueChange = { ticketName = it },
                                        label = "Nombre del evento *",
                                        placeholder = "Ej: Coldplay World Tour"
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        CustomInputField(value = eventDate, onValueChange = { }, label = "Fecha *", placeholder = "DD/MM/AAAA", modifier = Modifier.weight(1f), readOnly = true, onClick = { showDatePicker = true })
                                        Spacer(modifier = Modifier.width(16.dp))
                                        CustomInputField(value = eventTime, onValueChange = { }, label = "Hora", placeholder = "HH:MM", modifier = Modifier.weight(1f), readOnly = true, onClick = { showTimePicker = true })
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        CustomInputField(value = section, onValueChange = { section = it }, label = "Sección", placeholder = "Sección", modifier = Modifier.weight(1f))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        CustomInputField(value = row, onValueChange = { row = it }, label = "Fila", placeholder = "Fila", modifier = Modifier.weight(0.5f))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        CustomInputField(value = seat, onValueChange = { seat = it }, label = "Asiento", placeholder = "Asi", modifier = Modifier.weight(0.5f))
                                    }
                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                            }

                            // 5. SAVE BUTTON
                            item {
                                val saveInteractionSource = remember { MutableInteractionSource() }
                                val savePressed by saveInteractionSource.collectIsPressedAsState()
                                val saveScale by animateFloatAsState(if (savePressed) 0.97f else 1f, label = "saveScale")
                                val isEnabled = (ticketName.isNotBlank() || pendingTickets.isNotEmpty()) && selectedUri != null && !isVerifying

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .graphicsLayer {
                                            scaleX = if (isEnabled) saveScale else 1f
                                            scaleY = if (isEnabled) saveScale else 1f
                                        }
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isEnabled) NovaColors.AccentPrimary else NovaColors.GlassLight
                                        )
                                        .clickable(
                                            enabled = isEnabled,
                                            interactionSource = saveInteractionSource,
                                            indication = LocalIndication.current
                                        ) {
                                            selectedUri?.let { uri ->
                                                coroutineScope.launch {
                                                    try {
                                                        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    } catch (e: Exception) { e.printStackTrace() }
                                                    
                                                    if (pendingTickets.size > 1) {
                                                        pendingTickets.forEach { pTicket ->
                                                            onAddTicket(pTicket.eventName, uri, pTicket.category, pTicket.date, pTicket.time, "", pTicket.section, pTicket.row, pTicket.seat, pTicket.pageIndex)
                                                        }
                                                    } else {
                                                        onAddTicket(ticketName, uri, selectedCategory, eventDate, eventTime, location, section, row, seat, currentPageIndex)
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

    // Diálogo de Selección de Fecha (Compose Material 3)
    if (showDatePicker || showIndividualDatePicker != null) {
        DatePickerDialog(
            onDismissRequest = { 
                showDatePicker = false
                showIndividualDatePicker = null
            },
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
                        } else {
                            eventDate = date
                        }
                    }
                    showDatePicker = false
                    showIndividualDatePicker = null
                }) { Text("Aceptar", color = NovaPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDatePicker = false
                    showIndividualDatePicker = null
                }) { Text("Cancelar", color = Color.White.copy(alpha = 0.6f)) }
            },
            colors = DatePickerDefaults.colors(
                containerColor = NovaBackground,
                titleContentColor = Color.White,
                headlineContentColor = Color.White,
                selectedDayContainerColor = NovaPrimary,
                selectedDayContentColor = Color.Black,
                todayContentColor = NovaPrimary,
                todayDateBorderColor = NovaPrimary,
                dayContentColor = Color.White,
                weekdayContentColor = Color.White.copy(alpha = 0.5f),
                yearContentColor = Color.White,
                currentYearContentColor = NovaPrimary
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = NovaBackground,
                    titleContentColor = Color.White,
                    headlineContentColor = Color.White,
                    selectedDayContainerColor = NovaPrimary,
                    selectedDayContentColor = Color.Black,
                    todayContentColor = NovaPrimary,
                    todayDateBorderColor = NovaPrimary,
                    dayContentColor = Color.White,
                    weekdayContentColor = Color.White.copy(alpha = 0.5f),
                    yearContentColor = Color.White,
                    currentYearContentColor = NovaPrimary
                )
            )
        }
    }

    // Diálogo de Selección de Hora (Compose Material 3 - Estilo Reloj)
    if (showTimePicker || showIndividualTimePicker != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { 
            showTimePicker = false
            showIndividualTimePicker = null
        }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = NovaBackground,
                tonalElevation = 6.dp,
                modifier = Modifier.width(IntrinsicSize.Max).height(IntrinsicSize.Min)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Selecciona la hora",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 20.dp)
                    )
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = Color.White.copy(alpha = 0.05f),
                            clockDialSelectedContentColor = Color.Black,
                            clockDialUnselectedContentColor = Color.White,
                            selectorColor = NovaPrimary,
                            periodSelectorSelectedContainerColor = NovaPrimary,
                            periodSelectorSelectedContentColor = Color.Black,
                            periodSelectorUnselectedContentColor = Color.White,
                            timeSelectorSelectedContainerColor = NovaPrimary.copy(alpha = 0.2f),
                            timeSelectorSelectedContentColor = NovaPrimary,
                            timeSelectorUnselectedContainerColor = Color.White.copy(alpha = 0.05f),
                            timeSelectorUnselectedContentColor = Color.White
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { 
                            showTimePicker = false
                            showIndividualTimePicker = null
                        }) {
                            Text("Cancelar", color = Color.White.copy(alpha = 0.6f))
                        }
                        TextButton(onClick = {
                            val timeStr = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                            if (showIndividualTimePicker != null) {
                                val idx = showIndividualTimePicker!!
                                val newList = pendingTickets.toMutableList()
                                newList[idx] = newList[idx].copy(time = timeStr)
                                pendingTickets = newList
                            } else {
                                eventTime = timeStr
                            }
                            showTimePicker = false
                            showIndividualTimePicker = null
                        }) {
                            Text("Aceptar", color = NovaPrimary)
                        }
                    }
                }
            }
        }
    }
}
}

// ============================================================
// NovaPass Premium Design System — UI Components
// ============================================================

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
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

@Composable
fun CustomInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Text(
            label, 
            style = MaterialTheme.typography.labelMedium,
            color = NovaColors.TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(NovaSpacing.sm))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    1.dp, 
                    Color.Transparent,
                    RoundedCornerShape(20.dp)
                )
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder, color = NovaColors.TextSecondary.copy(alpha = 0.4f)) },
                shape = RoundedCornerShape(20.dp),
                readOnly = readOnly,
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NovaColors.AccentPrimary.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = NovaColors.GlassMedium,
                    unfocusedContainerColor = NovaColors.GlassLight,
                    disabledBorderColor = Color.Transparent,
                    disabledContainerColor = NovaColors.GlassLight.copy(alpha = 0.5f),
                    disabledTextColor = NovaColors.TextSecondary
                ),
                singleLine = true
            )
            // Overlay invisible para interceptar el clic si es readOnly y tiene onClick
            if (onClick != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Transparent)
                        .clickable { onClick() }
                )
            }
        }
    }
}

@Composable
fun TicketItem(ticket: TicketEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    val categoryIcon = when (ticket.category) {
        "Concierto" -> Icons.Default.MusicNote
        "Deportes" -> Icons.Default.SportsBaseball
        "Cine" -> Icons.Default.Movie
        else -> Icons.Default.ConfirmationNumber
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
            // Header: Glass layer with subtle accent glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NovaColors.AccentPrimary.copy(alpha = 0.02f),
                                NovaColors.AccentPrimary.copy(alpha = 0.08f)
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
                            .border(1.dp, NovaColors.AccentPrimary.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            categoryIcon,
                            contentDescription = null,
                            tint = NovaColors.AccentPrimary,
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
                        Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = NovaColors.TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Perforación con agujeros reales usando BlendMode.Clear
            // Perforación con agujeros reales usando BlendMode.Clear
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                val halfH = size.height / 2f
                val notchR = 12.dp.toPx()

                // Background continuity layers
                drawRect(
                    color = NovaColors.AccentPrimary.copy(alpha = 0.08f),
                    size = Size(size.width, halfH)
                )
                drawRect(
                    color = Color.Transparent, // El fondo de la GlassCard ya es GlassLight
                    topLeft = Offset(0f, halfH),
                    size = Size(size.width, halfH)
                )
                
                // Muesca izquierda (Clear to show background blobs)
                drawCircle(
                    color = Color.Black,
                    radius = notchR,
                    center = Offset(-notchR * 0.1f, halfH),
                    blendMode = BlendMode.Clear
                )
                // Muesca derecha
                drawCircle(
                    color = Color.Black,
                    radius = notchR,
                    center = Offset(size.width + notchR * 0.1f, halfH),
                    blendMode = BlendMode.Clear
                )
                // Línea punteada premium
                drawLine(
                    color = NovaColors.BorderSubtle,
                    start = Offset(notchR + 10.dp.toPx(), halfH),
                    end = Offset(size.width - notchR - 10.dp.toPx(), halfH),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                )
            }

            // Capa Inferior (Body Content)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = NovaSpacing.md, end = NovaSpacing.md, bottom = NovaSpacing.md, top = 4.dp)
            ) {
                // Fila de Fecha y Hora
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, null, tint = NovaColors.AccentPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(NovaSpacing.sm))
                    Text(
                        ticket.eventDate ?: "Fecha TBD",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaColors.TextSecondary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(NovaSpacing.sm))
                    
                    Icon(Icons.Default.AccessTime, null, tint = NovaColors.AccentPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(NovaSpacing.xs))
                    
                    val formattedTime = remember(ticket.eventTime) {
                        try {
                            if (ticket.eventTime.isNullOrBlank()) "--:--"
                            else {
                                val sdf24 = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                val sdf12 = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                val date = sdf24.parse(ticket.eventTime)
                                if (date != null) sdf12.format(date) else ticket.eventTime
                            }
                        } catch (e: Exception) {
                            ticket.eventTime ?: "--:--"
                        }
                    }
                    
                    // Hora: no gold, use white (design system rule)
                    Text(
                        formattedTime,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(NovaSpacing.sm))
                
                // Fila de Ubicación/Asiento Combinada
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Weekend, null, tint = NovaColors.AccentPrimary, modifier = Modifier.size(16.dp))
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
                    tint = NovaColors.AccentPrimary,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(uri: Uri, pageIndex: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<android.os.ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    val renderMutex = remember { Mutex() }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale == 1f) {
            offset = androidx.compose.ui.geometry.Offset.Zero
        } else {
            val maxX = (screenWidthPx * scale - screenWidthPx) / 2f
            val maxY = (screenHeightPx * scale - screenHeightPx) / 2f
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visualizar Boleto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (pdfRenderer != null && pageCount > 0) {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .transformable(state = transformableState)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    PdfPageImage(pdfRenderer = pdfRenderer!!, pageIndex = pageIndex.coerceIn(0, pageCount - 1), renderMutex = renderMutex)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Cargando o error al leer PDF")
            }
        }
    }
}

@Composable
fun PdfPageImage(pdfRenderer: PdfRenderer, pageIndex: Int, renderMutex: Mutex) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) {
            renderMutex.withLock {
                try {
                    val page = pdfRenderer.openPage(pageIndex)
                    // Multiplicamos por 2 para mayor definicion en el zoom
                    val width = page.width * 2
                    val height = page.height * 2
                    val currentBitmap = android.graphics.Bitmap.createBitmap(
                        width,
                        height,
                        android.graphics.Bitmap.Config.ARGB_8888
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
            contentDescription = "Página $pageIndex del PDF",
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

suspend fun isValidTicket(context: Context, uri: Uri): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val fd = context.contentResolver.openFileDescriptor(uri, "r")
            if (fd != null) {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val document = PDDocument.load(inputStream)
                    val stripper = PDFTextStripper()
                    stripper.startPage = 1
                    stripper.endPage = 2
                    val text = stripper.getText(document).lowercase()
                    document.close()
                    inputStream.close()
                    
                    val keywords = listOf("boleto", "ticket", "butaca", "fila", "sector", "zona", "acceso", "seat", "row", "section")
                    for (keyword in keywords) {
                        if (text.contains(keyword)) {
                            return@withContext true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                    stripper.startPage = pageIndex
                    stripper.endPage = pageIndex
                    val text = stripper.getText(document)
                    
                    var eventName = ""
                    var category = "Otro"
                    var date = ""
                    var time = ""
                    var section = ""
                    var row = ""
                    var seat = ""
                    
                    val lines = text.split("\n").map { it.trim() }
                    
                    // Lógica de Nombre de Evento (Buscando "VS")
                    val vsLine = lines.find { it.contains(" VS ", ignoreCase = true) }
                    if (vsLine != null) {
                        eventName = vsLine
                            .substringBefore(" SERIE") 
                            .trim()
                            .uppercase()
                        category = "Deportes"
                    }

                    // Lógica de Fecha
                    val datePattern = Regex("(?i)(lunes|martes|miércoles|jueves|viernes|sábado|domingo),\\s+\\d+\\s+DE\\s+[A-Z]+\\s+DE\\s+\\d{4}")
                    val dateMatch = datePattern.find(text)
                    if (dateMatch != null) {
                        date = dateMatch.value
                    }

                    // Lógica de Sección
                    if (text.contains("SECCION", ignoreCase = true)) {
                        val sectionMatch = Regex("(?i)SECCION\\s+([A-Z0-9 ]+)").find(text)
                        section = sectionMatch?.groupValues?.get(1)?.trim() ?: ""
                    }
                    
                    // Lógica de Fila y Asiento
                    val rowSeatPattern = Regex("(?i)FILA\\s*\\|?\\s*BUTACA\\s*\\n\\s*([A-Z0-9]+)\\s+([A-Z0-9]+)")
                    val rowSeatMatch = rowSeatPattern.find(text)
                    if (rowSeatMatch != null) {
                        row = rowSeatMatch.groupValues[1]
                        seat = rowSeatMatch.groupValues[2]
                    }

                    // Si no se encontró nombre con VS, usar el título del PDF
                    if (eventName.isBlank()) {
                        eventName = getPdfTitle(context, uri).uppercase()
                    }
                    
                    // Solo agregamos si detectamos algo de información (para evitar páginas en blanco)
                    if (eventName.isNotBlank() || section.isNotBlank() || seat.isNotBlank()) {
                        extractedTickets.add(ExtractedTicketData(eventName, category, date, time, "", section, row, seat, pageIndex - 1))
                    }
                }
                document.close()
                inputStream.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        extractedTickets
    }
}

suspend fun getPdfTitle(context: Context, uri: Uri): String {
    return withContext(Dispatchers.IO) {
        var title = ""
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    val displayName = cursor.getString(nameIndex)
                    title = displayName.substringBeforeLast(".")
                }
            }
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val document = PDDocument.load(inputStream)
                val pdfTitle = document.documentInformation?.title
                document.close()
                inputStream.close()
                if (!pdfTitle.isNullOrBlank()) {
                    title = pdfTitle
                }
            }
        } catch (e: Exception) {}
        return@withContext title
    }
}
