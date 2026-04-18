package com.example.novapass.models

data class ExtractedTicketData(
    val eventName: String = "",
    val category: String = "Otro",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val section: String = "",
    val row: String = "",
    val seat: String = "",
    val pageIndex: Int = 0
)
