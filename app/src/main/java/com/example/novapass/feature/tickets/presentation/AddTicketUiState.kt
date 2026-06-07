package com.example.novapass.feature.tickets.presentation

import android.net.Uri
import com.example.novapass.domain.model.ExtractedTicketData

// Estado completo del formulario. Vive en el ViewModel para sobrevivir
// rotaciones y recomposiciones.
data class AddTicketUiState(
    val ticketName: String = "",
    val eventDate: String = "",
    val eventTime: String = "",
    val location: String = "",
    val section: String = "",
    val row: String = "",
    val seat: String = "",
    val selectedCategory: String = "Otro",
    val selectedUri: Uri? = null,
    val selectedFileName: String = "",
    val isVerifying: Boolean = false,
    val pendingTickets: List<ExtractedTicketData> = emptyList()
)

// Evento de un solo disparo para toasts y navegacion.
sealed class AddTicketResult {
    data class Processed(val addedCount: Int, val skippedCount: Int) : AddTicketResult()
    object InvalidFile : AddTicketResult()
    data class Error(val message: String) : AddTicketResult()
}
