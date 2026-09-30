package es.sandra.horario.data

import es.sandra.horario.model.DaySchedule
import es.sandra.horario.model.LibraryResponsible
import es.sandra.horario.model.PatioStatus
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

    fun patioStatus(date: LocalDate): PatioStatus? {
        if (
            courseStatus(date) != SchoolCourseStatus.ACTIVE ||
            DateUtils.isWeekend(date) ||
            SchoolCalendarRepository.isNonTeachingDay(date)
        ) return null
        val workdayIndex = WorkdayCalculator.getWorkdayIndex(date)
        return if (workdayIndex % 4 == 0) {
            // The duty calendar keeps its four-pair rotation on every weekday.
            // Sandra is the library lead whenever her Sandra/Mª Carmen pair appears.
            PatioStatus.NoPatio(libraryResponsible = LibraryResponsible.SANDRA)
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
        val entries = weeklyEntries(date.dayOfWeek).map {
            if (it.type == ScheduleType.PATIO) patioEntry(it.startTime, it.endTime, patio) else it
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
            entry("12:00", "13:00", "Sociales", type = ScheduleType.SOCIAL_SCIENCE),
            entry("13:00", "14:00", "Matemáticas", type = ScheduleType.MATH),
            entry(
                "14:00",
                "15:00",
                "Atención a padres",
                "Sandra - Luis\nVespertino: María López",
                ScheduleType.PARENT_ATTENTION
            )
        )
        DayOfWeek.TUESDAY -> listOf(
            entry("09:00", "10:00", "Matemáticas", type = ScheduleType.MATH),
            entry(
                "10:00",
                "11:00",
                "Lengua",
                type = ScheduleType.LANGUAGE,
                supportTeacher = "Lucía Moya"
            ),
            patioSlot(),
            entry("12:00", "13:00", "Naturales", type = ScheduleType.NATURAL_SCIENCE),
            entry("13:00", "14:00", "Despacho", "Reuniones del equipo", ScheduleType.OFFICE)
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
            entry("12:00", "13:00", "Despacho", "Reuniones del equipo", ScheduleType.OFFICE)
        )
        DayOfWeek.THURSDAY -> listOf(
            entry("09:00", "10:00", "Complementaria Tutoría", type = ScheduleType.COMPLEMENTARY_TUTORING),
            entry("10:00", "10:30", "Religión", type = ScheduleType.RELIGION),
            entry("10:30", "11:00", "Matemáticas", type = ScheduleType.MATH),
            patioSlot(),
            entry("12:00", "13:00", "Despacho", "Reuniones del equipo", ScheduleType.OFFICE),
            entry(
                "13:00",
                "14:00",
                "Lengua",
                type = ScheduleType.LANGUAGE,
                supportTeacher = "María Carmen Noguera Cayuelas"
            )
        )
        DayOfWeek.FRIDAY -> listOf(
            entry("09:00", "10:00", "Matemáticas", type = ScheduleType.MATH),
            entry("10:00", "10:30", "Religión", type = ScheduleType.RELIGION),
            entry("10:30", "11:00", "Naturales", type = ScheduleType.NATURAL_SCIENCE),
            patioSlot(),
            entry("12:00", "13:00", "Despacho", "Reuniones del equipo", ScheduleType.OFFICE),
            entry(
                "13:00",
                "14:00",
                "Lengua",
                type = ScheduleType.LANGUAGE,
                supportTeacher = "María Carmen Noguera Cayuelas"
            ),
            entry("14:00", "15:00", "Docencia", "Vespertino: Sandra - Natalia", ScheduleType.VESPERTINO)
        )
        else -> emptyList()
    }

    private fun patioSlot(): ScheduleEntry =
        entry("11:30", "12:00", "PATIO · Primaria", type = ScheduleType.PATIO)

    private fun patioEntry(start: LocalTime, end: LocalTime, patio: PatioStatus?): ScheduleEntry =
        when (patio) {
            is PatioStatus.NoPatio -> {
                val responsible = patio.libraryResponsible.displayName
                val subtitle = "Sandra - ${patio.companion}\nResponsable de biblioteca:"
                ScheduleEntry(start, end, "NO PATIO", subtitle, ScheduleType.NO_PATIO, highlight = responsible)
            }
            else -> ScheduleEntry(start, end, "PATIO · Primaria", type = ScheduleType.PATIO)
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
