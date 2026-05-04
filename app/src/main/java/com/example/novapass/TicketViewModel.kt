package com.example.novapass

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.novapass.data.AppDatabase
import com.example.novapass.data.TicketEntity
import com.example.novapass.data.repository.PdfRepository
import com.example.novapass.models.ExtractedTicketData
import com.example.novapass.ui.state.AddTicketResult
import com.example.novapass.ui.state.AddTicketUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class TicketViewModel(application: Application) : AndroidViewModel(application) {

    private val ticketDao     = AppDatabase.getDatabase(application).ticketDao()
    private val pdfRepository = PdfRepository(application)

    // ── Lista de boletos ─────────────────────────────────────────────────────
    val tickets: StateFlow<List<TicketEntity>> = ticketDao.getAllTickets()
        .stateIn(
            scope   = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ── Estado del formulario ────────────────────────────────────────────────
    private val _formState = MutableStateFlow(AddTicketUiState())
    val formState: StateFlow<AddTicketUiState> = _formState.asStateFlow()

    // ── Eventos de un solo disparo → toasts en la UI ─────────────────────────
    private val _addTicketResult = MutableSharedFlow<AddTicketResult>()
    val addTicketResult: SharedFlow<AddTicketResult> = _addTicketResult.asSharedFlow()

    // ── Actualizadores de campos del formulario ───────────────────────────────
    fun updateTicketName(v: String)   = _formState.update { it.copy(ticketName = v) }
    fun updateEventDate(v: String)    = _formState.update { it.copy(eventDate = v) }
    fun updateEventTime(v: String)    = _formState.update { it.copy(eventTime = v) }
    fun updateLocation(v: String)     = _formState.update { it.copy(location = v) }
    fun updateSection(v: String)      = _formState.update { it.copy(section = v) }
    fun updateRow(v: String)          = _formState.update { it.copy(row = v) }
    fun updateSeat(v: String)         = _formState.update { it.copy(seat = v) }
    fun updateCategory(v: String)     = _formState.update { it.copy(selectedCategory = v) }

    fun updatePendingTicket(index: Int, updated: ExtractedTicketData) {
        _formState.update {
            val list = it.pendingTickets.toMutableList()
            if (index in list.indices) list[index] = updated
            it.copy(pendingTickets = list)
        }
    }

    fun resetForm() { _formState.value = AddTicketUiState() }

    // ── Lógica de PDF ────────────────────────────────────────────────────────
    /** Lanzado desde la UI al recibir un Uri del SAF file picker. */
    fun onPdfSelected(uri: Uri) {
        viewModelScope.launch {
            _formState.update { it.copy(isVerifying = true, selectedUri = uri) }

            val displayName = pdfRepository.getDisplayName(uri)
            _formState.update { it.copy(selectedFileName = displayName) }

            if (!pdfRepository.isValidTicket(uri)) {
                _formState.update { it.copy(isVerifying = false, selectedUri = null, selectedFileName = "") }
                _addTicketResult.emit(AddTicketResult.InvalidFile)
                return@launch
            }

            val dataList = pdfRepository.extractTicketData(uri)
            _formState.update { state ->
                when {
                    dataList.isEmpty() -> state.copy(isVerifying = false, pendingTickets = emptyList())
                    dataList.size == 1 -> {
                        val d = dataList[0]
                        state.copy(
                            isVerifying      = false,
                            pendingTickets   = dataList,
                            ticketName       = d.eventName,
                            selectedCategory = d.category,
                            eventDate        = d.date,
                            eventTime        = d.time,
                            section          = d.section,
                            row              = d.row,
                            seat             = d.seat
                        )
                    }
                    else -> state.copy(
                        isVerifying    = false,
                        pendingTickets = dataList,
                        ticketName     = dataList[0].eventName
                    )
                }
            }
        }
    }

    // ── Guardar boleto(s) ────────────────────────────────────────────────────
    /** Toda la lógica de guardado corre en viewModelScope — no se expone como suspend. */
    fun saveTickets(context: Context) {
        viewModelScope.launch {
            val state = _formState.value
            val uri   = state.selectedUri ?: return@launch

            // Persistir permiso de URI para futuras sesiones
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) { e.printStackTrace() }

            var addedCount   = 0
            var skippedCount = 0

            if (state.pendingTickets.size > 1) {
                state.pendingTickets.forEach { p ->
                    if (insertTicket(p.eventName, uri, p.category, p.date.ifBlank { null },
                            p.time.ifBlank { null }, null,
                            p.section.ifBlank { null }, p.row.ifBlank { null },
                            p.seat.ifBlank { null }, p.pageIndex)
                    ) addedCount++ else skippedCount++
                }
            } else {
                if (insertTicket(
                        state.ticketName, uri, state.selectedCategory,
                        state.eventDate.ifBlank { null }, state.eventTime.ifBlank { null },
                        state.location.ifBlank { null },  state.section.ifBlank { null },
                        state.row.ifBlank { null },        state.seat.ifBlank { null }, 0)
                ) addedCount++ else skippedCount++
            }

            _addTicketResult.emit(AddTicketResult.Processed(addedCount, skippedCount))
            resetForm()
        }
    }

    private suspend fun insertTicket(
        name: String, uri: Uri, category: String,
        eventDate: String?, eventTime: String?, location: String?,
        section: String?,  row: String?,       seat: String?,
        pageIndex: Int
    ): Boolean {
        // Validación atómica directamente en la base de datos para evitar estados obsoletos del StateFlow
        val duplicateCount = ticketDao.countDuplicates(
            name = name,
            eventDate = eventDate,
            section = section,
            row = row,
            seat = seat,
            uriString = uri.toString(),
            pageIndex = pageIndex
        )

        if (duplicateCount > 0) return false

        ticketDao.insertTicket(
            TicketEntity(
                id        = UUID.randomUUID().toString(),
                name      = name,
                uri       = uri.toString(),
                category  = category,
                eventDate = eventDate,
                eventTime = eventTime,
                location  = location,
                section   = section,
                row       = row,
                seat      = seat,
                pageIndex = pageIndex
            )
        )
        return true
    }

    // ── Eliminar boleto ──────────────────────────────────────────────────────
    fun removeTicket(ticket: TicketEntity) {
        viewModelScope.launch { ticketDao.deleteTicket(ticket) }
    }
}
