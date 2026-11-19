package com.example.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.editor.model.AspectRatioPreset
import com.example.editor.model.ClipType
import com.example.editor.model.ContextDrawerMode
import com.example.editor.model.EditorUiState
import com.example.editor.model.STICKER_OPTIONS
import com.example.editor.model.TEXT_COLOR_SWATCHES
import com.example.editor.model.TEXT_FONT_OPTIONS
import com.example.editor.model.TextAnimation
import com.example.editor.model.TextProps
import com.example.editor.model.TransitionType
import com.example.ui.theme.LabelCapsStyle
import com.example.ui.theme.TimecodeSmallStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContextInspectorDrawer(
    state: EditorUiState,
    onClose: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onTransformChange: (scale: Float?, rot: Float?, x: Float?, y: Float?, flipH: Boolean?) -> Unit,
    onResetTransform: () -> Unit,
    onTransitionSelect: (TransitionType) -> Unit,
    onSaveText: (TextProps) -> Unit,
    onAddSticker: (String) -> Unit,
    onImportAudio: () -> Unit,
    onToggleVoiceRecord: () -> Unit,
    onSelectAspect: (AspectRatioPreset) -> Unit
) {
    val clip = state.selectedClip
    val drawerTitle = when (state.drawerMode) {
        ContextDrawerMode.VOLUME -> "Volume — ${clip?.name ?: "Clip"}"
        ContextDrawerMode.TRANSFORM -> "Crop, Scale & Rotate — ${clip?.name ?: "Clip"}"
        ContextDrawerMode.TRANSITION -> "Transition In — ${clip?.name ?: "Clip"}"
        ContextDrawerMode.TEXT -> if (clip?.type == ClipType.TEXT) "Edit Animated Text" else "Add Animated Text"
        ContextDrawerMode.STICKER -> "Add Sticker Overlay (Track T1)"
        ContextDrawerMode.AUDIO -> "Audio, Music Import & Voiceover"
        ContextDrawerMode.CANVAS -> "Canvas Aspect Ratio Presets"
        ContextDrawerMode.NONE -> return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp)
            .testTag("context_inspector_drawer"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = drawerTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .testTag("close_drawer_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_close_drawer),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        when (state.drawerMode) {
            ContextDrawerMode.VOLUME -> {
                if (clip != null) {
                    val pct = (clip.volume * 100f).roundToInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Clip Gain", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "$pct%",
                            style = TimecodeSmallStyle,
                            color = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                    Slider(
                        value = clip.volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0f..2f,
                        colors = neonSliderColors(),
                        modifier = Modifier.testTag("volume_slider")
                    )
                    Button(
                        onClick = onToggleMute,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("mute_toggle_button")
                    ) {
                        Text(if (clip.muted) "Unmute Clip" else "Mute Clip")
                    }
                }
            }

            ContextDrawerMode.TRANSFORM -> {
                if (clip != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            SliderHeader("Scale", "${(clip.scale * 100).roundToInt()}%")
                            Slider(
                                value = clip.scale,
                                onValueChange = { onTransformChange(it, null, null, null, null) },
                                valueRange = 0.4f..2.5f,
                                colors = neonSliderColors()
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            SliderHeader("Rotate", "${clip.rotation.roundToInt()}°")
                            Slider(
                                value = clip.rotation,
                                onValueChange = { onTransformChange(null, it, null, null, null) },
                                valueRange = -180f..180f,
                                colors = neonSliderColors()
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            SliderHeader("Offset X", "${(clip.offsetX * 100).roundToInt()}%")
                            Slider(
                                value = clip.offsetX,
                                onValueChange = { onTransformChange(null, null, it, null, null) },
                                valueRange = -0.5f..0.5f,
                                colors = neonSliderColors()
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            SliderHeader("Offset Y", "${(clip.offsetY * 100).roundToInt()}%")
                            Slider(
                                value = clip.offsetY,
                                onValueChange = { onTransformChange(null, null, null, it, null) },
                                valueRange = -0.5f..0.5f,
                                colors = neonSliderColors()
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallActionChip(
                            label = "+90°",
                            icon = Icons.Filled.Rotate90DegreesCw,
                            onClick = { onTransformChange(null, (clip.rotation + 90f) % 360f, null, null, null) }
                        )
                        SmallActionChip(
                            label = "Flip H",
                            icon = Icons.Filled.Flip,
                            onClick = { onTransformChange(null, null, null, null, !clip.flipH) }
                        )
                        SmallActionChip(
                            label = "Reset",
                            icon = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            onClick = onResetTransform
                        )
                    }
                }
            }

            ContextDrawerMode.TRANSITION -> {
                if (clip != null) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TransitionType.entries.forEach { tx ->
                            val selected = clip.transitionIn == tx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                    .clickable { onTransitionSelect(tx) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("transition_${tx.name.lowercase()}")
                            ) {
                                Text(
                                    text = tx.label.uppercase(),
                                    style = LabelCapsStyle,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            ContextDrawerMode.TEXT -> {
                val existing = clip?.takeIf { it.type == ClipType.TEXT }?.textProps
                var content by remember(clip?.id) { mutableStateOf(existing?.content ?: "CYBER VLOG") }
                var fontName by remember(clip?.id) { mutableStateOf(existing?.fontName ?: TEXT_FONT_OPTIONS[0]) }
                var animation by remember(clip?.id) { mutableStateOf(existing?.animation ?: TextAnimation.POP) }
                var textColor by remember(clip?.id) { mutableStateOf(existing?.textColor ?: 0xFF002022L) }
                var bgColor by remember(clip?.id) { mutableStateOf(existing?.bgColor ?: 0xFF00F0FFL) }
                var hasBg by remember(clip?.id) { mutableStateOf(existing?.hasBg ?: true) }

                LaunchedEffect(existing) {
                    if (existing != null) {
                        content = existing.content
                        fontName = existing.fontName
                        animation = existing.animation
                        textColor = existing.textColor
                        bgColor = existing.bgColor
                        hasBg = existing.hasBg
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Enter overlay text...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("text_overlay_input")
                )

                // Font chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TEXT_FONT_OPTIONS.forEach { f ->
                        val active = fontName == f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (active) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable { fontName = f }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                f,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Animation chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextAnimation.entries.forEach { anim ->
                        val active = animation == anim
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (active) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable { animation = anim }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                anim.label,
                                style = LabelCapsStyle,
                                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Text & Badge color swatches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Ink", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        TEXT_COLOR_SWATCHES.take(5).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(
                                        width = if (textColor == c) 2.dp else 1.dp,
                                        color = if (textColor == c) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .clickable { textColor = c }
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (hasBg) MaterialTheme.colorScheme.surfaceContainerHigh
                                else MaterialTheme.colorScheme.surfaceContainerLowest
                            )
                            .clickable { hasBg = !hasBg }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            if (hasBg) "Badge ON" else "No Badge",
                            style = LabelCapsStyle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (hasBg) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Bg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        TEXT_COLOR_SWATCHES.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(
                                        width = if (bgColor == c) 2.dp else 1.dp,
                                        color = if (bgColor == c) Color.White else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .clickable { bgColor = c }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        onSaveText(
                            TextProps(
                                content = content.ifBlank { "TITLE" },
                                fontName = fontName,
                                textColor = textColor,
                                bgColor = bgColor,
                                hasBg = hasBg,
                                sizePx = 44f,
                                animation = animation
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_text_overlay_button")
                ) {
                    Text(
                        if (existing != null) "Update Text Overlay" else "Add to Track T1",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            ContextDrawerMode.STICKER -> {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    STICKER_OPTIONS.forEachIndexed { idx, emoji ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { onAddSticker(emoji) }
                                .testTag("sticker_option_$idx"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }
            }

            ContextDrawerMode.AUDIO -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onImportAudio,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("import_bgm_button")
                    ) {
                        Icon(
                            Icons.Filled.LibraryMusic,
                            contentDescription = "Import audio file",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Import BGM", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onToggleVoiceRecord,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isRecordingVoice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer,
                            contentColor = if (state.isRecordingVoice) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("record_voiceover_button")
                    ) {
                        Icon(
                            imageVector = if (state.isRecordingVoice) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = "Record voiceover",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (state.isRecordingVoice) {
                                "Stop ${String.format("%.1f", state.recordingSeconds)}s"
                            } else "Voiceover",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    text = if (state.isRecordingVoice) {
                        "Recording live microphone PCM directly onto Track A1..."
                    } else {
                        "Recorded voiceovers and imported audio tracks appear on Track A1 at the playhead."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            ContextDrawerMode.CANVAS -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AspectRatioPreset.entries.forEach { preset ->
                        val active = state.aspect == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (active) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable { onSelectAspect(preset) }
                                .testTag("canvas_ratio_${preset.label}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            ContextDrawerMode.NONE -> Unit
        }
    }
}

@Composable
private fun SliderHeader(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = TimecodeSmallStyle, color = MaterialTheme.colorScheme.primaryContainer)
    }
}

@Composable
private fun SmallActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(15.dp))
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = tint)
    }
}

@Composable
fun neonSliderColors() = SliderDefaults.colors(
    thumbColor = MaterialTheme.colorScheme.primaryContainer,
    activeTrackColor = MaterialTheme.colorScheme.primaryContainer,
    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
)
