package com.example.novapass.data.repository

import com.example.novapass.data.pdf.PdfTicketDataSource
import com.example.novapass.domain.model.ExtractedTicketData
import com.example.novapass.domain.repository.PdfTicketRepository

class PdfTicketRepositoryImpl(
    private val dataSource: PdfTicketDataSource
) : PdfTicketRepository {
    override suspend fun getDisplayName(uri: String): String = dataSource.getDisplayName(uri)

    override suspend fun isValidTicketPdf(uri: String): Boolean = dataSource.isValidTicketPdf(uri)

    override suspend fun extractTicketData(uri: String): List<ExtractedTicketData> {
        return dataSource.extractTicketData(uri)
    }
}
