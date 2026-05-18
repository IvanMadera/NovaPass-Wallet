package com.example.novapass.domain.usecase

import com.example.novapass.domain.repository.TicketRepository

class ObserveTicketsUseCase(private val repository: TicketRepository) {
    operator fun invoke() = repository.observeTickets()
}
