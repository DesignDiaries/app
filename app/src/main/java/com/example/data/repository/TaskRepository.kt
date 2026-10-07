package com.example.data.repository

import android.content.Context
import com.example.data.db.TaskLaunchDatabase
import com.example.data.model.Priority
import com.example.data.model.Recurrence
import com.example.data.model.SubtaskEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskListEntity
import com.example.data.model.TaskWithSubtasks
import com.example.service.AlarmScheduler
import com.example.util.NaturalLanguageParser
import com.example.util.ParsedTaskInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Calendar

class TaskRepository(private val context: Context) {

    private val db = TaskLaunchDatabase.getInstance(context)
    private val taskDao = db.taskDao()
    private val subtaskDao = db.subtaskDao()
    private val listDao = db.taskListDao()

    init {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                TaskLaunchDatabase.seedIfEmpty(db)
            } catch (_: Throwable) {}
        }
    }

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val allTasksWithSubtasks: Flow<List<TaskWithSubtasks>> = taskDao.getTasksWithSubtasks()
    val allLists: Flow<List<TaskListEntity>> = listDao.getAllLists()
    val pendingCount: Flow<Int> = taskDao.getPendingCount()
    val nextPendingTask: Flow<TaskEntity?> = taskDao.getNextPendingTask()

    fun getTaskWithSubtasks(id: Long): Flow<TaskWithSubtasks?> = taskDao.getTaskWithSubtasksById(id)

    fun searchTasks(query: String): Flow<List<TaskEntity>> = taskDao.searchTasks(query)

    suspend fun createTask(
        title: String,
        notes: String = "",
        priority: Priority = Priority.MEDIUM,
        dueDate: Long? = null,
        dueTimeMinutes: Int? = null,
        listId: Long = 1L,
        tags: String = "",
        recurrence: Recurrence = Recurrence.NONE,
        linkedPackageName: String? = null,
        reminderTime: Long? = null,
        subtaskTitles: List<String> = emptyList()
    ): Long {
        val task = TaskEntity(
            title = title,
            notes = notes,
            priority = priority,
            dueDate = dueDate,
            dueTimeMinutes = dueTimeMinutes,
            listId = listId,
            tags = tags,
            recurrence = recurrence,
            linkedPackageName = linkedPackageName,
            reminderTime = reminderTime
        )
        val taskId = taskDao.insertTask(task)

        if (subtaskTitles.isNotEmpty()) {
            val subtasks = subtaskTitles.mapIndexed { index, subtaskTitle ->
                SubtaskEntity(taskId = taskId, title = subtaskTitle, sortOrder = index)
            }
            subtaskDao.insertAll(subtasks)
        }

        if (reminderTime != null && reminderTime > System.currentTimeMillis()) {
            AlarmScheduler.scheduleReminder(context, task.copy(id = taskId))
        }

        return taskId
    }

    suspend fun quickAddTaskWithNlp(input: String, defaultListId: Long = 1L): Long {
        val parsed = NaturalLanguageParser.parse(input)
        val title = if (parsed.cleanTitle.isNotBlank()) parsed.cleanTitle else input.trim()

        // Calculate reminder time if due time was provided
        val reminder = if (parsed.dueDate != null && parsed.dueTimeMinutes != null) {
            parsed.dueDate + (parsed.dueTimeMinutes * 60 * 1000L)
        } else null

        return createTask(
            title = title,
            priority = parsed.priority,
            dueDate = parsed.dueDate,
            dueTimeMinutes = parsed.dueTimeMinutes,
            listId = defaultListId,
            tags = parsed.tags.joinToString(","),
            recurrence = parsed.recurrence,
            reminderTime = reminder
        )
    }

    suspend fun updateTask(task: TaskEntity, subtasks: List<SubtaskEntity>? = null) {
        taskDao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
        if (subtasks != null) {
            subtaskDao.deleteSubtasksForTask(task.id)
            subtaskDao.insertAll(subtasks)
        }
        if (task.reminderTime != null && task.reminderTime > System.currentTimeMillis() && !task.isCompleted) {
            AlarmScheduler.scheduleReminder(context, task)
        } else {
            AlarmScheduler.cancelReminder(context, task.id)
        }
    }

    suspend fun toggleTaskCompletion(task: TaskEntity): Boolean {
        val newStatus = !task.isCompleted
        val completedAt = if (newStatus) System.currentTimeMillis() else null
        taskDao.setTaskCompletion(task.id, newStatus, completedAt)

        if (newStatus) {
            AlarmScheduler.cancelReminder(context, task.id)
            // Handle recurrence
            if (task.recurrence != Recurrence.NONE) {
                createNextRecurringTask(task)
            }
        } else if (task.reminderTime != null && task.reminderTime > System.currentTimeMillis()) {
            AlarmScheduler.scheduleReminder(context, task)
        }
        return newStatus
    }

    private suspend fun createNextRecurringTask(task: TaskEntity) {
        val cal = Calendar.getInstance()
        if (task.dueDate != null) {
            cal.timeInMillis = task.dueDate
        }
        when (task.recurrence) {
            Recurrence.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            Recurrence.WEEKDAYS -> {
                do {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                } while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            }
            Recurrence.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            Recurrence.MONTHLY -> cal.add(Calendar.MONTH, 1)
            Recurrence.NONE -> return
        }

        val newDueDate = cal.timeInMillis
        val newReminder = if (task.dueTimeMinutes != null) {
            newDueDate + (task.dueTimeMinutes * 60 * 1000L)
        } else null

        taskDao.insertTask(
            task.copy(
                id = 0,
                isCompleted = false,
                completedAt = null,
                dueDate = newDueDate,
                reminderTime = newReminder,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteTask(task: TaskEntity) {
        AlarmScheduler.cancelReminder(context, task.id)
        taskDao.deleteTask(task)
    }

    suspend fun snoozeTask(task: TaskEntity, minutes: Int) {
        val newReminder = System.currentTimeMillis() + (minutes * 60 * 1000L)
        val updated = task.copy(reminderTime = newReminder)
        taskDao.updateTask(updated)
        AlarmScheduler.scheduleReminder(context, updated)
    }

    suspend fun rescheduleOverdueToToday(tasks: List<TaskEntity>) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val today = cal.timeInMillis

        for (task in tasks) {
            val updated = task.copy(dueDate = today)
            taskDao.updateTask(updated)
        }
    }

    suspend fun toggleSubtaskCompletion(subtask: SubtaskEntity) {
        subtaskDao.setSubtaskCompletion(subtask.id, !subtask.isCompleted)
    }

    suspend fun addSubtask(taskId: Long, title: String) {
        subtaskDao.insertSubtask(SubtaskEntity(taskId = taskId, title = title))
    }

    suspend fun createList(name: String, colorHex: String, iconName: String): Long {
        return listDao.insertList(TaskListEntity(name = name, colorHex = colorHex, iconName = iconName))
    }

    val availableTemplates = listOf(
        TaskTemplate(
            title = "Weekly Review & Setup",
            description = "Align calendar, clear clutter, and set big goals",
            priority = Priority.HIGH,
            tags = "planning,weekly",
            subtasks = listOf(
                "Clear desktop & download clutter",
                "Review upcoming calendar commitments",
                "Define top 3 goals for this week",
                "Schedule deep work time blocks"
            )
        ),
        TaskTemplate(
            title = "Daily Standup Routine",
            description = "Quick daily focus check-in",
            priority = Priority.MEDIUM,
            tags = "work,standup",
            subtasks = listOf(
                "What did I achieve yesterday?",
                "What is my main focus today?",
                "Any blockers to clear?"
            )
        ),
        TaskTemplate(
            title = "Travel Packing Checklist",
            description = "Essential trip travel checklist",
            priority = Priority.HIGH,
            tags = "travel,personal",
            subtasks = listOf(
                "Passport, ID, cards & boarding pass",
                "Phone charger, cables & power bank",
                "Medications, vitamins & toiletries",
                "Weather-appropriate clothes & shoes",
                "Lock all doors & windows"
            )
        ),
        TaskTemplate(
            title = "Grocery & Home Essentials",
            description = "Pantry and kitchen restock",
            priority = Priority.LOW,
            tags = "shopping,home",
            subtasks = listOf(
                "Fresh fruits & greens",
                "Milk, bread & coffee beans",
                "Pantry essentials",
                "Paper towels & detergent"
            )
        )
    )

    suspend fun createFromTemplate(template: TaskTemplate, defaultListId: Long = 1L): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return createTask(
            title = template.title,
            notes = template.description,
            priority = template.priority,
            dueDate = cal.timeInMillis,
            listId = defaultListId,
            tags = template.tags,
            subtaskTitles = template.subtasks
        )
    }
}

data class TaskTemplate(
    val title: String,
    val description: String,
    val priority: Priority,
    val tags: String,
    val subtasks: List<String>
)
