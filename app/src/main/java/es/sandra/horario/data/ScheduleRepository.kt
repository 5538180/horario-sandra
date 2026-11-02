package es.sandra.horario.data

import es.sandra.horario.model.DaySchedule
import es.sandra.horario.model.LibraryResponsible
import es.sandra.horario.model.PatioStatus
import es.sandra.horario.model.PlaygroundDuty
import es.sandra.horario.model.ScheduleEntry
import es.sandra.horario.model.ScheduleType
import es.sandra.horario.model.SchoolCourseStatus
import es.sandra.horario.util.DateUtils
import es.sandra.horario.util.WorkdayCalculator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

object ScheduleRepository {
    fun courseStatus(date: LocalDate): SchoolCourseStatus = when {
        date.isBefore(DateUtils.courseStart) -> SchoolCourseStatus.NOT_STARTED
        date.isAfter(DateUtils.courseEnd) -> SchoolCourseStatus.FINISHED
        else -> SchoolCourseStatus.ACTIVE
    }

    private val dutyPairs = listOf(
        LibraryResponsible.SANDRA to LibraryResponsible.JOSE_CARLOS,
        LibraryResponsible.LUCIA to LibraryResponsible.FRAN,
        LibraryResponsible.CARLOS to LibraryResponsible.LUIS,
        LibraryResponsible.MARIA_CARMEN to LibraryResponsible.MARIANO
    )

    fun dailyDuty(date: LocalDate): PlaygroundDuty? {
        if (
            courseStatus(date) != SchoolCourseStatus.ACTIVE ||
            DateUtils.isWeekend(date) ||
            SchoolCalendarRepository.isNonTeachingDay(date)
        ) return null
        // The rota advances on every weekday, including holidays.
        val index = WorkdayCalculator.getWorkdayIndex(date)
        val pair = dutyPairs[index % dutyPairs.size]
        val responsible = if ((index / dutyPairs.size) % 2 == 0) pair.first else pair.second
        return PlaygroundDuty(pair.first, pair.second, responsible)
    }

    fun patioStatus(date: LocalDate): PatioStatus? {
        val duty = dailyDuty(date) ?: return null
        return if (duty.sandraHasNoPatio) {
            PatioStatus.NoPatio(libraryResponsible = duty.libraryResponsible)
        } else {
            PatioStatus.Patio
        }
    }

    fun scheduleFor(date: LocalDate): DaySchedule {
        val status = courseStatus(date)
        if (status != SchoolCourseStatus.ACTIVE) {
            return DaySchedule(date, status, DateUtils.isWeekend(date), emptyList())
        }
        val schoolDayEvent = SchoolCalendarRepository.eventFor(date)
        if (schoolDayEvent != null) {
            return DaySchedule(
                date = date,
                courseStatus = status,
                isWeekend = DateUtils.isWeekend(date),
                entries = emptyList(),
                schoolDayEvent = schoolDayEvent
            )
        }
        if (DateUtils.isWeekend(date)) {
            return DaySchedule(date, status, isWeekend = true, entries = emptyList())
        }
        val patio = patioStatus(date)
        val duty = requireNotNull(dailyDuty(date))
        val entries = weeklyEntries(date.dayOfWeek).map {
            if (it.type == ScheduleType.PATIO) patioEntry(it.startTime, it.endTime, patio, duty) else it
        }
        return DaySchedule(date, status, isWeekend = false, entries = entries, patioStatus = patio)
    }

    private fun weeklyEntries(day: DayOfWeek): List<ScheduleEntry> = when (day) {
        DayOfWeek.MONDAY -> listOf(
            entry("09:00", "10:00", "Despacho", "Reuniones del equipo", ScheduleType.OFFICE),
            entry(
                "10:00",
                "11:00",
                "Lengua",
                type = ScheduleType.LANGUAGE,
                supportTeacher = "Lucía Moya"
            ),
            entry("11:00", "11:30", "Sociales", type = ScheduleType.SOCIAL_SCIENCE),
            patioSlot(),
            entry("12:00", "13:00", "Sociales", type = ScheduleType.SOCIAL_SCIENCE, supportTeacher = "AT: Nazaret"),
            entry("13:00", "14:00", "Matemáticas", type = ScheduleType.MATH),
            entry(
                "14:00",
                "15:00",
                "Turnos de tarde",
                "Vespertino: María López\nAtención a padres: Luis y Sandra",
                ScheduleType.VESPERTINO
            )
        )
        DayOfWeek.TUESDAY -> listOf(
            entry("09:00", "10:00", "Matemáticas", type = ScheduleType.MATH),
            entry(
                "10:00",
                "11:00",
                "Lengua",
                type = ScheduleType.LANGUAGE,
                supportTeacher = "Luis García"
            ),
            patioSlot(),
            entry("12:00", "13:00", "Naturales", type = ScheduleType.NATURAL_SCIENCE),
            entry("13:00", "14:00", "Despacho", "Reuniones del equipo", ScheduleType.OFFICE),
            eveningSlot("Irene", "M.ª Carmen y Marta")
        )
        DayOfWeek.WEDNESDAY -> listOf(
            entry(
                "09:00",
                "10:00",
                "Lengua",
                type = ScheduleType.LANGUAGE,
                supportTeacher = "María Carmen Noguera Cayuelas"
            ),
            entry("10:00", "10:30", "Religión", type = ScheduleType.RELIGION),
            entry("10:30", "11:00", "Matemáticas", type = ScheduleType.MATH),
            patioSlot(),
            entry("12:00", "13:00", "Despacho", "Reuniones del equipo", ScheduleType.OFFICE),
            eveningSlot("José Carlos", "Lucía y Mariano")
        )
        DayOfWeek.THURSDAY -> listOf(
            entry("09:00", "10:00", "Matemáticas", "Yo apoyo en 3.º C\nMaría Carmen Noguera Cayuelas", ScheduleType.MATH),
            entry("10:00", "10:30", "Religión", type = ScheduleType.RELIGION),
            entry("10:30", "11:00", "Matemáticas", type = ScheduleType.MATH),
            patioSlot(),
            entry("12:00", "13:00", "Despacho", "Reuniones del equipo", ScheduleType.OFFICE, supportTeacher = "AT: Nazaret"),
            entry(
                "13:00",
                "14:00",
                "Lengua",
                type = ScheduleType.LANGUAGE,
                supportTeacher = "María Carmen Noguera Cayuelas"
            ),
            eveningSlot("Rocío", "Fran y Carlos-Natalia")
        )
        DayOfWeek.FRIDAY -> listOf(
            entry("09:00", "10:00", "Matemáticas", type = ScheduleType.MATH),
            entry("10:00", "10:30", "Religión", type = ScheduleType.RELIGION),
            entry("10:30", "11:00", "Naturales", type = ScheduleType.NATURAL_SCIENCE),
            patioSlot(),
            entry("12:00", "13:00", "Despacho", "Reuniones del equipo", ScheduleType.OFFICE, supportTeacher = "AT: Nazaret"),
            entry(
                "13:00",
                "14:00",
                "Lengua",
                type = ScheduleType.LANGUAGE,
                supportTeacher = "María Carmen Noguera Cayuelas\nAT: Nazaret"
            ),
            eveningSlot("Natalia", "Sin turno")
        )
        else -> emptyList()
    }

    private fun eveningSlot(vespertino: String, parents: String): ScheduleEntry =
        entry(
            "14:00",
            "15:00",
            "Turnos de tarde",
            "Vespertino: $vespertino\nAtención a padres: $parents",
            ScheduleType.VESPERTINO
        )

    private fun patioSlot(): ScheduleEntry =
        entry("11:30", "12:00", "PATIO · Primaria", type = ScheduleType.PATIO)

    private fun patioEntry(
        start: LocalTime,
        end: LocalTime,
        patio: PatioStatus?,
        duty: PlaygroundDuty
    ): ScheduleEntry {
        val noPatio = patio is PatioStatus.NoPatio
        return ScheduleEntry(
            startTime = start,
            endTime = end,
            title = if (noPatio) "NO PATIO" else "PATIO · Primaria",
            subtitle = "Sin patio: ${duty.pairName}\nResponsable de biblioteca:",
            type = if (noPatio) ScheduleType.NO_PATIO else ScheduleType.PATIO,
            highlight = duty.libraryResponsible.displayName
        )
    }

    private fun entry(
        start: String,
        end: String,
        title: String,
        subtitle: String? = null,
        type: ScheduleType,
        supportTeacher: String? = null
    ): ScheduleEntry = ScheduleEntry(
        startTime = LocalTime.parse(start),
        endTime = LocalTime.parse(end),
        title = title,
        subtitle = subtitle,
        type = type,
        supportTeacher = supportTeacher
    )
}
