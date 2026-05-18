package com.example.novapass.data.mapper

import com.example.novapass.data.local.TicketEntity
import com.example.novapass.domain.model.Ticket
import com.example.novapass.domain.model.TicketDraft
import java.util.UUID

fun TicketEntity.toDomain(): Ticket = Ticket(
    id = id,
    name = name,
    uri = uri,
    dateAdded = dateAdded,
    category = category,
    eventDate = eventDate,
    eventTime = eventTime,
    location = location,
    section = section,
    row = row,
    seat = seat,
    thumbnailPath = thumbnailPath,
    pageIndex = pageIndex,
    isArchived = isArchived
)

fun Ticket.toEntity(): TicketEntity = TicketEntity(
    id = id,
    name = name,
    uri = uri,
    dateAdded = dateAdded,
    category = category,
    eventDate = eventDate,
    eventTime = eventTime,
    location = location,
    section = section,
    row = row,
    seat = seat,
    thumbnailPath = thumbnailPath,
    pageIndex = pageIndex,
    isArchived = isArchived
)

fun TicketDraft.toEntity(): TicketEntity = TicketEntity(
    id = UUID.randomUUID().toString(),
    name = name,
    uri = uri,
    category = category,
    eventDate = eventDate,
    eventTime = eventTime,
    location = location,
    section = section,
    row = row,
    seat = seat,
    pageIndex = pageIndex
)
