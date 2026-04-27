package com.example.novapass.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// ─────────────────────────────────────────────────────────────────────────
// Migraciones Room — añade aquí objetos MIGRATION_X_Y al cambiar el schema.
// Ejemplo: si agregas una columna en v4 →
//
//   val MIGRATION_3_4 = object : Migration(3, 4) {
//       override fun migrate(db: SupportSQLiteDatabase) {
//           db.execSQL("ALTER TABLE tickets ADD COLUMN myNewColumn TEXT")
//       }
//   }
//
// y luego en getDatabase → .addMigrations(MIGRATION_3_4)
// ─────────────────────────────────────────────────────────────────────────

@Database(entities = [TicketEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ticketDao(): TicketDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ticket_database"
                )
                // NOTA: No usar fallbackToDestructiveMigration() en producción.
                // Registra aquí todas las migraciones pendientes con addMigrations(…).
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
