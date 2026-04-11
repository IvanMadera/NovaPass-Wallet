package com.example.novapass

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.example.novapass.ui.theme.NovaPassTheme
import com.example.novapass.ui.theme.NovaInputBackground
import android.content.Intent
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import android.widget.Toast

import android.graphics.pdf.PdfRenderer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

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
        enableEdgeToEdge()
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

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    // Resetear estados al abrir
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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = androidx.compose.foundation.shape.CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Ticket")
            }
        },
        containerColor = Color.Transparent  // Transparente para que los blobs se vean en toda la pantalla
    ) { innerPadding ->
        // Box global con blobs para cubrir TODA la pantalla (header + search + tickets)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // Fondo base
        ) {
            Canvas(modifier = Modifier.fillMaxSize().blur(60.dp)) {
                // Blob 1: Dorado cálido arriba-derecha (tarjeta dorada del logo)
                drawCircle(
                    color = androidx.compose.ui.graphics.Color(0xFFC09A3C).copy(alpha = 0.22f),
                    radius = size.width * 0.45f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.12f)
                )
                // Blob 2: Verde bosque abajo-izquierda (tarjeta verde del logo)
                drawCircle(
                    color = androidx.compose.ui.graphics.Color(0xFF2D4A3E).copy(alpha = 0.30f),
                    radius = size.width * 0.55f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.65f)
                )
                // Blob 3: Teal apagado centro-derecha (fondo del logo)
                drawCircle(
                    color = androidx.compose.ui.graphics.Color(0xFF4D7A73).copy(alpha = 0.16f),
                    radius = size.width * 0.3f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.45f)
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
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "NovaPass Wallet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${tickets.size} boletos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            // MODAL DE CONFIRMACIÓN DE ELIMINACIÓN
            if (ticketToDelete != null) {
                AlertDialog(
                    onDismissRequest = { ticketToDelete = null },
                    containerColor = Color(0xFF172321), // Gris Premium de NovaPass
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
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("Eliminar", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                )
            }

            // SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar boletos...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                singleLine = true
            )


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
                containerColor = MaterialTheme.colorScheme.surface,
                scrimColor = Color.Black.copy(alpha = 0.6f),
                dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Detalles del Evento",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showBottomSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Selecciona el archivo *", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { filePickerLauncher.launch(arrayOf("application/pdf")) },
                        color = if (selectedUri == null) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, 
                            if (selectedUri == null) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (selectedUri == null) Icons.Default.Description else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (selectedUri == null) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (selectedUri == null) "Toque para seleccionar PDF" else selectedFileName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (pendingTickets.size > 1) {
                        // VISTA DE MÚLTIPLES BOLETOS DETECTADOS
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Se han detectado ${pendingTickets.size} boletos en este archivo",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Revisa la información extraída de cada página:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pendingTickets.forEachIndexed { index, pTicket ->
                                val isEditing = editingTicketIndex == index
                                Card(
                                    onClick = { editingTicketIndex = if (isEditing) null else index },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isEditing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = if (isEditing) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                shape = androidx.compose.foundation.shape.CircleShape,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("${index + 1}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(pTicket.eventName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                if (!isEditing) {
                                                    Text(
                                                        "SEC ${pTicket.section} | FILA ${pTicket.row} | ASIENTO ${pTicket.seat}", 
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                            Icon(
                                                if (isEditing) Icons.Default.KeyboardArrowUp else Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        
                                        androidx.compose.animation.AnimatedVisibility(visible = isEditing) {
                                            Column {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                
                                                CustomInputField(
                                                    value = pTicket.eventName,
                                                    onValueChange = { newVal ->
                                                        val newList = pendingTickets.toMutableList()
                                                        newList[index] = pTicket.copy(eventName = newVal)
                                                        pendingTickets = newList
                                                    },
                                                    label = "Evento",
                                                    placeholder = "Nombre del evento"
                                                )
                                                
                                                Spacer(modifier = Modifier.height(12.dp))
                                                
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    CustomInputField(
                                                        value = pTicket.section,
                                                        onValueChange = { newVal ->
                                                            val newList = pendingTickets.toMutableList()
                                                            newList[index] = pTicket.copy(section = newVal)
                                                            pendingTickets = newList
                                                        },
                                                        label = "Sec",
                                                        placeholder = "117",
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    CustomInputField(
                                                        value = pTicket.row,
                                                        onValueChange = { newVal ->
                                                            val newList = pendingTickets.toMutableList()
                                                            newList[index] = pTicket.copy(row = newVal)
                                                            pendingTickets = newList
                                                        },
                                                        label = "Fila",
                                                        placeholder = "J",
                                                        modifier = Modifier.weight(0.7f)
                                                    )
                                                    CustomInputField(
                                                        value = pTicket.seat,
                                                        onValueChange = { newVal ->
                                                            val newList = pendingTickets.toMutableList()
                                                            newList[index] = pTicket.copy(seat = newVal)
                                                            pendingTickets = newList
                                                        },
                                                        label = "Asiento",
                                                        placeholder = "10",
                                                        modifier = Modifier.weight(0.7f)
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.height(12.dp))
                                                
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    CustomInputField(
                                                        value = pTicket.date,
                                                        onValueChange = { },
                                                        label = "Fecha",
                                                        placeholder = "dd/mm/aaaa",
                                                        modifier = Modifier.weight(1f),
                                                        readOnly = true,
                                                        onClick = { showIndividualDatePicker = index }
                                                    )
                                                    CustomInputField(
                                                        value = pTicket.time,
                                                        onValueChange = { },
                                                        label = "Hora",
                                                        placeholder = "--:--",
                                                        modifier = Modifier.weight(0.6f),
                                                        readOnly = true,
                                                        onClick = { showIndividualTimePicker = index }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Date and Time Pickers por boleto individual
                        if (showIndividualDatePicker != null) {
                            val idx = showIndividualDatePicker!!
                            val datePickerState = rememberDatePickerState()
                            DatePickerDialog(
                                onDismissRequest = { showIndividualDatePicker = null },
                                confirmButton = {
                                    TextButton(onClick = {
                                        datePickerState.selectedDateMillis?.let {
                                            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                            val newList = pendingTickets.toMutableList()
                                            newList[idx] = pendingTickets[idx].copy(date = sdf.format(java.util.Date(it)))
                                            pendingTickets = newList
                                        }
                                        showIndividualDatePicker = null
                                    }) { Text("Aceptar", color = MaterialTheme.colorScheme.primary) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showIndividualDatePicker = null }) { Text("Cancelar", color = MaterialTheme.colorScheme.primary) }
                                },
                                colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                DatePicker(
                                    state = datePickerState,
                                    colors = DatePickerDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                                        headlineContentColor = MaterialTheme.colorScheme.onSurface,
                                        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                                        todayContentColor = MaterialTheme.colorScheme.primary,
                                        todayDateBorderColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        if (showIndividualTimePicker != null) {
                            val idx = showIndividualTimePicker!!
                            val timePickerState = rememberTimePickerState()
                            AlertDialog(
                                onDismissRequest = { showIndividualTimePicker = null },
                                confirmButton = {
                                    TextButton(onClick = {
                                        val timeStr = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                                        val newList = pendingTickets.toMutableList()
                                        newList[idx] = pendingTickets[idx].copy(time = timeStr)
                                        pendingTickets = newList
                                        showIndividualTimePicker = null
                                    }) { Text("Aceptar", color = MaterialTheme.colorScheme.primary) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showIndividualTimePicker = null }) { Text("Cancelar", color = MaterialTheme.colorScheme.primary) }
                                },
                                containerColor = MaterialTheme.colorScheme.surface,
                                text = {
                                    TimePicker(
                                        state = timePickerState,
                                        colors = TimePickerDefaults.colors(
                                            clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                                            clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                                            clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            selectorColor = MaterialTheme.colorScheme.primary,
                                            periodSelectorBorderColor = MaterialTheme.colorScheme.primary,
                                            periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                                            periodSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                                            periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                                            timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                                            timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            )
                        }
                    } else {
                        // VISTA DE EDICIÓN MANUAL (1 solo boleto)
                        Text("Categoría", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { category ->
                                val isSelected = selectedCategory == category
                                Surface(
                                    onClick = { selectedCategory = category },
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            category,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        CustomInputField(
                            value = ticketName,
                            onValueChange = { ticketName = it },
                            label = "Nombre del evento *",
                            placeholder = "Ej: Coldplay World Tour"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        var showDatePicker by remember { mutableStateOf(false) }
                        var showTimePicker by remember { mutableStateOf(false) }
                        
                        if (showDatePicker) {
                            val datePickerState = rememberDatePickerState()
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        datePickerState.selectedDateMillis?.let {
                                            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                            eventDate = sdf.format(java.util.Date(it))
                                        }
                                        showDatePicker = false
                                    }) { Text("Aceptar", color = MaterialTheme.colorScheme.primary) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDatePicker = false }) { Text("Cancelar", color = MaterialTheme.colorScheme.primary) }
                                },
                                colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                DatePicker(
                                    state = datePickerState,
                                    colors = DatePickerDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                                        headlineContentColor = MaterialTheme.colorScheme.onSurface,
                                        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                                        todayContentColor = MaterialTheme.colorScheme.primary,
                                        todayDateBorderColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        if (showTimePicker) {
                            val timePickerState = rememberTimePickerState()
                            AlertDialog(
                                onDismissRequest = { showTimePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        eventTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                                        showTimePicker = false
                                    }) { Text("Aceptar", color = MaterialTheme.colorScheme.primary) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showTimePicker = false }) { Text("Cancelar", color = MaterialTheme.colorScheme.primary) }
                                },
                                containerColor = MaterialTheme.colorScheme.surface,
                                text = {
                                    TimePicker(
                                        state = timePickerState,
                                        colors = TimePickerDefaults.colors(
                                            clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                                            clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                                            clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            selectorColor = MaterialTheme.colorScheme.primary,
                                            periodSelectorBorderColor = MaterialTheme.colorScheme.primary,
                                            periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                                            periodSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                                            periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                                            timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                                            timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            CustomInputField(
                                value = eventDate,
                                onValueChange = { },
                                label = "Fecha *",
                                placeholder = "dd/mm/aa",
                                modifier = Modifier.weight(1f),
                                readOnly = true,
                                onClick = { showDatePicker = true }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            CustomInputField(
                                value = eventTime,
                                onValueChange = { },
                                label = "Hora",
                                placeholder = "--:--",
                                modifier = Modifier.weight(1f),
                                readOnly = true,
                                onClick = { showTimePicker = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            CustomInputField(
                                value = section,
                                onValueChange = { section = it },
                                label = "Sección",
                                placeholder = "Sector",
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            CustomInputField(
                                value = row,
                                onValueChange = { row = it },
                                label = "Fila",
                                placeholder = "J",
                                modifier = Modifier.weight(0.5f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            CustomInputField(
                                value = seat,
                                onValueChange = { seat = it },
                                label = "Asiento",
                                placeholder = "10",
                                modifier = Modifier.weight(0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            selectedUri?.let { uri ->
                                coroutineScope.launch {
                                    try {
                                        context.contentResolver.takePersistableUriPermission(
                                            uri,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    if (pendingTickets.size > 1) {
                                        pendingTickets.forEach { pTicket ->
                                            onAddTicket(
                                                pTicket.eventName, 
                                                uri, 
                                                pTicket.category, 
                                                pTicket.date, 
                                                pTicket.time, 
                                                "", 
                                                pTicket.section, 
                                                pTicket.row, 
                                                pTicket.seat,
                                                pTicket.pageIndex
                                            )
                                        }
                                    } else {
                                        // Guardar el boleto individual editado
                                        onAddTicket(ticketName, uri, selectedCategory, eventDate, eventTime, location, section, row, seat, currentPageIndex)
                                    }
                                    showBottomSheet = false
                                }
                            }
                        },
                        enabled = (ticketName.isNotBlank() || pendingTickets.isNotEmpty()) && selectedUri != null && !isVerifying,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            val buttonText = if (pendingTickets.size > 1) "Guardar ${pendingTickets.size} Boletos" else "Guardar Boleto"
                            Text(buttonText, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .clip(RoundedCornerShape(12.dp))
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) },
                shape = RoundedCornerShape(12.dp),
                readOnly = readOnly,
                enabled = enabled,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = NovaInputBackground,
                    unfocusedContainerColor = NovaInputBackground,
                    disabledBorderColor = Color.Transparent,
                    disabledContainerColor = NovaInputBackground,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface
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

    // Body: Charcoal oscuro del logo (cuerpo del wallet)
    val bodyBackgroundColor = Color(0xFF141620)
    // Header: Plateado — blanco desaturado semi-transparente sobre el fondo negro
    val headerGlassColor = Color(0xFFCDD5E0).copy(alpha = 0.13f)

    // Box con offscreen para que BlendMode.Clear funcione correctamente
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Glassmorphism Plateado
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f), // Toque de plata arriba
                                Color.White.copy(alpha = 0.08f)  // Se suaviza abajo
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                categoryIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = ticket.name.uppercase(),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 2,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Eliminar", modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Perforación con agujeros reales usando BlendMode.Clear
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                val halfH = size.height / 2f
                val notchR = 13.dp.toPx()

                // Fondo superior de la perforación (efecto glass plateado)
                drawRect(
                    color = Color.White.copy(alpha = 0.12f),
                    size = Size(size.width, halfH)
                )
                // Fondo inferior de la perforación (navy body)
                drawRect(
                    color = bodyBackgroundColor,
                    topLeft = Offset(0f, halfH),
                    size = Size(size.width, halfH)
                )
                // Muesca izquierda: agujero real (transparente al fondo con blobs)
                drawCircle(
                    color = Color.Black,
                    radius = notchR,
                    center = Offset(-notchR * 0.25f, halfH),
                    blendMode = BlendMode.Clear
                )
                // Muesca derecha: agujero real
                drawCircle(
                    color = Color.Black,
                    radius = notchR,
                    center = Offset(size.width + notchR * 0.25f, halfH),
                    blendMode = BlendMode.Clear
                )
                // Línea punteada divisoria
                drawLine(
                    color = Color.White.copy(alpha = 0.20f),
                    start = Offset(notchR + 10.dp.toPx(), halfH),
                    end = Offset(size.width - notchR - 10.dp.toPx(), halfH),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 10f), 0f)
                )
            }

            // Capa Inferior (Body)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bodyBackgroundColor)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp)
            ) {
                // Fila de Fecha y Hora (AM/PM)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        ticket.eventDate ?: "Fecha TBD",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    
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
                    
                    Text(
                        formattedTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Fila de Ubicación/Asiento Combinada
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EventSeat, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    val locationParts = mutableListOf<String>()
                    if (!ticket.section.isNullOrBlank()) locationParts.add("Sección ${ticket.section}")
                    if (!ticket.row.isNullOrBlank()) locationParts.add("Fila ${ticket.row}")
                    if (!ticket.seat.isNullOrBlank()) locationParts.add("Asiento ${ticket.seat}")
                    
                    val locationText = if (locationParts.isEmpty()) "Boleto Digital" else locationParts.joinToString(" | ")
                    
                    Text(
                        locationText.uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.ConfirmationNumber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = if (isSearch) "Sin coincidencias" else "Tu wallet está vacía",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isSearch) 
                "No encontramos boletos que coincidan con tu búsqueda." 
            else 
                "Agrega tu primer boleto tocando el botón de abajo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
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
