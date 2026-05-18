package com.example.novapass.domain.usecase

import com.example.novapass.domain.model.TicketDraft
import com.example.novapass.domain.repository.TicketRepository

class SaveTicketsUseCase(private val repository: TicketRepository) {
    suspend operator fun invoke(drafts: List<TicketDraft>) = repository.saveTickets(drafts)
}
