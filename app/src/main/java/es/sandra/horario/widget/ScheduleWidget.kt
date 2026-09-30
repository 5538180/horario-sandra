package es.sandra.horario.widget

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import es.sandra.horario.data.ScheduleRepository
import es.sandra.horario.data.TaskRepository
import es.sandra.horario.model.DaySchedule
import es.sandra.horario.model.ScheduleEntry
import es.sandra.horario.model.ScheduleType
import es.sandra.horario.model.SchoolCourseStatus
import es.sandra.horario.model.SchoolDayEvent
import es.sandra.horario.model.TaskItem
import es.sandra.horario.model.TaskPriority
import es.sandra.horario.ui.MainActivity
import es.sandra.horario.util.CalendarIcsExporter
import es.sandra.horario.util.DateUtils
import es.sandra.horario.util.DisplayDatePolicy
import java.time.LocalDate

class ScheduleWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: android.content.Context, id: androidx.glance.GlanceId) {
        provideContent { WidgetContent() }
    }
}

@Composable
private fun WidgetContent() {
    val context = LocalContext.current
    val preferences = currentState<Preferences>()
    val offset = preferences[WidgetState.offsetDaysKey] ?: 0
    val weekMode = (preferences[WidgetState.viewModeKey] ?: 0) == 1
    val tasksExpanded = preferences[WidgetState.tasksExpandedKey] ?: false
    val taskRevision = preferences[WidgetState.taskRevisionKey] ?: 0L
    val date = DisplayDatePolicy.preparationDate(LocalDate.now()).plusDays(offset.toLong())
    val tasks = TaskRepository.load(context)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header(date, weekMode)
        Spacer(GlanceModifier.height(5.dp))
        ModeSelector(weekMode)
        Spacer(GlanceModifier.height(5.dp))
        PeriodNavigation(weekMode)
        Spacer(GlanceModifier.height(6.dp))
        TasksPanel(tasks, tasksExpanded, taskRevision)
        Spacer(GlanceModifier.height(6.dp))
        if (weekMode) {
            WeekBody(date)
        } else {
            DayBody(ScheduleRepository.scheduleFor(date), if (tasksExpanded) 3 else 8)
        }
    }
}

@Composable
private fun Header(date: LocalDate, weekMode: Boolean) {
    val openSheet = actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DateUtils.googleSheetUrl)))
    val monday = CalendarIcsExporter.mondayOf(date)
    val period = if (weekMode) {
        "SEMANA ${DateUtils.formatDate(monday)} - ${DateUtils.formatDate(monday.plusDays(4))}"
    } else {
        "${DateUtils.dayName(date)} · ${DateUtils.formatDate(date)}"
    }
    Column(
        modifier = GlanceModifier.fillMaxWidth().clickable(openSheet),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "HORARIO · SANDRA",
            style = TextStyle(
                color = ColorProvider(Color(0xFF30475E)),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            modifier = GlanceModifier.fillMaxWidth()
        )
        Text(
            text = period,
            style = TextStyle(
                color = ColorProvider(Color(0xFF1F2933)),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            ),
            modifier = GlanceModifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ModeSelector(weekMode: Boolean) {
    Row(
        modifier = GlanceModifier
            .width(180.dp)
            .background(Color(0xFFE4E8ED))
            .cornerRadius(8.dp)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeSegment("DÍA", !weekMode, actionRunCallback<SetDayViewAction>())
        ModeSegment("SEMANA", weekMode, actionRunCallback<SetWeekViewAction>())
    }
}

@Composable
private fun ModeSegment(label: String, selected: Boolean, action: Action) {
    Box(
        modifier = GlanceModifier
            .width(88.dp)
            .height(30.dp)
            .background(if (selected) Color(0xFF30475E) else Color(0xFFE4E8ED))
            .cornerRadius(6.dp)
            .clickable(action),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(if (selected) Color.White else Color(0xFF30475E)),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun PeriodNavigation(weekMode: Boolean) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NavButton("‹", actionRunCallback<PreviousPeriodAction>())
        Spacer(GlanceModifier.width(6.dp))
        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .height(32.dp)
                .background(Color(0xFFF0F2F5))
                .cornerRadius(7.dp)
                .clickable(actionRunCallback<TodayAction>()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (weekMode) "ESTA SEMANA" else "HOY",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF30475E)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }
        Spacer(GlanceModifier.width(6.dp))
        NavButton("›", actionRunCallback<NextPeriodAction>())
    }
}

@Composable
private fun NavButton(label: String, action: Action) {
    Box(
        modifier = GlanceModifier
            .size(width = 48.dp, height = 32.dp)
            .background(Color(0xFF30475E))
            .cornerRadius(7.dp)
            .clickable(action),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun TasksPanel(tasks: List<TaskItem>, expanded: Boolean, taskRevision: Long) {
    val context = LocalContext.current
    val pending = tasks.count { !it.completed }
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(Color.White)
            .cornerRadius(7.dp)
            .padding(horizontal = 7.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionRunCallback<ToggleTasksPanelAction>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TAREAS · $pending pendientes",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF30475E)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            PriorityCount(tasks, TaskPriority.URGENT)
            PriorityCount(tasks, TaskPriority.IMPORTANT)
            PriorityCount(tasks, TaskPriority.NORMAL)
            Text(
                text = if (expanded) "▲" else "▼",
                style = TextStyle(color = ColorProvider(Color(0xFF30475E)), fontSize = 10.sp),
                modifier = GlanceModifier.padding(start = 5.dp)
            )
        }

        if (expanded) {
            Spacer(GlanceModifier.height(4.dp))
            val orderedTasks = tasks.sortedWith(
                compareBy<TaskItem> { it.priority.order }.thenBy { it.id }
            )
            if (orderedTasks.isEmpty()) {
                Text(
                    text = "No hay tareas. Ábrelas en la aplicación para añadir la primera.",
                    style = TextStyle(color = ColorProvider(Color(0xFF4B5563)), fontSize = 10.sp)
                )
            } else {
                LazyColumn(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    items(orderedTasks, itemId = { task -> task.id xor taskRevision }) { task ->
                        TaskRow(task)
                    }
                }
            }
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(Color(0xFFECEFF4))
                    .cornerRadius(6.dp)
                    .clickable(
                        actionStartActivity(
                            Intent(context, MainActivity::class.java)
                                .putExtra(MainActivity.EXTRA_OPEN_TASKS, true)
                        )
                    )
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "EDITAR O AÑADIR TAREAS",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF30475E)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

@Composable
private fun PriorityCount(tasks: List<TaskItem>, priority: TaskPriority) {
    val count = tasks.count { it.priority == priority && !it.completed }
    Box(
        modifier = GlanceModifier
            .padding(start = 3.dp)
            .background(Color(android.graphics.Color.parseColor(priority.hexColor)))
            .cornerRadius(5.dp)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            style = TextStyle(color = ColorProvider(Color(0xFF263238)), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun TaskRow(task: TaskItem) {
    val toggleAction = actionRunCallback<ToggleTaskCompletedAction>(
        actionParametersOf(WidgetActionKeys.taskId to task.id)
    )
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 3.dp)
            .background(Color(android.graphics.Color.parseColor(task.priority.hexColor)))
            .cornerRadius(5.dp)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        CheckBox(
            checked = task.completed,
            onCheckedChange = toggleAction,
            text = task.title + if (task.completed) " · hecha" else "",
            style = TextStyle(
                color = ColorProvider(Color(0xFF263238)),
                fontSize = 10.sp
            ),
            modifier = GlanceModifier
                .fillMaxWidth(),
            maxLines = 1
        )
    }
}

@Composable
private fun DayBody(schedule: DaySchedule, maxEntries: Int) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        when {
            schedule.courseStatus != SchoolCourseStatus.ACTIVE -> Notice(schedule.courseStatus.label)
            schedule.schoolDayEvent != null -> SchoolDayNotice(schedule.schoolDayEvent)
            schedule.isWeekend -> Notice("FIN DE SEMANA\nPróximo día lectivo: lunes")
            else -> schedule.entries.take(maxEntries).forEach { EntryRow(it) }
        }
    }
}

@Composable
private fun WeekBody(dateInWeek: LocalDate) {
    val monday = CalendarIcsExporter.mondayOf(dateInWeek)
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        (0L..4L).forEach { offset ->
            val date = monday.plusDays(offset)
            val schedule = ScheduleRepository.scheduleFor(date)
            val summary = schedule.schoolDayEvent?.title ?: schedule.entries
                .filter {
                    it.type == ScheduleType.PATIO || it.type == ScheduleType.NO_PATIO ||
                        it.type == ScheduleType.PARENT_ATTENTION || it.type == ScheduleType.VESPERTINO
                }
                .joinToString(" · ") { it.title.replace(" · Primaria", "") }
            val rowColor = schedule.schoolDayEvent?.type?.hexColor?.let {
                Color(android.graphics.Color.parseColor(it))
            } ?: Color.White
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .background(rowColor)
                    .cornerRadius(6.dp)
                    .padding(horizontal = 7.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateUtils.dayName(date).take(3),
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF30475E)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.width(38.dp)
                )
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = DateUtils.formatDate(date),
                        style = TextStyle(color = ColorProvider(Color(0xFF1F2933)), fontSize = 9.sp)
                    )
                    Text(
                        text = summary.ifBlank { "Sin datos" },
                        style = TextStyle(color = ColorProvider(Color(0xFF1F2933)), fontSize = 10.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SchoolDayNotice(event: SchoolDayEvent) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(Color(android.graphics.Color.parseColor(event.type.hexColor)))
            .cornerRadius(8.dp)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = event.type.label,
            style = TextStyle(
                color = ColorProvider(Color(0xFF263238)),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            modifier = GlanceModifier.fillMaxWidth()
        )
        Text(
            text = event.title,
            style = TextStyle(
                color = ColorProvider(Color(0xFF263238)),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp)
        )
        Text(
            text = event.detail,
            style = TextStyle(
                color = ColorProvider(Color(0xFF3F4A52)),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            ),
            modifier = GlanceModifier.fillMaxWidth().padding(top = 3.dp)
        )
    }
}

@Composable
private fun Notice(text: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(Color.White)
            .cornerRadius(8.dp)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = ColorProvider(Color(0xFF1F2933)),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun EntryRow(entry: ScheduleEntry) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .background(Color(android.graphics.Color.parseColor(entry.type.hexColor)))
            .cornerRadius(6.dp)
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.timeRange,
            style = TextStyle(
                color = ColorProvider(Color(0xFF1F2933)),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.width(88.dp)
        )
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = entry.title,
                style = TextStyle(
                    color = ColorProvider(Color(0xFF1F2933)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            entry.subtitle?.let {
                Text(
                    text = it,
                    style = TextStyle(
                        color = ColorProvider(if (it.contains("BIBLIOTECA")) Color(0xFF7A5A00) else Color(0xFF333333)),
                        fontSize = 9.sp
                    )
                )
            }
            entry.supportTeacher?.let {
                Text(
                    text = "Apoyo conmigo: $it",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF1F5C70)),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            entry.highlight?.let {
                Box(
                    modifier = GlanceModifier
                        .padding(top = 2.dp)
                        .background(Color(0xFFFFF2A8))
                        .cornerRadius(6.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = it,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF3A2F00)),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
