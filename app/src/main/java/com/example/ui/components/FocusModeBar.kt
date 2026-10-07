package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FocusMode

@Composable
fun FocusModeBar(
    activeMode: FocusMode,
    onSelectMode: (FocusMode) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("focus_mode_bar"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(FocusMode.entries.toTypedArray()) { mode ->
            val isSelected = mode == activeMode
            val icon = getFocusIcon(mode)
            val modeColor = try {
                Color(android.graphics.Color.parseColor(mode.colorHex))
            } catch (_: Throwable) {
                MaterialTheme.colorScheme.primary
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) modeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable { onSelectMode(mode) }
                    .testTag("focus_mode_${mode.name}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = mode.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

private fun getFocusIcon(mode: FocusMode): ImageVector {
    return when (mode) {
        FocusMode.OFF -> Icons.Default.AllInclusive
        FocusMode.WORK -> Icons.Default.Work
        FocusMode.STUDY -> Icons.Default.Book
        FocusMode.PERSONAL -> Icons.Default.Home
        FocusMode.DEEP_WORK -> Icons.Default.Lock
        FocusMode.ZEN -> Icons.Default.SelfImprovement
    }
}
