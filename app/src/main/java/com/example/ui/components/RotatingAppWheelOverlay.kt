package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.util.InstalledApp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

data class WheelAppItem(
    val packageName: String,
    val title: String,
    val app: InstalledApp
)

/**
 * High-performance Radial / Rotating App Widget overlay.
 * Appears when long-pressing the round App List icon.
 * Displays a spinning wheel of quick-launch apps around an animated center hub.
 * Tracks the drag angle / finger position and highlights apps with bouncy spring physics & haptics.
 * Releasing launches the selected app or triggers wheel editing.
 */
@Composable
fun RotatingAppWheelOverlay(
    isActive: Boolean,
    touchPosition: Offset,
    wheelApps: List<WheelAppItem>,
    hoveredTargetId: String?,
    onHoverChanged: (String?) -> Unit,
    onLaunchApp: (String) -> Unit,
    onOpenEditWheel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val density = LocalDensity.current
    var wheelCenterInWindow by remember { mutableStateOf(Offset.Zero) }

    // Entrance spin animation
    val entranceSpin = remember { Animatable(0f) }
    val entranceScale = remember { Animatable(0.6f) }
    LaunchedEffect(isActive) {
        if (isActive) {
            entranceSpin.snapTo(-45f)
            entranceScale.snapTo(0.6f)
            entranceSpin.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            entranceScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    // Geometry constants in px
    val radiusDp = 110.dp
    val radiusPx = with(density) { radiusDp.toPx() }
    val totalApps = wheelApps.size.coerceAtLeast(1)

    // Calculate hover selection based on touchPosition relative to wheelCenter
    LaunchedEffect(touchPosition, wheelCenterInWindow, wheelApps) {
        if (wheelCenterInWindow == Offset.Zero || wheelApps.isEmpty()) return@LaunchedEffect

        val dx = touchPosition.x - wheelCenterInWindow.x
        val dy = touchPosition.y - wheelCenterInWindow.y
        val dist = hypot(dx, dy)

        val editThresholdPx = with(density) { 38.dp.toPx() }
        val maxReachPx = with(density) { 190.dp.toPx() }

        if (dist <= editThresholdPx) {
            // Finger in Center Hub near Edit button
            if (hoveredTargetId != "EDIT_WHEEL") {
                onHoverChanged("EDIT_WHEEL")
            }
        } else if (dist <= maxReachPx) {
            // Calculate angle from center (-PI to PI)
            var angle = atan2(dy, dx)
            // Adjust so top (12 o'clock) is 0 radians
            var normalizedAngle = angle + (PI / 2).toFloat()
            if (normalizedAngle < 0) normalizedAngle += (2 * PI).toFloat()

            val step = (2 * PI).toFloat() / totalApps
            val rawIndex = ((normalizedAngle + step / 2) / step).toInt() % totalApps
            val selectedApp = wheelApps.getOrNull(rawIndex)
            val newTargetId = selectedApp?.packageName

            if (newTargetId != hoveredTargetId) {
                onHoverChanged(newTargetId)
            }
        } else {
            if (hoveredTargetId != null) {
                onHoverChanged(null)
            }
        }
    }

    val selectedApp = wheelApps.firstOrNull { it.packageName == hoveredTargetId }
    val isEditHovered = hoveredTargetId == "EDIT_WHEEL"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
            .testTag("rotating_app_wheel_overlay")
    ) {
        // Top Tooltip / Status Banner
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RotateRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isEditHovered -> "Release to Customize Quick Wheel ✏️"
                        selectedApp != null -> "Release to launch \"${selectedApp.title}\" 🚀"
                        else -> "Drag finger around wheel to quick-select • Release to launch"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (hoveredTargetId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // The Spinning Wheel Widget Container (Positioned in lower center above bottom docks)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp)
                .size(310.dp)
                .graphicsLayer {
                    scaleX = entranceScale.value
                    scaleY = entranceScale.value
                }
                .onGloballyPositioned { coords ->
                    wheelCenterInWindow = coords.boundsInWindow().center
                },
            contentAlignment = Alignment.Center
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

            // Orbital Ring Decorative Track
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(size.width / 2, size.height / 2)
                // Outer orbital path
                drawCircle(
                    color = outlineColor,
                    radius = radiusPx,
                    center = centerOffset,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), 0f)
                    )
                )
                // Inner glow ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.15f), Color.Transparent),
                        center = centerOffset,
                        radius = radiusPx * 1.25f
                    ),
                    radius = radiusPx * 1.25f,
                    center = centerOffset
                )
            }

            // Center Hub
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 10.dp,
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isEditHovered) 2.5.dp else 1.5.dp,
                    color = if (isEditHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .clickable { onOpenEditWheel() }
                    .testTag("wheel_center_hub")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (selectedApp != null && !isEditHovered) {
                        if (selectedApp.app.iconBitmap != null) {
                            Image(
                                bitmap = selectedApp.app.iconBitmap,
                                contentDescription = selectedApp.title,
                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = selectedApp.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Wheel",
                            tint = if (isEditHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isEditHovered) "EDIT WHEEL" else "CUSTOMIZE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = if (isEditHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Radial App Items along the circle
            wheelApps.forEachIndexed { index, item ->
                // Calculate position on the circle (0 is at top / 12 o'clock, moving clockwise)
                val baseAngle = (-PI / 2.0) + (2.0 * PI * index / totalApps)
                val currentAngle = baseAngle + Math.toRadians(entranceSpin.value.toDouble())
                val xOffsetDp = (radiusDp.value * cos(currentAngle)).dp
                val yOffsetDp = (radiusDp.value * sin(currentAngle)).dp

                val isHovered = item.packageName == hoveredTargetId
                val itemScale by animateFloatAsState(
                    targetValue = if (isHovered) 1.35f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "item_scale_${item.packageName}"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset(x = xOffsetDp, y = yOffsetDp)
                        .graphicsLayer {
                            scaleX = itemScale
                            scaleY = itemScale
                        }
                        .testTag("wheel_app_${item.packageName}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .shadow(
                                elevation = if (isHovered) 14.dp else 3.dp,
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
                        if (item.app.iconBitmap != null) {
                            Image(
                                bitmap = item.app.iconBitmap,
                                contentDescription = item.title,
                                modifier = Modifier.size(34.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = item.title,
                                tint = if (isHovered) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = if (isHovered) FontWeight.ExtraBold else FontWeight.Medium
                        ),
                        color = if (isHovered) MaterialTheme.colorScheme.primary else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(62.dp)
                            .background(
                                color = if (isHovered) MaterialTheme.colorScheme.surface.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                    )
                }
            }
        }

        // Active Finger Drag Indicator Ring
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (touchPosition.x - with(density) { 22.dp.toPx() }).roundToInt(),
                        (touchPosition.y - with(density) { 22.dp.toPx() }).roundToInt()
                    )
                }
                .size(44.dp)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape)
        )
    }
}
