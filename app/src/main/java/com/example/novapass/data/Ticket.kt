package com.example.novapass.data

import android.net.Uri

data class Ticket(
    val id: String,
    val name: String,
    val uri: Uri,
    val dateAdded: Long = System.currentTimeMillis()
)
