package com.example.novapass.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val id: String,
    val name: String,
    val uri: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val category: String = "Otro",
    val eventDate: String? = null,
    val eventTime: String? = null,
    val location: String? = null,
    val section: String? = null,
    val row: String? = null,
    val seat: String? = null,
    val thumbnailPath: String? = null
)
