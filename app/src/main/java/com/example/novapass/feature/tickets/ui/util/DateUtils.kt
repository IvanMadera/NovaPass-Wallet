package com.example.novapass.feature.tickets.ui.util

import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun parseEventDate(dateStr: String?): Date? {
    if (dateStr.isNullOrBlank()) return null
    try {
        val normalized = Normalizer.normalize(dateStr, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .uppercase(Locale.ROOT)
            .trim()

        // Patrón DD/MM/AAAA o DD-MM-AAAA
        val dmyMatch = Regex("""^(\d{1,2})[/-](\d{1,2})[/-](\d{4})$""").find(normalized)
        if (dmyMatch != null) {
            val day = dmyMatch.groupValues[1].toInt()
            val month = dmyMatch.groupValues[2].toInt() - 1
            val year = dmyMatch.groupValues[3].toInt()
            val cal = Calendar.getInstance().apply {
                clear()
                set(year, month, day)
            }
            return cal.time
        }

        // Patrón EEEE, DD DE MMMM DE AAAA
        val match = Regex("""(\d{1,2})\s+DE\s+([A-Z]+)\s+DE\s+(\d{4})""").find(normalized)
            ?: return null

        val day = match.groupValues[1].toInt()
        val monthStr = match.groupValues[2]
        val year = match.groupValues[3].toInt()

        val month = when (monthStr) {
            "ENERO" -> 0
            "FEBRERO" -> 1
            "MARZO" -> 2
            "ABRIL" -> 3
            "MAYO" -> 4
            "JUNIO" -> 5
            "JULIO" -> 6
            "AGOSTO" -> 7
            "SEPTIEMBRE" -> 8
            "OCTUBRE" -> 9
            "NOVIEMBRE" -> 10
            "DICIEMBRE" -> 11
            else -> return null
        }

        val cal = Calendar.getInstance().apply {
            clear()
            set(year, month, day)
        }
        return cal.time
    } catch (e: Exception) {
        return null
    }
}

fun formatEventDate(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return "Fecha TBD"
    val parsedDate = parseEventDate(dateStr) ?: return dateStr
    val formatter = SimpleDateFormat("EEEE, dd 'DE' MMMM 'DE' yyyy", Locale.forLanguageTag("es-ES"))
    return formatter.format(parsedDate).uppercase(Locale.forLanguageTag("es-ES"))
}
