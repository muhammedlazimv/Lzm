package com.example.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.editor.model.ContextDrawerMode
import com.example.editor.model.EditorUiState
import com.example.editor.model.EditorView
import com.example.ui.theme.LabelCapsStyle

private data class BottomNavItem(
    val id: String,
    val labelRes: Int,
    val icon: ImageVector,
    val isActive: (EditorUiState) -> Boolean,
    val onClick: () -> Unit
)

@Composable
fun BottomToolBar(
    state: EditorUiState,
    onSelectEdit: () -> Unit,
    onSelectAudio: () -> Unit,
    onSelectText: () -> Unit,
    onSelectStickers: () -> Unit,
    onSelectOverlay: () -> Unit,
    onSelectEffects: () -> Unit,
    onSelectSpeed: () -> Unit,
    onSelectCanvas: () -> Unit
) {
    val items = listOf(
        BottomNavItem(
            id = "nav_edit",
            labelRes = R.string.nav_edit,
            icon = Icons.Filled.MovieCreation,
            isActive = { it.activeView == EditorView.TIMELINE && it.drawerMode == ContextDrawerMode.NONE },
            onClick = onSelectEdit
        ),
        BottomNavItem(
            id = "nav_audio",
            labelRes = R.string.nav_audio,
            icon = Icons.Filled.GraphicEq,
            isActive = { it.activeView == EditorView.TIMELINE && it.drawerMode == ContextDrawerMode.AUDIO },
            onClick = onSelectAudio
        ),
        BottomNavItem(
            id = "nav_text",
            labelRes = R.string.nav_text,
            icon = Icons.Filled.Title,
            isActive = { it.activeView == EditorView.TIMELINE && it.drawerMode == ContextDrawerMode.TEXT },
            onClick = onSelectText
        ),
        BottomNavItem(
            id = "nav_stickers",
            labelRes = R.string.nav_stickers,
            icon = Icons.Filled.AddReaction,
            isActive = { it.activeView == EditorView.TIMELINE && it.drawerMode == ContextDrawerMode.STICKER },
            onClick = onSelectStickers
        ),
        BottomNavItem(
            id = "nav_overlay",
            labelRes = R.string.nav_overlay,
            icon = Icons.Filled.Layers,
            isActive = { false },
            onClick = onSelectOverlay
        ),
        BottomNavItem(
            id = "nav_effects",
            labelRes = R.string.nav_effects,
            icon = Icons.Filled.AutoAwesome,
            isActive = { it.activeView == EditorView.EFFECTS },
            onClick = onSelectEffects
        ),
        BottomNavItem(
            id = "nav_speed",
            labelRes = R.string.nav_speed,
            icon = Icons.Filled.Speed,
            isActive = { it.activeView == EditorView.SPEED },
            onClick = onSelectSpeed
        ),
        BottomNavItem(
            id = "nav_canvas",
            labelRes = R.string.nav_canvas,
            icon = Icons.Filled.AspectRatio,
            isActive = { it.activeView == EditorView.TIMELINE && it.drawerMode == ContextDrawerMode.CANVAS },
            onClick = onSelectCanvas
        )
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.97f),
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                val active = item.isActive(state)
                val fg = if (active) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                val bg = if (active) {
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
                } else {
                    Color.Transparent
                }
                Column(
                    modifier = Modifier
                        .width(58.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable(onClick = item.onClick)
                        .testTag(item.id),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.labelRes),
                        tint = fg,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(item.labelRes),
                        style = LabelCapsStyle.copy(fontSize = 9.sp),
                        color = fg,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
