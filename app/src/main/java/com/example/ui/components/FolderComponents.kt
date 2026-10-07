package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomAppFolder
import com.example.util.InstalledApp

fun getFolderIcon(iconName: String): ImageVector {
    return when (iconName.uppercase()) {
        "WORK" -> Icons.Default.Work
        "SOCIAL" -> Icons.Default.People
        "FINANCE" -> Icons.Default.Payments
        "READING" -> Icons.Default.MenuBook
        "STAR" -> Icons.Default.Star
        "BOLT" -> Icons.Default.Bolt
        else -> Icons.Default.Folder
    }
}

/**
 * Preview icon for a custom folder, rendering a miniature 2x2 grid of app icons inside
 * with the folder's accent tint.
 */
@Composable
fun FolderIconView(
    folder: CustomAppFolder,
    installedApps: List<InstalledApp>,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    showLabel: Boolean = true,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val folderApps = remember(folder.packageNames, installedApps) {
        val set = folder.packageNames.toSet()
        installedApps.filter { it.packageName in set }
    }

    val folderColor = try {
        Color(android.graphics.Color.parseColor(folder.colorHex))
    } catch (_: Throwable) {
        MaterialTheme.colorScheme.primary
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(4.dp)
            .testTag("folder_icon_${folder.id}")
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(16.dp))
                .background(folderColor.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            if (folderApps.isNotEmpty()) {
                // Miniature 2x2 preview of apps inside
                Column(
                    modifier = Modifier.padding(5.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val firstTwo = folderApps.take(2)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        firstTwo.forEach { app ->
                            MiniIcon(app = app)
                        }
                    }
                    if (folderApps.size > 2) {
                        val nextTwo = folderApps.drop(2).take(2)
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            nextTwo.forEach { app ->
                                MiniIcon(app = app)
                            }
                        }
                    }
                }
            } else {
                Icon(
                    imageVector = getFolderIcon(folder.iconName),
                    contentDescription = folder.name,
                    tint = folderColor,
                    modifier = Modifier.size(size * 0.55f)
                )
            }

            // Pinned indicator badge
            if (folder.isPinnedToDock) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(folderColor)
                        .align(Alignment.TopEnd)
                )
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MiniIcon(app: InstalledApp) {
    if (app.iconBitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = app.iconBitmap,
            contentDescription = null,
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        )
    }
}

/**
 * Dialog displaying all apps inside the clicked folder, allowing launching,
 * removing an app, or editing/pinning the folder.
 */
@Composable
fun FolderContentsDialog(
    folder: CustomAppFolder,
    installedApps: List<InstalledApp>,
    onLaunchApp: (String) -> Unit,
    onEditFolder: () -> Unit,
    onTogglePinToDock: () -> Unit,
    onDeleteFolder: () -> Unit,
    onDismiss: () -> Unit,
    isAppRestricted: (InstalledApp) -> Boolean = { false }
) {
    val folderApps = remember(folder.packageNames, installedApps) {
        val set = folder.packageNames.toSet()
        installedApps.filter { it.packageName in set }
    }

    val folderColor = try {
        Color(android.graphics.Color.parseColor(folder.colorHex))
    } catch (_: Throwable) {
        MaterialTheme.colorScheme.primary
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(folderColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getFolderIcon(folder.iconName),
                            contentDescription = null,
                            tint = folderColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${folderApps.size} apps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTogglePinToDock,
                        modifier = Modifier.size(32.dp).testTag("folder_pin_dock_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = if (folder.isPinnedToDock) "Unpin from Dock" else "Pin to Dock",
                            tint = if (folder.isPinnedToDock) folderColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onEditFolder,
                        modifier = Modifier.size(32.dp).testTag("folder_edit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Folder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (folderApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No apps in this folder yet.\nTap Edit to choose apps.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(folderApps, key = { it.packageName }) { app ->
                            AppIconView(
                                app = app,
                                iconSize = 46.dp,
                                showLabel = true,
                                isRestricted = isAppRestricted(app),
                                onClick = {
                                    onLaunchApp(app.packageName)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDeleteFolder()
                    onDismiss()
                }
            ) {
                Text("Delete Folder", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}
