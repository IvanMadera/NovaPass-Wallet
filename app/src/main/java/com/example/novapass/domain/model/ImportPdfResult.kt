package com.example.novapass.domain.model

sealed class ImportPdfResult {
    data class Success(
        val displayName: String,
        val tickets: List<ExtractedTicketData>
    ) : ImportPdfResult()

    data class InvalidFile(val displayName: String) : ImportPdfResult()
}
