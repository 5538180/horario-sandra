package es.sandra.horario.widget

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey

object WidgetState {
    val offsetDaysKey = intPreferencesKey("offset_days")
    val viewModeKey = intPreferencesKey("view_mode")
    val tasksExpandedKey = booleanPreferencesKey("tasks_expanded")
    val taskRevisionKey = longPreferencesKey("task_revision")
}
