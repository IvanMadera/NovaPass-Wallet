package com.example.novapass

import android.content.Context
import com.example.novapass.data.local.AppDatabase
import com.example.novapass.data.pdf.PdfTicketDataSource
import com.example.novapass.data.repository.PdfTicketRepositoryImpl
import com.example.novapass.data.repository.TicketRepositoryImpl
import com.example.novapass.domain.usecase.ArchiveTicketUseCase
import com.example.novapass.domain.usecase.DeleteTicketUseCase
import com.example.novapass.domain.usecase.ImportPdfTicketsUseCase
import com.example.novapass.domain.usecase.ObserveTicketsUseCase
import com.example.novapass.domain.usecase.SaveTicketsUseCase

class AppContainer(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val ticketRepository = TicketRepositoryImpl(database.ticketDao())
    private val pdfTicketDataSource = PdfTicketDataSource(context.applicationContext)
    private val pdfTicketRepository = PdfTicketRepositoryImpl(pdfTicketDataSource)

    val observeTicketsUseCase = ObserveTicketsUseCase(ticketRepository)
    val importPdfTicketsUseCase = ImportPdfTicketsUseCase(pdfTicketRepository)
    val saveTicketsUseCase = SaveTicketsUseCase(ticketRepository)
    val deleteTicketUseCase = DeleteTicketUseCase(ticketRepository)
    val archiveTicketUseCase = ArchiveTicketUseCase(ticketRepository)
}
