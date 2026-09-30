package es.sandra.horario

import es.sandra.horario.data.ScheduleRepository
import es.sandra.horario.data.SchoolCalendarRepository
import es.sandra.horario.model.LibraryResponsible
import es.sandra.horario.model.PatioStatus
import es.sandra.horario.model.SchoolCourseStatus
import es.sandra.horario.model.SchoolDayType
import es.sandra.horario.util.CalendarIcsExporter
import es.sandra.horario.util.DisplayDatePolicy
import es.sandra.horario.util.WorkdayCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ScheduleRepositoryTest {
    @Test
    fun courseStatusBoundariesAreCorrect() {
        assertEquals(SchoolCourseStatus.NOT_STARTED, ScheduleRepository.courseStatus(date("2026-09-06")))
        assertEquals(SchoolCourseStatus.ACTIVE, ScheduleRepository.courseStatus(date("2026-09-07")))
        assertEquals(SchoolCourseStatus.ACTIVE, ScheduleRepository.courseStatus(date("2027-06-30")))
        assertEquals(SchoolCourseStatus.FINISHED, ScheduleRepository.courseStatus(date("2027-07-01")))
    }

    @Test
    fun workdayIndexFollowsEveryWeekdayInThePatioCalendar() {
        assertEquals(0, WorkdayCalculator.getWorkdayIndex(date("2026-09-07")))
        assertEquals(4, WorkdayCalculator.getWorkdayIndex(date("2026-09-11")))
        assertEquals(10, WorkdayCalculator.getWorkdayIndex(date("2026-09-21")))
        assertEquals(17, WorkdayCalculator.getWorkdayIndex(date("2026-09-30")))
    }

    @Test
    fun patioPatternMatchesRequiredDates() {
        assertNoPatio("2026-09-07", LibraryResponsible.SANDRA)
        assertPatio("2026-09-08")
        assertPatio("2026-09-09")
        assertPatio("2026-09-10")
        assertNoPatio("2026-09-11", LibraryResponsible.SANDRA)
        assertPatio("2026-09-14")
        assertNull(ScheduleRepository.patioStatus(date("2026-09-15")))
        assertNoPatio("2026-09-17", LibraryResponsible.SANDRA)
        assertPatio("2026-09-18")
        assertNoPatio("2026-09-23", LibraryResponsible.SANDRA)
        assertNoPatio("2026-09-29", LibraryResponsible.SANDRA)
        assertPatio("2026-09-30")
        assertNoPatio("2026-10-05", LibraryResponsible.SANDRA)
        assertNoPatio("2026-10-09", LibraryResponsible.SANDRA)
    }

    @Test
    fun weekendsDoNotHavePatioAndMondayContinuesPattern() {
        assertTrue(ScheduleRepository.scheduleFor(date("2026-09-19")).isWeekend)
        assertTrue(ScheduleRepository.scheduleFor(date("2026-09-20")).isWeekend)
        assertPatio("2026-09-21")
    }

    @Test
    fun weeklySubjectsMatchPrompt() {
        assertEntry("2026-09-07", "09:00", "Despacho")
        assertEntry("2026-09-07", "11:00", "Sociales")
        assertEntry("2026-09-07", "13:00", "Matemáticas")
        assertEntry("2026-09-08", "09:00", "Matemáticas")
        assertEntry("2026-09-09", "10:30", "Matemáticas")
        assertEntry("2026-09-10", "09:00", "Complementaria Tutoría")
        assertEntry("2026-09-11", "10:30", "Naturales")
        val vespertino = ScheduleRepository.scheduleFor(date("2026-09-11")).entries
            .first { it.startTime == LocalTime.parse("14:00") }
        assertEquals("Docencia", vespertino.title)
        assertEquals("Vespertino: Sandra - Natalia", vespertino.subtitle)
        val parents = ScheduleRepository.scheduleFor(date("2026-09-07")).entries
            .first { it.startTime == LocalTime.parse("14:00") }
        assertEquals("Atención a padres", parents.title)
        assertEquals("Sandra - Luis\nVespertino: María López", parents.subtitle)
    }

    @Test
    fun noPatioShowsSandraCompanionAndHighlightedResponsible() {
        val noPatioSandra = ScheduleRepository.scheduleFor(date("2026-09-07")).entries
            .first { it.startTime == LocalTime.parse("11:30") }
        assertEquals("NO PATIO", noPatioSandra.title)
        assertEquals("Sandra - Mª Carmen\nResponsable de biblioteca:", noPatioSandra.subtitle)
        assertEquals("Sandra", noPatioSandra.highlight)
    }

    @Test
    fun supportTeachersAreShownInTheCorrectLessons() {
        assertSupport("2026-09-07", "10:00", "11:00", "Lucía Moya")
        assertSupport("2026-09-08", "10:00", "11:00", "Lucía Moya")
        assertSupport("2026-09-09", "09:00", "10:00", "María Carmen Noguera Cayuelas")
        assertSupport("2026-09-10", "13:00", "14:00", "María Carmen Noguera Cayuelas")
        assertSupport("2026-09-11", "13:00", "14:00", "María Carmen Noguera Cayuelas")
    }

    @Test
    fun preparationDateShowsNextMondayFromWeekend() {
        assertEquals(date("2026-09-18"), DisplayDatePolicy.preparationDate(date("2026-09-18")))
        assertEquals(date("2026-09-21"), DisplayDatePolicy.preparationDate(date("2026-09-19")))
        assertEquals(date("2026-09-21"), DisplayDatePolicy.preparationDate(date("2026-09-20")))
    }

    @Test
    fun weekIcsContainsSchoolEvents() {
        val ics = CalendarIcsExporter.buildIcs(date("2026-09-07"))
        assertTrue(ics.contains("BEGIN:VCALENDAR"))
        assertTrue(ics.contains("SUMMARY:Horario Sandra · Matemáticas"))
        assertTrue(ics.contains("DTSTART;TZID=Europe/Madrid:20260907T090000"))
        assertTrue(ics.contains("Apoyo conmigo: Lucía Moya"))
        assertTrue(ics.contains("SUMMARY:Horario Sandra · Docencia"))
    }

    @Test
    fun officialMurciaSchoolCalendarMarksEveryKindOfNonTeachingDay() {
        assertEquals(SchoolDayType.LOCAL_HOLIDAY, event("2026-09-15").type)
        assertEquals(SchoolDayType.NATIONAL_HOLIDAY, event("2026-10-12").type)
        assertEquals(SchoolDayType.SCHOOL_NON_TEACHING, event("2026-11-27").type)
        assertEquals(SchoolDayType.SCHOOL_VACATION, event("2026-12-24").type)
        assertEquals(SchoolDayType.NATIONAL_HOLIDAY, event("2026-12-25").type)
        assertEquals(SchoolDayType.NATIONAL_HOLIDAY, event("2027-01-01").type)
        assertEquals(SchoolDayType.REGIONAL_HOLIDAY, event("2027-03-19").type)
        assertEquals(SchoolDayType.REGIONAL_HOLIDAY, event("2027-03-25").type)
        assertEquals(SchoolDayType.NATIONAL_HOLIDAY, event("2027-03-26").type)
        assertEquals(SchoolDayType.LOCAL_HOLIDAY, event("2027-03-30").type)
        assertEquals(SchoolDayType.SCHOOL_NON_TEACHING, event("2027-03-31").type)
        assertTrue(ScheduleRepository.scheduleFor(date("2027-03-31")).entries.isEmpty())
    }

    @Test
    fun weekIcsReplacesClassesWithAllDayHolidayEvent() {
        val ics = CalendarIcsExporter.buildIcs(date("2026-09-14"))
        assertTrue(ics.contains("DTSTART;VALUE=DATE:20260915"))
        assertTrue(ics.contains("SUMMARY:Horario Sandra · Romería de Nuestra Señora de la Fuensanta"))
        assertTrue(!ics.contains("DTSTART;TZID=Europe/Madrid:20260915"))
    }

    private fun assertPatio(value: String) {
        assertEquals(PatioStatus.Patio, ScheduleRepository.patioStatus(date(value)))
    }

    private fun assertNoPatio(value: String, responsible: LibraryResponsible) {
        val patio = ScheduleRepository.patioStatus(date(value))
        assertTrue(patio is PatioStatus.NoPatio)
        assertEquals(responsible, (patio as PatioStatus.NoPatio).libraryResponsible)
    }

    private fun assertEntry(day: String, time: String, title: String) {
        val entry = ScheduleRepository.scheduleFor(date(day)).entries.first { it.startTime == LocalTime.parse(time) }
        assertEquals(title, entry.title)
    }

    private fun assertSupport(day: String, start: String, end: String, teacher: String) {
        val entry = ScheduleRepository.scheduleFor(date(day)).entries
            .first { it.startTime == LocalTime.parse(start) }
        assertEquals(LocalTime.parse(end), entry.endTime)
        assertEquals(teacher, entry.supportTeacher)
    }

    private fun event(value: String) = SchoolCalendarRepository.eventFor(date(value))
        ?: error("No se encontró el evento escolar esperado para $value")

    private fun date(value: String): LocalDate = LocalDate.parse(value)
}
