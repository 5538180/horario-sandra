package es.sandra.horario.util

import android.content.Context
import androidx.core.content.FileProvider
import es.sandra.horario.data.ScheduleRepository
import es.sandra.horario.model.ScheduleEntry
import es.sandra.horario.model.SchoolDayEvent
import es.sandra.horario.model.SchoolCourseStatus
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object CalendarIcsExporter {
    private const val timeZone = "Europe/Madrid"
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", Locale.ROOT)
    private val dateFormatter = DateTimeFormatter.BASIC_ISO_DATE

    fun createWeekIcs(context: Context, dateInWeek: LocalDate): android.net.Uri {
        val monday = mondayOf(dateInWeek)
        val file = File(File(context.cacheDir, "calendar").also { it.mkdirs() }, "horario-sandra-semana.ics")
        file.writeText(buildIcs(monday), Charsets.UTF_8)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun buildIcs(monday: LocalDate): String {
        val events = (0L..4L).flatMap { offset ->
            val date = monday.plusDays(offset)
            val schedule = ScheduleRepository.scheduleFor(date)
            when {
                schedule.schoolDayEvent != null -> listOf(eventFor(schedule.schoolDayEvent))
                schedule.courseStatus == SchoolCourseStatus.ACTIVE && !schedule.isWeekend -> {
                    schedule.entries.map { eventFor(date, it) }
                }
                else -> emptyList()
            }
        }
        return buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//Horario Sandra//ES")
            appendLine("CALSCALE:GREGORIAN")
            appendLine("METHOD:PUBLISH")
            events.forEach { append(it) }
            appendLine("END:VCALENDAR")
        }
    }

    fun mondayOf(date: LocalDate): LocalDate =
        date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())

    private fun eventFor(date: LocalDate, entry: ScheduleEntry): String {
        val start = LocalDateTime.of(date, entry.startTime).format(dateTimeFormatter)
        val end = LocalDateTime.of(date, entry.endTime).format(dateTimeFormatter)
        val title = escape("Horario Sandra · ${entry.title}")
        val description = escape(
            listOfNotNull(
                entry.timeRange,
                entry.subtitle,
                entry.supportTeacher?.let { "Apoyo conmigo: $it" },
                entry.highlight
            ).joinToString("\\n")
        )
        val uid = "horario-sandra-${date}-${entry.startTime}-${entry.title.hashCode()}@local"
        return buildString {
            appendLine("BEGIN:VEVENT")
            appendLine("UID:$uid")
            appendLine("DTSTAMP:${LocalDateTime.now().format(dateTimeFormatter)}Z")
            appendLine("DTSTART;TZID=$timeZone:$start")
            appendLine("DTEND;TZID=$timeZone:$end")
            appendLine("SUMMARY:$title")
            appendLine("DESCRIPTION:$description")
            appendLine("END:VEVENT")
        }
    }

    private fun eventFor(event: SchoolDayEvent): String {
        val start = event.date.format(dateFormatter)
        val end = event.date.plusDays(1).format(dateFormatter)
        val uid = "horario-sandra-no-lectivo-${event.date}@local"
        return buildString {
            appendLine("BEGIN:VEVENT")
            appendLine("UID:$uid")
            appendLine("DTSTAMP:${LocalDateTime.now().format(dateTimeFormatter)}Z")
            appendLine("DTSTART;VALUE=DATE:$start")
            appendLine("DTEND;VALUE=DATE:$end")
            appendLine("SUMMARY:${escape("Horario Sandra · ${event.title}")}")
            appendLine("DESCRIPTION:${escape("${event.type.label}\n${event.detail}")}")
            appendLine("END:VEVENT")
        }
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\n", "\\n")
}
