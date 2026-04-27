package com.example.novapass.data.repository

import android.content.Context
import android.net.Uri
import com.example.novapass.models.ExtractedTicketData
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────
// PdfRepository — toda la lógica de I/O y parsing de PDF en un solo lugar.
// Antes estaba dispersa como funciones top-level en TicketListScreen.kt.
// ─────────────────────────────────────────────────────────────────────────
class PdfRepository(private val context: Context) {

    /** Devuelve true si el PDF contiene palabras clave que indican un boleto. */
    suspend fun isValidTicket(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext false
            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper().apply { startPage = 1; endPage = 2 }
            val text = stripper.getText(document).lowercase()
            document.close()
            inputStream.close()
            val keywords = listOf(
                "boleto", "ticket", "butaca", "fila",
                "sector", "zona", "acceso", "seat", "row", "section"
            )
            keywords.any { text.contains(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Extrae datos de cada página del PDF y devuelve una lista de boletos. */
    suspend fun extractTicketData(uri: Uri): List<ExtractedTicketData> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ExtractedTicketData>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext result
            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()

            for (pageIndex in 1..document.numberOfPages) {
                stripper.startPage = pageIndex
                stripper.endPage   = pageIndex
                val text = stripper.getText(document)

                var eventName = ""
                var category  = "Otro"
                var date      = ""
                var section   = ""
                var row       = ""
                var seat      = ""

                // Detección de partido deportivo
                val lines  = text.split("\n").map { it.trim() }
                val vsLine = lines.find { it.contains(" VS ", ignoreCase = true) }
                if (vsLine != null) {
                    eventName = vsLine.substringBefore(" SERIE").trim().uppercase()
                    category  = "Deportes"
                }

                // Fecha en español
                val datePattern = Regex(
                    "(?i)(lunes|martes|miércoles|jueves|viernes|sábado|domingo)," +
                    "\\s+\\d+\\s+DE\\s+[A-Z]+\\s+DE\\s+\\d{4}"
                )
                datePattern.find(text)?.let { date = it.value }

                // Sección
                if (text.contains("SECCION", ignoreCase = true)) {
                    section = Regex("(?i)SECCION\\s+([A-Z0-9 ]+)")
                        .find(text)?.groupValues?.get(1)?.trim() ?: ""
                }

                // Fila / Butaca
                val rowSeat = Regex(
                    "(?i)FILA\\s*\\|?\\s*BUTACA\\s*\\n\\s*([A-Z0-9]+)\\s+([A-Z0-9]+)"
                ).find(text)
                if (rowSeat != null) {
                    row  = rowSeat.groupValues[1]
                    seat = rowSeat.groupValues[2]
                }

                if (eventName.isBlank()) eventName = getDisplayName(uri).uppercase()

                if (eventName.isNotBlank() || section.isNotBlank() || seat.isNotBlank()) {
                    result.add(
                        ExtractedTicketData(
                            eventName = eventName,
                            category  = category,
                            date      = date,
                            time      = "",
                            location  = "",
                            section   = section,
                            row       = row,
                            seat      = seat,
                            pageIndex = pageIndex - 1
                        )
                    )
                }
            }
            document.close()
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    /** Obtiene el nombre para mostrar del PDF (título del documento o nombre de archivo). */
    suspend fun getDisplayName(uri: Uri): String = withContext(Dispatchers.IO) {
        var name = ""
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) {
                    name = cursor.getString(idx).substringBeforeLast(".")
                }
            }
            // Intentar obtener el título embebido en el PDF
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val doc = PDDocument.load(stream)
                doc.documentInformation?.title
                    ?.takeIf { it.isNotBlank() }
                    ?.let { name = it }
                doc.close()
            }
        } catch (e: Exception) { /* retorna el nombre de archivo */ }
        name
    }
}
