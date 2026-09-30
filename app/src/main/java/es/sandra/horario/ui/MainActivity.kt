package es.sandra.horario.ui

import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.sandra.horario.BuildConfig
import es.sandra.horario.data.SchoolCalendarRepository
import es.sandra.horario.data.ScheduleRepository
import es.sandra.horario.data.TaskRepository
import es.sandra.horario.model.DaySchedule
import es.sandra.horario.model.ScheduleEntry
import es.sandra.horario.model.SchoolDayEvent
import es.sandra.horario.model.TaskItem
import es.sandra.horario.model.TaskPriority
import es.sandra.horario.ui.theme.HorarioTheme
import es.sandra.horario.util.CalendarIcsExporter
import es.sandra.horario.util.DateUtils
import es.sandra.horario.util.DisplayDatePolicy
import es.sandra.horario.widget.requestWidgetRefresh
import java.time.LocalDate

private enum class DisplayMode { DAY, WEEK }

private val releaseChanges = listOf(
    "Cada clase con apoyo muestra de forma independiente quién acompaña a Sandra.",
    "Lucía Moya aparece los lunes y martes de 10:00 a 11:00 en Lengua.",
    "María Carmen Noguera Cayuelas aparece en los demás tramos de Lengua con apoyo.",
    "La indicación de apoyo se muestra tanto en la aplicación como en el widget.",
    "La rotación de patio y biblioteca continúa ajustada al calendario del centro."
)

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_OPEN_TASKS = "open_tasks"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HorarioTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val releasePreferences = remember {
        context.getSharedPreferences("release_notes", android.content.Context.MODE_PRIVATE)
    }
    var selectedDate by remember { mutableStateOf(DisplayDatePolicy.preparationDate()) }
    var displayMode by remember { mutableStateOf(DisplayMode.DAY) }
    var showUpdatedAlert by remember {
        mutableStateOf(releasePreferences.getString("last_seen_version", null) != BuildConfig.VERSION_NAME)
    }
    val schedule = ScheduleRepository.scheduleFor(selectedDate)
    val dismissUpdatedAlert = {
        releasePreferences.edit().putString("last_seen_version", BuildConfig.VERSION_NAME).apply()
        showUpdatedAlert = false
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (showUpdatedAlert) {
            AlertDialog(
                onDismissRequest = dismissUpdatedAlert,
                title = { Text("Versión ${BuildConfig.VERSION_NAME} actualizada") },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Cambios incluidos:")
                        releaseChanges.forEach { change ->
                            Text("• $change")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = dismissUpdatedAlert) {
                        Text("Entendido")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("HORARIO SANDRA", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("${DateUtils.dayName(selectedDate)} · ${DateUtils.formatDate(selectedDate)}")
            Text(
                schedule.schoolDayEvent?.type?.label ?: schedule.courseStatus.label,
                fontWeight = FontWeight.SemiBold
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { displayMode = DisplayMode.DAY },
                    enabled = displayMode != DisplayMode.DAY
                ) { Text("DÍA") }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { displayMode = DisplayMode.WEEK },
                    enabled = displayMode != DisplayMode.WEEK
                ) { Text("SEMANA") }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    modifier = Modifier.width(64.dp),
                    onClick = {
                        selectedDate = selectedDate.minusDays(if (displayMode == DisplayMode.WEEK) 7 else 1)
                    }
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = if (displayMode == DisplayMode.WEEK) "Semana anterior" else "Día anterior")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { selectedDate = DisplayDatePolicy.preparationDate() }
                ) { Text(if (displayMode == DisplayMode.WEEK) "ESTA SEMANA" else "HOY") }
                OutlinedButton(
                    modifier = Modifier.width(64.dp),
                    onClick = {
                        selectedDate = selectedDate.plusDays(if (displayMode == DisplayMode.WEEK) 7 else 1)
                    }
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = if (displayMode == DisplayMode.WEEK) "Semana siguiente" else "Día siguiente")
                }
            }

            TasksSection()

            if (displayMode == DisplayMode.DAY) {
                ScheduleList(schedule)
            } else {
                WeekScheduleList(selectedDate)
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DateUtils.googleSheetUrl)))
                }
            ) {
                Text("ABRIR CALENDARIO EN GOOGLE SHEETS")
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val uri = CalendarIcsExporter.createWeekIcs(context, selectedDate)
                    val openCalendarIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "text/calendar")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(openCalendarIntent)
                    } catch (_: ActivityNotFoundException) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/calendar"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Añadir semana a Google Calendar"))
                    }
                }
            ) {
                Text("AÑADIR SEMANA A GOOGLE CALENDAR")
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(SchoolCalendarRepository.officialCalendarUrl))
                    )
                }
            ) {
                Text("ABRIR CALENDARIO ESCOLAR OFICIAL")
            }

            InfoSection()
        }
    }
}

@Composable
private fun TasksSection() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val tasksFlow = remember(appContext) { TaskRepository.observe(appContext) }
    val tasks by tasksFlow.collectAsState(initial = TaskRepository.load(appContext))
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskPriority by remember { mutableStateOf(TaskPriority.NORMAL) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("TAREAS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = newTaskTitle,
            onValueChange = { newTaskTitle = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Nueva tarea") },
            placeholder = { Text("Escribe la tarea", maxLines = 1) }
        )

        PrioritySelector(
            selected = newTaskPriority,
            onSelected = { newTaskPriority = it }
        )

        Button(
            onClick = {
                TaskRepository.add(appContext, newTaskTitle, newTaskPriority)
                newTaskTitle = ""
                requestWidgetRefresh(appContext)
            },
            enabled = newTaskTitle.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("AÑADIR TAREA", modifier = Modifier.padding(start = 8.dp))
        }

        if (tasks.isEmpty()) {
            Text(
                "No hay tareas todavía. Las que añadas aquí aparecerán también en el widget.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            TaskPriority.entries.forEach { priority ->
                val priorityTasks = tasks.filter { it.priority == priority }
                if (priorityTasks.isNotEmpty()) {
                    Text(
                        "${priority.label.uppercase()} · ${priorityTasks.count { !it.completed }} pendientes",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238)
                    )
                    priorityTasks.sortedBy { it.id }.forEach { task ->
                        TaskEditorRow(
                            task = task,
                            onCompletedChange = { completed ->
                                TaskRepository.setCompleted(appContext, task.id, completed)
                                requestWidgetRefresh(appContext)
                            },
                            onEdit = { editingTask = task },
                            onDelete = {
                                TaskRepository.delete(appContext, task.id)
                                requestWidgetRefresh(appContext)
                            }
                        )
                    }
                }
            }
        }
    }

    editingTask?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { editingTask = null },
            onSave = { updatedTask ->
                TaskRepository.update(appContext, updatedTask)
                editingTask = null
                requestWidgetRefresh(appContext)
            }
        )
    }
}

@Composable
private fun PrioritySelector(selected: TaskPriority, onSelected: (TaskPriority) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TaskPriority.entries.forEach { priority ->
            val shape = RoundedCornerShape(7.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(Color(android.graphics.Color.parseColor(priority.hexColor)), shape)
                    .border(
                        width = if (selected == priority) 2.dp else 1.dp,
                        color = if (selected == priority) Color(0xFF30475E) else Color(0xFF9AA3AD),
                        shape = shape
                    )
                    .clickable { onSelected(priority) },
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    priority.label,
                    maxLines = 1,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )
            }
        }
    }
}

@Composable
private fun TaskEditorRow(
    task: TaskItem,
    onCompletedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(android.graphics.Color.parseColor(task.priority.hexColor)),
                RoundedCornerShape(7.dp)
            )
            .padding(start = 4.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Checkbox(checked = task.completed, onCheckedChange = onCompletedChange)
        Text(
            text = task.title,
            modifier = Modifier.weight(1f),
            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
            color = if (task.completed) Color(0xFF667085) else Color(0xFF263238)
        )
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Editar tarea")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar tarea")
        }
    }
}

@Composable
private fun EditTaskDialog(task: TaskItem, onDismiss: () -> Unit, onSave: (TaskItem) -> Unit) {
    var title by remember(task.id) { mutableStateOf(task.title) }
    var priority by remember(task.id) { mutableStateOf(task.priority) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar tarea") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Tarea") }
                )
                PrioritySelector(selected = priority, onSelected = { priority = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(task.copy(title = title.trim(), priority = priority)) },
                enabled = title.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ScheduleList(schedule: DaySchedule) {
    when {
        schedule.courseStatus != es.sandra.horario.model.SchoolCourseStatus.ACTIVE -> {
            Notice(schedule.courseStatus.label)
        }
        schedule.schoolDayEvent != null -> {
            SchoolDayNotice(schedule.schoolDayEvent)
        }
        schedule.isWeekend -> {
            Notice("FIN DE SEMANA\nPróximo día lectivo: lunes")
        }
        else -> {
            schedule.entries.forEach { ScheduleEntryCard(it) }
        }
    }
}

@Composable
private fun WeekScheduleList(dateInWeek: LocalDate) {
    val monday = CalendarIcsExporter.mondayOf(dateInWeek)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Semana del ${DateUtils.formatDate(monday)} al ${DateUtils.formatDate(monday.plusDays(4))}",
            fontWeight = FontWeight.Bold
        )
        (0L..4L).forEach { offset ->
            val date = monday.plusDays(offset)
            val schedule = ScheduleRepository.scheduleFor(date)
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("${DateUtils.dayName(date)} · ${DateUtils.formatDate(date)}", fontWeight = FontWeight.Bold)
                if (schedule.schoolDayEvent != null) {
                    SchoolDayNotice(schedule.schoolDayEvent)
                } else {
                    schedule.entries.forEach { ScheduleEntryCard(it) }
                }
            }
        }
    }
}

@Composable
private fun SchoolDayNotice(event: SchoolDayEvent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(android.graphics.Color.parseColor(event.type.hexColor)),
                RoundedCornerShape(8.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(event.type.label, fontWeight = FontWeight.Bold)
        Text(event.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(event.detail, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Notice(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF))) {
        Text(text, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ScheduleEntryCard(entry: ScheduleEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${entry.timeRange} ${entry.title} ${entry.subtitle.orEmpty()} " +
                    entry.supportTeacher?.let { "Apoyo conmigo: $it" }.orEmpty()
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(android.graphics.Color.parseColor(entry.type.hexColor)))
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(entry.timeRange, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.85f))
            Column(Modifier.weight(1.5f)) {
                Text(entry.title, fontWeight = FontWeight.SemiBold)
                entry.subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                entry.supportTeacher?.let {
                    Text("Apoyo conmigo: $it", style = MaterialTheme.typography.bodyMedium)
                }
                entry.highlight?.let { HighlightBadge(it) }
            }
        }
    }
}

@Composable
private fun HighlightBadge(text: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFFFFF2A8), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, color = Color(0xFF3A2F00))
    }
}

@Composable
private fun InfoSection() {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text("INFORMACIÓN", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("El horario y los turnos de patio se calculan automáticamente según la fecha del dispositivo. El patrón comienza el 7 de septiembre de 2026 y está configurado hasta el 30 de junio de 2027.")
        Spacer(Modifier.height(8.dp))
        Text("El calendario original puede abrirse en Google Sheets desde esta aplicación.")
        Spacer(Modifier.height(8.dp))
        Text("Los sábados y domingos la vista HOY salta al lunes siguiente para preparar la semana.")
        Spacer(Modifier.height(8.dp))
        Text("Las tareas se guardan en este móvil y se actualizan automáticamente en el widget.")
        Spacer(Modifier.height(8.dp))
        Text("Festivos y vacaciones: calendario oficial 2026-2027 de la Región de Murcia, aplicado al municipio de Murcia y Sangonera la Verde.")
        Spacer(Modifier.height(8.dp))
        Text("Versión ${BuildConfig.VERSION_NAME}")
    }
}
