package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomAppFolder
import com.example.ui.components.AddToFolderDialog
import com.example.ui.components.AppIconView
import com.example.ui.components.FolderContentsDialog
import com.example.ui.components.FolderEditorDialog
import com.example.ui.components.FolderIconView
import com.example.util.AppCategory
import com.example.util.InstalledApp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerSheet(
    installedApps: List<InstalledApp>,
    customFolders: List<CustomAppFolder> = emptyList(),
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedCategory: AppCategory,
    onCategoryChanged: (AppCategory) -> Unit,
    selectedAppForMenu: InstalledApp?,
    onOpenAppMenu: (InstalledApp) -> Unit,
    onCloseAppMenu: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onOpenAppInfo: (String) -> Unit,
    onUninstallApp: (String) -> Unit,
    onLinkTaskToApp: (InstalledApp) -> Unit,
    onCreateOrUpdateFolder: (CustomAppFolder) -> Unit = {},
    onDeleteFolder: (String) -> Unit = {},
    onToggleFolderPinToDock: (CustomAppFolder) -> Unit = {},
    onAddAppToFolder: (folderId: String, packageName: String) -> Unit = { _, _ -> },
    onRemoveAppFromFolder: (folderId: String, packageName: String) -> Unit = { _, _ -> },
    onGenerateAutomaticPresets: () -> Unit = {},
    isAppRestricted: (InstalledApp) -> Boolean = { false },
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    // State for viewing/editing custom folders
    var openedFolder by remember { mutableStateOf<CustomAppFolder?>(null) }
    var editingFolder by remember { mutableStateOf<CustomAppFolder?>(null) }
    var isCreatingFolder by remember { mutableStateOf(false) }
    var appToAddToFolder by remember { mutableStateOf<InstalledApp?>(null) }

    val filteredApps = remember(installedApps, searchQuery, selectedCategory) {
        installedApps.filter { app ->
            val matchQuery = searchQuery.isBlank() ||
                    app.label.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            val matchCategory = selectedCategory == AppCategory.ALL || app.category == selectedCategory
            matchQuery && matchCategory
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        },
        modifier = Modifier.testTag("app_drawer_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            // Search Bar & Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = {
                        Text(
                            text = "Search apps…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("drawer_search_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    modifier = Modifier.testTag("close_drawer_btn")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Custom Folders & Smart Presets Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CUSTOM FOLDERS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Auto Presets button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier
                            .clickable { onGenerateAutomaticPresets() }
                            .testTag("auto_presets_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Auto Presets",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // New Folder Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { isCreatingFolder = true }
                            .testTag("new_folder_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Custom Folders Horizontal Strip
            if (customFolders.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_folders_row")
                ) {
                    items(customFolders, key = { it.id }) { folder ->
                        FolderIconView(
                            folder = folder,
                            installedApps = installedApps,
                            size = 54.dp,
                            showLabel = true,
                            onClick = { openedFolder = folder },
                            onLongClick = { editingFolder = folder }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Category Filter Row (All, Recent, Productivity, Work, Social, Communication, Finance, Reading, Media, Tools, Games)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("app_categories_row")
            ) {
                items(AppCategory.entries.toTypedArray()) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategoryChanged(cat) },
                        label = { Text(cat.title) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main App Grid (4 Columns) with alphabet scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = gridState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                        .testTag("app_drawer_grid"),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppIconView(
                            app = app,
                            iconSize = 52.dp,
                            showLabel = true,
                            isRestricted = isAppRestricted(app),
                            onClick = {
                                onLaunchApp(app.packageName)
                                onDismiss()
                            },
                            onLongClick = { onOpenAppMenu(app) }
                        )
                    }
                }

                // A-Z Quick Jump strip
                Column(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val alphabet = ('A'..'Z').toList()
                    alphabet.forEach { letter ->
                        Text(
                            text = letter.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .clickable {
                                    val targetIndex = filteredApps.indexOfFirst {
                                        it.label.startsWith(letter, ignoreCase = true)
                                    }
                                    if (targetIndex >= 0) {
                                        scope.launch { gridState.scrollToItem(targetIndex) }
                                    }
                                }
                                .padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }

    // Long Press Context Menu Dialog for App
    if (selectedAppForMenu != null) {
        AlertDialog(
            onDismissRequest = onCloseAppMenu,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedAppForMenu.iconBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = selectedAppForMenu.iconBitmap,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(text = selectedAppForMenu.label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Open App
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLaunchApp(selectedAppForMenu.packageName)
                                onCloseAppMenu()
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Launch, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Open App")
                    }

                    // Add to Folder
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                appToAddToFolder = selectedAppForMenu
                                onCloseAppMenu()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Add to Folder…")
                    }

                    // App Info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenAppInfo(selectedAppForMenu.packageName)
                                onCloseAppMenu()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("App Info")
                    }

                    // Uninstall
                    if (!selectedAppForMenu.isSystemApp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUninstallApp(selectedAppForMenu.packageName)
                                    onCloseAppMenu()
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Uninstall", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onCloseAppMenu) {
                    Text("Close")
                }
            }
        )
    }

    // Contents dialog for clicked folder
    if (openedFolder != null) {
        val currentFolder = customFolders.firstOrNull { it.id == openedFolder!!.id } ?: openedFolder!!
        FolderContentsDialog(
            folder = currentFolder,
            installedApps = installedApps,
            onLaunchApp = { pkg ->
                onLaunchApp(pkg)
                openedFolder = null
                onDismiss()
            },
            onEditFolder = {
                editingFolder = currentFolder
                openedFolder = null
            },
            onTogglePinToDock = {
                onToggleFolderPinToDock(currentFolder)
            },
            onDeleteFolder = {
                onDeleteFolder(currentFolder.id)
                openedFolder = null
            },
            onDismiss = { openedFolder = null },
            isAppRestricted = isAppRestricted
        )
    }

    // Create New Folder Dialog
    if (isCreatingFolder) {
        FolderEditorDialog(
            initialFolder = null,
            installedApps = installedApps,
            onSave = { newFolder ->
                onCreateOrUpdateFolder(newFolder)
                isCreatingFolder = false
            },
            onDismiss = { isCreatingFolder = false }
        )
    }

    // Edit Existing Folder Dialog
    if (editingFolder != null) {
        FolderEditorDialog(
            initialFolder = editingFolder,
            installedApps = installedApps,
            onSave = { updatedFolder ->
                onCreateOrUpdateFolder(updatedFolder)
                editingFolder = null
            },
            onDismiss = { editingFolder = null }
        )
    }

    // Add to Folder Dialog from app menu
    if (appToAddToFolder != null) {
        AddToFolderDialog(
            app = appToAddToFolder!!,
            folders = customFolders,
            onAddToFolder = { folderId ->
                onAddAppToFolder(folderId, appToAddToFolder!!.packageName)
            },
            onRemoveFromFolder = { folderId ->
                onRemoveAppFromFolder(folderId, appToAddToFolder!!.packageName)
            },
            onCreateNewFolder = {
                isCreatingFolder = true
            },
            onDismiss = { appToAddToFolder = null }
        )
    }
}
