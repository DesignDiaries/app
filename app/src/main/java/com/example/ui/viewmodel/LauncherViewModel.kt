package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CustomAppFolder
import com.example.data.model.FocusMode
import com.example.data.model.HabitItem
import com.example.data.model.LauncherSettings
import com.example.data.model.Priority
import com.example.data.model.Recurrence
import com.example.data.model.SubtaskEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskListEntity
import com.example.data.model.TaskWithSubtasks
import com.example.data.model.WorkspaceItemEntity
import com.example.data.repository.LauncherRepository
import com.example.data.repository.TaskRepository
import com.example.util.AppCategory
import com.example.util.InstalledApp
import com.example.util.NaturalLanguageParser
import com.example.util.ParsedTaskInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class PomodoroMode(val title: String, val defaultMinutes: Int) {
    WORK("Focus", 25),
    DEEP_SPRINT("Deep Sprint", 50),
    SHORT_BREAK("Short Break", 5),
    LONG_BREAK("Long Break", 15)
}

data class PomodoroState(
    val isRunning: Boolean = false,
    val secondsRemaining: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val mode: PomodoroMode = PomodoroMode.WORK,
    val completedCycles: Int = 0,
    val linkedTaskId: Long? = null,
    val linkedTaskTitle: String? = null
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    val taskRepository = TaskRepository(application)
    val launcherRepository = LauncherRepository(application)

    // UI overlays & sheets
    private val _isSearchOpen = MutableStateFlow(false)
    val isSearchOpen = _isSearchOpen.asStateFlow()

    private val _isAppDrawerOpen = MutableStateFlow(false)
    val isAppDrawerOpen = _isAppDrawerOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen = _isSettingsOpen.asStateFlow()

    private val _editingTask = MutableStateFlow<TaskWithSubtasks?>(null)
    val editingTask = _editingTask.asStateFlow()

    private val _isTaskDetailOpen = MutableStateFlow(false)
    val isTaskDetailOpen = _isTaskDetailOpen.asStateFlow()

    private val _selectedAppForMenu = MutableStateFlow<InstalledApp?>(null)
    val selectedAppForMenu = _selectedAppForMenu.asStateFlow()

    private val _isDefaultLauncher = MutableStateFlow(false)
    val isDefaultLauncher = _isDefaultLauncher.asStateFlow()

    // Search query & results
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Drawer category
    private val _drawerCategory = MutableStateFlow(AppCategory.ALL)
    val drawerCategory = _drawerCategory.asStateFlow()

    private val _drawerQuery = MutableStateFlow("")
    val drawerQuery = _drawerQuery.asStateFlow()

    // Quick add NLP parsing
    private val _quickAddText = MutableStateFlow("")
    val quickAddText = _quickAddText.asStateFlow()

    private val _parsedPreview = MutableStateFlow<ParsedTaskInput?>(null)
    val parsedPreview = _parsedPreview.asStateFlow()

    // Task feed filters
    private val _selectedListFilter = MutableStateFlow<Long?>(null) // null = all lists
    val selectedListFilter = _selectedListFilter.asStateFlow()

    private val _selectedPriorityFilter = MutableStateFlow<Priority?>(null)
    val selectedPriorityFilter = _selectedPriorityFilter.asStateFlow()

    // Scratchpad notes
    private val notePrefs = application.getSharedPreferences("scratchpad_notes", Context.MODE_PRIVATE)
    private val _scratchpadNotes = MutableStateFlow(notePrefs.getString("content", "• Review presentation slides !high #work\n• 15 min team alignment sync\n• Buy groceries & vitamins") ?: "")
    val scratchpadNotes = _scratchpadNotes.asStateFlow()

    // Daily Habits
    private val habitPrefs = application.getSharedPreferences("launcher_habits", Context.MODE_PRIVATE)
    private val _habits = MutableStateFlow(loadHabits())
    val habits = _habits.asStateFlow()

    // Focus time logged today (minutes)
    private val _dailyFocusMinutes = MutableStateFlow(notePrefs.getInt("focus_mins_today", 50))
    val dailyFocusMinutes = _dailyFocusMinutes.asStateFlow()

    // Pomodoro Timer
    private val _pomodoroState = MutableStateFlow(PomodoroState())
    val pomodoroState = _pomodoroState.asStateFlow()
    private var pomodoroJob: Job? = null

    // Snackbar message events
    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent = _snackbarEvent.asSharedFlow()

    // Settings & workspace
    val settings: StateFlow<LauncherSettings> = launcherRepository.settings
    val workspaceItems: StateFlow<List<WorkspaceItemEntity>> = launcherRepository.workspaceItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Custom App Folders & Groupings
    val customFolders: StateFlow<List<CustomAppFolder>> = launcherRepository.customFolders

    // Custom Dock App Package Names
    val customDockApps: StateFlow<List<String>> = launcherRepository.customDockApps

    // Installed apps
    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    // Task data streams
    val allTasks = taskRepository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasksWithSubtasks = taskRepository.allTasksWithSubtasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLists: StateFlow<List<TaskListEntity>> = taskRepository.allLists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> = taskRepository.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val nextPendingTask: StateFlow<TaskEntity?> = taskRepository.nextPendingTask
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filtered tasks categorized into Today, Overdue, Upcoming, Completed
    val categorizedTasks = combine(
        allTasks,
        selectedListFilter,
        selectedPriorityFilter,
        settings
    ) { tasks, listFilter, priorityFilter, currentSettings ->
        val activeTag = currentSettings.activeFocusMode.allowedTag

        val filtered = tasks.filter { task ->
            val matchTag = activeTag == null || task.tags.split(",").any { it.trim().equals(activeTag, ignoreCase = true) }
            val matchList = listFilter == null || task.listId == listFilter
            val matchPriority = priorityFilter == null || task.priority == priorityFilter
            matchTag && matchList && matchPriority
        }

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStart = cal.timeInMillis

        val todayTasks = mutableListOf<TaskEntity>()
        val overdueTasks = mutableListOf<TaskEntity>()
        val upcomingTasks = mutableListOf<TaskEntity>()
        val completedTasks = mutableListOf<TaskEntity>()

        for (task in filtered) {
            if (task.isCompleted) {
                completedTasks.add(task)
            } else {
                val due = task.dueDate
                when {
                    due == null -> todayTasks.add(task) // Unscheduled tasks show in today's agenda
                    due < todayStart -> overdueTasks.add(task)
                    due in todayStart until tomorrowStart -> todayTasks.add(task)
                    else -> upcomingTasks.add(task)
                }
            }
        }

        TaskCategories(
            today = todayTasks,
            overdue = overdueTasks,
            upcoming = upcomingTasks,
            completed = completedTasks
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TaskCategories(emptyList(), emptyList(), emptyList(), emptyList())
    )

    init {
        loadInstalledApps()
        checkDefaultLauncherStatus()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = launcherRepository.appManager.loadInstalledApps()
            _installedApps.value = apps
        }
    }

    // Sheet / Dialog controls
    fun openSearch() { _isSearchOpen.value = true }
    fun closeSearch() {
        _isSearchOpen.value = false
        _searchQuery.value = ""
    }

    fun openAppDrawer() { _isAppDrawerOpen.value = true }
    fun closeAppDrawer() {
        _isAppDrawerOpen.value = false
        _drawerQuery.value = ""
    }

    fun openSettings() { _isSettingsOpen.value = true }
    fun closeSettings() { _isSettingsOpen.value = false }

    fun openTaskDetail(task: TaskWithSubtasks?) {
        _editingTask.value = task
        _isTaskDetailOpen.value = true
    }
    fun closeTaskDetail() {
        _isTaskDetailOpen.value = false
        _editingTask.value = null
    }

    fun openAppMenu(app: InstalledApp) {
        _selectedAppForMenu.value = app
    }
    fun closeAppMenu() {
        _selectedAppForMenu.value = null
    }

    // Search query update
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onDrawerQueryChanged(query: String) {
        _drawerQuery.value = query
    }

    fun onDrawerCategoryChanged(category: AppCategory) {
        _drawerCategory.value = category
    }

    // Quick add NLP text changed
    fun onQuickAddTextChanged(text: String) {
        _quickAddText.value = text
        if (text.isNotBlank()) {
            _parsedPreview.value = NaturalLanguageParser.parse(text)
        } else {
            _parsedPreview.value = null
        }
    }

    fun submitQuickAdd() {
        val input = _quickAddText.value.trim()
        if (input.isBlank()) return

        viewModelScope.launch {
            val listId = _selectedListFilter.value ?: 1L
            taskRepository.quickAddTaskWithNlp(input, defaultListId = listId)
            _quickAddText.value = ""
            _parsedPreview.value = null
            _snackbarEvent.emit("Task created")
        }
    }

    // Task Actions
    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val isDone = taskRepository.toggleTaskCompletion(task)
            _snackbarEvent.emit(if (isDone) "Task completed! 🎉" else "Task restored")
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
            _snackbarEvent.emit("Task deleted")
        }
    }

    fun snoozeTask(task: TaskEntity, minutes: Int) {
        viewModelScope.launch {
            taskRepository.snoozeTask(task, minutes)
            _snackbarEvent.emit("Snoozed for ${if (minutes >= 60) "${minutes / 60}h" else "${minutes}m"}")
        }
    }

    fun rescheduleOverdueToToday(overdueTasks: List<TaskEntity>) {
        viewModelScope.launch {
            taskRepository.rescheduleOverdueToToday(overdueTasks)
            _snackbarEvent.emit("Rescheduled ${overdueTasks.size} tasks to Today")
        }
    }

    val availableTemplates = taskRepository.availableTemplates

    fun createFromTemplate(template: com.example.data.repository.TaskTemplate) {
        viewModelScope.launch {
            val listId = _selectedListFilter.value ?: 1L
            taskRepository.createFromTemplate(template, defaultListId = listId)
            _snackbarEvent.emit("Template '${template.title}' added to Today")
        }
    }

    fun toggleSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            taskRepository.toggleSubtaskCompletion(subtask)
        }
    }

    fun saveTaskDetail(
        id: Long,
        title: String,
        notes: String,
        priority: Priority,
        dueDate: Long?,
        dueTimeMinutes: Int?,
        listId: Long,
        tags: String,
        recurrence: Recurrence,
        linkedPackage: String?,
        reminderTime: Long?,
        subtasks: List<SubtaskEntity>
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                taskRepository.createTask(
                    title = title,
                    notes = notes,
                    priority = priority,
                    dueDate = dueDate,
                    dueTimeMinutes = dueTimeMinutes,
                    listId = listId,
                    tags = tags,
                    recurrence = recurrence,
                    linkedPackageName = linkedPackage,
                    reminderTime = reminderTime,
                    subtaskTitles = subtasks.map { it.title }
                )
                _snackbarEvent.emit("Task created")
            } else {
                val updated = TaskEntity(
                    id = id,
                    title = title,
                    notes = notes,
                    priority = priority,
                    dueDate = dueDate,
                    dueTimeMinutes = dueTimeMinutes,
                    listId = listId,
                    tags = tags,
                    recurrence = recurrence,
                    linkedPackageName = linkedPackage,
                    reminderTime = reminderTime
                )
                taskRepository.updateTask(updated, subtasks)
                _snackbarEvent.emit("Task updated")
            }
            closeTaskDetail()
        }
    }

    // Filter selectors
    fun selectListFilter(listId: Long?) {
        _selectedListFilter.value = if (_selectedListFilter.value == listId) null else listId
    }

    fun selectPriorityFilter(priority: Priority?) {
        _selectedPriorityFilter.value = if (_selectedPriorityFilter.value == priority) null else priority
    }

    // App launches
    fun isAppRestricted(app: InstalledApp): Boolean {
        val focusMode = settings.value.activeFocusMode
        val isPomodoroRunning = pomodoroState.value.isRunning
        val isFocusActive = focusMode != FocusMode.OFF || isPomodoroRunning
        if (!isFocusActive) return false

        // Essential system apps are never restricted
        val pkg = app.packageName.lowercase()
        val isEssential = pkg.contains("dialer") || pkg.contains("phone") || pkg.contains("telecom") ||
                pkg.contains("emergency") || pkg.contains("alarm") || pkg.contains("clock") ||
                pkg.contains("settings") || pkg.contains("packageinstaller")
        if (isEssential) return false

        return when {
            isPomodoroRunning || focusMode == FocusMode.DEEP_WORK || focusMode == FocusMode.ZEN -> {
                // In Deep Work or active Pomodoro or Zen, restrict games, social & media
                app.category == AppCategory.GAMES || app.category == AppCategory.SOCIAL || app.category == AppCategory.MEDIA
            }
            focusMode == FocusMode.WORK || focusMode == FocusMode.STUDY -> {
                // In Work/Study, block games and social distracting apps
                app.category == AppCategory.GAMES || app.category == AppCategory.SOCIAL
            }
            else -> false
        }
    }

    fun launchApp(packageName: String): Boolean {
        val app = _installedApps.value.firstOrNull { it.packageName == packageName }
        if (app != null && isAppRestricted(app)) {
            val modeName = if (pomodoroState.value.isRunning) "Pomodoro Focus" else settings.value.activeFocusMode.title
            viewModelScope.launch {
                _snackbarEvent.emit("🔒 ${app.label} is paused during $modeName mode.")
            }
            return false
        }
        return launcherRepository.appManager.launchApp(packageName)
    }

    fun openAppInfo(packageName: String) {
        launcherRepository.appManager.openAppInfo(packageName)
    }

    fun uninstallApp(packageName: String) {
        launcherRepository.appManager.uninstallApp(packageName)
    }

    fun checkDefaultLauncherStatus() {
        try {
            _isDefaultLauncher.value = launcherRepository.appManager.isDefaultLauncher()
        } catch (_: Throwable) {}
    }

    fun requestDefaultLauncher(context: Context) {
        try {
            val intent = launcherRepository.appManager.requestDefaultLauncherIntent()
            context.startActivity(intent)
        } catch (_: Throwable) {
            try {
                val fallbackIntent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Throwable) {}
        }
    }

    // Focus mode
    fun setFocusMode(mode: FocusMode) {
        launcherRepository.setFocusMode(mode)
    }

    fun toggleZenGlanceMode() {
        val current = settings.value.zenGlanceMode
        val updated = !current
        updateSettings(settings.value.copy(zenGlanceMode = updated))
        viewModelScope.launch {
            if (updated) {
                _snackbarEvent.emit("🌿 Zen Glance activated: Single Focus layout")
            } else {
                _snackbarEvent.emit("Default Home view restored")
            }
        }
    }

    // Daily Habits
    private fun loadHabits(): List<HabitItem> {
        val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val savedDate = habitPrefs.getString("habits_date", "")
        val isNewDay = savedDate != todayStr

        val defaults = listOf(
            HabitItem("water", "Drink 2L Water", "💧", isCompleted = if (isNewDay) false else habitPrefs.getBoolean("habit_water", true), streak = habitPrefs.getInt("streak_water", 4)),
            HabitItem("read", "15 Min Deep Reading", "📚", isCompleted = if (isNewDay) false else habitPrefs.getBoolean("habit_read", false), streak = habitPrefs.getInt("streak_read", 2)),
            HabitItem("move", "Physical Movement & Stretch", "🏃", isCompleted = if (isNewDay) false else habitPrefs.getBoolean("habit_move", true), streak = habitPrefs.getInt("streak_move", 5)),
            HabitItem("breath", "Mindful Breathing Reset", "🧘", isCompleted = if (isNewDay) false else habitPrefs.getBoolean("habit_breath", false), streak = habitPrefs.getInt("streak_breath", 3)),
            HabitItem("journal", "Evening Brain Dump", "📝", isCompleted = if (isNewDay) false else habitPrefs.getBoolean("habit_journal", false), streak = habitPrefs.getInt("streak_journal", 1))
        )
        if (isNewDay) {
            habitPrefs.edit().putString("habits_date", todayStr).apply()
        }
        return defaults
    }

    private fun saveHabits(list: List<HabitItem>) {
        val editor = habitPrefs.edit()
        list.forEach { habit ->
            editor.putBoolean("habit_${habit.id}", habit.isCompleted)
            editor.putInt("streak_${habit.id}", habit.streak)
        }
        editor.apply()
    }

    fun toggleHabit(id: String) {
        val current = _habits.value
        val updated = current.map { habit ->
            if (habit.id == id) {
                val newStatus = !habit.isCompleted
                val newStreak = if (newStatus) habit.streak + 1 else maxOf(0, habit.streak - 1)
                habit.copy(isCompleted = newStatus, streak = newStreak)
            } else habit
        }
        _habits.value = updated
        saveHabits(updated)
    }

    // Pomodoro timer
    fun setPomodoroMode(mode: PomodoroMode) {
        pomodoroJob?.cancel()
        val totalSecs = mode.defaultMinutes * 60
        _pomodoroState.value = _pomodoroState.value.copy(
            isRunning = false,
            mode = mode,
            secondsRemaining = totalSecs,
            totalSeconds = totalSecs
        )
    }

    fun addPomodoroMinutes(minutes: Int) {
        val currentSecs = _pomodoroState.value.secondsRemaining
        val addedSecs = minutes * 60
        val newSecs = maxOf(60, currentSecs + addedSecs)
        val newTotal = maxOf(_pomodoroState.value.totalSeconds, newSecs)
        _pomodoroState.value = _pomodoroState.value.copy(
            secondsRemaining = newSecs,
            totalSeconds = newTotal
        )
    }

    fun linkTaskToPomodoro(task: TaskEntity?) {
        _pomodoroState.value = _pomodoroState.value.copy(
            linkedTaskId = task?.id,
            linkedTaskTitle = task?.title
        )
    }

    fun completeCurrentTaskAndPomodoro() {
        val taskId = _pomodoroState.value.linkedTaskId
        viewModelScope.launch {
            if (taskId != null) {
                val task = taskRepository.allTasks.first().firstOrNull { it.id == taskId }
                if (task != null) {
                    taskRepository.toggleTaskCompletion(task)
                }
            }
            val newCycles = _pomodoroState.value.completedCycles + 1
            _pomodoroState.value = _pomodoroState.value.copy(
                completedCycles = newCycles,
                linkedTaskId = null,
                linkedTaskTitle = null
            )
            _snackbarEvent.emit("Task completed & focus cycle counted! 🎉")
        }
    }

    fun togglePomodoro(targetTaskTitle: String? = null) {
        if (_pomodoroState.value.isRunning) {
            pomodoroJob?.cancel()
            _pomodoroState.value = _pomodoroState.value.copy(isRunning = false)
        } else {
            val title = targetTaskTitle ?: _pomodoroState.value.linkedTaskTitle
            _pomodoroState.value = _pomodoroState.value.copy(
                isRunning = true,
                linkedTaskTitle = title
            )
            pomodoroJob = viewModelScope.launch {
                while (_pomodoroState.value.secondsRemaining > 0 && _pomodoroState.value.isRunning) {
                    delay(1000)
                    val remaining = _pomodoroState.value.secondsRemaining - 1
                    _pomodoroState.value = _pomodoroState.value.copy(secondsRemaining = remaining)
                }
                if (_pomodoroState.value.secondsRemaining <= 0) {
                    val completedMins = _pomodoroState.value.totalSeconds / 60
                    val newFocusMins = _dailyFocusMinutes.value + completedMins
                    _dailyFocusMinutes.value = newFocusMins
                    notePrefs.edit().putInt("focus_mins_today", newFocusMins).apply()

                    val nextMode = if (_pomodoroState.value.mode == PomodoroMode.WORK || _pomodoroState.value.mode == PomodoroMode.DEEP_SPRINT) {
                        PomodoroMode.SHORT_BREAK
                    } else {
                        PomodoroMode.WORK
                    }

                    _pomodoroState.value = _pomodoroState.value.copy(
                        isRunning = false,
                        mode = nextMode,
                        secondsRemaining = nextMode.defaultMinutes * 60,
                        totalSeconds = nextMode.defaultMinutes * 60,
                        completedCycles = _pomodoroState.value.completedCycles + 1
                    )
                    _snackbarEvent.emit("Focus session finished! Logged +$completedMins mins. Take a break ☕")
                }
            }
        }
    }

    fun resetPomodoro() {
        pomodoroJob?.cancel()
        val totalSecs = _pomodoroState.value.mode.defaultMinutes * 60
        _pomodoroState.value = _pomodoroState.value.copy(
            isRunning = false,
            secondsRemaining = totalSecs,
            totalSeconds = totalSecs
        )
    }

    // Scratchpad notes
    fun updateScratchpadNotes(content: String) {
        _scratchpadNotes.value = content
        notePrefs.edit().putString("content", content).apply()
    }

    fun convertScratchpadToTasks(): Int {
        val lines = _scratchpadNotes.value.lines()
        var createdCount = 0
        viewModelScope.launch {
            for (rawLine in lines) {
                val cleaned = rawLine.trim().trimStart('•', '-', '*', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', ')').trim()
                if (cleaned.length >= 2) {
                    taskRepository.quickAddTaskWithNlp(cleaned)
                    createdCount++
                }
            }
            if (createdCount > 0) {
                _snackbarEvent.emit("Created $createdCount tasks from scratchpad! 🚀")
            } else {
                _snackbarEvent.emit("No text found to convert to tasks.")
            }
        }
        return createdCount
    }

    fun addTimestampToScratchpad() {
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val current = _scratchpadNotes.value
        val updated = if (current.isBlank()) "[$time] " else "$current\n[$time] "
        updateScratchpadNotes(updated)
    }

    fun clearScratchpad() {
        updateScratchpadNotes("")
        viewModelScope.launch {
            _snackbarEvent.emit("Scratchpad cleared")
        }
    }

    // Custom App Folders & Groupings Management
    fun createOrUpdateFolder(folder: CustomAppFolder) {
        launcherRepository.saveFolder(folder)
        viewModelScope.launch {
            _snackbarEvent.emit("Folder '${folder.name}' saved")
        }
    }

    fun deleteFolder(folderId: String) {
        launcherRepository.deleteFolder(folderId)
        viewModelScope.launch {
            _snackbarEvent.emit("Folder removed")
        }
    }

    fun toggleFolderPinnedToDock(folder: CustomAppFolder) {
        launcherRepository.toggleFolderPinnedToDock(folder.id)
        viewModelScope.launch {
            val status = if (!folder.isPinnedToDock) "pinned to dock" else "unpinned from dock"
            _snackbarEvent.emit("'${folder.name}' $status")
        }
    }

    fun addAppToFolder(folderId: String, packageName: String) {
        launcherRepository.addAppToFolder(folderId, packageName)
        viewModelScope.launch {
            _snackbarEvent.emit("App added to folder")
        }
    }

    fun removeAppFromFolder(folderId: String, packageName: String) {
        launcherRepository.removeAppFromFolder(folderId, packageName)
        viewModelScope.launch {
            _snackbarEvent.emit("App removed from folder")
        }
    }

    fun generateAutomaticPresets() {
        viewModelScope.launch {
            val apps = _installedApps.value
            launcherRepository.generateAutomaticPresets(apps)
            _snackbarEvent.emit("✨ Smart folders generated for Productivity, Work, Social, Finance & Reading!")
        }
    }

    // Dock Customization & Management
    fun setDockApps(packageNames: List<String>) {
        launcherRepository.setDockApps(packageNames)
        viewModelScope.launch {
            _snackbarEvent.emit("Home dock updated")
        }
    }

    fun resetDockAppsToDefault(installedApps: List<InstalledApp>) {
        val preferred = installedApps.filter { app ->
            val p = app.packageName.lowercase()
            p.contains("dialer") || p.contains("phone") || p.contains("message") ||
                    p.contains("chrome") || p.contains("browser") || p.contains("camera")
        }
        val defaultPkgs = (if (preferred.size >= 4) preferred.take(4) else installedApps.take(4)).map { it.packageName }
        launcherRepository.setDockApps(defaultPkgs)
        viewModelScope.launch {
            _snackbarEvent.emit("Home dock reset to defaults")
        }
    }

    fun addAppToDock(packageName: String) {
        launcherRepository.addAppToDock(packageName)
        viewModelScope.launch {
            _snackbarEvent.emit("App pinned to dock")
        }
    }

    fun removeAppFromDock(packageName: String) {
        launcherRepository.removeAppFromDock(packageName)
        viewModelScope.launch {
            _snackbarEvent.emit("App removed from dock")
        }
    }

    // Settings update
    fun updateSettings(newSettings: LauncherSettings) {
        launcherRepository.updateSettings(newSettings)
    }

    suspend fun exportBackupJson(): String = launcherRepository.exportBackupJson()

    suspend fun importBackupJson(json: String): Boolean = launcherRepository.importBackupJson(json)
}

data class TaskCategories(
    val today: List<TaskEntity>,
    val overdue: List<TaskEntity>,
    val upcoming: List<TaskEntity>,
    val completed: List<TaskEntity>
)
