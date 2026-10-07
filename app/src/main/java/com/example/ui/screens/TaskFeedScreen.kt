package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Priority
import com.example.data.model.SubtaskEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskListEntity
import com.example.data.model.TaskWithSubtasks
import com.example.ui.components.QuickAddBar
import com.example.ui.components.TaskItemView
import com.example.ui.viewmodel.LauncherViewModel
import com.example.ui.viewmodel.TaskCategories
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFeedScreen(
    categories: TaskCategories,
    tasksWithSubtasks: List<TaskWithSubtasks>,
    allLists: List<TaskListEntity>,
    selectedListFilter: Long?,
    selectedPriorityFilter: Priority?,
    quickAddText: String,
    onQuickAddTextChanged: (String) -> Unit,
    onSubmitQuickAdd: () -> Unit,
    parsedPreview: com.example.util.ParsedTaskInput?,
    onToggleTaskComplete: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onSnoozeTask: (TaskEntity, Int) -> Unit,
    onToggleSubtask: (SubtaskEntity) -> Unit,
    onOpenTaskDetail: (TaskWithSubtasks?) -> Unit,
    onSelectListFilter: (Long?) -> Unit,
    onSelectPriorityFilter: (Priority?) -> Unit,
    onRescheduleOverdue: (List<TaskEntity>) -> Unit,
    onLaunchApp: (String) -> Unit,
    availableTemplates: List<com.example.data.repository.TaskTemplate> = emptyList(),
    onApplyTemplate: (com.example.data.repository.TaskTemplate) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Today", "Overdue", "Upcoming", "Completed")

    val subtasksByTaskId = remember(tasksWithSubtasks) {
        tasksWithSubtasks.associate { it.task.id to it.subtasks }
    }

    val currentTasks = when (selectedTab) {
        0 -> categories.today
        1 -> categories.overdue
        2 -> categories.upcoming
        3 -> categories.completed
        else -> categories.today
    }

    val cal = Calendar.getInstance()
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
    val dateHeader = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("task_feed_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Section
            item {
                Column(modifier = Modifier.padding(bottom = 6.dp)) {
                    Text(
                        text = dateHeader.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${categories.today.size} tasks on your radar today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Morning Planning Prompt / Overdue Banner (Requirement TH-10)
            if (categories.overdue.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("overdue_planning_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${categories.overdue.size} Overdue Tasks",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Plan your day with 1-tap reschedule to Today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                            Button(
                                onClick = { onRescheduleOverdue(categories.overdue) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("reschedule_today_btn")
                            ) {
                                Text(
                                    text = "To Today",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // List Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedListFilter == null,
                            onClick = { onSelectListFilter(null) },
                            label = { Text("All Lists") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                    items(allLists) { list ->
                        val isSelected = selectedListFilter == list.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectListFilter(list.id) },
                            label = { Text(list.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Quick Templates Carousel (Requirement TM-13)
            if (availableTemplates.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "QUICK TEMPLATES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(availableTemplates) { template ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .clickable { onApplyTemplate(template) }
                                        .testTag("template_chip_${template.title.take(10)}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = template.title,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tabs: Today | Overdue | Upcoming | Completed
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        val count = when (index) {
                            0 -> categories.today.size
                            1 -> categories.overdue.size
                            2 -> categories.upcoming.size
                            3 -> categories.completed.size
                            else -> 0
                        }
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = "$title ($count)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            }
                        )
                    }
                }
            }

            // Task List Items
            if (currentTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No tasks here!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Use the quick-add bar below to add your next goal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    items = currentTasks,
                    key = { it.id }
                ) { task ->
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

        // Floating Quick Add Bar at bottom of task feed
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            QuickAddBar(
                text = quickAddText,
                onTextChanged = onQuickAddTextChanged,
                onSubmit = onSubmitQuickAdd,
                parsedPreview = parsedPreview
            )
        }
    }
}
