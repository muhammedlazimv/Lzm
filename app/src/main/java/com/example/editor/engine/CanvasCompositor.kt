package com.example.editor.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.editor.model.ClipType
import com.example.editor.model.ColorAdjustments
import com.example.editor.model.FX_PRESETS
import com.example.editor.model.ProceduralVariant
import com.example.editor.model.TextAnimation
import com.example.editor.model.TimelineClip
import com.example.editor.model.TrackType
import com.example.editor.model.TransitionType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

object CanvasCompositor {

    fun renderComposite(
        canvas: Canvas,
        width: Float,
        height: Float,
        clips: List<TimelineClip>,
        mediaRegistry: MediaRegistry,
        timeSec: Float,
        skipFx: Boolean = false
    ) {
        val bgPaint = Paint().apply { color = Color.parseColor("#08080C") }
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        val trackOrder = listOf(TrackType.VIDEO, TrackType.OVERLAY, TrackType.TEXT)
        for (track in trackOrder) {
            val active = clips.filter {
                it.track == track && timeSec >= it.startTime && timeSec <= it.startTime + it.duration
            }
            for (clip in active) {
                drawVisualClip(canvas, width, height, clip, mediaRegistry, timeSec, skipFx)
            }
        }
    }

    private fun drawVisualClip(
        canvas: Canvas,
        w: Float,
        h: Float,
        clip: TimelineClip,
        mediaRegistry: MediaRegistry,
        timeSec: Float,
        skipFx: Boolean
    ) {
        val localTime = max(0f, timeSec - clip.startTime)
        val kf = clip.evalKeyframes(localTime)

        var txAlpha = kf.opacity
        var txOffsetX = 0f
        var txScale = 1f
        if (clip.transitionIn != TransitionType.NONE && localTime < clip.transitionDuration) {
            val p = (localTime / max(0.05f, clip.transitionDuration)).coerceIn(0f, 1f)
            when (clip.transitionIn) {
                TransitionType.CROSSFADE -> txAlpha *= p
                TransitionType.SLIDE -> txOffsetX = (1f - p) * w
                TransitionType.ZOOM -> txScale = 1.6f - 0.6f * p
                TransitionType.GLITCH -> {
                    txOffsetX = sin(p * 40f) * (1f - p) * (w * 0.05f)
                    txAlpha *= min(1f, p * 1.4f)
                }
                TransitionType.NONE -> Unit
            }
        }

        val clampedAlpha = (txAlpha.coerceIn(0f, 1f) * 255).toInt()
        val layerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = clampedAlpha
            if (!skipFx && (clip.track == TrackType.VIDEO || clip.track == TrackType.OVERLAY)) {
                colorFilter = buildColorFilter(clip.adjustments, clip.filterId == "noir")
            }
        }

        val saveId = canvas.saveLayer(0f, 0f, w, h, layerPaint)
        canvas.translate(w / 2f + kf.x * w + txOffsetX, h / 2f + kf.y * h)
        canvas.rotate(kf.rotation)
        canvas.scale((if (clip.flipH) -1f else 1f) * kf.scale * txScale, kf.scale * txScale)
        canvas.translate(-w / 2f, -h / 2f)

        when (clip.type) {
            ClipType.PROCEDURAL -> {
                val animTime = localTime * clip.evalSpeedAt(localTime) + clip.trimStart
                drawProceduralScene(canvas, w, h, clip.variant, animTime)
            }

            ClipType.IMAGE -> {
                val asset = mediaRegistry.get(clip.mediaId) as? MediaAsset.ImageAsset
                if (asset != null) {
                    drawBitmapFitted(canvas, w, h, asset.bitmap, isOverlay = clip.track == TrackType.OVERLAY)
                }
            }

            ClipType.VIDEO -> {
                val asset = mediaRegistry.get(clip.mediaId) as? MediaAsset.VideoAsset
                val sampleTime = clip.trimStart + localTime * clip.evalSpeedAt(localTime)
                val frame = asset?.frameAt(sampleTime)
                if (frame != null) {
                    drawBitmapFitted(canvas, w, h, frame, isOverlay = clip.track == TrackType.OVERLAY)
                } else {
                    drawProceduralScene(canvas, w, h, ProceduralVariant.NEON_TUNNEL, sampleTime)
                }
            }

            ClipType.TEXT -> {
                drawTextClip(canvas, w, h, clip, localTime)
            }

            ClipType.STICKER -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = min(w, h) * 0.22f
                    textAlign = Paint.Align.CENTER
                }
                val fm = paint.fontMetrics
                val baseline = h / 2f - (fm.ascent + fm.descent) / 2f
                canvas.drawText(clip.stickerChar ?: "⚡", w / 2f, baseline, paint)
            }

            ClipType.AUDIO -> Unit
        }

        canvas.restoreToCount(saveId)

        // Post-FX Overlays on Master Video Track
        if (!skipFx && clip.track == TrackType.VIDEO) {
            val adj = clip.adjustments
            val preset = FX_PRESETS.firstOrNull { it.id == clip.filterId }
            if (preset?.tintArgb != null) {
                val tintPaint = Paint().apply { color = preset.tintArgb }
                canvas.drawRect(0f, 0f, w, h, tintPaint)
            }
            if (adj.chroma > 5) {
                val shift = (adj.chroma / 100f) * (w * 0.025f)
                val alphaC = ((adj.chroma / 100f) * 48).toInt().coerceIn(0, 80)
                val cyanPaint = Paint().apply { color = Color.argb(alphaC, 0, 240, 255) }
                val magPaint = Paint().apply { color = Color.argb(alphaC, 255, 0, 90) }
                canvas.drawRect(-shift, 0f, w - shift, h, cyanPaint)
                canvas.drawRect(shift, 0f, w + shift, h, magPaint)
            }
            if (adj.glow > 25) {
                val glowAlpha = (((adj.glow - 25) / 75f) * 65).toInt().coerceIn(0, 80)
                val glowPaint = Paint().apply {
                    shader = RadialGradient(
                        w * 0.5f, h * 0.4f, max(w, h) * 0.55f,
                        intArrayOf(Color.argb(glowAlpha, 0, 240, 255), Color.TRANSPARENT),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, glowPaint)
            }
            if (clip.filterId == "vhs") {
                val scanPaint = Paint().apply { color = Color.argb(18, 255, 255, 255) }
                var y = 0f
                val step = max(4f, h / 90f)
                while (y < h) {
                    canvas.drawRect(0f, y, w, y + step * 0.35f, scanPaint)
                    y += step
                }
            }
            if (adj.vignette > 0) {
                val vigAlpha = ((adj.vignette / 100f) * 210).toInt().coerceIn(0, 225)
                val vigPaint = Paint().apply {
                    shader = RadialGradient(
                        w / 2f, h / 2f, max(w, h) * 0.72f,
                        intArrayOf(Color.TRANSPARENT, Color.argb(vigAlpha, 0, 0, 0)),
                        floatArrayOf(0.4f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, vigPaint)
            }
        }
    }

    private fun drawBitmapFitted(
        canvas: Canvas,
        w: Float,
        h: Float,
        bitmap: android.graphics.Bitmap,
        isOverlay: Boolean
    ) {
        val bw = max(1f, bitmap.width.toFloat())
        val bh = max(1f, bitmap.height.toFloat())
        val scale = if (isOverlay) {
            min((w * 0.65f) / bw, (h * 0.65f) / bh)
        } else {
            max(w / bw, h / bh)
        }
        val dw = bw * scale
        val dh = bh * scale
        val left = (w - dw) / 2f
        val top = (h - dh) / 2f
        val dst = RectF(left, top, left + dw, top + dh)
        val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(bitmap, null, dst, bmpPaint)
        if (isOverlay) {
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(2f, w * 0.006f)
                color = Color.argb(160, 0, 240, 255)
            }
            canvas.drawRoundRect(dst, 12f, 12f, border)
        }
    }

    private fun drawTextClip(
        canvas: Canvas,
        w: Float,
        h: Float,
        clip: TimelineClip,
        localTime: Float
    ) {
        val tp = clip.textProps ?: return
        var animScale = 1f
        var animY = 0f
        var animAlpha = 1f
        when (tp.animation) {
            TextAnimation.POP -> if (localTime < 0.35f) animScale = 0.5f + (localTime / 0.35f) * 0.5f
            TextAnimation.SLIDE -> if (localTime < 0.4f) animY = (1f - localTime / 0.4f) * (h * 0.08f)
            TextAnimation.FADE -> if (localTime < 0.5f) animAlpha = localTime / 0.5f
            TextAnimation.TYPEWRITER -> Unit
        }
        val textToShow = if (tp.animation == TextAnimation.TYPEWRITER) {
            val chars = max(1, (localTime * 12f).toInt()).coerceAtMost(tp.content.length)
            tp.content.take(chars)
        } else tp.content

        val fontScale = w / 540f
        val computedSize = (tp.sizePx * fontScale * animScale).coerceIn(14f, w * 0.25f)

        val tf = when (tp.fontName) {
            "JetBrains Mono" -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            "Serif Bold" -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            "Condensed" -> Typeface.create("sans-serif-condensed", Typeface.BOLD)
            else -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = computedSize
            typeface = tf
            color = tp.textColor.toInt()
            alpha = (animAlpha * 255).toInt()
            textAlign = Paint.Align.CENTER
        }

        val textWidth = textPaint.measureText(textToShow)
        val padX = computedSize * 0.65f
        val padY = computedSize * 0.42f
        val boxW = textWidth + padX * 2f
        val boxH = computedSize + padY * 2f
        val cx = w / 2f
        val cy = h / 2f + animY

        if (tp.hasBg) {
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tp.bgColor.toInt()
                alpha = (animAlpha * 75).toInt()
            }
            val glowRect = RectF(
                cx - boxW / 2f - 6f * fontScale,
                cy - boxH / 2f - 6f * fontScale,
                cx + boxW / 2f + 6f * fontScale,
                cy + boxH / 2f + 6f * fontScale
            )
            canvas.drawRoundRect(glowRect, 18f * fontScale, 18f * fontScale, glowPaint)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tp.bgColor.toInt()
                alpha = (animAlpha * 255).toInt()
            }
            val rect = RectF(cx - boxW / 2f, cy - boxH / 2f, cx + boxW / 2f, cy + boxH / 2f)
            canvas.drawRoundRect(rect, 14f * fontScale, 14f * fontScale, bgPaint)
        }

        val fm = textPaint.fontMetrics
        val baseline = cy - (fm.ascent + fm.descent) / 2f
        canvas.drawText(textToShow, cx, baseline, textPaint)
    }

    private fun drawProceduralScene(
        canvas: Canvas,
        w: Float,
        h: Float,
        variant: ProceduralVariant,
        t: Float
    ) {
        if (variant == ProceduralVariant.CYBER_RAIN) {
            val skyPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(
                        Color.parseColor("#0A0E1A"),
                        Color.parseColor("#15122B"),
                        Color.parseColor("#090B12")
                    ),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, w, h, skyPaint)

            // Glowing Horizon Orb
            val pulse = sin(t * 2.5f) * (w * 0.025f)
            val orbRadius = max(20f, w * 0.35f + pulse)
            val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    w * 0.5f, h * 0.36f, orbRadius,
                    intArrayOf(
                        Color.argb(220, 0, 240, 255),
                        Color.argb(95, 58, 144, 255),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0.08f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, w, h, orbPaint)

            // Skyline silhouettes + neon windows
            val bldgPaint = Paint().apply { color = Color.parseColor("#0B0D17") }
            val winCyan = Paint().apply { color = Color.argb(120, 0, 240, 255) }
            val winPink = Paint().apply { color = Color.argb(110, 255, 100, 150) }
            val cols = 10
            val horizon = h * 0.62f
            for (i in 0 until cols) {
                val bw = w / cols + 4f
                val bh = h * (0.25f + ((i * 37) % 5) * 0.055f)
                val x = i * (w / cols) - 2f
                canvas.drawRect(x, horizon - bh, x + bw, horizon + 10f, bldgPaint)
                val wp = if (i % 2 == 0) winCyan else winPink
                for (wy in 0 until 6) {
                    val topY = horizon - bh + (h * 0.02f) + wy * (h * 0.032f)
                    if (topY < horizon - 6f) {
                        canvas.drawRect(
                            x + bw * 0.18f,
                            topY,
                            x + bw * 0.65f,
                            topY + max(2f, h * 0.007f),
                            wp
                        )
                    }
                }
            }

            // Perspective Neon Grid Floor
            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(125, 0, 240, 255)
                strokeWidth = max(1.5f, w * 0.003f)
            }
            for (i in -6..6) {
                canvas.drawLine(
                    w * 0.5f + i * (w * 0.035f),
                    horizon,
                    w * 0.5f + i * (w * 0.22f),
                    h,
                    gridPaint
                )
            }
            for (r in 0 until 9) {
                val prog = (r / 9f) + ((t * 0.45f) % (1f / 9f))
                val y = horizon + prog.toDouble().pow(2.2).toFloat() * (h - horizon)
                canvas.drawLine(0f, y, w, y, gridPaint)
            }

            // Cyber Rain Streaks
            val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(95, 125, 244, 255)
                strokeWidth = max(1f, w * 0.0022f)
            }
            val streakLen = h * 0.035f
            for (i in 0 until 28) {
                val rx = ((i * 73f + t * 80f) % w + w) % w
                val ry = ((i * 137f + t * 480f) % h + h) % h
                canvas.drawLine(rx, ry, rx - streakLen * 0.16f, ry + streakLen, rainPaint)
            }
        } else {
            // Neon Tunnel
            val bg = Paint().apply { color = Color.parseColor("#0B0816") }
            canvas.drawRect(0f, 0f, w, h, bg)
            val cx = w * 0.5f
            val cy = h * 0.48f
            val maxDim = max(w, h)
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
            }
            for (i in 0 until 12) {
                val phase = ((i / 12f) + (t * 0.35f)) % 1f
                val size = phase * phase * maxDim * 0.9f
                val alpha = ((1f - phase * 0.6f) * 255).toInt().coerceIn(30, 255)
                strokePaint.color = if (i % 2 == 0) {
                    Color.argb(alpha, 0, 240, 255)
                } else {
                    Color.argb(alpha, 255, 110, 160)
                }
                strokePaint.strokeWidth = max(2f, (2f + phase * 3f) * (w / 540f))
                canvas.save()
                canvas.translate(cx, cy)
                canvas.rotate((t * 18f) + i * 7f)
                canvas.drawRect(-size / 2f, -size / 2f, size / 2f, size / 2f, strokePaint)
                canvas.restore()
            }
        }
    }

    private fun buildColorFilter(adj: ColorAdjustments, isNoir: Boolean): ColorMatrixColorFilter {
        val cm = ColorMatrix()

        // Saturation
        val sat = if (isNoir) 0f else (adj.saturation / 100f).coerceIn(0f, 2.5f)
        val satMatrix = ColorMatrix().apply { setSaturation(sat) }
        cm.postConcat(satMatrix)

        // Contrast & Brightness
        val contrast = (adj.contrast / 100f).coerceIn(0.3f, 2.2f)
        val brightness = (adj.brightness - 100) * 1.4f
        val translate = (-0.5f * contrast + 0.5f) * 255f + brightness
        val cbMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(cbMatrix)
        return ColorMatrixColorFilter(cm)
    }
}
