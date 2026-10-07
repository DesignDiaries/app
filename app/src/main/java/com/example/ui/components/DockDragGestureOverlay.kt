package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomAppFolder
import com.example.util.InstalledApp
import kotlin.math.roundToInt

data class DockTargetItem(
    val id: String,
    val title: String,
    val packageName: String? = null,
    val isFolder: Boolean = false,
    val folder: CustomAppFolder? = null,
    val app: InstalledApp? = null,
    val isAction: Boolean = false,
    val actionType: String? = null,
    val actionIcon: ImageVector? = null
)

/**
 * Overlay shown when the user long-presses and holds on the home dock.
 * Displays a quick folder / fan shelf of apps, folders, and dock actions directly above the finger.
 * As the user drags their finger across the items, the hovered item enlarges with haptic & visual feedback.
 * Releasing the finger on an app launches it immediately. Releasing on a folder or action activates it.
 */
@Composable
fun DockDragGestureOverlay(
    isActive: Boolean,
    touchPosition: Offset,
    targetItems: List<DockTargetItem>,
    folderTitle: String = "FAVORITES",
    hoveredTargetId: String?,
    onTargetBoundsReported: (String, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val density = LocalDensity.current

    // Background scrim that indicates gesture mode
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .testTag("dock_drag_gesture_overlay")
    ) {
        // Drag guide tooltip banner near top
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 44.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val hovered = targetItems.firstOrNull { it.id == hoveredTargetId }
                Text(
                    text = when {
                        hovered == null -> "Drag finger to an app in $folderTitle & release to launch"
                        hovered.actionType == "EDIT_DOCK" -> "Release to Edit Home Dock ✏️"
                        hovered.actionType == "APP_DRAWER" -> "Release to open All Apps 📱"
                        hovered.actionType == "SEARCH" -> "Release to Search 🔍"
                        hovered.isFolder -> "Release to open \"${hovered.title}\" 📁"
                        else -> "Release to launch \"${hovered.title}\" 🚀"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (hoveredTargetId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Floating fan / shelf of items positioned right above the touch point
        val popupY = (touchPosition.y - with(density) { 210.dp.toPx() })
            .coerceIn(with(density) { 90.dp.toPx() }, with(density) { 560.dp.toPx() })

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, popupY.roundToInt()) },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.97f),
                tonalElevation = 12.dp,
                shadowElevation = 16.dp,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⭐ ${folderTitle.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.3.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Group items into chunks if more than 5 so they fit comfortably on mobile screens
                    val rows = if (targetItems.size > 5) targetItems.chunked(4) else listOf(targetItems)

                    rows.forEachIndexed { rowIndex, rowItems ->
                        if (rowIndex > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowItems.forEach { item ->
                                val isHovered = item.id == hoveredTargetId
                                val scale by animateFloatAsState(
                                    targetValue = if (isHovered) 1.25f else 1.0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "target_scale"
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .onGloballyPositioned { coords ->
                                            onTargetBoundsReported(item.id, coords.boundsInWindow())
                                        }
                                        .testTag("dock_drag_target_${item.id}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .shadow(
                                                elevation = if (isHovered) 12.dp else 2.dp,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isHovered) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .border(
                                                width = if (isHovered) 2.5.dp else 1.dp,
                                                color = if (isHovered) Color.White else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            item.isAction -> {
                                                Icon(
                                                    imageVector = item.actionIcon ?: Icons.Default.Edit,
                                                    contentDescription = item.title,
                                                    tint = if (isHovered) Color.White else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            item.isFolder -> {
                                                val folderColor = try {
                                                    Color(android.graphics.Color.parseColor(item.folder?.colorHex ?: "#6366F1"))
                                                } catch (_: Throwable) {
                                                    MaterialTheme.colorScheme.primary
                                                }
                                                Icon(
                                                    imageVector = getFolderIcon(item.folder?.iconName ?: "FOLDER"),
                                                    contentDescription = item.title,
                                                    tint = if (isHovered) Color.White else folderColor,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                            item.app?.iconBitmap != null -> {
                                                Image(
                                                    bitmap = item.app.iconBitmap,
                                                    contentDescription = item.title,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                            else -> {
                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = item.title,
                                                    tint = if (isHovered) Color.White else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = if (isHovered) FontWeight.ExtraBold else FontWeight.Medium
                                        ),
                                        color = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.width(58.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Animated ripple / ring following the finger drag position
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (touchPosition.x - with(density) { 24.dp.toPx() }).roundToInt(),
                        (touchPosition.y - with(density) { 24.dp.toPx() }).roundToInt()
                    )
                }
                .size(48.dp)
                .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
        )
    }
}
