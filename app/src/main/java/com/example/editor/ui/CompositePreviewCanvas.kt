package com.example.editor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.example.editor.engine.CanvasCompositor
import com.example.editor.engine.MediaRegistry
import com.example.editor.model.TimelineClip

@Composable
fun CompositePreviewCanvas(
    clips: List<TimelineClip>,
    mediaRegistry: MediaRegistry,
    timeSec: Float,
    skipFx: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawIntoCanvas { composeCanvas ->
            CanvasCompositor.renderComposite(
                canvas = composeCanvas.nativeCanvas,
                width = size.width,
                height = size.height,
                clips = clips,
                mediaRegistry = mediaRegistry,
                timeSec = timeSec,
                skipFx = skipFx
            )
        }
    }
}
