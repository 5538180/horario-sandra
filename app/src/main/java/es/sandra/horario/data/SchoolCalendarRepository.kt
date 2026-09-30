package es.sandra.horario.data

import es.sandra.horario.model.SchoolDayEvent
import es.sandra.horario.model.SchoolDayType
import java.time.LocalDate

object SchoolCalendarRepository {
    const val officialCalendarUrl =
        "https://servicios.educarm.es/templates/portal/ficheros/websDinamicas/45/2748%20Period%20lecti%2026-27%20BORM%2011-6-26.pdf"

    private val christmasStart = date(2026, 12, 24)
    private val christmasEnd = date(2027, 1, 6)
    private val easterStart = date(2027, 3, 22)
    private val easterEnd = date(2027, 3, 29)

    private val fixedEvents = listOf(
        event(2026, 9, 15, "Romería de Nuestra Señora de la Fuensanta", SchoolDayType.LOCAL_HOLIDAY,
            "Festivo del municipio de Murcia, aplicable a Sangonera la Verde."),
        event(2026, 10, 12, "Fiesta Nacional de España", SchoolDayType.NATIONAL_HOLIDAY,
            "Día festivo en toda España."),
        event(2026, 11, 27, "Patrón de Infantil y Primaria", SchoolDayType.SCHOOL_NON_TEACHING,
            "Día no lectivo para Infantil, Primaria y Educación Especial."),
        event(2026, 12, 7, "Día de la Constitución", SchoolDayType.NATIONAL_HOLIDAY,
            "El descanso del domingo 6 de diciembre se traslada al lunes 7."),
        event(2026, 12, 8, "Inmaculada Concepción", SchoolDayType.NATIONAL_HOLIDAY,
            "Día festivo en toda España."),
        event(2026, 12, 23, "Día no lectivo", SchoolDayType.SCHOOL_NON_TEACHING,
            "Establecido por la Dirección General de Centros Educativos e Infraestructuras."),
        event(2026, 12, 25, "Navidad", SchoolDayType.NATIONAL_HOLIDAY,
            "Día festivo en toda España."),
        event(2027, 1, 1, "Año Nuevo", SchoolDayType.NATIONAL_HOLIDAY, "Día festivo en toda España."),
        event(2027, 1, 6, "Epifanía del Señor", SchoolDayType.NATIONAL_HOLIDAY, "Día festivo en toda España."),
        event(2027, 3, 19, "San José", SchoolDayType.REGIONAL_HOLIDAY,
            "Festivo seleccionado en el calendario de la Región de Murcia."),
        event(2027, 3, 25, "Jueves Santo", SchoolDayType.REGIONAL_HOLIDAY,
            "Día festivo en la Región de Murcia."),
        event(2027, 3, 26, "Viernes Santo", SchoolDayType.NATIONAL_HOLIDAY,
            "Día festivo en toda España."),
        event(2027, 3, 30, "Bando de la Huerta", SchoolDayType.LOCAL_HOLIDAY,
            "Festivo local trasladado del municipio de Murcia, aplicable a Sangonera la Verde."),
        event(2027, 3, 31, "Día no lectivo municipal", SchoolDayType.SCHOOL_NON_TEACHING,
            "Aprobado por el Consejo Escolar Municipal de Murcia."),
        event(2027, 4, 1, "Día no lectivo municipal", SchoolDayType.SCHOOL_NON_TEACHING,
            "Aprobado por el Consejo Escolar Municipal de Murcia."),
        event(2027, 4, 2, "Día no lectivo municipal", SchoolDayType.SCHOOL_NON_TEACHING,
            "Aprobado por el Consejo Escolar Municipal de Murcia."),
        event(2027, 5, 1, "Fiesta del Trabajo", SchoolDayType.NATIONAL_HOLIDAY,
            "Día festivo en toda España."),
        event(2027, 6, 9, "Día de la Región de Murcia", SchoolDayType.REGIONAL_HOLIDAY,
            "Festivo autonómico de la Región de Murcia.")
    ).associateBy { it.date }

    fun eventFor(date: LocalDate): SchoolDayEvent? = fixedEvents[date] ?: when {
        date in christmasStart..christmasEnd -> SchoolDayEvent(
            date,
            "Vacaciones de Navidad",
            SchoolDayType.SCHOOL_VACATION,
            "Del 24 de diciembre de 2026 al 6 de enero de 2027, ambos inclusive."
        )
        date in easterStart..easterEnd -> SchoolDayEvent(
            date,
            "Vacaciones de Semana Santa",
            SchoolDayType.SCHOOL_VACATION,
            "Del 22 al 29 de marzo de 2027, ambos inclusive."
        )
        else -> null
    }

    fun isNonTeachingDay(date: LocalDate): Boolean = eventFor(date) != null

    private fun event(
        year: Int,
        month: Int,
        day: Int,
        title: String,
        type: SchoolDayType,
        detail: String
    ) = SchoolDayEvent(date(year, month, day), title, type, detail)

    private fun date(year: Int, month: Int, day: Int): LocalDate = LocalDate.of(year, month, day)
}
