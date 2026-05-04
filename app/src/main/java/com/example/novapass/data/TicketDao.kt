package com.example.novapass.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets ORDER BY dateAdded DESC")
    fun getAllTickets(): Flow<List<TicketEntity>>

    @Query("""
        SELECT COUNT(*) FROM tickets 
        WHERE (name = :name AND eventDate IS :eventDate AND section IS :section AND row IS :row AND seat IS :seat)
        OR (uri = :uriString AND pageIndex = :pageIndex)
    """)
    suspend fun countDuplicates(
        name: String, 
        eventDate: String?, 
        section: String?, 
        row: String?, 
        seat: String?, 
        uriString: String, 
        pageIndex: Int
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: TicketEntity)

    @Delete
    suspend fun deleteTicket(ticket: TicketEntity)
}
