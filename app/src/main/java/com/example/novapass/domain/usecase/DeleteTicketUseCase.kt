package com.example.novapass.domain.usecase

import com.example.novapass.domain.model.Ticket
import com.example.novapass.domain.repository.TicketRepository

class DeleteTicketUseCase(private val repository: TicketRepository) {
    suspend operator fun invoke(ticket: Ticket) = repository.deleteTicket(ticket)
}
