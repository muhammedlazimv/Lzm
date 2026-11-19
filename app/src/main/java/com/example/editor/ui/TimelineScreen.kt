package com.example.editor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.editor.engine.MediaRegistry
import com.example.editor.model.AspectRatioPreset
import com.example.editor.model.ClipType
import com.example.editor.model.ContextDrawerMode
import com.example.editor.model.EditorUiState
import com.example.editor.model.EditorView
import com.example.editor.model.TextProps
import com.example.editor.model.TimelineClip
import com.example.editor.model.TrackType
import com.example.editor.model.TransitionType
import com.example.editor.model.formatTimecode
import com.example.ui.theme.LabelCapsStyle
import com.example.ui.theme.PrimaryContainerCyan
import com.example.ui.theme.PrimaryFixed
import com.example.ui.theme.SurfaceBright
import com.example.ui.theme.TertiaryFixedDim
import com.example.ui.theme.TimecodeSmallStyle
import com.example.ui.theme.TimecodeTextStyle
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun TimelineScreen(
    state: EditorUiState,
    mediaRegistry: MediaRegistry,
    onTogglePlay: () -> Unit,
    onJumpStart: () -> Unit,
    onJumpEnd: () -> Unit,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    onSeek: (Float) -> Unit,
    onQuickRotate: () -> Unit,
    onToggleGuides: () -> Unit,
    onSplit: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onAddKeyframe: () -> Unit,
    onToggleSnap: () -> Unit,
    onSelectClip: (String) -> Unit,
    onBeginClipGesture: () -> Unit,
    onMoveClipBy: (String, Float) -> Unit,
    onTrimLeft: (String, Float) -> Unit,
    onTrimRight: (String, Float) -> Unit,
    onOpenTransitionForClip: (String) -> Unit,
    onImportMedia: () -> Unit,
    onRibbonAction: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onTransformChange: (Float?, Float?, Float?, Float?, Boolean?) -> Unit,
    onResetTransform: () -> Unit,
    onTransitionSelect: (TransitionType) -> Unit,
    onSaveText: (TextProps) -> Unit,
    onAddSticker: (String) -> Unit,
    onImportAudio: () -> Unit,
    onToggleVoiceRecord: () -> Unit,
    onSelectAspect: (AspectRatioPreset) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 12.dp)
    ) {
        // 1. TOP PREVIEW STAGE
        PreviewStageSection(
            state = state,
            mediaRegistry = mediaRegistry,
            onQuickRotate = onQuickRotate,
            onToggleGuides = onToggleGuides
        )

        // 2. CENTER TRANSPORT & JOG STRIP
        TransportStrip(
            state = state,
            onJumpStart = onJumpStart,
            onStepBack = onStepBack,
            onTogglePlay = onTogglePlay,
            onStepForward = onStepForward,
            onJumpEnd = onJumpEnd,
            onSplit = onSplit
        )

        // 3. MULTITRACK TIMELINE & RULER
        MultiTrackTimelineSection(
            state = state,
            mediaRegistry = mediaRegistry,
            onZoomOut = onZoomOut,
            onZoomIn = onZoomIn,
            onAddKeyframe = onAddKeyframe,
            onToggleSnap = onToggleSnap,
            onSeek = onSeek,
            onSelectClip = onSelectClip,
            onBeginClipGesture = onBeginClipGesture,
            onMoveClipBy = onMoveClipBy,
            onTrimLeft = onTrimLeft,
            onTrimRight = onTrimRight,
            onOpenTransitionForClip = onOpenTransitionForClip
        )

        // 4. LOCAL GALLERY & MEDIA INGEST BANNER
        MediaIngestBanner(
            status = state.importerStatus,
            onImportClick = onImportMedia
        )

        // 5. QUICK CLIP CONTEXT ACTION RIBBON
        ClipContextRibbon(
            selectedClip = state.selectedClip,
            onAction = onRibbonAction
        )

        // 6. INLINE CONTEXTUAL INSPECTOR DRAWER
        AnimatedVisibility(visible = state.drawerMode != ContextDrawerMode.NONE) {
            ContextInspectorDrawer(
                state = state,
                onClose = onCloseDrawer,
                onVolumeChange = onVolumeChange,
                onToggleMute = onToggleMute,
                onTransformChange = onTransformChange,
                onResetTransform = onResetTransform,
                onTransitionSelect = onTransitionSelect,
                onSaveText = onSaveText,
                onAddSticker = onAddSticker,
                onImportAudio = onImportAudio,
                onToggleVoiceRecord = onToggleVoiceRecord,
                onSelectAspect = onSelectAspect
            )
        }
    }
}

@Composable
private fun PreviewStageSection(
    state: EditorUiState,
    mediaRegistry: MediaRegistry,
    onQuickRotate: () -> Unit,
    onToggleGuides: () -> Unit
) {
    val (boxW, boxH) = when (state.aspect) {
        AspectRatioPreset.RATIO_9_16 -> 178.dp to 316.dp
        AspectRatioPreset.RATIO_16_9 -> 316.dp to 178.dp
        AspectRatioPreset.RATIO_1_1 -> 236.dp to 236.dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(boxW)
                .height(boxH)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                )
        ) {
            CompositePreviewCanvas(
                clips = state.clips,
                mediaRegistry = mediaRegistry,
                timeSec = state.playheadSec,
                modifier = Modifier.fillMaxSize()
            )

            // Safe Area Dashed Guides Overlay
            if (state.showSafeGuides) {
                val guideColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    drawRoundRect(
                        color = guideColor,
                        cornerRadius = CornerRadius(12f, 12f),
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.78f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${state.aspect.label} REC",
                                style = LabelCapsStyle.copy(fontSize = 8.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.78f))
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryContainerCyan)
                            )
                            Text(
                                "CANVAS 60",
                                style = TimecodeSmallStyle.copy(fontSize = 8.sp),
                                color = PrimaryContainerCyan
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "SAFE FRAME",
                            style = LabelCapsStyle.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.7f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                        val sc = ((state.selectedClip?.scale ?: 1f) * 100f).roundToInt()
                        Text(
                            "SCALE $sc%",
                            style = TimecodeSmallStyle.copy(fontSize = 8.sp),
                            color = PrimaryContainerCyan,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.7f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Quick Rotate & Guides Toggle Buttons
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 34.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f))
                        .clickable(onClick = onQuickRotate)
                        .testTag("quick_rotate_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.RotateRight,
                        contentDescription = stringResource(R.string.cd_rotate_quick),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f))
                        .clickable(onClick = onToggleGuides)
                        .testTag("toggle_guides_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.GridOn,
                        contentDescription = stringResource(R.string.cd_toggle_guides),
                        tint = if (state.showSafeGuides) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TransportStrip(
    state: EditorUiState,
    onJumpStart: () -> Unit,
    onStepBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onStepForward: () -> Unit,
    onJumpEnd: () -> Unit,
    onSplit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Timecode + Frame counter
        Column {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = formatTimecode(state.playheadSec),
                    style = TimecodeTextStyle,
                    color = PrimaryFixed
                )
                Text(
                    text = "/ ${formatTimecode(state.totalDurationSec)}",
                    style = TimecodeSmallStyle,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                )
                Text(
                    text = "FRAME ${(state.playheadSec * 30).toInt()}",
                    style = LabelCapsStyle.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Transport Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TransportIconButton(Icons.Filled.SkipPrevious, stringResource(R.string.cd_jump_start), "jump_start_button", onJumpStart)
            TransportIconButton(Icons.Filled.Replay5, stringResource(R.string.cd_step_back), "step_back_button", onStepBack)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(10.dp, CircleShape, ambientColor = PrimaryContainerCyan, spotColor = PrimaryContainerCyan)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onTogglePlay)
                    .testTag("play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play_pause),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            TransportIconButton(Icons.Filled.Forward5, stringResource(R.string.cd_step_forward), "step_forward_button", onStepForward)
            TransportIconButton(Icons.Filled.SkipNext, stringResource(R.string.cd_jump_end), "jump_end_button", onJumpEnd)
        }

        // Real-time VU Meter + Quick Split
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                VuBar(level = state.vuLeft, color = PrimaryContainerCyan)
                VuBar(level = state.vuRight, color = MaterialTheme.colorScheme.secondary)
            }

            Row(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable(onClick = onSplit)
                    .padding(horizontal = 8.dp)
                    .testTag("quick_split_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    Icons.Filled.ContentCut,
                    contentDescription = stringResource(R.string.cd_split),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text("Split", style = LabelCapsStyle, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun TransportIconButton(
    icon: ImageVector,
    cd: String,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = cd, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun VuBar(level: Float, color: Color) {
    Box(
        modifier = Modifier
            .width(4.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(level.coerceIn(0.08f, 1f))
                .background(color)
        )
    }
}

@Composable
private fun MultiTrackTimelineSection(
    state: EditorUiState,
    mediaRegistry: MediaRegistry,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onAddKeyframe: () -> Unit,
    onToggleSnap: () -> Unit,
    onSeek: (Float) -> Unit,
    onSelectClip: (String) -> Unit,
    onBeginClipGesture: () -> Unit,
    onMoveClipBy: (String, Float) -> Unit,
    onTrimLeft: (String, Float) -> Unit,
    onTrimRight: (String, Float) -> Unit,
    onOpenTransitionForClip: (String) -> Unit
) {
    val density = LocalDensity.current
    val pxPerSecDp = state.pxPerSec.dp
    val totalSecs = max(14, ceil(state.totalDurationSec).toInt() + 4)
    val trackWidthDp = (totalSecs * state.pxPerSec).dp
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        // Zoom / Keyframe / Snap bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable(onClick = onZoomOut)
                        .testTag("zoom_out_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Remove, stringResource(R.string.cd_zoom_out), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                }
                Text(
                    "${((state.pxPerSec / 64f) * 100f).roundToInt()}%",
                    style = TimecodeSmallStyle,
                    color = MaterialTheme.colorScheme.primary
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable(onClick = onZoomIn)
                        .testTag("zoom_in_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, stringResource(R.string.cd_zoom_in), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable(onClick = onAddKeyframe)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("add_keyframe_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("◆", color = PrimaryContainerCyan, fontSize = 10.sp)
                    Text("Keyframe", style = LabelCapsStyle, color = PrimaryContainerCyan)
                }

                Text(
                    text = if (state.snapEnabled) "SNAP ON" else "SNAP OFF",
                    style = LabelCapsStyle,
                    color = if (state.snapEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .clickable(onClick = onToggleSnap)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("snap_toggle_button")
                )
            }
        }

        // Fixed Left Labels + Scrollable Multi-track Lane Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Left Fixed Track Headers Column
            Column(
                modifier = Modifier
                    .width(52.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Spacer(Modifier.height(22.dp)) // Ruler spacer
                TrackHeaderCell("T1", Icons.Filled.Title, MaterialTheme.colorScheme.primary, 32.dp)
                TrackHeaderCell("V2", Icons.Filled.Layers, MaterialTheme.colorScheme.secondary, 42.dp)
                TrackHeaderCell("V1", Icons.Filled.Movie, MaterialTheme.colorScheme.primary, 52.dp, bold = true)
                TrackHeaderCell("A1", Icons.Filled.GraphicEq, MaterialTheme.colorScheme.secondary, 40.dp)
            }

            // Horizontally Scrollable Tracks + Ruler + Master Playhead
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
            ) {
                Box(modifier = Modifier.width(trackWidthDp + 80.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Time Ruler (tap or drag to scrub)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .pointerInput(state.pxPerSec) {
                                    detectTapGestures { offset ->
                                        val pxPerSecReal = with(density) { state.pxPerSec.dp.toPx() }
                                        onSeek(offset.x / max(1f, pxPerSecReal))
                                    }
                                }
                                .pointerInput(state.pxPerSec) {
                                    detectHorizontalDragGestures { change, _ ->
                                        change.consume()
                                        val pxPerSecReal = with(density) { state.pxPerSec.dp.toPx() }
                                        onSeek(change.position.x / max(1f, pxPerSecReal))
                                    }
                                }
                        ) {
                            for (s in 0..totalSecs step 2) {
                                Text(
                                    text = "00:${s.toString().padStart(2, '0')}",
                                    style = TimecodeSmallStyle.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.offset(x = (s * state.pxPerSec).dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            )
                        }

                        // T1 Text & Stickers Track
                        TrackLaneRow(
                            height = 32.dp,
                            clips = state.clips.filter { it.track == TrackType.TEXT },
                            selectedId = state.selectedClipId,
                            pxPerSec = state.pxPerSec,
                            mediaRegistry = mediaRegistry,
                            onSelect = onSelectClip,
                            onBeginGesture = onBeginClipGesture,
                            onMoveBy = onMoveClipBy,
                            onTrimLeft = onTrimLeft,
                            onTrimRight = onTrimRight,
                            onOpenTransition = onOpenTransitionForClip
                        )

                        // V2 Overlay / PIP Track
                        TrackLaneRow(
                            height = 42.dp,
                            clips = state.clips.filter { it.track == TrackType.OVERLAY },
                            selectedId = state.selectedClipId,
                            pxPerSec = state.pxPerSec,
                            mediaRegistry = mediaRegistry,
                            onSelect = onSelectClip,
                            onBeginGesture = onBeginClipGesture,
                            onMoveBy = onMoveClipBy,
                            onTrimLeft = onTrimLeft,
                            onTrimRight = onTrimRight,
                            onOpenTransition = onOpenTransitionForClip
                        )

                        // V1 Master Video Track
                        TrackLaneRow(
                            height = 52.dp,
                            clips = state.clips.filter { it.track == TrackType.VIDEO },
                            selectedId = state.selectedClipId,
                            pxPerSec = state.pxPerSec,
                            mediaRegistry = mediaRegistry,
                            onSelect = onSelectClip,
                            onBeginGesture = onBeginClipGesture,
                            onMoveBy = onMoveClipBy,
                            onTrimLeft = onTrimLeft,
                            onTrimRight = onTrimRight,
                            onOpenTransition = onOpenTransitionForClip
                        )

                        // A1 Audio / BGM / Voiceover Track
                        TrackLaneRow(
                            height = 40.dp,
                            clips = state.clips.filter { it.track == TrackType.AUDIO },
                            selectedId = state.selectedClipId,
                            pxPerSec = state.pxPerSec,
                            mediaRegistry = mediaRegistry,
                            onSelect = onSelectClip,
                            onBeginGesture = onBeginClipGesture,
                            onMoveBy = onMoveClipBy,
                            onTrimLeft = onTrimLeft,
                            onTrimRight = onTrimRight,
                            onOpenTransition = onOpenTransitionForClip
                        )
                    }

                    // Master Playhead Needle Overlay
                    val playheadX = (state.playheadSec * state.pxPerSec).dp
                    Column(
                        modifier = Modifier
                            .offset(x = playheadX - 10.dp)
                            .width(20.dp)
                            .height(216.dp)
                            .pointerInput(state.pxPerSec, state.playheadSec) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    change.consume()
                                    val pxPerSecReal = with(density) { state.pxPerSec.dp.toPx() }
                                    onSeek(state.playheadSec + dragAmount / max(1f, pxPerSecReal))
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 18.dp, height = 22.dp)
                                .shadow(8.dp, RoundedCornerShape(4.dp), ambientColor = PrimaryContainerCyan, spotColor = PrimaryContainerCyan)
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryContainerCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = "Playhead handle",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .weight(1f)
                                .background(PrimaryContainerCyan)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackHeaderCell(
    label: String,
    icon: ImageVector,
    tint: Color,
    height: Dp,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = LabelCapsStyle.copy(fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold),
            color = if (bold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun TrackLaneRow(
    height: Dp,
    clips: List<TimelineClip>,
    selectedId: String?,
    pxPerSec: Float,
    mediaRegistry: MediaRegistry,
    onSelect: (String) -> Unit,
    onBeginGesture: () -> Unit,
    onMoveBy: (String, Float) -> Unit,
    onTrimLeft: (String, Float) -> Unit,
    onTrimRight: (String, Float) -> Unit,
    onOpenTransition: (String) -> Unit
) {
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        clips.forEach { clip ->
            val isSelected = clip.id == selectedId
            val leftDp = (clip.startTime * pxPerSec).dp
            val widthDp = max(36f, clip.duration * pxPerSec).dp

            Row(
                modifier = Modifier
                    .offset(x = leftDp)
                    .width(widthDp)
                    .fillMaxHeight()
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) PrimaryContainerCyan else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .testTag("clip_${clip.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Trim Handle
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .fillMaxHeight()
                        .background(
                            if (isSelected) PrimaryContainerCyan
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                        .pointerInput(clip.id, pxPerSec) {
                            detectHorizontalDragGestures(
                                onDragStart = { onBeginGesture() },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    val pxSec = with(density) { pxPerSec.dp.toPx() }
                                    onTrimLeft(clip.id, dragAmount / max(1f, pxSec))
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline)
                    )
                }

                // Center Draggable Clip Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (clip.track == TrackType.TEXT) PrimaryContainerCyan.copy(alpha = 0.16f)
                            else Color.Transparent
                        )
                        .clickable { onSelect(clip.id) }
                        .pointerInput(clip.id, pxPerSec) {
                            detectHorizontalDragGestures(
                                onDragStart = { onBeginGesture() },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    val pxSec = with(density) { pxPerSec.dp.toPx() }
                                    onMoveBy(clip.id, dragAmount / max(1f, pxSec))
                                }
                            )
                        }
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    when (clip.track) {
                        TrackType.AUDIO -> {
                            val wave = mediaRegistry.getWaveform(clip.mediaId)
                            val waveColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
                            Canvas(modifier = Modifier.fillMaxSize().padding(vertical = 4.dp)) {
                                if (wave.isNotEmpty()) {
                                    val barW = size.width / wave.size
                                    wave.forEachIndexed { idx, v ->
                                        val h = size.height * v.coerceIn(0.12f, 1f)
                                        val y = (size.height - h) / 2f
                                        drawLine(
                                            color = waveColor,
                                            start = Offset(idx * barW + barW / 2f, y),
                                            end = Offset(idx * barW + barW / 2f, y + h),
                                            strokeWidth = max(2f, barW * 0.45f)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = clip.name,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.78f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        TrackType.TEXT -> {
                            val label = if (clip.type == ClipType.STICKER) {
                                "${clip.stickerChar ?: "⚡"} Sticker"
                            } else {
                                clip.textProps?.content ?: clip.name
                            }
                            Text(
                                text = label,
                                style = LabelCapsStyle,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        else -> {
                            Column {
                                Text(
                                    text = clip.name,
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${String.format("%.1f", clip.duration)}s",
                                        style = TimecodeSmallStyle.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    if (clip.speed != 1f || clip.speedCurve != null) {
                                        Text(
                                            text = if (clip.speedCurve != null) "Curve" else "${String.format("%.1f", clip.speed)}x",
                                            style = TimecodeSmallStyle.copy(fontSize = 9.sp),
                                            color = TertiaryFixedDim
                                        )
                                    }
                                    if (clip.keyframes.isNotEmpty()) {
                                        Text(
                                            text = "◆${clip.keyframes.size}",
                                            style = TimecodeSmallStyle.copy(fontSize = 9.sp),
                                            color = PrimaryContainerCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Trim Handle
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .fillMaxHeight()
                        .background(
                            if (isSelected) PrimaryContainerCyan
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                        .pointerInput(clip.id, pxPerSec) {
                            detectHorizontalDragGestures(
                                onDragStart = { onBeginGesture() },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    val pxSec = with(density) { pxPerSec.dp.toPx() }
                                    onTrimRight(clip.id, dragAmount / max(1f, pxSec))
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline)
                    )
                }
            }

            // Transition Badge on Video Track Clips
            if (clip.track == TrackType.VIDEO && clip.startTime > 0.1f) {
                val hasTx = clip.transitionIn != TransitionType.NONE
                Box(
                    modifier = Modifier
                        .offset(x = leftDp - 11.dp, y = (height - 22.dp) / 2)
                        .size(22.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (hasTx) PrimaryContainerCyan else SurfaceBright)
                        .clickable { onOpenTransition(clip.id) }
                        .testTag("transition_badge_${clip.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⧓",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasTx) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaIngestBanner(
    status: String,
    onImportClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryContainerCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.AddPhotoAlternate,
                    contentDescription = stringResource(R.string.cd_add_media),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.media_ingest_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = status,
                    style = LabelCapsStyle.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceBright)
                .clickable(onClick = onImportClick)
                .padding(horizontal = 10.dp)
                .testTag("add_media_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Text(
                stringResource(R.string.btn_add_media),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ClipContextRibbon(
    selectedClip: TimelineClip?,
    onAction: (String) -> Unit
) {
    val speedTxt = selectedClip?.let { "${String.format("%.1f", it.speed)}x" } ?: "1.0x"
    val volTxt = selectedClip?.let { "${(it.volume * 100f).roundToInt()}%" } ?: "100%"

    val buttons = listOf(
        Triple("split", "Split", Icons.Filled.Splitscreen),
        Triple("speed", speedTxt, Icons.Filled.Speed),
        Triple("volume", volTxt, Icons.Filled.VolumeUp),
        Triple("transform", "Crop/Rot", Icons.Filled.CropRotate),
        Triple("extract-audio", "Extract", Icons.Filled.AudioFile),
        Triple("duplicate", "Duplicate", Icons.Filled.ContentCopy),
        Triple("transition", "Transition", Icons.Filled.Compare),
        Triple("delete", "Delete", Icons.Filled.Delete)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        buttons.forEach { (action, label, icon) ->
            val isDanger = action == "delete"
            Column(
                modifier = Modifier
                    .width(58.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isDanger) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                    .clickable { onAction(action) }
                    .testTag("ribbon_$action"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isDanger) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = LabelCapsStyle.copy(fontSize = 9.sp),
                    color = if (isDanger) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
