package es.sandra.horario.widget

import android.content.Context
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.ToggleableStateKey
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import es.sandra.horario.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object WidgetActionKeys {
    val taskId = ActionParameters.Key<Long>("task_id")
}

private val widgetRefreshScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
private val widgetRefreshRequests = Channel<Context>(Channel.CONFLATED)
private val widgetRefreshMutex = Mutex()

private suspend fun refreshWidgets(context: Context) {
    widgetRefreshMutex.withLock {
        val appContext = context.applicationContext
        val manager = GlanceAppWidgetManager(appContext)
        manager.getGlanceIds(ScheduleWidget::class.java).forEach { glanceId ->
            updateAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId) { preferences ->
                mutablePreferencesOf(
                    WidgetState.offsetDaysKey to (preferences[WidgetState.offsetDaysKey] ?: 0),
                    WidgetState.viewModeKey to (preferences[WidgetState.viewModeKey] ?: 0),
                    WidgetState.tasksExpandedKey to (preferences[WidgetState.tasksExpandedKey] ?: false),
                    WidgetState.taskRevisionKey to
                        ((preferences[WidgetState.taskRevisionKey] ?: 0L) + 1L)
                )
            }
            ScheduleWidget().update(appContext, glanceId)
        }
    }
}

private val widgetRefreshWorker = widgetRefreshScope.launch {
    for (context in widgetRefreshRequests) {
        refreshWidgets(context)
    }
}

fun requestWidgetRefresh(context: Context) {
    widgetRefreshRequests.trySend(context.applicationContext)
}

class PreviousPeriodAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        changePeriod(context, glanceId, -1)
    }
}

class NextPeriodAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        changePeriod(context, glanceId, 1)
    }
}

class TodayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateWidgetPreferences(context, glanceId) { it.copy(offsetDays = 0) }
    }
}

class SetDayViewAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateWidgetPreferences(context, glanceId) { it.copy(viewMode = 0) }
    }
}

class SetWeekViewAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateWidgetPreferences(context, glanceId) { it.copy(viewMode = 1) }
    }
}

class ToggleTasksPanelAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateWidgetPreferences(context, glanceId) { it.copy(tasksExpanded = !it.tasksExpanded) }
    }
}

class ToggleTaskCompletedAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[WidgetActionKeys.taskId] ?: return
        val completed = parameters[ToggleableStateKey] ?: return
        TaskRepository.setCompleted(context, taskId, completed)
        refreshWidgets(context)
    }
}

suspend fun resetAllWidgetsToToday(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    manager.getGlanceIds(ScheduleWidget::class.java).forEach { glanceId ->
        updateWidgetPreferences(context, glanceId) { it.copy(offsetDays = 0) }
    }
}

private suspend fun changePeriod(context: Context, glanceId: GlanceId, direction: Int) {
    updateWidgetPreferences(context, glanceId) { current ->
        val dayStep = if (current.viewMode == 1) 7 else 1
        current.copy(offsetDays = current.offsetDays + direction * dayStep)
    }
}

private data class WidgetPreferences(
    val offsetDays: Int,
    val viewMode: Int,
    val tasksExpanded: Boolean,
    val taskRevision: Long
)

private suspend fun updateWidgetPreferences(
    context: Context,
    glanceId: GlanceId,
    update: (WidgetPreferences) -> WidgetPreferences
) {
    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { preferences ->
        val current = WidgetPreferences(
            offsetDays = preferences[WidgetState.offsetDaysKey] ?: 0,
            viewMode = preferences[WidgetState.viewModeKey] ?: 0,
            tasksExpanded = preferences[WidgetState.tasksExpandedKey] ?: false,
            taskRevision = preferences[WidgetState.taskRevisionKey] ?: 0L
        )
        val updated = update(current)
        mutablePreferencesOf(
            WidgetState.offsetDaysKey to updated.offsetDays,
            WidgetState.viewModeKey to updated.viewMode,
            WidgetState.tasksExpandedKey to updated.tasksExpanded,
            WidgetState.taskRevisionKey to updated.taskRevision
        )
    }
    ScheduleWidget().update(context, glanceId)
}
