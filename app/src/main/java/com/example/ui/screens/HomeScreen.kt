package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomAppFolder
import com.example.data.model.FocusMode
import com.example.data.model.Priority
import com.example.data.model.SubtaskEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskWithSubtasks
import com.example.ui.components.AppIconView
import com.example.ui.components.DockDragGestureOverlay
import com.example.ui.components.DockTargetItem
import com.example.ui.components.FolderContentsDialog
import com.example.ui.components.FolderEditorDialog
import com.example.ui.components.FolderIconView
import com.example.ui.components.SmartStrip
import com.example.ui.components.TaskItemView
import com.example.ui.viewmodel.PomodoroState
import com.example.util.InstalledApp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    todayTasks: List<TaskEntity>,
    nextPendingTask: TaskEntity?,
    tasksWithSubtasks: List<TaskWithSubtasks>,
    installedApps: List<InstalledApp>,
    customFolders: List<CustomAppFolder> = emptyList(),
    activeFocusMode: FocusMode,
    pendingTasksCount: Int,
    isZenGlanceMode: Boolean = false,
    onToggleZenGlanceMode: () -> Unit = {},
    pomodoroState: PomodoroState = PomodoroState(),
    onTogglePomodoro: () -> Unit = {},
    isAppRestricted: (InstalledApp) -> Boolean = { false },
    onOpenSearch: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTaskDetail: (TaskWithSubtasks?) -> Unit,
    onToggleTaskComplete: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onSnoozeTask: (TaskEntity, Int) -> Unit,
    onToggleSubtask: (SubtaskEntity) -> Unit,
    onLaunchApp: (String) -> Unit,
    onAppLongClick: (InstalledApp) -> Unit,
    onUpdateFolder: (CustomAppFolder) -> Unit = {},
    onDeleteFolder: (String) -> Unit = {},
    onToggleFolderPinToDock: (CustomAppFolder) -> Unit = {},
    onJumpToTaskFeed: () -> Unit,
    onJumpToProductivity: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Current time ticker
    var currentTime by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = Date()
        }
    }

    val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault()).format(currentTime)
    val amPmFormat = SimpleDateFormat("a", Locale.getDefault()).format(currentTime)
    val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(currentTime)

    val subtasksByTaskId = remember(tasksWithSubtasks) {
        tasksWithSubtasks.associate { it.task.id to it.subtasks }
    }

    // Determine the single highest priority task right now for Zen Glance mode
    val singleHighestPriorityTask = remember(todayTasks, nextPendingTask) {
        val pending = todayTasks.filter { !it.isCompleted }
        pending.firstOrNull { it.priority == Priority.URGENT }
            ?: pending.firstOrNull { it.priority == Priority.HIGH }
            ?: nextPendingTask
            ?: pending.firstOrNull()
    }

    // Pinned Dock apps: take Phone, Messages, Camera, Chrome, or first 4 apps
    val dockApps = remember(installedApps) {
        val preferred = installedApps.filter { app ->
            val p = app.packageName.lowercase()
            p.contains("dialer") || p.contains("phone") || p.contains("message") ||
                    p.contains("chrome") || p.contains("browser") || p.contains("camera")
        }
        if (preferred.size >= 4) preferred.take(4) else installedApps.take(4)
    }

    val pinnedFolders = remember(customFolders) {
        customFolders.filter { it.isPinnedToDock }
    }

    var openedFolderInHome by remember { mutableStateOf<CustomAppFolder?>(null) }
    var editingFolderInHome by remember { mutableStateOf<CustomAppFolder?>(null) }

    // Swipe down on empty areas to toggle Zen Glance Mode
    var dragAccumulator by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen")
            .pointerInput(isZenGlanceMode) {
                detectVerticalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDragEnd = {
                        if (dragAccumulator > 120f) {
                            // Swiped down -> Toggle Zen Glance mode
                            onToggleZenGlanceMode()
                        } else if (dragAccumulator < -120f && isZenGlanceMode) {
                            // Swiped up in Zen Glance -> Exit to standard mode
                            onToggleZenGlanceMode()
                        }
                        dragAccumulator = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        dragAccumulator += dragAmount
                    }
                )
            }
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP: Settings button, Zen toggle, Clock & Universal Search Bar
        Column {
            Spacer(modifier = Modifier.height(16.dp))

            // Status bar row: Focus mode chip, Zen Glance toggle & Settings icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (activeFocusMode != FocusMode.OFF || pomodoroState.isRunning) {
                        val modeColor = try {
                            Color(android.graphics.Color.parseColor(activeFocusMode.colorHex))
                        } catch (_: Throwable) {
                            MaterialTheme.colorScheme.primary
                        }
                        val displayText = if (pomodoroState.isRunning) {
                            val mins = pomodoroState.secondsRemaining / 60
                            val secs = pomodoroState.secondsRemaining % 60
                            "⏱ ${String.format(Locale.getDefault(), "%02d:%02d", mins, secs)} • ${activeFocusMode.title}"
                        } else {
                            "Focus: ${activeFocusMode.title}"
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = modeColor.copy(alpha = 0.2f),
                            contentColor = modeColor,
                            modifier = Modifier.clickable { onJumpToProductivity() }
                        ) {
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Zen Glance Mode Quick Toggle Indicator
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isZenGlanceMode) Color(0xFF10B981).copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        contentColor = if (isZenGlanceMode) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { onToggleZenGlanceMode() }
                            .testTag("zen_glance_toggle_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isZenGlanceMode) Icons.Default.Spa else Icons.Default.SelfImprovement,
                                contentDescription = "Zen Glance Toggle",
                                tint = if (isZenGlanceMode) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isZenGlanceMode) "Zen Active" else "Zen Glance",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("home_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Digital Clock
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    text = timeFormat,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isZenGlanceMode) 68.sp else 58.sp,
                        letterSpacing = (-1.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = amPmFormat,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }

            // Date & Task count badge (only when not in Zen Glance mode, or as minimalist date in Zen)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateFormat,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isZenGlanceMode) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (pendingTasksCount > 0) MaterialTheme.colorScheme.primary else Color(0xFF10B981),
                        modifier = Modifier
                            .clickable { onJumpToTaskFeed() }
                            .testTag("task_counter_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TaskAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (pendingTasksCount > 0) "$pendingTasksCount due" else "All done",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Active Pomodoro Live Timer Banner (requirement: if focus is active, show timer on home screen)
            if (pomodoroState.isRunning) {
                Spacer(modifier = Modifier.height(10.dp))
                val pMinutes = pomodoroState.secondsRemaining / 60
                val pSeconds = pomodoroState.secondsRemaining % 60
                val pTimeFormatted = String.format(Locale.getDefault(), "%02d:%02d", pMinutes, pSeconds)
                val focusTaskName = pomodoroState.linkedTaskTitle ?: "Deep Sprint Session"

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onJumpToProductivity() }
                        .testTag("home_focus_timer_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Focus Active: $pTimeFormatted",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "$focusTaskName • Restricting distracting apps",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onTogglePomodoro,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause Session",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtle Universal Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSearch() }
                    .testTag("universal_search_bar"),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isZenGlanceMode) 0.55f else 0.85f),
                tonalElevation = if (isZenGlanceMode) 1.dp else 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = if (isZenGlanceMode) 10.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isZenGlanceMode) "Search or add task…" else "Search apps, tasks, or add task…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // ==========================================
        // MIDDLE: ZEN GLANCE SINGLE-FOCUS VS FULL TODAY WIDGET
        // ==========================================
        if (isZenGlanceMode) {
            // Minimalist Single Highest-Priority Task View
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 14.dp)
                    .testTag("zen_single_task_widget"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SINGLE FOCUS NOW",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = Color(0xFF10B981)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (singleHighestPriorityTask != null) {
                        Text(
                            text = singleHighestPriorityTask.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (singleHighestPriorityTask.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = singleHighestPriorityTask.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .clickable { onToggleTaskComplete(singleHighestPriorityTask) }
                                    .testTag("zen_complete_task_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Complete",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Complete",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clickable {
                                        val fullTask = tasksWithSubtasks.firstOrNull { it.task.id == singleHighestPriorityTask.id }
                                            ?: TaskWithSubtasks(singleHighestPriorityTask, emptyList())
                                        onOpenTaskDetail(fullTask)
                                    }
                            ) {
                                Text(
                                    text = "Details",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No pending tasks right now",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enjoy your mindful focus or take a breath 🌿",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = "Swipe down or tap Zen Active to exit Single Focus",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
            }
        } else {
            // Standard Today's Task Widget (Requirement TH-01)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 10.dp)
                    .testTag("home_task_widget"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Today\'s Tasks",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onOpenTaskDetail(null) },
                                modifier = Modifier.size(28.dp).testTag("widget_add_task_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Task",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View Feed",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onJumpToTaskFeed() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (todayTasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No pending tasks for today 🎉",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(todayTasks.take(4), key = { it.id }) { task ->
                                val taskSubtasks = subtasksByTaskId[task.id] ?: emptyList()
                                TaskItemView(
                                    task = task,
                                    subtasks = taskSubtasks,
                                    onToggleComplete = onToggleTaskComplete,
                                    onClick = {
                                        val fullTask = tasksWithSubtasks.firstOrNull { it.task.id == task.id }
                                            ?: TaskWithSubtasks(task, taskSubtasks)
                                        onOpenTaskDetail(fullTask)
                                    },
                                    onDelete = onDeleteTask,
                                    onSnooze = onSnoozeTask,
                                    onToggleSubtask = onToggleSubtask,
                                    onLaunchLinkedApp = onLaunchApp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // BOTTOM: SMART STRIP & PINNED DOCK (Stripped in Zen Glance mode for zero distraction)
        // ==========================================
        if (isZenGlanceMode) {
            // Subtle Zen bottom control: Minimal app drawer quick-pill or exit Zen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clickable { onOpenAppDrawer() }
                        .testTag("zen_app_drawer_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "Apps",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Apps",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        } else {
            Column {
                // Next-Up Smart Strip above dock (Requirement TH-06)
                SmartStrip(
                    nextTask = nextPendingTask,
                    onCompleteTask = onToggleTaskComplete,
                    onClickTask = { task ->
                        val fullTask = tasksWithSubtasks.firstOrNull { it.task.id == task.id }
                            ?: TaskWithSubtasks(task, emptyList())
                        onOpenTaskDetail(fullTask)
                    },
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Pinned Bottom Dock (Requirement LC-03)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("home_dock"),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pinned Apps
                        dockApps.forEach { app ->
                            AppIconView(
                                app = app,
                                iconSize = 46.dp,
                                showLabel = false,
                                isRestricted = isAppRestricted(app),
                                onClick = { onLaunchApp(app.packageName) },
                                onLongClick = { onAppLongClick(app) }
                            )
                        }

                        // Pinned Custom Folders (e.g. Work, Productivity groups)
                        pinnedFolders.forEach { folder ->
                            FolderIconView(
                                folder = folder,
                                installedApps = installedApps,
                                size = 46.dp,
                                showLabel = false,
                                onClick = { openedFolderInHome = folder },
                                onLongClick = { editingFolderInHome = folder }
                            )
                        }

                        // App Drawer Trigger Button
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .clickable { onOpenAppDrawer() }
                                .testTag("dock_app_drawer_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = "App Drawer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Contents dialog for clicked folder in home dock
    if (openedFolderInHome != null) {
        val currentFolder = customFolders.firstOrNull { it.id == openedFolderInHome!!.id } ?: openedFolderInHome!!
        FolderContentsDialog(
            folder = currentFolder,
            installedApps = installedApps,
            onLaunchApp = { pkg ->
                onLaunchApp(pkg)
                openedFolderInHome = null
            },
            onEditFolder = {
                editingFolderInHome = currentFolder
                openedFolderInHome = null
            },
            onTogglePinToDock = {
                onToggleFolderPinToDock(currentFolder)
            },
            onDeleteFolder = {
                onDeleteFolder(currentFolder.id)
                openedFolderInHome = null
            },
            onDismiss = { openedFolderInHome = null },
            isAppRestricted = isAppRestricted
        )
    }

    // Edit Existing Folder Dialog from Home
    if (editingFolderInHome != null) {
        FolderEditorDialog(
            initialFolder = editingFolderInHome,
            installedApps = installedApps,
            onSave = { updatedFolder ->
                onUpdateFolder(updatedFolder)
                editingFolderInHome = null
            },
            onDismiss = { editingFolderInHome = null }
        )
    }
}
