package es.sandra.horario.data

import android.content.Context
import es.sandra.horario.model.TaskItem
import es.sandra.horario.model.TaskPriority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

object TaskRepository {
    private const val preferencesName = "sandra_tasks"
    private const val taskIdsKey = "task_ids"

    @Synchronized
    fun load(context: Context): List<TaskItem> {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        return preferences.getStringSet(taskIdsKey, emptySet()).orEmpty()
            .mapNotNull { storedId ->
                val id = storedId.toLongOrNull() ?: return@mapNotNull null
                val title = preferences.getString(titleKey(id), null)?.trim().orEmpty()
                if (title.isBlank()) return@mapNotNull null
                val priority = runCatching {
                    TaskPriority.valueOf(preferences.getString(priorityKey(id), TaskPriority.NORMAL.name).orEmpty())
                }.getOrDefault(TaskPriority.NORMAL)
                TaskItem(id, title, priority, preferences.getBoolean(completedKey(id), false))
            }
            .sortedBy { it.id }
    }

    fun observe(context: Context): Flow<List<TaskItem>> {
        val appContext = context.applicationContext
        return callbackFlow {
            val preferences = appContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                trySend(load(appContext))
            }
            preferences.registerOnSharedPreferenceChangeListener(listener)
            trySend(load(appContext))
            awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
        }.distinctUntilChanged()
    }

    @Synchronized
    fun add(context: Context, title: String, priority: TaskPriority): List<TaskItem> {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return load(context)
        val current = load(context)
        val id = maxOf(System.currentTimeMillis(), (current.maxOfOrNull { it.id } ?: 0L) + 1L)
        val task = TaskItem(id, cleanTitle, priority)
        writeTask(context, task, current.map { it.id }.toSet() + id)
        return load(context)
    }

    @Synchronized
    fun update(context: Context, task: TaskItem): List<TaskItem> {
        val cleanTitle = task.title.trim()
        if (cleanTitle.isBlank()) return load(context)
        val currentIds = load(context).map { it.id }.toSet()
        if (task.id !in currentIds) return load(context)
        writeTask(context, task.copy(title = cleanTitle), currentIds)
        return load(context)
    }

    @Synchronized
    fun toggle(context: Context, taskId: Long): List<TaskItem> {
        val tasks = load(context)
        val task = tasks.firstOrNull { it.id == taskId } ?: return tasks
        return setCompleted(context, taskId, !task.completed)
    }

    @Synchronized
    fun setCompleted(context: Context, taskId: Long, completed: Boolean): List<TaskItem> {
        val tasks = load(context)
        val task = tasks.firstOrNull { it.id == taskId } ?: return tasks
        if (task.completed == completed) return tasks
        writeTask(context, task.copy(completed = completed), tasks.map { it.id }.toSet())
        return load(context)
    }

    @Synchronized
    fun delete(context: Context, taskId: Long): List<TaskItem> {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val remainingIds = load(context).map { it.id }.filterNot { it == taskId }.map(Long::toString).toSet()
        preferences.edit()
            .putStringSet(taskIdsKey, remainingIds)
            .remove(titleKey(taskId))
            .remove(priorityKey(taskId))
            .remove(completedKey(taskId))
            .commit()
        return load(context)
    }

    private fun writeTask(context: Context, task: TaskItem, ids: Set<Long>) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit()
            .putStringSet(taskIdsKey, ids.map(Long::toString).toSet())
            .putString(titleKey(task.id), task.title)
            .putString(priorityKey(task.id), task.priority.name)
            .putBoolean(completedKey(task.id), task.completed)
            .commit()
    }

    private fun titleKey(id: Long) = "task_${id}_title"
    private fun priorityKey(id: Long) = "task_${id}_priority"
    private fun completedKey(id: Long) = "task_${id}_completed"
}
