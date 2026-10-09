package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.TaskLaunchDatabase
import com.example.data.model.FocusMode
import com.example.data.model.LauncherSettings
import com.example.data.model.Priority
import com.example.data.model.Recurrence
import com.example.data.model.SubtaskEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskListEntity
import com.example.data.model.WorkspaceItemEntity
import com.example.util.AppManager
import com.example.data.model.CustomAppFolder
import com.example.util.InstalledApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class LauncherRepository(private val context: Context) {

    private val db = TaskLaunchDatabase.getInstance(context)
    private val workspaceDao = db.workspaceDao()
    private val taskDao = db.taskDao()
    private val subtaskDao = db.subtaskDao()
    private val listDao = db.taskListDao()
    private val prefs: SharedPreferences = context.getSharedPreferences("tasklaunch_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings = _settings.asStateFlow()

    private val _customFolders = MutableStateFlow<List<CustomAppFolder>>(loadCustomFolders())
    val customFolders = _customFolders.asStateFlow()

    private val _customDockApps = MutableStateFlow<List<String>>(loadCustomDockApps())
    val customDockApps = _customDockApps.asStateFlow()

    val workspaceItems: Flow<List<WorkspaceItemEntity>> = workspaceDao.getAllItems()

    val appManager = AppManager(context)

    private fun loadSettings(): LauncherSettings {
        val theme = prefs.getString("theme", "OBSIDIAN") ?: "OBSIDIAN"
        val wallpaper = prefs.getString("wallpaper", "NEBULA") ?: "NEBULA"
        val opacity = prefs.getFloat("widget_opacity", 0.85f)
        val density = prefs.getString("widget_density", "COMFORTABLE") ?: "COMFORTABLE"
        val cols = prefs.getInt("cols", 4)
        val rows = prefs.getInt("rows", 5)
        val iconSize = prefs.getInt("icon_size", 56)
        val labels = prefs.getBoolean("show_labels", true)
        val locked = prefs.getBoolean("lock_layout", false)
        val focusModeStr = prefs.getString("focus_mode", FocusMode.OFF.name) ?: FocusMode.OFF.name
        val focusMode = try { FocusMode.valueOf(focusModeStr) } catch (_: Exception) { FocusMode.OFF }
        val zenGlance = prefs.getBoolean("zen_glance_mode", false)
        val compactWidget = prefs.getBoolean("compact_widget", false)
        val searchEngine = prefs.getString("search_engine", "Google") ?: "Google"
        val customWallpaper = prefs.getString("custom_wallpaper_uri", null)
        val scrimAlpha = prefs.getFloat("wallpaper_scrim_alpha", 0.45f)

        return LauncherSettings(
            themePalette = theme,
            wallpaperPreset = wallpaper,
            customWallpaperUri = customWallpaper,
            wallpaperScrimAlpha = scrimAlpha,
            widgetGlassOpacity = opacity,
            widgetDensity = density,
            gridColumns = cols,
            gridRows = rows,
            iconSizeDp = iconSize,
            showLabels = labels,
            lockLayout = locked,
            activeFocusMode = focusMode,
            zenGlanceMode = zenGlance,
            taskWidgetCompact = compactWidget,
            defaultSearchEngine = searchEngine
        )
    }

    fun updateSettings(newSettings: LauncherSettings) {
        _settings.value = newSettings
        prefs.edit()
            .putString("theme", newSettings.themePalette)
            .putString("wallpaper", newSettings.wallpaperPreset)
            .putString("custom_wallpaper_uri", newSettings.customWallpaperUri)
            .putFloat("wallpaper_scrim_alpha", newSettings.wallpaperScrimAlpha)
            .putFloat("widget_opacity", newSettings.widgetGlassOpacity)
            .putString("widget_density", newSettings.widgetDensity)
            .putInt("cols", newSettings.gridColumns)
            .putInt("rows", newSettings.gridRows)
            .putInt("icon_size", newSettings.iconSizeDp)
            .putBoolean("show_labels", newSettings.showLabels)
            .putBoolean("lock_layout", newSettings.lockLayout)
            .putString("focus_mode", newSettings.activeFocusMode.name)
            .putBoolean("zen_glance_mode", newSettings.zenGlanceMode)
            .putBoolean("compact_widget", newSettings.taskWidgetCompact)
            .putString("search_engine", newSettings.defaultSearchEngine)
            .apply()
    }

    fun setFocusMode(mode: FocusMode) {
        updateSettings(_settings.value.copy(activeFocusMode = mode))
    }

    suspend fun addWorkspaceItem(item: WorkspaceItemEntity): Long {
        return workspaceDao.insertItem(item)
    }

    suspend fun removeWorkspaceItem(id: Long) {
        workspaceDao.deleteItemById(id)
    }

    private fun loadCustomFolders(): List<CustomAppFolder> {
        val jsonStr = prefs.getString("custom_app_folders", null)
        if (jsonStr.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<CustomAppFolder>()
            for (i in 0 until arr.length()) {
                try {
                    val obj = arr.getJSONObject(i)
                    val pkgsArr = obj.optJSONArray("packageNames") ?: JSONArray()
                    val pkgs = mutableListOf<String>()
                    for (j in 0 until pkgsArr.length()) {
                        val pkg = pkgsArr.optString(j)
                        if (!pkg.isNullOrBlank()) pkgs.add(pkg)
                    }
                    val folderName = obj.optString("name", "").ifBlank { "Folder ${i + 1}" }
                    list.add(
                        CustomAppFolder(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            name = folderName,
                            iconName = obj.optString("iconName", "FOLDER"),
                            colorHex = obj.optString("colorHex", "#6366F1"),
                            packageNames = pkgs,
                            isPinnedToDock = obj.optBoolean("isPinnedToDock", false),
                            isPreset = obj.optBoolean("isPreset", false)
                        )
                    )
                } catch (_: Exception) {}
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistCustomFolders(folders: List<CustomAppFolder>) {
        _customFolders.value = folders
        val arr = JSONArray()
        for (f in folders) {
            val obj = JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("iconName", f.iconName)
                put("colorHex", f.colorHex)
                put("isPinnedToDock", f.isPinnedToDock)
                put("isPreset", f.isPreset)
                val pkgsArr = JSONArray()
                f.packageNames.forEach { pkgsArr.put(it) }
                put("packageNames", pkgsArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString("custom_app_folders", arr.toString()).apply()
    }

    fun saveFolder(folder: CustomAppFolder) {
        val current = _customFolders.value.toMutableList()
        val idx = current.indexOfFirst { it.id == folder.id }
        if (idx >= 0) {
            current[idx] = folder
        } else {
            current.add(folder)
        }
        persistCustomFolders(current)
    }

    fun deleteFolder(folderId: String) {
        val current = _customFolders.value.filter { it.id != folderId }
        persistCustomFolders(current)
    }

    fun toggleFolderPinnedToDock(folderId: String) {
        val current = _customFolders.value.map {
            if (it.id == folderId) it.copy(isPinnedToDock = !it.isPinnedToDock) else it
        }
        persistCustomFolders(current)
    }

    fun addAppToFolder(folderId: String, packageName: String) {
        val current = _customFolders.value.map {
            if (it.id == folderId && !it.packageNames.contains(packageName)) {
                it.copy(packageNames = it.packageNames + packageName)
            } else it
        }
        persistCustomFolders(current)
    }

    fun removeAppFromFolder(folderId: String, packageName: String) {
        val current = _customFolders.value.map {
            if (it.id == folderId) {
                it.copy(packageNames = it.packageNames.filter { p -> p != packageName })
            } else it
        }
        persistCustomFolders(current)
    }

    fun generateAutomaticPresets(installedApps: List<InstalledApp>) {
        val presets = listOf(
            Triple("Productivity", "#6366F1", "WORK"),
            Triple("Social", "#EC4899", "SOCIAL"),
            Triple("Work", "#3B82F6", "WORK"),
            Triple("Finance", "#10B981", "FINANCE"),
            Triple("Reading", "#F59E0B", "READING")
        )

        val updatedList = _customFolders.value.toMutableList()

        for ((name, colorHex, iconName) in presets) {
            val matchingApps = installedApps.filter { app ->
                when (name) {
                    "Productivity" -> app.category == com.example.util.AppCategory.PRODUCTIVITY
                    "Social" -> app.category == com.example.util.AppCategory.SOCIAL || app.category == com.example.util.AppCategory.COMMUNICATION
                    "Work" -> app.category == com.example.util.AppCategory.WORK
                    "Finance" -> app.category == com.example.util.AppCategory.FINANCE
                    "Reading" -> app.category == com.example.util.AppCategory.READING
                    else -> false
                }
            }.map { it.packageName }.distinct()

            val existingIdx = updatedList.indexOfFirst { it.name.equals(name, ignoreCase = true) }
            if (existingIdx >= 0) {
                // Merge apps
                val existing = updatedList[existingIdx]
                val merged = (existing.packageNames + matchingApps).distinct()
                updatedList[existingIdx] = existing.copy(packageNames = merged)
            } else {
                updatedList.add(
                    CustomAppFolder(
                        name = name,
                        iconName = iconName,
                        colorHex = colorHex,
                        packageNames = matchingApps,
                        isPinnedToDock = (name == "Productivity" || name == "Work"),
                        isPreset = true
                    )
                )
            }
        }
        persistCustomFolders(updatedList)
    }

    private fun loadCustomDockApps(): List<String> {
        val jsonStr = prefs.getString("custom_dock_apps", null)
        if (jsonStr.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setDockApps(packageNames: List<String>) {
        _customDockApps.value = packageNames
        val arr = JSONArray()
        packageNames.forEach { arr.put(it) }
        prefs.edit().putString("custom_dock_apps", arr.toString()).apply()
    }

    fun addAppToDock(packageName: String) {
        if (!_customDockApps.value.contains(packageName)) {
            val updated = _customDockApps.value + packageName
            setDockApps(updated)
        }
    }

    fun removeAppFromDock(packageName: String) {
        val updated = _customDockApps.value.filter { it != packageName }
        setDockApps(updated)
    }

    suspend fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        val tasks = taskDao.getAllTasks().first()
        val tasksArr = JSONArray()
        for (t in tasks) {
            val tObj = JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("notes", t.notes)
                put("isCompleted", t.isCompleted)
                put("priority", t.priority.name)
                put("dueDate", t.dueDate ?: -1L)
                put("dueTimeMinutes", t.dueTimeMinutes ?: -1)
                put("listId", t.listId)
                put("tags", t.tags)
                put("recurrence", t.recurrence.name)
                put("linkedPackageName", t.linkedPackageName ?: "")
            }
            tasksArr.put(tObj)
        }
        root.put("tasks", tasksArr)

        val lists = listDao.getAllLists().first()
        val listsArr = JSONArray()
        for (l in lists) {
            val lObj = JSONObject().apply {
                put("id", l.id)
                put("name", l.name)
                put("colorHex", l.colorHex)
                put("iconName", l.iconName)
                put("isDefault", l.isDefault)
            }
            listsArr.put(lObj)
        }
        root.put("lists", listsArr)

        val workspace = workspaceDao.getAllItems().first()
        val wsArr = JSONArray()
        for (w in workspace) {
            val wObj = JSONObject().apply {
                put("id", w.id)
                put("page", w.page)
                put("itemType", w.itemType)
                put("packageName", w.packageName ?: "")
                put("activityName", w.activityName ?: "")
                put("title", w.title)
                put("gridX", w.gridX)
                put("gridY", w.gridY)
                put("spanX", w.spanX)
                put("spanY", w.spanY)
            }
            wsArr.put(wObj)
        }
        root.put("workspace", wsArr)

        val foldersArr = JSONArray()
        for (f in _customFolders.value) {
            val fObj = JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("iconName", f.iconName)
                put("colorHex", f.colorHex)
                put("isPinnedToDock", f.isPinnedToDock)
                put("isPreset", f.isPreset)
                val pkgsArr = JSONArray()
                f.packageNames.forEach { pkgsArr.put(it) }
                put("packageNames", pkgsArr)
            }
            foldersArr.put(fObj)
        }
        root.put("customAppFolders", foldersArr)

        val dockAppsArr = JSONArray()
        _customDockApps.value.forEach { dockAppsArr.put(it) }
        root.put("customDockApps", dockAppsArr)

        return root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            if (root.has("tasks")) {
                val tasksArr = root.getJSONArray("tasks")
                for (i in 0 until tasksArr.length()) {
                    try {
                        val obj = tasksArr.getJSONObject(i)
                        val title = obj.optString("title", "").ifBlank { "Untitled Task" }
                        val task = TaskEntity(
                            title = title,
                            notes = obj.optString("notes", ""),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            priority = Priority.fromString(obj.optString("priority", "MEDIUM")),
                            dueDate = obj.optLong("dueDate", -1L).takeIf { it > 0 },
                            dueTimeMinutes = obj.optInt("dueTimeMinutes", -1).takeIf { it >= 0 },
                            listId = obj.optLong("listId", 1L),
                            tags = obj.optString("tags", ""),
                            recurrence = Recurrence.fromString(obj.optString("recurrence", "NONE")),
                            linkedPackageName = obj.optString("linkedPackageName", "").takeIf { it.isNotBlank() }
                        )
                        taskDao.insertTask(task)
                    } catch (_: Exception) {}
                }
            }
            if (root.has("customAppFolders")) {
                val foldersArr = root.getJSONArray("customAppFolders")
                val restoredFolders = mutableListOf<CustomAppFolder>()
                for (i in 0 until foldersArr.length()) {
                    try {
                        val fObj = foldersArr.getJSONObject(i)
                        val pkgsArr = fObj.optJSONArray("packageNames") ?: JSONArray()
                        val pkgs = mutableListOf<String>()
                        for (j in 0 until pkgsArr.length()) {
                            val pkg = pkgsArr.optString(j)
                            if (!pkg.isNullOrBlank()) pkgs.add(pkg)
                        }
                        val name = fObj.optString("name", "").ifBlank { "Folder ${i + 1}" }
                        restoredFolders.add(
                            CustomAppFolder(
                                id = fObj.optString("id", java.util.UUID.randomUUID().toString()),
                                name = name,
                                iconName = fObj.optString("iconName", "FOLDER"),
                                colorHex = fObj.optString("colorHex", "#6366F1"),
                                packageNames = pkgs,
                                isPinnedToDock = fObj.optBoolean("isPinnedToDock", false),
                                isPreset = fObj.optBoolean("isPreset", false)
                            )
                        )
                    } catch (_: Exception) {}
                }
                persistCustomFolders(restoredFolders)
            }
            if (root.has("customDockApps")) {
                val dockAppsArr = root.getJSONArray("customDockApps")
                val list = mutableListOf<String>()
                for (i in 0 until dockAppsArr.length()) {
                    val pkg = dockAppsArr.optString(i)
                    if (!pkg.isNullOrBlank()) list.add(pkg)
                }
                setDockApps(list)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
