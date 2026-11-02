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
        assertNoPatio("2026-09-11", LibraryResponsible.JOSE_CARLOS)
        assertPatio("2026-09-14")
        assertNull(ScheduleRepository.patioStatus(date("2026-09-15")))
        assertNoPatio("2026-09-17", LibraryResponsible.SANDRA)
        assertPatio("2026-09-18")
        assertNoPatio("2026-09-23", LibraryResponsible.JOSE_CARLOS)
        assertNoPatio("2026-09-29", LibraryResponsible.SANDRA)
        assertPatio("2026-09-30")
        assertNoPatio("2026-10-05", LibraryResponsible.JOSE_CARLOS)
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
        assertEntry("2026-09-10", "09:00", "Matemáticas")
        assertEntry("2026-09-11", "10:30", "Naturales")
        val fridayEvening = ScheduleRepository.scheduleFor(date("2026-09-11")).entries
            .single { it.startTime == LocalTime.parse("14:00") }
        assertEquals("Vespertino: Natalia\nAtención a padres: Sin turno", fridayEvening.subtitle)
        val thursdaySupport = ScheduleRepository.scheduleFor(date("2026-09-10")).entries
            .first { it.startTime == LocalTime.parse("09:00") }
        assertEquals(LocalTime.parse("10:00"), thursdaySupport.endTime)
        assertEquals("Yo apoyo en 3.º C\nMaría Carmen Noguera Cayuelas", thursdaySupport.subtitle)
        assertNull(thursdaySupport.supportTeacher)
        val parents = ScheduleRepository.scheduleFor(date("2026-09-07")).entries
            .first { it.startTime == LocalTime.parse("14:00") }
        assertEquals("Turnos de tarde", parents.title)
        assertEquals("Vespertino: María López\nAtención a padres: Luis y Sandra", parents.subtitle)
    }

    @Test
    fun noPatioShowsSandraCompanionAndHighlightedResponsible() {
        val noPatioSandra = ScheduleRepository.scheduleFor(date("2026-09-07")).entries
            .first { it.startTime == LocalTime.parse("11:30") }
        assertEquals("NO PATIO", noPatioSandra.title)
        assertEquals("Sin patio: Sandra y José Carlos\nResponsable de biblioteca:", noPatioSandra.subtitle)
        assertEquals("Sandra", noPatioSandra.highlight)
    }

    @Test
    fun supportTeachersAreShownInTheCorrectLessons() {
        assertSupport("2026-09-07", "10:00", "11:00", "Lucía Moya")
        assertSupport("2026-09-08", "10:00", "11:00", "Luis García")
        assertSupport("2026-09-09", "09:00", "10:00", "María Carmen Noguera Cayuelas")
        assertSupport("2026-09-10", "13:00", "14:00", "María Carmen Noguera Cayuelas")
        assertSupport("2026-09-07", "12:00", "13:00", "AT: Nazaret")
        assertSupport("2026-09-10", "12:00", "13:00", "AT: Nazaret")
        assertSupport("2026-09-11", "12:00", "13:00", "AT: Nazaret")
        assertSupport("2026-09-11", "13:00", "14:00", "María Carmen Noguera Cayuelas\nAT: Nazaret")
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
        assertTrue(ics.contains("Sin patio: Sandra y José Carlos"))
        assertTrue(ics.contains("Apoyo conmigo: María Carmen Noguera Cayuelas"))
        assertTrue(ics.contains("Apoyo conmigo: Lucía Moya"))
        assertTrue(ics.contains("Apoyo conmigo: Luis García"))
        assertTrue(ics.contains("Apoyo conmigo: AT: Nazaret"))
        assertTrue(ics.contains("Yo apoyo en 3.º C"))
        assertTrue(!ics.contains("SUMMARY:Horario Sandra · Docencia"))
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


    @Test
    fun dailyDutyMatchesTheConfirmedNovemberRotaAndAlternation() {
        val expected = listOf(
            Triple("2026-11-03", "Lucía y Fran", "Lucía"),
            Triple("2026-11-04", "Carlos y Luis", "Carlos"),
            Triple("2026-11-05", "M.ª Carmen y Mariano", "M.ª Carmen"),
            Triple("2026-11-06", "Sandra y José Carlos", "José Carlos"),
            Triple("2026-11-09", "Lucía y Fran", "Fran"),
            Triple("2026-11-10", "Carlos y Luis", "Luis"),
            Triple("2026-11-11", "M.ª Carmen y Mariano", "Mariano"),
            Triple("2026-11-12", "Sandra y José Carlos", "Sandra")
        )
        expected.forEach { (day, pair, responsible) ->
            val duty = requireNotNull(ScheduleRepository.dailyDuty(date(day)))
            assertEquals(pair, duty.pairName)
            assertEquals(responsible, duty.libraryResponsible.displayName)
            val recess = ScheduleRepository.scheduleFor(date(day)).entries
                .single { it.startTime == LocalTime.parse("11:30") }
            assertEquals("Sin patio: $pair\nResponsable de biblioteca:", recess.subtitle)
            assertEquals(responsible, recess.highlight)
            assertEquals(if (duty.sandraHasNoPatio) "NO PATIO" else "PATIO · Primaria", recess.title)
        }
    }

    @Test
    fun dailyDutyIsHiddenOnWeekendsHolidaysAndOutsideTheCourse() {
        listOf("2026-09-06", "2026-11-07", "2026-11-08", "2026-10-12",
            "2026-11-27", "2026-12-28", "2027-07-01").forEach {
            assertNull(ScheduleRepository.dailyDuty(date(it)))
        }
        // Holidays advance the rota even though no duty is displayed on them.
        assertEquals("Lucía y Fran", ScheduleRepository.dailyDuty(date("2026-09-08"))?.pairName)
        assertEquals("Carlos y Luis", ScheduleRepository.dailyDuty(date("2026-10-13"))?.pairName)
    }

    @Test
    fun calendarIncludesOtherPairsAndTheirLibraryResponsible() {
        val events = CalendarIcsExporter.buildIcs(date("2026-11-02")).split("BEGIN:VEVENT")
        val tuesday = events.single { it.contains("DTSTART;TZID=Europe/Madrid:20261103T113000") }
        assertTrue(tuesday.contains("Sin patio: Lucía y Fran"))
        assertTrue(tuesday.contains("Responsable de biblioteca:"))
        assertTrue(tuesday.contains("Lucía"))
        val friday = events.single { it.contains("DTSTART;TZID=Europe/Madrid:20261106T113000") }
        assertTrue(friday.contains("Sin patio: Sandra y José Carlos"))
        assertTrue(friday.contains("José Carlos"))
        assertTrue(events.any { it.contains("Apoyo conmigo: María Carmen Noguera Cayuelas") })
    }


    @Test
    fun everyWeekdayShowsTheConfirmedEveningAndParentAttentionTeams() {
        val expected = listOf(
            Triple("2026-11-02", "María López", "Luis y Sandra"),
            Triple("2026-11-03", "Irene", "M.ª Carmen y Marta"),
            Triple("2026-11-04", "José Carlos", "Lucía y Mariano"),
            Triple("2026-11-05", "Rocío", "Fran y Carlos-Natalia"),
            Triple("2026-11-06", "Natalia", "Sin turno")
        )
        expected.forEach { (day, evening, parents) ->
            val shifts = ScheduleRepository.scheduleFor(date(day)).entries
                .single { it.startTime == LocalTime.parse("14:00") }
            assertEquals(LocalTime.parse("15:00"), shifts.endTime)
            assertEquals("Turnos de tarde", shifts.title)
            assertEquals("Vespertino: $evening\nAtención a padres: $parents", shifts.subtitle)
        }
    }

    @Test
    fun calendarExportsAfternoonTeamsButNotOnHolidays() {
        val events = CalendarIcsExporter.buildIcs(date("2026-11-02")).split("BEGIN:VEVENT")
        val wednesday = events.single { it.contains("DTSTART;TZID=Europe/Madrid:20261104T140000") }
        assertTrue(wednesday.contains("Vespertino: José Carlos"))
        assertTrue(wednesday.contains("Atención a padres: Lucía y Mariano"))
        val thursday = events.single { it.contains("DTSTART;TZID=Europe/Madrid:20261105T140000") }
        assertTrue(thursday.contains("Atención a padres: Fran y Carlos-Natalia"))
        val holidayWeek = CalendarIcsExporter.buildIcs(date("2026-11-23"))
        assertTrue(!holidayWeek.contains("DTSTART;TZID=Europe/Madrid:20261127T140000"))
        assertTrue(ScheduleRepository.scheduleFor(date("2026-11-07")).entries.isEmpty())
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
