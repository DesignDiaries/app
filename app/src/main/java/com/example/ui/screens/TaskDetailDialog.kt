package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Priority
import com.example.data.model.Recurrence
import com.example.data.model.SubtaskEntity
import com.example.data.model.TaskListEntity
import com.example.data.model.TaskWithSubtasks
import com.example.util.InstalledApp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailDialog(
    taskWithSubtasks: TaskWithSubtasks?,
    allLists: List<TaskListEntity>,
    installedApps: List<InstalledApp>,
    onSave: (
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
    ) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val existing = taskWithSubtasks?.task
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var priority by remember { mutableStateOf(existing?.priority ?: Priority.MEDIUM) }
    var listId by remember { mutableStateOf(existing?.listId ?: 1L) }
    var tags by remember { mutableStateOf(existing?.tags ?: "") }
    var recurrence by remember { mutableStateOf(existing?.recurrence ?: Recurrence.NONE) }
    var dueDate by remember { mutableStateOf(existing?.dueDate) }
    var dueTimeMinutes by remember { mutableStateOf(existing?.dueTimeMinutes) }
    var linkedPackage by remember { mutableStateOf(existing?.linkedPackageName) }

    val subtasksList = remember {
        mutableStateListOf<SubtaskEntity>().apply {
            if (taskWithSubtasks != null) addAll(taskWithSubtasks.subtasks)
        }
    }
    var newSubtaskText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("task_detail_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header: Title & Close / Save buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }

                Text(
                    text = if (existing == null) "New Task" else "Edit Task",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val reminderTime = if (dueDate != null && dueTimeMinutes != null) {
                                dueDate!! + (dueTimeMinutes!! * 60 * 1000L)
                            } else null

                            onSave(
                                existing?.id ?: 0L,
                                title.trim(),
                                notes.trim(),
                                priority,
                                dueDate,
                                dueTimeMinutes,
                                listId,
                                tags.trim(),
                                recurrence,
                                linkedPackage,
                                reminderTime,
                                subtasksList.toList()
                            )
                        }
                    },
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("save_task_btn")
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Task Title
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title *") },
                        placeholder = { Text("What needs to be done?") },
                        modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Notes / Description
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Details") },
                        placeholder = { Text("Add instructions, checklist notes, or links…") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Priority Selection
                item {
                    Column {
                        Text(
                            text = "Priority",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Priority.entries.forEach { p ->
                                val isSelected = priority == p
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) p.color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { priority = p }
                                        .testTag("priority_chip_${p.name}")
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = p.title,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Due Date Quick Select (Today, Tomorrow, Clear)
                item {
                    Column {
                        Text(
                            text = "Due Date",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val cal = Calendar.getInstance()
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            val todayMillis = cal.timeInMillis
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            val tomorrowMillis = cal.timeInMillis

                            FilterChip(
                                selected = dueDate == todayMillis,
                                onClick = { dueDate = todayMillis },
                                label = { Text("Today") }
                            )
                            FilterChip(
                                selected = dueDate == tomorrowMillis,
                                onClick = { dueDate = tomorrowMillis },
                                label = { Text("Tomorrow") }
                            )
                            if (dueDate != null) {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        dueDate = null
                                        dueTimeMinutes = null
                                    },
                                    label = { Text("Clear Date") }
                                )
                            }
                        }
                    }
                }

                // List Selector
                item {
                    Column {
                        Text(
                            text = "List / Project",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(allLists) { list ->
                                FilterChip(
                                    selected = listId == list.id,
                                    onClick = { listId = list.id },
                                    label = { Text(list.name) }
                                )
                            }
                        }
                    }
                }

                // Recurrence Selector
                item {
                    Column {
                        Text(
                            text = "Repeat",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(Recurrence.entries.toTypedArray()) { rec ->
                                FilterChip(
                                    selected = recurrence == rec,
                                    onClick = { recurrence = rec },
                                    label = { Text(rec.displayName) }
                                )
                            }
                        }
                    }
                }

                // Tags Input
                item {
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Tags (comma separated e.g. work, client)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Subtasks / Checklist
                item {
                    Column {
                        Text(
                            text = "Subtasks & Checklist",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        subtasksList.forEachIndexed { index, subtask ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${subtask.title}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { subtasksList.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove")
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newSubtaskText,
                                onValueChange = { newSubtaskText = it },
                                placeholder = { Text("Add subtask item…") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newSubtaskText.isNotBlank()) {
                                        subtasksList.add(
                                            SubtaskEntity(
                                                taskId = existing?.id ?: 0L,
                                                title = newSubtaskText.trim(),
                                                sortOrder = subtasksList.size
                                            )
                                        )
                                        newSubtaskText = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }

                // Delete Task Option (If existing)
                if (existing != null) {
                    item {
                        Button(
                            onClick = {
                                onDelete(existing.id)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("delete_task_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Task")
                        }
                    }
                }
            }
        }
    }
}
