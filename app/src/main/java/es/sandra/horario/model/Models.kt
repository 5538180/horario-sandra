package es.sandra.horario.model

import java.time.LocalDate
import java.time.LocalTime

data class ScheduleEntry(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val title: String,
    val subtitle: String? = null,
    val type: ScheduleType,
    val highlight: String? = null,
    val supportTeacher: String? = null
) {
    val timeRange: String
        get() = "${startTime.formatHour()}–${endTime.formatHour()}"
}

enum class ScheduleType(val hexColor: String) {
    MATH("#F7AAAA"),
    LANGUAGE("#8FC3EE"),
    RELIGION("#C5A6ED"),
    NATURAL_SCIENCE("#B7E5A5"),
    SOCIAL_SCIENCE("#FDB36D"),
    OFFICE("#FFFFFF"),
    COMPLEMENTARY_TUTORING("#FFFFFF"),
    PATIO("#ECEFF4"),
    NO_PATIO("#E2F0D9"),
    LIBRARY_RESPONSIBLE("#FFF2A8"),
    VESPERTINO("#FCE4FB"),
    PARENT_ATTENTION("#FCE4FB")
}

sealed interface PatioStatus {
    data object Patio : PatioStatus
    data class NoPatio(
        val companion: String = "Mª Carmen",
        val libraryResponsible: LibraryResponsible
    ) : PatioStatus
}

enum class LibraryResponsible {
    SANDRA,
    MARIA_CARMEN
    ;

    val displayName: String
        get() = when (this) {
            SANDRA -> "Sandra"
            MARIA_CARMEN -> "Mª Carmen"
        }
}

enum class SchoolCourseStatus(val label: String) {
    NOT_STARTED("CURSO TODAVÍA NO INICIADO"),
    ACTIVE("CURSO ACTIVO"),
    FINISHED("CURSO FINALIZADO")
}

data class DaySchedule(
    val date: LocalDate,
    val courseStatus: SchoolCourseStatus,
    val isWeekend: Boolean,
    val entries: List<ScheduleEntry>,
    val patioStatus: PatioStatus? = null,
    val schoolDayEvent: SchoolDayEvent? = null
)

data class SchoolDayEvent(
    val date: LocalDate,
    val title: String,
    val type: SchoolDayType,
    val detail: String
)

enum class SchoolDayType(val label: String, val hexColor: String) {
    NATIONAL_HOLIDAY("FESTIVO NACIONAL", "#F4C6C6"),
    REGIONAL_HOLIDAY("FESTIVO AUTONÓMICO · REGIÓN DE MURCIA", "#FFE38E"),
    LOCAL_HOLIDAY("FESTIVO LOCAL · SANGONERA LA VERDE", "#B8E0BC"),
    SCHOOL_NON_TEACHING("DÍA NO LECTIVO ESCOLAR", "#B8D7F2"),
    SCHOOL_VACATION("VACACIONES ESCOLARES", "#D8C7EF")
}

data class TaskItem(
    val id: Long,
    val title: String,
    val priority: TaskPriority,
    val completed: Boolean = false
)

enum class TaskPriority(val label: String, val hexColor: String, val order: Int) {
    URGENT("Urgente", "#F6B1B1", 0),
    IMPORTANT("Importante", "#FFE28A", 1),
    NORMAL("Normal", "#BFE3C0", 2)
}

private fun LocalTime.formatHour(): String = "%02d:%02d".format(hour, minute)
