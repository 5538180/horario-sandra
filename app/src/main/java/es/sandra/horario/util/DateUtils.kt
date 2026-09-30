package es.sandra.horario.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    val courseStart: LocalDate = LocalDate.of(2026, 9, 7)
    val courseEnd: LocalDate = LocalDate.of(2027, 6, 30)
    const val googleSheetUrl: String =
        "https://docs.google.com/spreadsheets/d/1k-Eong0RNhqxRm491K1YGs6XoaUlgtf73UIUQ7xRv7g/edit"

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun formatDate(date: LocalDate): String = date.format(dateFormatter)

    fun dayName(date: LocalDate): String = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "LUNES"
        DayOfWeek.TUESDAY -> "MARTES"
        DayOfWeek.WEDNESDAY -> "MIÉRCOLES"
        DayOfWeek.THURSDAY -> "JUEVES"
        DayOfWeek.FRIDAY -> "VIERNES"
        DayOfWeek.SATURDAY -> "SÁBADO"
        DayOfWeek.SUNDAY -> "DOMINGO"
    }.uppercase(Locale("es", "ES"))

    fun isWeekend(date: LocalDate): Boolean =
        date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
}
