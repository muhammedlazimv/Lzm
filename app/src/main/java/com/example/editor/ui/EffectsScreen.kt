package com.example.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.editor.engine.MediaRegistry
import com.example.editor.model.ColorAdjustments
import com.example.editor.model.EditorUiState
import com.example.editor.model.FX_PRESETS
import com.example.editor.model.FxCategory
import com.example.editor.model.TrackType
import com.example.ui.theme.LabelCapsStyle
import com.example.ui.theme.PrimaryContainerCyan
import com.example.ui.theme.TimecodeSmallStyle
import kotlin.math.roundToInt

@Composable
fun EffectsScreen(
    state: EditorUiState,
    mediaRegistry: MediaRegistry,
    onSplitRatioChange: (Float) -> Unit,
    onUpdateAdjustments: ((ColorAdjustments) -> ColorAdjustments) -> Unit,
    onResetFx: () -> Unit,
    onSelectCategory: (FxCategory) -> Unit,
    onSelectPreset: (String) -> Unit,
    onApplyToClip: () -> Unit,
    onApplyToTrack: () -> Unit
) {
    val activeClip = state.selectedClip?.takeIf { it.track == TrackType.VIDEO || it.track == TrackType.OVERLAY }
        ?: state.clips.firstOrNull { it.track == TrackType.VIDEO }
    val adj = activeClip?.adjustments ?: ColorAdjustments()
    val currentFilterId = activeClip?.filterId ?: "none"
    val activePresetName = FX_PRESETS.firstOrNull { it.id == currentFilterId }?.name?.uppercase() ?: "CUSTOM"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. LIVE SPLIT COMPARISON MONITOR
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .clipToBounds()
        ) {
            val totalWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val splitDp = maxWidth * state.fxSplitRatio

            // Processed Layer (Full Canvas)
            CompositePreviewCanvas(
                clips = state.clips,
                mediaRegistry = mediaRegistry,
                timeSec = state.playheadSec,
                skipFx = false,
                modifier = Modifier.fillMaxSize()
            )

            // Active FX Badge (top-right)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainerCyan)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = activePresetName,
                    style = LabelCapsStyle.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Original Unedited Layer (Clipped horizontally on left)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(splitDp)
                    .clipToBounds()
            ) {
                CompositePreviewCanvas(
                    clips = state.clips,
                    mediaRegistry = mediaRegistry,
                    timeSec = state.playheadSec,
                    skipFx = true,
                    modifier = Modifier
                        .width(this@BoxWithConstraints.maxWidth)
                        .fillMaxHeight()
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outline)
                    )
                    Text(
                        text = "ORIGINAL",
                        style = LabelCapsStyle.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Draggable Glowing Split Scrubber Handle
            Box(
                modifier = Modifier
                    .offset(x = splitDp - 20.dp)
                    .width(40.dp)
                    .fillMaxHeight()
                    .pointerInput(totalWidthPx, state.fxSplitRatio) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            if (totalWidthPx > 0f) {
                                onSplitRatioChange(state.fxSplitRatio + dragAmount / totalWidthPx)
                            }
                        }
                    }
                    .testTag("fx_split_scrubber"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(PrimaryContainerCyan)
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .shadow(10.dp, CircleShape, ambientColor = PrimaryContainerCyan, spotColor = PrimaryContainerCyan)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(PrimaryContainerCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.DragIndicator,
                            contentDescription = "Drag comparison divider",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 2. COLOR GRADING & FX SLIDERS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = PrimaryContainerCyan, modifier = Modifier.size(18.dp))
                    Text("Color Grading & FX", style = MaterialTheme.typography.headlineSmall)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onResetFx)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("fx_reset_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = "Reset filters", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(15.dp))
                    Text("RESET", style = LabelCapsStyle, color = MaterialTheme.colorScheme.secondary)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FxSliderCell("Brightness", adj.brightness, 40f..160f, Modifier.weight(1f)) { v ->
                        onUpdateAdjustments { it.copy(brightness = v) }
                    }
                    FxSliderCell("Contrast", adj.contrast, 40f..180f, Modifier.weight(1f)) { v ->
                        onUpdateAdjustments { it.copy(contrast = v) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FxSliderCell("Saturation", adj.saturation, 0f..200f, Modifier.weight(1f)) { v ->
                        onUpdateAdjustments { it.copy(saturation = v) }
                    }
                    FxSliderCell("RGB Chroma", adj.chroma, 0f..100f, Modifier.weight(1f)) { v ->
                        onUpdateAdjustments { it.copy(chroma = v) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FxSliderCell("Glow / Bloom", adj.glow, 0f..100f, Modifier.weight(1f)) { v ->
                        onUpdateAdjustments { it.copy(glow = v) }
                    }
                    FxSliderCell("Vignette", adj.vignette, 0f..100f, Modifier.weight(1f)) { v ->
                        onUpdateAdjustments { it.copy(vignette = v) }
                    }
                }
            }
        }

        // 3. FILTER PRESETS CATEGORY PILLS & GRID
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FxCategory.entries.forEach { cat ->
                    val active = state.activeFxCategory == cat
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) PrimaryContainerCyan else MaterialTheme.colorScheme.surfaceContainer
                            )
                            .clickable { onSelectCategory(cat) }
                            .padding(horizontal = 14.dp)
                            .testTag("fx_cat_${cat.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat.label,
                            style = LabelCapsStyle,
                            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            val presets = if (state.activeFxCategory == FxCategory.ALL) {
                FX_PRESETS
            } else {
                FX_PRESETS.filter { it.category == state.activeFxCategory }
            }

            presets.chunked(4).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { preset ->
                        val isSelected = currentFilterId == preset.id
                        val tintColor = preset.tintArgb?.let { Color(it) } ?: PrimaryContainerCyan.copy(alpha = 0.2f)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh
                                    else MaterialTheme.colorScheme.surfaceContainer
                                )
                                .then(
                                    if (isSelected) Modifier.border(2.dp, PrimaryContainerCyan, RoundedCornerShape(12.dp))
                                    else Modifier
                                )
                                .clickable { onSelectPreset(preset.id) }
                                .padding(6.dp)
                                .testTag("fx_preset_${preset.id}"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF131318), tintColor)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = preset.name,
                                    tint = PrimaryContainerCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                                if (preset.badge.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            preset.badge,
                                            style = LabelCapsStyle.copy(fontSize = 8.sp),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    repeat(4 - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            // 4. APPLY TO CLIP / WHOLE TRACK CTAs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApplyToClip,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("fx_apply_clip_button")
                ) {
                    Icon(Icons.Filled.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Apply to Clip", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onApplyToTrack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryContainerCyan,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("fx_apply_track_button")
                ) {
                    Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Whole Track", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FxSliderCell(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("$value%", style = TimecodeSmallStyle, color = PrimaryContainerCyan)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = range,
            colors = neonSliderColors()
        )
    }
}
