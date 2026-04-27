package com.example.novapass.ui.state

import android.net.Uri
import com.example.novapass.models.ExtractedTicketData

// ─────────────────────────────────────────────────────────────────────────
// AddTicketUiState — estado completo del formulario de agregar boleto
// Vive en el ViewModel → sobrevive rotaciones y recomposiciones
// ─────────────────────────────────────────────────────────────────────────
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

// ─────────────────────────────────────────────────────────────────────────
// AddTicketResult — evento de un solo disparo para toasts / navegación
// ─────────────────────────────────────────────────────────────────────────
sealed class AddTicketResult {
    /** Uno o más boletos procesados; [skippedCount] > 0 si había duplicados. */
    data class Processed(val addedCount: Int, val skippedCount: Int) : AddTicketResult()
    /** El PDF no contiene palabras clave de boleto válido. */
    object InvalidFile : AddTicketResult()
}
