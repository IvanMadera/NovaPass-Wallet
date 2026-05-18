package com.example.novapass.data.repository

import com.example.novapass.data.local.TicketDao
import com.example.novapass.data.mapper.toDomain
import com.example.novapass.data.mapper.toEntity
import com.example.novapass.domain.model.SaveTicketsResult
import com.example.novapass.domain.model.Ticket
import com.example.novapass.domain.model.TicketDraft
import com.example.novapass.domain.repository.TicketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TicketRepositoryImpl(
    private val ticketDao: TicketDao
) : TicketRepository {

    override fun observeTickets(): Flow<List<Ticket>> {
        return ticketDao.getAllTickets().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun saveTickets(drafts: List<TicketDraft>): SaveTicketsResult {
        var addedCount = 0
        var skippedCount = 0

        drafts.forEach { draft ->
            val duplicateCount = ticketDao.countDuplicates(
                name = draft.name,
                eventDate = draft.eventDate,
                section = draft.section,
                row = draft.row,
                seat = draft.seat,
                uriString = draft.uri,
                pageIndex = draft.pageIndex
            )

            if (duplicateCount > 0) {
                skippedCount++
            } else {
                ticketDao.insertTicket(draft.toEntity())
                addedCount++
            }
        }

        return SaveTicketsResult(addedCount, skippedCount)
    }

    override suspend fun deleteTicket(ticket: Ticket) {
        ticketDao.deleteTicket(ticket.toEntity())
    }
}
