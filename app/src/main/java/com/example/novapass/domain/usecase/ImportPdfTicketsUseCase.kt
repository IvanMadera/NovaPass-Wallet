package com.example.novapass.domain.usecase

import com.example.novapass.domain.model.ImportPdfResult
import com.example.novapass.domain.repository.PdfTicketRepository

class ImportPdfTicketsUseCase(
    private val pdfTicketRepository: PdfTicketRepository
) {
    suspend operator fun invoke(uri: String): ImportPdfResult {
        val displayName = pdfTicketRepository.getDisplayName(uri)
        if (!pdfTicketRepository.isValidTicketPdf(uri)) {
            return ImportPdfResult.InvalidFile(displayName)
        }
        return ImportPdfResult.Success(
            displayName = displayName,
            tickets = pdfTicketRepository.extractTicketData(uri)
        )
    }
}
