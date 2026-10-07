package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TaskWithSubtasks
import com.example.ui.screens.AppDrawerSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LauncherSettingsScreen
import com.example.ui.screens.ProductivityScreen
import com.example.ui.screens.TaskDetailDialog
import com.example.ui.screens.TaskFeedScreen
import com.example.ui.screens.UniversalSearchDialog
import com.example.ui.theme.TaskLaunchTheme
import com.example.ui.viewmodel.LauncherViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle shared text to create a task (Requirement IN-02)
        handleSendIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            TaskLaunchTheme(themeName = settings.themePalette) {
                LauncherRootApp(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkDefaultLauncherStatus()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSendIntent(intent)
        viewModel.loadInstalledApps()
        viewModel.checkDefaultLauncherStatus()
    }

    private fun handleSendIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.onQuickAddTextChanged(sharedText)
                viewModel.submitQuickAdd()
            }
        }
    }
}

@Composable
fun LauncherRootApp(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Observers
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isSearchOpen by viewModel.isSearchOpen.collectAsStateWithLifecycle()
    val isAppDrawerOpen by viewModel.isAppDrawerOpen.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val isTaskDetailOpen by viewModel.isTaskDetailOpen.collectAsStateWithLifecycle()
    val editingTask by viewModel.editingTask.collectAsStateWithLifecycle()
    val selectedAppForMenu by viewModel.selectedAppForMenu.collectAsStateWithLifecycle()

    val isDefaultLauncher by viewModel.isDefaultLauncher.collectAsStateWithLifecycle()

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                try {
                    notificationPermissionLauncher.launch(permission)
                } catch (_: Throwable) {}
            }
        }
    }

    val defaultHomeLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkDefaultLauncherStatus()
    }

    // Installed apps & folders
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val customFolders by viewModel.customFolders.collectAsStateWithLifecycle()
    val customDockApps by viewModel.customDockApps.collectAsStateWithLifecycle()
    val categorizedTasks by viewModel.categorizedTasks.collectAsStateWithLifecycle()
    val allTasksWithSubtasks by viewModel.allTasksWithSubtasks.collectAsStateWithLifecycle()
    val allLists by viewModel.allLists.collectAsStateWithLifecycle()
    val nextPendingTask by viewModel.nextPendingTask.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val drawerQuery by viewModel.drawerQuery.collectAsStateWithLifecycle()
    val drawerCategory by viewModel.drawerCategory.collectAsStateWithLifecycle()
    val quickAddText by viewModel.quickAddText.collectAsStateWithLifecycle()
    val parsedPreview by viewModel.parsedPreview.collectAsStateWithLifecycle()
    val selectedListFilter by viewModel.selectedListFilter.collectAsStateWithLifecycle()
    val selectedPriorityFilter by viewModel.selectedPriorityFilter.collectAsStateWithLifecycle()

    val pomodoroState by viewModel.pomodoroState.collectAsStateWithLifecycle()
    val scratchpadNotes by viewModel.scratchpadNotes.collectAsStateWithLifecycle()
    val dailyFocusMinutes by viewModel.dailyFocusMinutes.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()

    // 3 Horizontal workspace pages: Page 0 = Task Feed, Page 1 = Main Home, Page 2 = Productivity
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })

    // Listen to snackbar events
    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Android Back button handler
    BackHandler(enabled = isSettingsOpen || isSearchOpen || isAppDrawerOpen || isTaskDetailOpen || pagerState.currentPage != 1) {
        when {
            isSettingsOpen -> viewModel.closeSettings()
            isSearchOpen -> viewModel.closeSearch()
            isAppDrawerOpen -> viewModel.closeAppDrawer()
            isTaskDetailOpen -> viewModel.closeTaskDetail()
            pagerState.currentPage != 1 -> scope.launch { pagerState.animateScrollToPage(1) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag("launcher_root_app")
    ) { innerPadding ->
        if (isSettingsOpen) {
            LauncherSettingsScreen(
                settings = settings,
                isDefaultLauncher = isDefaultLauncher,
                onRequestDefaultLauncher = {
                    var launched = false
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
                            if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)) {
                                val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
                                defaultHomeLauncher.launch(intent)
                                launched = true
                            }
                        }
                    } catch (_: Throwable) {}

                    if (!launched) {
                        try {
                            val homeSettingsIntent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                            defaultHomeLauncher.launch(homeSettingsIntent)
                            launched = true
                        } catch (_: Throwable) {}
                    }

                    if (!launched) {
                        try {
                            val manageAppsIntent = Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                            defaultHomeLauncher.launch(manageAppsIntent)
                            launched = true
                        } catch (_: Throwable) {}
                    }

                    if (!launched) {
                        try {
                            val settingsIntent = Intent(android.provider.Settings.ACTION_SETTINGS)
                            defaultHomeLauncher.launch(settingsIntent)
                        } catch (_: Throwable) {}
                    }
                },
                onUpdateSettings = { viewModel.updateSettings(it) },
                onExportBackup = { viewModel.exportBackupJson() },
                onImportBackup = { viewModel.importBackupJson(it) },
                onBack = { viewModel.closeSettings() }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Wallpaper: Custom Gallery Photo or Selected Palette Brush
                if (settings.wallpaperPreset.equals("CUSTOM", ignoreCase = true) && !settings.customWallpaperUri.isNullOrBlank()) {
                    AsyncImage(
                        model = settings.customWallpaperUri,
                        contentDescription = "Custom Wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Dimming scrim overlay to keep text and widgets sharp and readable
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = settings.wallpaperScrimAlpha))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(com.example.ui.theme.getWallpaperBrush(settings.wallpaperPreset))
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Workspace Pager (Pages 0, 1, 2)
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { page ->
                        when (page) {
                            0 -> TaskFeedScreen(
                                categories = categorizedTasks,
                                tasksWithSubtasks = allTasksWithSubtasks,
                                allLists = allLists,
                                selectedListFilter = selectedListFilter,
                                selectedPriorityFilter = selectedPriorityFilter,
                                quickAddText = quickAddText,
                                onQuickAddTextChanged = { viewModel.onQuickAddTextChanged(it) },
                                onSubmitQuickAdd = { viewModel.submitQuickAdd() },
                                parsedPreview = parsedPreview,
                                onToggleTaskComplete = { viewModel.toggleTaskCompletion(it) },
                                onDeleteTask = { viewModel.deleteTask(it) },
                                onSnoozeTask = { task, min -> viewModel.snoozeTask(task, min) },
                                onToggleSubtask = { viewModel.toggleSubtask(it) },
                                onOpenTaskDetail = { viewModel.openTaskDetail(it) },
                                onSelectListFilter = { viewModel.selectListFilter(it) },
                                onSelectPriorityFilter = { viewModel.selectPriorityFilter(it) },
                                onRescheduleOverdue = { viewModel.rescheduleOverdueToToday(it) },
                                onLaunchApp = { viewModel.launchApp(it) },
                                availableTemplates = viewModel.availableTemplates,
                                onApplyTemplate = { viewModel.createFromTemplate(it) }
                            )

                            1 -> HomeScreen(
                                todayTasks = categorizedTasks.today,
                                nextPendingTask = nextPendingTask,
                                tasksWithSubtasks = allTasksWithSubtasks,
                                installedApps = installedApps,
                                customFolders = customFolders,
                                customDockApps = customDockApps,
                                onSaveDockApps = { viewModel.setDockApps(it) },
                                onResetDockApps = { viewModel.resetDockAppsToDefault(installedApps) },
                                activeFocusMode = settings.activeFocusMode,
                                pendingTasksCount = pendingCount,
                                isZenGlanceMode = settings.zenGlanceMode,
                                onToggleZenGlanceMode = { viewModel.toggleZenGlanceMode() },
                                pomodoroState = pomodoroState,
                                onTogglePomodoro = { viewModel.togglePomodoro() },
                                isAppRestricted = { viewModel.isAppRestricted(it) },
                                onOpenSearch = { viewModel.openSearch() },
                                onOpenAppDrawer = { viewModel.openAppDrawer() },
                                onOpenSettings = { viewModel.openSettings() },
                                onOpenTaskDetail = { viewModel.openTaskDetail(it) },
                                onToggleTaskComplete = { viewModel.toggleTaskCompletion(it) },
                                onDeleteTask = { viewModel.deleteTask(it) },
                                onSnoozeTask = { task, min -> viewModel.snoozeTask(task, min) },
                                onToggleSubtask = { viewModel.toggleSubtask(it) },
                                onLaunchApp = { viewModel.launchApp(it) },
                                onAppLongClick = { viewModel.openAppMenu(it) },
                                onUpdateFolder = { viewModel.createOrUpdateFolder(it) },
                                onDeleteFolder = { viewModel.deleteFolder(it) },
                                onToggleFolderPinToDock = { viewModel.toggleFolderPinnedToDock(it) },
                                onJumpToTaskFeed = { scope.launch { pagerState.animateScrollToPage(0) } },
                                onJumpToProductivity = { scope.launch { pagerState.animateScrollToPage(2) } }
                            )

                            2 -> ProductivityScreen(
                                activeFocusMode = settings.activeFocusMode,
                                onSelectFocusMode = { viewModel.setFocusMode(it) },
                                pomodoroState = pomodoroState,
                                onTogglePomodoro = { viewModel.togglePomodoro() },
                                onResetPomodoro = { viewModel.resetPomodoro() },
                                onSelectPomodoroMode = { viewModel.setPomodoroMode(it) },
                                onAddPomodoroMinutes = { viewModel.addPomodoroMinutes(it) },
                                onCompletePomodoroTask = { viewModel.completeCurrentTaskAndPomodoro() },
                                onLinkPomodoroTask = { viewModel.linkTaskToPomodoro(it) },
                                dailyFocusMinutes = dailyFocusMinutes,
                                habits = habits,
                                onToggleHabit = { viewModel.toggleHabit(it) },
                                scratchpadNotes = scratchpadNotes,
                                onScratchpadNotesChanged = { viewModel.updateScratchpadNotes(it) },
                                onConvertScratchpadToTasks = { viewModel.convertScratchpadToTasks() },
                                onAddTimestampToScratchpad = { viewModel.addTimestampToScratchpad() },
                                onClearScratchpad = { viewModel.clearScratchpad() },
                                allTasks = categorizedTasks.today + categorizedTasks.overdue + categorizedTasks.upcoming + categorizedTasks.completed,
                                onSelectPriorityFilter = { priority ->
                                    viewModel.selectPriorityFilter(priority)
                                    scope.launch { pagerState.animateScrollToPage(0) }
                                }
                            )
                        }
                    }

                    // Subtly visible Workspace Page Indicator Dots (hidden in Zen Glance Mode on home screen)
                    if (!settings.zenGlanceMode || pagerState.currentPage != 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(3) { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(if (isSelected) 8.dp else 5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                        )
                                        .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Universal Search Sheet
    if (isSearchOpen) {
        UniversalSearchDialog(
            searchQuery = searchQuery,
            onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
            installedApps = installedApps,
            allTasks = categorizedTasks.today + categorizedTasks.overdue + categorizedTasks.upcoming,
            onLaunchApp = { viewModel.launchApp(it) },
            onToggleTask = { viewModel.toggleTaskCompletion(it) },
            onQuickAddTask = { text ->
                viewModel.onQuickAddTextChanged(text)
                viewModel.submitQuickAdd()
            },
            isAppRestricted = { viewModel.isAppRestricted(it) },
            onDismiss = { viewModel.closeSearch() }
        )
    }

    // App Drawer Sheet
    if (isAppDrawerOpen) {
        AppDrawerSheet(
            installedApps = installedApps,
            customFolders = customFolders,
            searchQuery = drawerQuery,
            onSearchQueryChanged = { viewModel.onDrawerQueryChanged(it) },
            selectedCategory = drawerCategory,
            onCategoryChanged = { viewModel.onDrawerCategoryChanged(it) },
            selectedAppForMenu = selectedAppForMenu,
            onOpenAppMenu = { viewModel.openAppMenu(it) },
            onCloseAppMenu = { viewModel.closeAppMenu() },
            onLaunchApp = { viewModel.launchApp(it) },
            onOpenAppInfo = { viewModel.openAppInfo(it) },
            onUninstallApp = { viewModel.uninstallApp(it) },
            isAppRestricted = { viewModel.isAppRestricted(it) },
            onCreateOrUpdateFolder = { viewModel.createOrUpdateFolder(it) },
            onDeleteFolder = { viewModel.deleteFolder(it) },
            onToggleFolderPinToDock = { viewModel.toggleFolderPinnedToDock(it) },
            onAddAppToFolder = { folderId, pkg -> viewModel.addAppToFolder(folderId, pkg) },
            onRemoveAppFromFolder = { folderId, pkg -> viewModel.removeAppFromFolder(folderId, pkg) },
            onGenerateAutomaticPresets = { viewModel.generateAutomaticPresets() },
            onLinkTaskToApp = { app ->
                viewModel.openTaskDetail(
                    TaskWithSubtasks(
                        task = com.example.data.model.TaskEntity(
                            title = "Open ${app.label}",
                            linkedPackageName = app.packageName
                        ),
                        subtasks = emptyList()
                    )
                )
            },
            onDismiss = { viewModel.closeAppDrawer() }
        )
    }

    // Task Detail / Create Dialog
    if (isTaskDetailOpen) {
        TaskDetailDialog(
            taskWithSubtasks = editingTask,
            allLists = allLists,
            installedApps = installedApps,
            onSave = { id, title, notes, priority, dueDate, dueTimeMinutes, listId, tags, recurrence, linkedPackage, reminderTime, subtasks ->
                viewModel.saveTaskDetail(
                    id = id,
                    title = title,
                    notes = notes,
                    priority = priority,
                    dueDate = dueDate,
                    dueTimeMinutes = dueTimeMinutes,
                    listId = listId,
                    tags = tags,
                    recurrence = recurrence,
                    linkedPackage = linkedPackage,
                    reminderTime = reminderTime,
                    subtasks = subtasks
                )
            },
            onDelete = { id ->
                val task = editingTask?.task
                if (task != null) viewModel.deleteTask(task)
            },
            onDismiss = { viewModel.closeTaskDetail() }
        )
    }
}
