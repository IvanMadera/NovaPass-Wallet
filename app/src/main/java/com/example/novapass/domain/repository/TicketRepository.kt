package com.example.novapass.domain.repository

import com.example.novapass.domain.model.SaveTicketsResult
import com.example.novapass.domain.model.Ticket
import com.example.novapass.domain.model.TicketDraft
import kotlinx.coroutines.flow.Flow

interface TicketRepository {
    fun observeTickets(): Flow<List<Ticket>>
    suspend fun saveTickets(drafts: List<TicketDraft>): SaveTicketsResult
    suspend fun deleteTicket(ticket: Ticket)
    suspend fun updateTicket(ticket: Ticket)
}
