package com.example.novapass.data.pdf

import android.content.Context
import android.net.Uri
import com.example.novapass.domain.model.ExtractedTicketData
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale

class PdfTicketDataSource(private val context: Context) {

    suspend fun isValidTicketPdf(uri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(Uri.parse(uri))?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    val stripper = PDFTextStripper().apply { startPage = 1; endPage = 2 }
                    stripper.getText(document).lowercase()
                }
            } ?: return@withContext false

            val keywords = listOf(
                "boleto", "ticket", "butaca", "fila",
                "sector", "zona", "acceso", "seat", "row", "section"
            )
            keywords.any { text.contains(it) }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun extractTicketData(uri: String): List<ExtractedTicketData> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ExtractedTicketData>()
        try {
            val parsedUri = Uri.parse(uri)
            val displayName = getDisplayName(uri).uppercase()
            context.contentResolver.openInputStream(parsedUri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    val stripper = PDFTextStripper()

                    for (pageIndex in 1..document.numberOfPages) {
                        stripper.startPage = pageIndex
                        stripper.endPage = pageIndex
                        val text = stripper.getText(document)

                        var eventName = ""
                        var category = "Otro"
                        var section = ""
                        var row = ""
                        var seat = ""

                        val lines = text.split("\n").map { it.trim() }
                        val vsLine = lines.find { it.contains(" VS ", ignoreCase = true) }
                        if (vsLine != null) {
                            eventName = vsLine.substringBefore(" SERIE").trim().uppercase()
                            category = "Deportes"
                        }

                        val date = extractEventDate(text)
                        val time = extractEventTime(text)

                        if (text.contains("SECCION", ignoreCase = true)) {
                            section = Regex("(?i)SECCION\\s+([A-Z0-9 ]+)")
                                .find(text)?.groupValues?.get(1)?.trim() ?: ""
                        }

                        val rowSeat = Regex(
                            "(?i)FILA\\s*\\|?\\s*BUTACA\\s*\\n\\s*([A-Z0-9]+)\\s+([A-Z0-9]+)"
                        ).find(text)
                        if (rowSeat != null) {
                            row = rowSeat.groupValues[1]
                            seat = rowSeat.groupValues[2]
                        }

                        if (eventName.isBlank()) eventName = displayName

                        if (eventName.isNotBlank() || section.isNotBlank() || seat.isNotBlank()) {
                            result.add(
                                ExtractedTicketData(
                                    eventName = eventName,
                                    category = category,
                                    date = date,
                                    time = time,
                                    location = "",
                                    section = section,
                                    row = row,
                                    seat = seat,
                                    pageIndex = pageIndex - 1
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }
        result
    }

    suspend fun getDisplayName(uri: String): String = withContext(Dispatchers.IO) {
        val parsedUri = Uri.parse(uri)
        var name = parsedUri.lastPathSegment?.substringBeforeLast(".").orEmpty()

        try {
            context.contentResolver.query(parsedUri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) {
                    name = cursor.getString(idx).substringBeforeLast(".")
                }
            }
            context.contentResolver.openInputStream(parsedUri)?.use { stream ->
                PDDocument.load(stream).use { doc ->
                    doc.documentInformation?.title
                        ?.takeIf { it.isNotBlank() }
                        ?.let { name = it }
                }
            }
        } catch (e: Exception) {
            // Keep the URI/file-name fallback.
        }
        name
    }

    private fun extractEventDate(text: String): String {
        return Regex(
            """\b(LUNES|MARTES|MIERCOLES|JUEVES|VIERNES|SABADO|DOMINGO),\s+\d{1,2}\s+DE\s+[A-Z]+\s+DE\s+\d{4}\b"""
        ).find(normalizeForParsing(text))?.value.orEmpty()
    }

    private fun extractEventTime(text: String): String {
        val lines = text.lineSequence()
            .map { normalizeForParsing(it).trim() }
            .filter { it.isNotBlank() }
            .toList()

        lines.firstNotNullOfOrNull { line ->
            if (line.contains(" DE ")) parseTimeValue(line) else null
        }?.let { return it }

        lines.forEachIndexed { index, line ->
            if (line == "HORA" || line.startsWith("HORA ")) {
                val sameLineValue = line.removePrefix("HORA").trim()
                parseTimeValue(sameLineValue)?.let { return it }

                lines.drop(index + 1)
                    .take(8)
                    .firstNotNullOfOrNull { candidate -> parseTimeValue(candidate) }
                    ?.let { return it }
            }
        }

        return Regex("""\b([01]?\d|2[0-3])[:.](\d{2})\s*(?:H|HRS?|HORAS)?\b""")
            .find(normalizeForParsing(text))
            ?.let { formatTime(it.groupValues[1], it.groupValues[2], "") }
            .orEmpty()
    }

    private fun normalizeForParsing(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('\u00A0', ' ')
            .replace(Regex("[ \\t]+"), " ")
            .uppercase(Locale.ROOT)
    }

    private fun parseTimeValue(value: String): String? {
        val match = Regex(
            """\b(\d{1,2})(?:(?::|\.|H)\s*(\d{2}))\s*(A\.?\s*M\.?|P\.?\s*M\.?|AM|PM|H|HRS?|HORAS)?\b|\b(\d{1,2})\s*(A\.?\s*M\.?|P\.?\s*M\.?|AM|PM|H|HRS?|HORAS)\b"""
        ).find(value) ?: return null

        val hourValue = match.groupValues.getOrNull(1)
            ?.takeIf { it.isNotBlank() }
            ?: match.groupValues.getOrNull(4).orEmpty()
        val minuteValue = match.groupValues.getOrNull(2).orEmpty()
        val meridiemValue = match.groupValues.getOrNull(3)
            ?.takeIf { it.isNotBlank() }
            ?: match.groupValues.getOrNull(5).orEmpty()

        return formatTime(
            hourValue = hourValue,
            minuteValue = minuteValue,
            meridiemValue = meridiemValue
        )
    }

    private fun formatTime(
        hourValue: String,
        minuteValue: String,
        meridiemValue: String
    ): String? {
        var hour = hourValue.toIntOrNull() ?: return null
        val minute = minuteValue.ifBlank { "00" }.toIntOrNull() ?: return null
        if (minute !in 0..59) return null

        val meridiem = meridiemValue
            .replace(".", "")
            .replace(" ", "")
            .uppercase(Locale.ROOT)

        when {
            meridiem == "PM" && hour in 1..11 -> hour += 12
            meridiem == "AM" && hour == 12 -> hour = 0
            meridiem.isBlank() || meridiem == "H" || meridiem.startsWith("HR") || meridiem == "HORAS" -> {
                if (hour !in 0..23) return null
            }
            hour !in 1..12 -> return null
        }

        return String.format(Locale.ROOT, "%02d:%02d", hour, minute)
    }
}
