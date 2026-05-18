package com.example.novapass.domain.model

data class Ticket(
    val id: String,
    val name: String,
    val uri: String,
    val dateAdded: Long,
    val category: String = "Otro",
    val eventDate: String? = null,
    val eventTime: String? = null,
    val location: String? = null,
    val section: String? = null,
    val row: String? = null,
    val seat: String? = null,
    val thumbnailPath: String? = null,
    val pageIndex: Int = 0
)
