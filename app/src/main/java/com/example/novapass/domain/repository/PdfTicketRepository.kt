package com.example.novapass.domain.repository

import com.example.novapass.domain.model.ExtractedTicketData

interface PdfTicketRepository {
    suspend fun getDisplayName(uri: String): String
    suspend fun isValidTicketPdf(uri: String): Boolean
    suspend fun extractTicketData(uri: String): List<ExtractedTicketData>
}
