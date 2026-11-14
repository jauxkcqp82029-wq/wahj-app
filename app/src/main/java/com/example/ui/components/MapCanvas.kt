package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.data.local.LocationHistoryEntity
import com.example.data.local.SafeZoneEntity
import com.example.ui.theme.WahjBlue
import com.example.ui.theme.WahjOnlineGreen

@Composable
fun InteractiveFamilyMap(
    childLat: Double,
    childLng: Double,
    childName: String,
    safeZones: List<SafeZoneEntity>,
    history: List<LocationHistoryEntity>,
    modifier: Modifier = Modifier
) {
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline
    val roadColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(270.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    panOffset += dragAmount
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f) + panOffset
            val metersToPx = 0.25f * zoomScale

            // Cartographic grid lines
            val step = 56f * zoomScale.coerceIn(0.6f, 2.2f)
            var x = (center.x % step) - step
            while (x < size.width) {
                drawLine(gridColor.copy(alpha = 0.45f), Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = (center.y % step) - step
            while (y < size.height) {
                drawLine(gridColor.copy(alpha = 0.45f), Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }

            // Stylized clean city avenues around child coordinate
            drawLine(roadColor, Offset(0f, center.y - 45f), Offset(size.width, center.y - 15f), 10f)
            drawLine(roadColor, Offset(center.x + 35f, 0f), Offset(center.x - 15f, size.height), 10f)

            fun project(lat: Double, lng: Double): Offset {
                val dLatMeters = (lat - childLat) * 111_000.0
                val dLngMeters = (lng - childLng) * 111_000.0 * kotlin.math.cos(Math.toRadians(childLat))
                return Offset(
                    x = center.x + (dLngMeters * metersToPx).toFloat(),
                    y = center.y - (dLatMeters * metersToPx).toFloat()
                )
            }

            // Safe zones circles
            safeZones.filter { it.isActive }.forEach { zone ->
                val zonePos = project(zone.latitude, zone.longitude)
                val radPx = (zone.radiusMeters * metersToPx).coerceIn(28f, 220f)
                drawCircle(
                    color = WahjOnlineGreen.copy(alpha = 0.14f),
                    radius = radPx,
                    center = zonePos
                )
                drawCircle(
                    color = WahjOnlineGreen,
                    radius = radPx,
                    center = zonePos,
                    style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                )
                drawCircle(
                    color = WahjOnlineGreen,
                    radius = 7f,
                    center = zonePos
                )
            }

            // Location history trail
            if (history.size > 1) {
                val path = Path()
                history.forEachIndexed { idx, entry ->
                    val pt = project(entry.latitude, entry.longitude)
                    if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                drawPath(
                    path = path,
                    color = primaryColor.copy(alpha = 0.65f),
                    style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                )
            }

            // Child live pin at center
            drawCircle(
                color = WahjBlue.copy(alpha = 0.18f),
                radius = 34f * zoomScale.coerceIn(0.8f, 1.4f),
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 15f,
                center = center
            )
            drawCircle(
                color = WahjBlue,
                radius = 11f,
                center = center
            )
        }

        // Top-left live child badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text(
                text = "موقع $childName",
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Map zoom & recenter controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilledTonalIconButton(
                onClick = { zoomScale = (zoomScale * 1.3f).coerceAtMost(3f) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "تكبير الخريطة")
            }
            FilledTonalIconButton(
                onClick = { zoomScale = (zoomScale / 1.3f).coerceAtLeast(0.4f) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "تصغير الخريطة")
            }
            FilledTonalIconButton(
                onClick = {
                    panOffset = Offset.Zero
                    zoomScale = 1f
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "توسيط الموقع")
            }
        }
    }
}
