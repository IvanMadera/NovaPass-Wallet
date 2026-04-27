package com.example.novapass.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.novapass.TicketViewModel
import com.example.novapass.ui.state.AddTicketResult
import com.example.novapass.ui.state.AddTicketUiState
import com.example.novapass.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────
// AddTicketBottomSheet — formulario para agregar boleto(s) desde PDF
// Antes formaba parte de TicketListScreen (791 líneas → ahora ~200).
// ─────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTicketBottomSheet(
    viewModel: TicketViewModel,
    formState: AddTicketUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Pickers de fecha/hora (estado UI puro — no necesita sobrevivir al ViewModel)
    var showDatePicker           by remember { mutableStateOf(false) }
    var showTimePicker           by remember { mutableStateOf(false) }
    var showIndividualDatePicker by remember { mutableStateOf<Int?>(null) }
    var showIndividualTimePicker by remember { mutableStateOf<Int?>(null) }
    var editingTicketIndex       by remember { mutableStateOf<Int?>(null) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState(initialHour = 12, initialMinute = 0, is24Hour = false)
    val categories      = listOf("Concierto", "Cine", "Deportes", "Otro")

    // Consumir eventos de un solo disparo del ViewModel
    LaunchedEffect(Unit) {
        viewModel.addTicketResult.collect { result ->
            when (result) {
                is AddTicketResult.InvalidFile -> {
                    Toast.makeText(context, "El archivo no parece ser un boleto válido", Toast.LENGTH_LONG).show()
                }
                is AddTicketResult.Processed -> {
                    val added   = result.addedCount
                    val skipped = result.skippedCount
                    val msg = when {
                        added > 0 && skipped > 0 -> "$added boletos guardados, $skipped duplicados omitidos"
                        added > 0                -> if (added == 1) "Boleto guardado exitosamente" else "$added boletos guardados"
                        else                     -> if (skipped == 1) "El boleto ya existe en tu wallet" else "$skipped boletos ya existen"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    onDismiss()
                }
            }
        }
    }

    // SAF File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.onPdfSelected(uri)
    }

    // Scale al guardar
    val saveInteractionSource = remember { MutableInteractionSource() }
    val savePressed by saveInteractionSource.collectIsPressedAsState()
    val saveScale   by animateFloatAsState(if (savePressed) 0.97f else 1f, label = "saveScale")
    val isEnabled   = (formState.ticketName.isNotBlank() || formState.pendingTickets.isNotEmpty()) &&
                       formState.selectedUri != null && !formState.isVerifying

    Box(modifier = Modifier.fillMaxWidth()) {
        // Glows ambientales del sheet
        Canvas(modifier = Modifier.matchParentSize().blur(80.dp)) {
            drawCircle(color = NovaColors.GoldPrimary.copy(alpha = 0.10f),  radius = size.width * 0.6f, center = Offset(size.width * 0.3f, size.height * 0.1f))
            drawCircle(color = NovaColors.GreenPrimary.copy(alpha = 0.15f), radius = size.width * 0.85f, center = Offset(size.width * 0.8f, size.height * 0.4f))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = NovaSpacing.lg),
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
                    IconButton(onClick = { viewModel.resetForm(); onDismiss() }, modifier = Modifier.background(NovaColors.GlassMedium, CircleShape)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White.copy(alpha = 0.7f))
                    }
                }
                Spacer(modifier = Modifier.height(NovaSpacing.lg))
            }

            // PDF Picker
            item {
                Text("Selecciona el archivo *", style = MaterialTheme.typography.labelMedium, color = NovaColors.TextSecondary)
                Spacer(modifier = Modifier.height(NovaSpacing.sm))
                val hasFile = formState.selectedUri != null
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (hasFile) NovaColors.GlassMedium else NovaColors.GlassLight)
                        .border(1.dp, if (hasFile) NovaColors.GoldPrimary.copy(alpha = 0.5f) else NovaColors.BorderSubtle, RoundedCornerShape(20.dp))
                        .clickable { filePickerLauncher.launch(arrayOf("application/pdf")) }
                ) {
                    Row(modifier = Modifier.padding(NovaSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (hasFile) Icons.Default.CheckCircle else Icons.Default.Description,
                            contentDescription = null,
                            tint = if (hasFile) NovaColors.GoldPrimary else NovaColors.TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(NovaSpacing.md))
                        Text(
                            if (hasFile) formState.selectedFileName else "Toque para seleccionar sus boletos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hasFile) NovaColors.GoldPrimary else NovaColors.TextSecondary,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(NovaSpacing.lg))
            }

            // Multi-ticket o formulario individual
            if (formState.pendingTickets.size > 1) {
                item {
                    Text("Se han detectado ${formState.pendingTickets.size} boletos",
                        style = MaterialTheme.typography.titleMedium, color = NovaPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                itemsIndexed(formState.pendingTickets) { index, pTicket ->
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
                                Surface(color = if (isEditing) NovaColors.GoldPrimary else NovaColors.BackgroundSecondary, shape = CircleShape, modifier = Modifier.size(36.dp).border(1.dp, if (isEditing) Color.Transparent else NovaColors.BorderSubtle, CircleShape)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${index + 1}", color = if (isEditing) NovaColors.BackgroundPrimary else NovaColors.GoldPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(NovaSpacing.md))
                                Text(pTicket.eventName, modifier = Modifier.weight(1f), color = NovaColors.TextPrimary, fontWeight = FontWeight.Bold)
                                Icon(if (isEditing) Icons.Default.KeyboardArrowUp else Icons.Default.Edit, contentDescription = null, tint = NovaColors.GoldPrimary.copy(alpha = 0.7f))
                            }
                            AnimatedVisibility(visible = isEditing) {
                                Column {
                                    Spacer(modifier = Modifier.height(NovaSpacing.md))
                                    CustomInputField(value = pTicket.eventName, onValueChange = { viewModel.updatePendingTicket(index, pTicket.copy(eventName = it)) }, label = "Evento", placeholder = "Nombre")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CustomInputField(value = pTicket.date, onValueChange = {}, label = "Fecha", placeholder = "Fecha", modifier = Modifier.weight(1.5f), readOnly = true, onClick = { showIndividualDatePicker = index })
                                        CustomInputField(value = pTicket.time, onValueChange = {}, label = "Hora",  placeholder = "Hora",  modifier = Modifier.weight(1f),   readOnly = true, onClick = { showIndividualTimePicker = index })
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CustomInputField(value = pTicket.section, onValueChange = { viewModel.updatePendingTicket(index, pTicket.copy(section = it)) }, label = "Sec",     placeholder = "102", modifier = Modifier.weight(1f))
                                        CustomInputField(value = pTicket.row,     onValueChange = { viewModel.updatePendingTicket(index, pTicket.copy(row = it)) },     label = "Fila",    placeholder = "A",   modifier = Modifier.weight(1f))
                                        CustomInputField(value = pTicket.seat,    onValueChange = { viewModel.updatePendingTicket(index, pTicket.copy(seat = it)) },    label = "Asiento", placeholder = "15",  modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }

            } else {
                item {
                    // Categorías
                    Text("Categoría", style = MaterialTheme.typography.labelMedium, color = NovaColors.TextSecondary)
                    Spacer(modifier = Modifier.height(NovaSpacing.sm))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm)) {
                        categories.forEach { cat ->
                            val sel = formState.selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (sel) NovaBrushes.GoldGradient else SolidColor(NovaColors.GlassLight))
                                    .border(1.dp, if (sel) Color.Transparent else NovaColors.BorderSubtle, RoundedCornerShape(20.dp))
                                    .clickable { viewModel.updateCategory(cat) }
                                    .padding(horizontal = NovaSpacing.md, vertical = NovaSpacing.sm)
                            ) {
                                Text(cat, color = if (sel) NovaColors.BackgroundPrimary else NovaColors.TextSecondary, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(NovaSpacing.lg))

                    CustomInputField(value = formState.ticketName, onValueChange = { viewModel.updateTicketName(it) }, label = "Nombre del evento *", placeholder = "Ej: Coldplay World Tour")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        CustomInputField(value = formState.eventDate, onValueChange = {}, label = "Fecha *", placeholder = "DD/MM/AAAA", modifier = Modifier.weight(1f), readOnly = true, onClick = { showDatePicker = true })
                        Spacer(modifier = Modifier.width(16.dp))
                        CustomInputField(value = formState.eventTime, onValueChange = {}, label = "Hora", placeholder = "HH:MM", modifier = Modifier.weight(1f), readOnly = true, onClick = { showTimePicker = true })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        CustomInputField(value = formState.section, onValueChange = { viewModel.updateSection(it) }, label = "Sección", placeholder = "Ej: 102", modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(16.dp))
                        CustomInputField(value = formState.row,     onValueChange = { viewModel.updateRow(it) },     label = "Fila",    placeholder = "Ej: A",  modifier = Modifier.weight(0.5f))
                        Spacer(modifier = Modifier.width(16.dp))
                        CustomInputField(value = formState.seat,    onValueChange = { viewModel.updateSeat(it) },    label = "Asiento", placeholder = "Ej: 15", modifier = Modifier.weight(0.5f))
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Botón Guardar
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .graphicsLayer { scaleX = if (isEnabled) saveScale else 1f; scaleY = if (isEnabled) saveScale else 1f }
                        .clip(RoundedCornerShape(20.dp))
                        .then(if (isEnabled) Modifier.background(NovaBrushes.GoldGradient) else Modifier.background(NovaColors.GlassLight))
                        .clickable(enabled = isEnabled, interactionSource = saveInteractionSource, indication = LocalIndication.current) {
                            viewModel.saveTickets(context)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (formState.isVerifying) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(
                            if (formState.pendingTickets.size > 1) "Guardar ${formState.pendingTickets.size} Boletos" else "Guardar Boleto",
                            color = if (isEnabled) NovaColors.BackgroundPrimary else NovaColors.TextSecondary.copy(alpha = 0.3f),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }

    // ── Date Picker ────────────────────────────────────────────────────────
    if (showDatePicker || showIndividualDatePicker != null) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false; showIndividualDatePicker = null },
            confirmButton = {
                TextButton(onClick = {
                    val formatted = datePickerState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("EEEE, dd 'DE' MMMM 'DE' yyyy", java.util.Locale("es", "ES")))
                            .uppercase()
                    }
                    if (formatted != null) {
                        val idx = showIndividualDatePicker
                        if (idx != null) {
                            val ticket = formState.pendingTickets.getOrNull(idx)
                            if (ticket != null) viewModel.updatePendingTicket(idx, ticket.copy(date = formatted))
                        } else {
                            viewModel.updateEventDate(formatted)
                        }
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

    // ── Time Picker ────────────────────────────────────────────────────────
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
                            val idx = showIndividualTimePicker
                            if (idx != null) {
                                val ticket = formState.pendingTickets.getOrNull(idx)
                                if (ticket != null) viewModel.updatePendingTicket(idx, ticket.copy(time = timeStr))
                            } else {
                                viewModel.updateEventTime(timeStr)
                            }
                            showTimePicker = false; showIndividualTimePicker = null
                        }) { Text("Aceptar", color = NovaPrimary) }
                    }
                }
            }
        }
    }
}
