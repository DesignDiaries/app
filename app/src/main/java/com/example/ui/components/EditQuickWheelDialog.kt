package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.InstalledApp

/**
 * Dialog allowing the user to customize the apps that appear on the Spinning / Rotating App Wheel.
 */
@Composable
fun EditQuickWheelDialog(
    currentWheelPackages: List<String>,
    installedApps: List<InstalledApp>,
    onSaveWheelApps: (List<String>) -> Unit,
    onResetToDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    val defaultPackages = remember(installedApps) {
        val preferred = installedApps.filter { app ->
            val p = app.packageName.lowercase()
            p.contains("dialer") || p.contains("phone") || p.contains("message") ||
                    p.contains("chrome") || p.contains("browser") || p.contains("camera") ||
                    p.contains("photo") || p.contains("gallery") || p.contains("mail") ||
                    p.contains("map") || p.contains("clock")
        }
        (if (preferred.size >= 6) preferred.take(6) else installedApps.take(6)).map { it.packageName }
    }

    var wheelList by remember(currentWheelPackages, defaultPackages) {
        mutableStateOf(
            if (currentWheelPackages.isNotEmpty()) currentWheelPackages else defaultPackages
        )
    }
    var searchFilter by remember { mutableStateOf("") }
    var isAddingApp by remember { mutableStateOf(false) }

    val appMap = remember(installedApps) {
        installedApps.associateBy { it.packageName }
    }

    val availableAppsToAdd = remember(installedApps, wheelList, searchFilter) {
        val inWheel = wheelList.toSet()
        installedApps.filter {
            it.packageName !in inWheel && (searchFilter.isBlank() || it.label.contains(searchFilter, ignoreCase = true))
        }
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
                    Icon(
                        imageVector = Icons.Default.RotateRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Edit Spinning Wheel",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(
                    onClick = {
                        wheelList = defaultPackages
                    },
                    modifier = Modifier.size(28.dp).testTag("reset_wheel_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset to Defaults",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wheel Apps (${wheelList.size} / 8 max):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Drag to quick-select",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Current wheel items list with reorder and delete
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(wheelList) { pkg ->
                        val app = appMap[pkg]
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (app?.iconBitmap != null) {
                                        Image(
                                            bitmap = app.iconBitmap,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(32.dp)
                                        ) {}
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = app?.label ?: pkg,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val idx = wheelList.indexOf(pkg)
                                    IconButton(
                                        onClick = {
                                            if (idx > 0) {
                                                val m = wheelList.toMutableList()
                                                val item = m.removeAt(idx)
                                                m.add(idx - 1, item)
                                                wheelList = m
                                            }
                                        },
                                        enabled = idx > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move Up",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (idx < wheelList.size - 1) {
                                                val m = wheelList.toMutableList()
                                                val item = m.removeAt(idx)
                                                m.add(idx + 1, item)
                                                wheelList = m
                                            }
                                        },
                                        enabled = idx < wheelList.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move Down",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            wheelList = wheelList.filter { it != pkg }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Add App Button or App Search Picker
                if (!isAddingApp) {
                    if (wheelList.size < 8) {
                        Button(
                            onClick = { isAddingApp = true },
                            modifier = Modifier.fillMaxWidth().testTag("add_wheel_app_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add App to Wheel")
                        }
                    } else {
                        Text(
                            text = "Wheel is full (max 8 apps for optimal spinning layout)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Select App to Add",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                IconButton(
                                    onClick = { isAddingApp = false; searchFilter = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = searchFilter,
                                onValueChange = { searchFilter = it },
                                placeholder = { Text("Filter apps…", fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(availableAppsToAdd) { app ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (wheelList.size < 8) {
                                                    wheelList = wheelList + app.packageName
                                                    isAddingApp = false
                                                    searchFilter = ""
                                                }
                                            }
                                            .padding(vertical = 4.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (app.iconBitmap != null) {
                                            Image(
                                                bitmap = app.iconBitmap,
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = app.label, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveWheelApps(wheelList)
                },
                modifier = Modifier.testTag("save_wheel_btn")
            ) {
                Text("Save Wheel")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
