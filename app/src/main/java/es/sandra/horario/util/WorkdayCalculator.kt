package es.sandra.horario.util

import java.time.LocalDate

object WorkdayCalculator {
    fun getWorkdayIndex(date: LocalDate, anchor: LocalDate = DateUtils.courseStart): Int {
        require(!date.isBefore(anchor)) { "La fecha no puede ser anterior al inicio del patrón." }
        var cursor = anchor
        var index = 0
        while (cursor.isBefore(date)) {
            if (isCountedWorkday(cursor)) index++
            cursor = cursor.plusDays(1)
        }
        return index
    }

    /**
     * The playground duty rota shown by the school advances on every weekday.
     * It does not pause for a holiday or a non-teaching day, so that the next
     * teaching day matches the printed calendar.
     */
    fun isCountedWorkday(date: LocalDate): Boolean = !DateUtils.isWeekend(date)
}
