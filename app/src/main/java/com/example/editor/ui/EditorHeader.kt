package com.example.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.editor.model.AspectRatioPreset
import com.example.editor.model.EditorUiState
import com.example.editor.model.EditorView
import com.example.ui.theme.LabelCapsStyle
import com.example.ui.theme.PrimaryContainerCyan
import com.example.ui.theme.TimecodeSmallStyle

@Composable
fun EditorHeader(
    state: EditorUiState,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCycleAspect: () -> Unit,
    onOpenExport: () -> Unit
) {
    val titleRes = when (state.activeView) {
        EditorView.TIMELINE -> R.string.screen_timeline
        EditorView.EFFECTS -> R.string.screen_effects
        EditorView.SPEED -> R.string.screen_speed
        EditorView.EXPORT -> R.string.screen_export
    }

    val aspectIcon = when (state.aspect) {
        AspectRatioPreset.RATIO_9_16 -> Icons.Filled.CropPortrait
        AspectRatioPreset.RATIO_16_9 -> Icons.Filled.CropLandscape
        AspectRatioPreset.RATIO_1_1 -> Icons.Filled.CropSquare
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.96f),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("header_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.padding(end = 4.dp)) {
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${state.exportConfig.resolution}P ${state.exportConfig.fps}FPS",
                        style = TimecodeSmallStyle,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Undo / Redo Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onUndo,
                        enabled = state.canUndo,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = stringResource(R.string.cd_undo),
                            tint = if (state.canUndo) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onRedo,
                        enabled = state.canRedo,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = stringResource(R.string.cd_redo),
                            tint = if (state.canRedo) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Aspect Ratio Cycle Chip
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(onClick = onCycleAspect)
                        .padding(horizontal = 8.dp)
                        .testTag("aspect_cycle_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = aspectIcon,
                        contentDescription = stringResource(R.string.cd_aspect_ratio),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = state.aspect.label,
                        style = LabelCapsStyle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Glowing Cyan Export CTA
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(8.dp),
                            ambientColor = PrimaryContainerCyan,
                            spotColor = PrimaryContainerCyan
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onOpenExport)
                        .padding(horizontal = 10.dp)
                        .testTag("header_export_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.IosShare,
                            contentDescription = stringResource(R.string.header_export),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.header_export),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
