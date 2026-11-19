package com.example.editor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.editor.engine.MediaRegistry
import com.example.editor.model.EditorUiState
import com.example.editor.model.ExportStatus
import com.example.ui.theme.LabelCapsStyle
import com.example.ui.theme.PrimaryContainerCyan
import com.example.ui.theme.TimecodeSmallStyle
import com.example.ui.theme.TimecodeTextStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportScreen(
    state: EditorUiState,
    mediaRegistry: MediaRegistry,
    onSelectResolution: (Int) -> Unit,
    onSelectFps: (Int) -> Unit,
    onBitrateChange: (Int) -> Unit,
    onStartRender: () -> Unit,
    onShareExported: (uriString: String, mimeType: String) -> Unit,
    onExportAudioOnly: () -> Unit,
    onKeepEditing: () -> Unit
) {
    val cfg = state.exportConfig
    val estMb = (cfg.bitrateMbps * state.totalDurationSec) / 8f

    val progressFraction = when (val st = state.exportStatus) {
        is ExportStatus.Idle -> 0f
        is ExportStatus.Rendering -> st.progress.coerceIn(0f, 1f)
        is ExportStatus.Completed -> 1f
        is ExportStatus.Failed -> 0f
    }
    val pct = (progressFraction * 100f).roundToInt()

    val frameTag = when (val st = state.exportStatus) {
        is ExportStatus.Idle -> "READY"
        is ExportStatus.Rendering -> "FRAME ${st.currentFrame}/${st.totalFrames}"
        is ExportStatus.Completed -> "SAVED MP4"
        is ExportStatus.Failed -> "ERROR"
    }

    val statusText = when (val st = state.exportStatus) {
        is ExportStatus.Idle -> "Configure quality & tap Render Video"
        is ExportStatus.Rendering -> st.detail
        is ExportStatus.Completed -> "Saved ${st.displayName} (${String.format("%.1f", st.fileSizeMb)} MB)"
        is ExportStatus.Failed -> st.message
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. RENDER MONITOR CARD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CompositePreviewCanvas(
                    clips = state.clips,
                    mediaRegistry = mediaRegistry,
                    timeSec = if (state.exportStatus is ExportStatus.Rendering) {
                        progressFraction * state.totalDurationSec
                    } else state.playheadSec,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark scrim gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x660E0E13),
                                    Color.Transparent,
                                    Color(0xCC0E0E13)
                                )
                            )
                        )
                )

                // Top-left Frame Tag
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.Videocam, contentDescription = null, tint = PrimaryContainerCyan, modifier = Modifier.size(13.dp))
                    Text(frameTag, style = TimecodeSmallStyle, color = MaterialTheme.colorScheme.onSurface)
                }

                // Top-right Codec Tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "H.264 MP4 + PCM",
                        style = LabelCapsStyle.copy(fontSize = 9.sp),
                        color = PrimaryContainerCyan
                    )
                }

                // Center Progress Ring
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.88f)),
                    contentAlignment = Alignment.Center
                ) {
                    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    Canvas(modifier = Modifier.size(66.dp)) {
                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 7f, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = PrimaryContainerCyan,
                            startAngle = -90f,
                            sweepAngle = progressFraction * 360f,
                            useCenter = false,
                            style = Stroke(width = 7.5f, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Linear progress + Est size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "~${String.format("%.1f", estMb)} MB",
                    style = TimecodeTextStyle,
                    color = PrimaryContainerCyan
                )
            }

            LinearProgressIndicator(
                progress = { progressFraction },
                color = PrimaryContainerCyan,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
            )
        }

        // 2. EXPORT CONFIGURATOR
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Tune, contentDescription = null, tint = PrimaryContainerCyan, modifier = Modifier.size(20.dp))
                Text("Export Configurator", style = MaterialTheme.typography.headlineSmall)
            }

            // Resolution Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Resolution", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (cfg.resolution == 2160) "4K UHD" else "${cfg.resolution}p",
                        style = TimecodeSmallStyle,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(720 to "720p HD", 1080 to "1080p FHD", 2160 to "4K UHD").forEach { (res, lbl) ->
                        val active = cfg.resolution == res
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                                )
                                .clickable { onSelectResolution(res) }
                                .testTag("export_res_$res"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                lbl,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Frame Rate Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Frame Rate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${cfg.fps} fps", style = TimecodeSmallStyle, color = MaterialTheme.colorScheme.primary)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(24, 30, 60).forEach { fps ->
                        val active = cfg.fps == fps
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                                )
                                .clickable { onSelectFps(fps) }
                                .testTag("export_fps_$fps"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$fps fps",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bitrate Slider
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bitrate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${cfg.bitrateMbps} Mbps", style = TimecodeTextStyle, color = PrimaryContainerCyan)
                }
                Slider(
                    value = cfg.bitrateMbps.toFloat(),
                    onValueChange = { onBitrateChange(it.roundToInt()) },
                    valueRange = 4f..35f,
                    colors = neonSliderColors(),
                    modifier = Modifier.testTag("export_bitrate_slider")
                )
            }
        }

        // 3. ANDROID NATIVE MEDIA PIPELINE CARD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Android, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Text("Android Scoped Storage Pipeline", style = MaterialTheme.typography.titleSmall)
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("MediaStore Ready", style = LabelCapsStyle.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.primary)
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("PHOTO_PICKER", "MEDIA_STORE_MOVIES", "SAF_AUDIO", "MEDIACODEC_AVC").forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(tag, style = LabelCapsStyle.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // 4. RENDER & SHARE CTAS
        val isRendering = state.exportStatus is ExportStatus.Rendering
        Button(
            onClick = onStartRender,
            enabled = !isRendering,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryContainerCyan,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_render_button")
        ) {
            Icon(Icons.Filled.MovieFilter, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isRendering) "Rendering MP4..." else "Render & Export Video",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        val completed = state.exportStatus as? ExportStatus.Completed
        if (completed?.contentUriString != null) {
            Button(
                onClick = { onShareExported(completed.contentUriString, completed.mimeType) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("share_exported_button")
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Share ${completed.displayName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onExportAudioOnly,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("export_audio_only_button")
            ) {
                Icon(Icons.Filled.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export Audio", fontSize = 12.sp)
            }
            Button(
                onClick = onKeepEditing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("keep_editing_button")
            ) {
                Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Keep Editing", fontSize = 12.sp)
            }
        }
    }
}
