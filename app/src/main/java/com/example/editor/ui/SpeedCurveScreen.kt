package com.example.editor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.editor.engine.MediaRegistry
import com.example.editor.model.EditorUiState
import com.example.editor.model.SpeedCurvePreset
import com.example.editor.model.SpeedPoint
import com.example.ui.theme.LabelCapsStyle
import com.example.ui.theme.PrimaryContainerCyan
import com.example.ui.theme.PrimaryFixed
import com.example.ui.theme.SecondaryContainerBlue
import com.example.ui.theme.SurfaceBright
import com.example.ui.theme.TertiaryFixedDim
import com.example.ui.theme.TimecodeSmallStyle
import com.example.ui.theme.TimecodeTextStyle
import kotlin.math.abs
import kotlin.math.max

@Composable
fun SpeedCurveScreen(
    state: EditorUiState,
    mediaRegistry: MediaRegistry,
    onTogglePlay: () -> Unit,
    onUniformSpeedChange: (Float) -> Unit,
    onCurveNodeChange: (Int, Float) -> Unit,
    onSelectPreset: (SpeedCurvePreset) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit
) {
    val activeClip = state.selectedClip
    val playheadNorm = if (activeClip != null) {
        ((state.playheadSec - activeClip.startTime) / max(0.1f, activeClip.duration)).coerceIn(0f, 1f)
    } else 0f

    val liveSpeed = evalCurveSpeed(state.editingSpeedCurve, playheadNorm)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. TOP SPEED MONITOR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black)
        ) {
            CompositePreviewCanvas(
                clips = state.clips,
                mediaRegistry = mediaRegistry,
                timeSec = state.playheadSec,
                modifier = Modifier.fillMaxSize()
            )

            // Floating Speed Readout Pill
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Speed, contentDescription = null, tint = PrimaryContainerCyan, modifier = Modifier.size(16.dp))
                Text(
                    text = "${String.format("%.1f", liveSpeed)}x",
                    style = TimecodeTextStyle.copy(fontSize = 14.sp),
                    color = PrimaryContainerCyan
                )
            }

            // Play/Pause Button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceBright.copy(alpha = 0.72f))
                    .clickable(onClick = onTogglePlay)
                    .testTag("speed_play_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play_pause),
                    tint = PrimaryFixed,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 2. UNIFORM CLIP SPEED SLIDER (0.1x to 10.0x)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Uniform Clip Speed", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${String.format("%.1f", state.uniformSpeed)}x",
                    style = TimecodeTextStyle,
                    color = PrimaryContainerCyan
                )
            }
            Slider(
                value = state.uniformSpeed,
                onValueChange = onUniformSpeedChange,
                valueRange = 0.1f..10f,
                colors = neonSliderColors(),
                modifier = Modifier.testTag("uniform_speed_slider")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0.1x Slow", style = TimecodeSmallStyle, color = MaterialTheme.colorScheme.outline)
                Text("1.0x Normal", style = TimecodeSmallStyle, color = MaterialTheme.colorScheme.outline)
                Text("5.0x", style = TimecodeSmallStyle, color = MaterialTheme.colorScheme.outline)
                Text("10.0x Hyper", style = TimecodeSmallStyle, color = MaterialTheme.colorScheme.outline)
            }
        }

        // 3. INTERACTIVE BEZIER VELOCITY FLOW CURVE
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Velocity Flow Curve", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Drag cyan nodes vertically (0.1x – 10x)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Curve Active", style = LabelCapsStyle, color = PrimaryContainerCyan)
                }
            }

            // Curve Interactive Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                // Y-axis labels
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .padding(start = 6.dp, top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("10.0x", style = TimecodeSmallStyle.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.outline)
                    Text("5.0x", style = TimecodeSmallStyle.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.outline)
                    Text("1.0x", style = TimecodeSmallStyle.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = PrimaryContainerCyan)
                    Text("0.1x", style = TimecodeSmallStyle.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.outline)
                }

                var draggedNodeIndex by remember { mutableIntStateOf(-1) }
                val points = state.editingSpeedCurve

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    val leftPad = size.width * 0.13f
                                    val usableW = size.width - leftPad - 24f
                                    var bestIdx = -1
                                    var bestDist = Float.MAX_VALUE
                                    points.forEachIndexed { idx, pt ->
                                        val px = leftPad + pt.t * usableW
                                        val py = speedToCanvasY(pt.v, size.height.toFloat())
                                        val dist = abs(startOffset.x - px) + abs(startOffset.y - py)
                                        if (dist < bestDist && dist < 110f) {
                                            bestDist = dist
                                            bestIdx = idx
                                        }
                                    }
                                    draggedNodeIndex = bestIdx
                                },
                                onDragEnd = { draggedNodeIndex = -1 },
                                onDragCancel = { draggedNodeIndex = -1 },
                                onDrag = { change, _ ->
                                    change.consume()
                                    if (draggedNodeIndex in points.indices) {
                                        val newVal = canvasYToSpeed(change.position.y, size.height.toFloat())
                                        onCurveNodeChange(draggedNodeIndex, newVal)
                                    }
                                }
                            )
                        }
                        .testTag("speed_curve_canvas")
                ) {
                    val leftPad = size.width * 0.13f
                    val rightPad = 24f
                    val usableW = size.width - leftPad - rightPad
                    val topY = 24f
                    val baseOneY = size.height * 0.60f
                    val bottomY = size.height - 22f

                    // Grid lines
                    val gridColor = Color(0xFF2A292F)
                    drawLine(gridColor, Offset(leftPad, topY), Offset(size.width - rightPad, topY), 1.5f)
                    drawLine(gridColor, Offset(leftPad, (topY + baseOneY) / 2f), Offset(size.width - rightPad, (topY + baseOneY) / 2f), 1.5f)
                    drawLine(
                        color = PrimaryContainerCyan.copy(alpha = 0.65f),
                        start = Offset(leftPad, baseOneY),
                        end = Offset(size.width - rightPad, baseOneY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )
                    drawLine(gridColor, Offset(leftPad, bottomY), Offset(size.width - rightPad, bottomY), 1.5f)

                    if (points.size >= 2) {
                        val coords = points.map { pt ->
                            Offset(leftPad + pt.t * usableW, speedToCanvasY(pt.v, size.height))
                        }

                        val curvePath = Path().apply {
                            moveTo(coords[0].x, coords[0].y)
                            for (i in 0 until coords.lastIndex) {
                                val mx = (coords[i].x + coords[i + 1].x) / 2f
                                cubicTo(mx, coords[i].y, mx, coords[i + 1].y, coords[i + 1].x, coords[i + 1].y)
                            }
                        }

                        val areaPath = Path().apply {
                            addPath(curvePath)
                            lineTo(coords.last().x, bottomY)
                            lineTo(coords.first().x, bottomY)
                            close()
                        }

                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    PrimaryContainerCyan.copy(alpha = 0.26f),
                                    Color.Transparent
                                )
                            )
                        )

                        drawPath(
                            path = curvePath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(SecondaryContainerBlue, PrimaryContainerCyan, TertiaryFixedDim)
                            ),
                            style = Stroke(width = 6f, cap = StrokeCap.Round)
                        )

                        // Playhead line
                        val phX = leftPad + playheadNorm * usableW
                        drawLine(
                            color = Color.White.copy(alpha = 0.8f),
                            start = Offset(phX, 0f),
                            end = Offset(phX, size.height),
                            strokeWidth = 2.5f
                        )

                        // Interactive Cyan Nodes
                        coords.forEachIndexed { idx, offset ->
                            val r = if (idx == draggedNodeIndex) 16f else 13f
                            drawCircle(PrimaryContainerCyan.copy(alpha = 0.25f), radius = r + 7f, center = offset)
                            drawCircle(Color.White, radius = r + 2.5f, center = offset)
                            drawCircle(PrimaryContainerCyan, radius = r, center = offset)
                        }
                    }
                }
            }

            // Curve Presets Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpeedCurvePreset.entries.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onSelectPreset(preset) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("speed_preset_${preset.name.lowercase()}")
                    ) {
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Reset & Apply CTAs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(0.35f)
                        .height(48.dp)
                        .testTag("speed_reset_button")
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset")
                }
                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryContainerCyan,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(0.65f)
                        .height(48.dp)
                        .testTag("speed_apply_button")
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Apply Speed to Clip", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun speedToCanvasY(v: Float, height: Float): Float {
    val topY = 24f
    val baseOneY = height * 0.60f
    val bottomY = height - 22f
    return if (v >= 1f) {
        baseOneY - ((v - 1f) / 9f) * (baseOneY - topY)
    } else {
        baseOneY + ((1f - v) / 0.9f) * (bottomY - baseOneY)
    }
}

private fun canvasYToSpeed(y: Float, height: Float): Float {
    val topY = 24f
    val baseOneY = height * 0.60f
    val bottomY = height - 22f
    val cy = y.coerceIn(topY, bottomY)
    return if (cy <= baseOneY) {
        1f + ((baseOneY - cy) / max(1f, baseOneY - topY)) * 9f
    } else {
        1f - ((cy - baseOneY) / max(1f, bottomY - baseOneY)) * 0.9f
    }
}

private fun evalCurveSpeed(points: List<SpeedPoint>, norm: Float): Float {
    if (points.isEmpty()) return 1f
    for (i in 0 until points.lastIndex) {
        val a = points[i]
        val b = points[i + 1]
        if (norm in a.t..b.t) {
            val f = (norm - a.t) / max(0.0001f, b.t - a.t)
            return a.v + (b.v - a.v) * f
        }
    }
    return points.last().v
}
