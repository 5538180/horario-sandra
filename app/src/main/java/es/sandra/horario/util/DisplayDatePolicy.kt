package es.sandra.horario.util

import java.time.DayOfWeek
import java.time.LocalDate

object DisplayDatePolicy {
    fun preparationDate(today: LocalDate = LocalDate.now()): LocalDate = when (today.dayOfWeek) {
        DayOfWeek.SATURDAY -> today.plusDays(2)
        DayOfWeek.SUNDAY -> today.plusDays(1)
        else -> today
    }
}
