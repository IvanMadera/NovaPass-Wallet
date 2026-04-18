package com.example.novapass

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.novapass.data.AppDatabase
import com.example.novapass.data.TicketEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TicketViewModel(application: Application) : AndroidViewModel(application) {
    private val ticketDao = AppDatabase.getDatabase(application).ticketDao()

    val tickets: StateFlow<List<TicketEntity>> = ticketDao.getAllTickets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    suspend fun addTicket(
        name: String,
        uri: Uri,
        category: String = "Otro",
        eventDate: String? = null,
        eventTime: String? = null,
        location: String? = null,
        section: String? = null,
        row: String? = null,
        seat: String? = null,
        pageIndex: Int = 0
    ): Boolean {
        // Validación de duplicados: Fecha, Sección, Fila, Asiento
        val isDuplicate = tickets.value.any { existing ->
            existing.eventDate == eventDate &&
            existing.section == section &&
            existing.row == row &&
            existing.seat == seat
        }

        if (isDuplicate) return false

        val newTicket = TicketEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            uri = uri.toString(),
            category = category,
            eventDate = eventDate,
            eventTime = eventTime,
            location = location,
            section = section,
            row = row,
            seat = seat,
            pageIndex = pageIndex
        )
        ticketDao.insertTicket(newTicket)
        return true
    }

    fun removeTicket(ticket: TicketEntity) {
        viewModelScope.launch {
            ticketDao.deleteTicket(ticket)
        }
    }
}
