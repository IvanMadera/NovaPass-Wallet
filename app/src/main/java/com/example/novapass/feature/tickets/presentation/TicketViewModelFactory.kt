package com.example.novapass.feature.tickets.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.novapass.AppContainer

class TicketViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TicketViewModel::class.java)) {
            return TicketViewModel(
                observeTicketsUseCase = appContainer.observeTicketsUseCase,
                importPdfTicketsUseCase = appContainer.importPdfTicketsUseCase,
                saveTicketsUseCase = appContainer.saveTicketsUseCase,
                deleteTicketUseCase = appContainer.deleteTicketUseCase,
                archiveTicketUseCase = appContainer.archiveTicketUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
