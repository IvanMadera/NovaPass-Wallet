package com.example.novapass.domain.model

data class TicketDraft(
    val name: String,
    val uri: String,
    val category: String,
    val eventDate: String? = null,
    val eventTime: String? = null,
    val location: String? = null,
    val section: String? = null,
    val row: String? = null,
    val seat: String? = null,
    val pageIndex: Int = 0
)
