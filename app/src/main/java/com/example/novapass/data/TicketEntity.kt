package com.example.novapass.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val id: String,
    val name: String,
    val uri: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val eventDate: Long? = null,
    val thumbnailPath: String? = null
)
