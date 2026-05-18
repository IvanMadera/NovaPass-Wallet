package com.example.novapass.domain.usecase

import com.example.novapass.domain.model.Ticket
import com.example.novapass.domain.repository.TicketRepository

class ArchiveTicketUseCase(private val repository: TicketRepository) {
    suspend operator fun invoke(ticket: Ticket, archive: Boolean) {
        val updatedTicket = ticket.copy(isArchived = archive)
        repository.updateTicket(updatedTicket)
    }
}
