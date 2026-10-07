package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Priority
import com.example.data.model.TaskEntity

@Composable
fun EisenhowerMatrixCard(
    tasks: List<TaskEntity>,
    onSelectPriorityFilter: (Priority?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pending = tasks.filter { !it.isCompleted }

    val urgentCount = pending.count { it.priority == Priority.URGENT }
    val highCount = pending.count { it.priority == Priority.HIGH }
    val mediumCount = pending.count { it.priority == Priority.MEDIUM }
    val lowCount = pending.count { it.priority == Priority.LOW }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("eisenhower_matrix_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Priority Matrix (Eisenhower)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${pending.size} Active",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2x2 Matrix Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Q1: Do First (Urgent)
                MatrixQuadrant(
                    title = "DO FIRST",
                    subtitle = "Urgent & Critical",
                    count = urgentCount,
                    accentColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectPriorityFilter(Priority.URGENT) }
                )

                // Q2: Schedule (High)
                MatrixQuadrant(
                    title = "SCHEDULE",
                    subtitle = "Important & High",
                    count = highCount,
                    accentColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectPriorityFilter(Priority.HIGH) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Q3: Delegate / Quick (Medium)
                MatrixQuadrant(
                    title = "QUICK WINS",
                    subtitle = "Routine & Medium",
                    count = mediumCount,
                    accentColor = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectPriorityFilter(Priority.MEDIUM) }
                )

                // Q4: Backlog (Low)
                MatrixQuadrant(
                    title = "BACKLOG",
                    subtitle = "Low Priority",
                    count = lowCount,
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectPriorityFilter(Priority.LOW) }
                )
            }
        }
    }
}

@Composable
private fun MatrixQuadrant(
    title: String,
    subtitle: String,
    count: Int,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = accentColor
                )
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}
