package com.example.novapass.domain.usecase

import com.example.novapass.domain.repository.TicketRepository

import kotlinx.coroutines.flow.map

class ObserveTicketsUseCase(private val repository: TicketRepository) {
    operator fun invoke(isArchived: Boolean = false) = repository.observeTickets().map { list ->
        list.filter { it.isArchived == isArchived }
    }
}
