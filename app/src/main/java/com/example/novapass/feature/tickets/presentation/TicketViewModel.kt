package com.example.novapass.feature.tickets.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.novapass.domain.model.ExtractedTicketData
import com.example.novapass.domain.model.ImportPdfResult
import com.example.novapass.domain.model.Ticket
import com.example.novapass.domain.model.TicketDraft
import com.example.novapass.domain.usecase.ArchiveTicketUseCase
import com.example.novapass.domain.usecase.DeleteTicketUseCase
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.example.novapass.domain.usecase.ImportPdfTicketsUseCase
import com.example.novapass.domain.usecase.ObserveTicketsUseCase
import com.example.novapass.domain.usecase.SaveTicketsUseCase
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

class TicketViewModel(
    observeTicketsUseCase: ObserveTicketsUseCase,
    private val importPdfTicketsUseCase: ImportPdfTicketsUseCase,
    private val saveTicketsUseCase: SaveTicketsUseCase,
    private val deleteTicketUseCase: DeleteTicketUseCase,
    private val archiveTicketUseCase: ArchiveTicketUseCase
) : ViewModel() {

    private val _isShowingArchived = MutableStateFlow(false)
    val isShowingArchived: StateFlow<Boolean> = _isShowingArchived.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val tickets: StateFlow<List<Ticket>> = _isShowingArchived
        .flatMapLatest { isArchived ->
            observeTicketsUseCase(isArchived)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _formState = MutableStateFlow(AddTicketUiState())
    val formState: StateFlow<AddTicketUiState> = _formState.asStateFlow()

    private val _addTicketResult = MutableSharedFlow<AddTicketResult>()
    val addTicketResult: SharedFlow<AddTicketResult> = _addTicketResult.asSharedFlow()

    fun updateTicketName(v: String) = _formState.update { it.copy(ticketName = v) }
    fun updateEventDate(v: String) = _formState.update { it.copy(eventDate = v) }
    fun updateEventTime(v: String) = _formState.update { it.copy(eventTime = v) }
    fun updateLocation(v: String) = _formState.update { it.copy(location = v) }
    fun updateSection(v: String) = _formState.update { it.copy(section = v) }
    fun updateRow(v: String) = _formState.update { it.copy(row = v) }
    fun updateSeat(v: String) = _formState.update { it.copy(seat = v) }
    fun updateCategory(v: String) = _formState.update { it.copy(selectedCategory = v) }

    fun updatePendingTicket(index: Int, updated: ExtractedTicketData) {
        _formState.update {
            val list = it.pendingTickets.toMutableList()
            if (index in list.indices) list[index] = updated
            it.copy(pendingTickets = list)
        }
    }

    fun resetForm() {
        _formState.value = AddTicketUiState()
    }

    fun onPdfSelected(uri: Uri) {
        viewModelScope.launch {
            _formState.update { it.copy(isVerifying = true, selectedUri = uri) }

            try {
                when (val result = importPdfTicketsUseCase(uri.toString())) {
                    is ImportPdfResult.InvalidFile -> {
                        _formState.update {
                            it.copy(
                                isVerifying = false,
                                selectedUri = null,
                                selectedFileName = ""
                            )
                        }
                        _addTicketResult.emit(AddTicketResult.InvalidFile)
                    }

                    is ImportPdfResult.Success -> {
                        _formState.update { state ->
                            val dataList = result.tickets
                            when {
                                dataList.isEmpty() -> state.copy(
                                    isVerifying = false,
                                    selectedFileName = result.displayName,
                                    pendingTickets = emptyList()
                                )

                                dataList.size == 1 -> {
                                    val d = dataList[0]
                                    state.copy(
                                        isVerifying = false,
                                        selectedFileName = result.displayName,
                                        pendingTickets = dataList,
                                        ticketName = d.eventName,
                                        selectedCategory = d.category,
                                        eventDate = d.date,
                                        eventTime = d.time,
                                        section = d.section,
                                        row = d.row,
                                        seat = d.seat
                                    )
                                }

                                else -> state.copy(
                                    isVerifying = false,
                                    selectedFileName = result.displayName,
                                    pendingTickets = dataList,
                                    ticketName = dataList[0].eventName
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        isVerifying = false,
                        selectedUri = null,
                        selectedFileName = "",
                        pendingTickets = emptyList()
                    )
                }
                _addTicketResult.emit(AddTicketResult.Error("No se pudo leer el archivo seleccionado"))
            }
        }
    }

    fun saveTickets(context: Context) {
        viewModelScope.launch {
            val state = _formState.value
            val uri = state.selectedUri ?: return@launch

            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                _addTicketResult.emit(AddTicketResult.Error("No se pudo conservar el permiso del archivo"))
                return@launch
            }

            try {
                val drafts = state.toDrafts(uri.toString())
                val result = saveTicketsUseCase(drafts)

                _addTicketResult.emit(
                    AddTicketResult.Processed(
                        addedCount = result.addedCount,
                        skippedCount = result.skippedCount
                    )
                )
                resetForm()
            } catch (e: Exception) {
                _addTicketResult.emit(AddTicketResult.Error("No se pudo guardar el boleto"))
            }
        }
    }

    fun removeTicket(ticket: Ticket) {
        viewModelScope.launch {
            deleteTicketUseCase(ticket)
        }
    }

    fun toggleArchiveView() {
        _isShowingArchived.value = !_isShowingArchived.value
    }

    fun archiveTicket(ticket: Ticket, archive: Boolean = true) {
        viewModelScope.launch {
            archiveTicketUseCase(ticket, archive)
        }
    }

    private fun AddTicketUiState.toDrafts(uri: String): List<TicketDraft> {
        return if (pendingTickets.size > 1) {
            pendingTickets.map { p ->
                TicketDraft(
                    name = p.eventName,
                    uri = uri,
                    category = p.category,
                    eventDate = p.date.ifBlank { null },
                    eventTime = p.time.ifBlank { null },
                    location = null,
                    section = p.section.ifBlank { null },
                    row = p.row.ifBlank { null },
                    seat = p.seat.ifBlank { null },
                    pageIndex = p.pageIndex
                )
            }
        } else {
            listOf(
                TicketDraft(
                    name = ticketName,
                    uri = uri,
                    category = selectedCategory,
                    eventDate = eventDate.ifBlank { null },
                    eventTime = eventTime.ifBlank { null },
                    location = location.ifBlank { null },
                    section = section.ifBlank { null },
                    row = row.ifBlank { null },
                    seat = seat.ifBlank { null },
                    pageIndex = 0
                )
            )
        }
    }
}
