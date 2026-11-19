package com.example.editor.model

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

enum class AspectRatioPreset(
    val label: String,
    val previewWidth: Int,
    val previewHeight: Int,
    val exportWidth: Int,
    val exportHeight: Int,
    val aspectFloat: Float
) {
    RATIO_9_16("9:16", 540, 960, 720, 1280, 9f / 16f),
    RATIO_16_9("16:9", 960, 540, 1280, 720, 16f / 9f),
    RATIO_1_1("1:1", 720, 720, 720, 720, 1f)
}

enum class TrackType { TEXT, OVERLAY, VIDEO, AUDIO }

enum class ClipType { PROCEDURAL, VIDEO, IMAGE, TEXT, STICKER, AUDIO }

enum class ProceduralVariant { CYBER_RAIN, NEON_TUNNEL }

enum class TransitionType(val label: String) {
    NONE("None"),
    CROSSFADE("Crossfade"),
    GLITCH("Glitch"),
    SLIDE("Slide"),
    ZOOM("Zoom")
}

enum class TextAnimation(val label: String) {
    POP("Pop"),
    FADE("Fade"),
    SLIDE("Slide"),
    TYPEWRITER("Typewriter")
}

enum class FxCategory(val label: String) {
    ALL("All FX"),
    CYBER("Glitch & Cyber"),
    RETRO("Retro & VHS"),
    CINEMATIC("Cinematic")
}

data class ColorAdjustments(
    val brightness: Int = 100,
    val contrast: Int = 100,
    val saturation: Int = 100,
    val chroma: Int = 0,
    val glow: Int = 0,
    val vignette: Int = 0
)

data class FxPreset(
    val id: String,
    val name: String,
    val category: FxCategory,
    val badge: String,
    val adjustments: ColorAdjustments,
    val tintArgb: Int? = null
)

val FX_PRESETS = listOf(
    FxPreset(
        id = "none",
        name = "Original",
        category = FxCategory.ALL,
        badge = "",
        adjustments = ColorAdjustments(100, 100, 100, 0, 0, 0),
        tintArgb = null
    ),
    FxPreset(
        id = "cyber",
        name = "Cyber Matrix",
        category = FxCategory.CYBER,
        badge = "LIVE",
        adjustments = ColorAdjustments(108, 128, 135, 45, 55, 35),
        tintArgb = 0x1F00F0FF
    ),
    FxPreset(
        id = "rgb",
        name = "RGB Split",
        category = FxCategory.CYBER,
        badge = "PRO",
        adjustments = ColorAdjustments(102, 118, 125, 85, 30, 20),
        tintArgb = 0x15BE0042
    ),
    FxPreset(
        id = "vhs",
        name = "Retro 90s",
        category = FxCategory.RETRO,
        badge = "VHS",
        adjustments = ColorAdjustments(96, 115, 85, 35, 15, 55),
        tintArgb = 0x1AFFBE78
    ),
    FxPreset(
        id = "duotone",
        name = "Duotone Neon",
        category = FxCategory.CYBER,
        badge = "PRO",
        adjustments = ColorAdjustments(105, 135, 150, 20, 45, 30),
        tintArgb = 0x383A90FF
    ),
    FxPreset(
        id = "heatmap",
        name = "Thermal",
        category = FxCategory.CYBER,
        badge = "",
        adjustments = ColorAdjustments(110, 145, 185, 15, 40, 40),
        tintArgb = 0x2EFF5028
    ),
    FxPreset(
        id = "dreamy",
        name = "Dreamy Mist",
        category = FxCategory.CINEMATIC,
        badge = "",
        adjustments = ColorAdjustments(112, 92, 110, 0, 80, 25),
        tintArgb = 0x1FFFCDD1
    ),
    FxPreset(
        id = "noir",
        name = "Noir Mono",
        category = FxCategory.RETRO,
        badge = "",
        adjustments = ColorAdjustments(98, 145, 0, 0, 10, 60),
        tintArgb = null
    )
)

data class SpeedPoint(val t: Float, val v: Float)

enum class SpeedCurvePreset(val label: String, val points: List<SpeedPoint>) {
    NORMAL(
        "Normal 1x",
        listOf(
            SpeedPoint(0f, 1f),
            SpeedPoint(0.25f, 1f),
            SpeedPoint(0.5f, 1f),
            SpeedPoint(0.75f, 1f),
            SpeedPoint(1f, 1f)
        )
    ),
    HERO(
        "Hero Time",
        listOf(
            SpeedPoint(0f, 2.5f),
            SpeedPoint(0.25f, 4.5f),
            SpeedPoint(0.5f, 0.3f),
            SpeedPoint(0.75f, 4.5f),
            SpeedPoint(1f, 1.5f)
        )
    ),
    BULLET(
        "Bullet Curve",
        listOf(
            SpeedPoint(0f, 6.0f),
            SpeedPoint(0.25f, 0.4f),
            SpeedPoint(0.5f, 0.2f),
            SpeedPoint(0.75f, 0.5f),
            SpeedPoint(1f, 5.0f)
        )
    ),
    MONTAGE(
        "Montage",
        listOf(
            SpeedPoint(0f, 0.5f),
            SpeedPoint(0.25f, 2.0f),
            SpeedPoint(0.5f, 5.5f),
            SpeedPoint(0.75f, 2.0f),
            SpeedPoint(1f, 0.6f)
        )
    ),
    FLASH(
        "Flash In",
        listOf(
            SpeedPoint(0f, 8.0f),
            SpeedPoint(0.25f, 3.0f),
            SpeedPoint(0.5f, 1.0f),
            SpeedPoint(0.75f, 1.0f),
            SpeedPoint(1f, 1.0f)
        )
    )
}

data class Keyframe(
    val time: Float,
    val scale: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f,
    val rotation: Float = 0f,
    val opacity: Float = 1f
)

data class TextProps(
    val content: String = "NEON GLOW",
    val fontName: String = "Plus Jakarta Sans",
    val textColor: Long = 0xFF002022,
    val bgColor: Long = 0xFF00F0FF,
    val hasBg: Boolean = true,
    val sizePx: Float = 44f,
    val animation: TextAnimation = TextAnimation.POP
)

val TEXT_FONT_OPTIONS = listOf(
    "Plus Jakarta Sans",
    "Inter",
    "JetBrains Mono",
    "Serif Bold",
    "Condensed"
)

val TEXT_COLOR_SWATCHES = listOf(
    0xFFFFFFFFL,
    0xFF002022L,
    0xFF00F0FFL,
    0xFF3A90FFL,
    0xFFFFB2BAL,
    0xFFFFE066L,
    0xFF0E0E13L
)

val STICKER_OPTIONS = listOf(
    "🔥", "⚡", "🎬", "✨", "💎", "🚀",
    "❤️", "🎵", "👑", "💯", "🌈", "💥"
)

data class TimelineClip(
    val id: String,
    val track: TrackType,
    val type: ClipType,
    val name: String,
    val mediaId: String? = null,
    val variant: ProceduralVariant = ProceduralVariant.CYBER_RAIN,
    val startTime: Float = 0f,
    val duration: Float = 4f,
    val trimStart: Float = 0f,
    val sourceDuration: Float = 15f,
    val speed: Float = 1f,
    val speedCurve: List<SpeedPoint>? = null,
    val volume: Float = 1f,
    val muted: Boolean = false,
    val rotation: Float = 0f,
    val flipH: Boolean = false,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val opacity: Float = 1f,
    val transitionIn: TransitionType = TransitionType.NONE,
    val transitionDuration: Float = 0.6f,
    val filterId: String = "none",
    val adjustments: ColorAdjustments = ColorAdjustments(),
    val keyframes: List<Keyframe> = emptyList(),
    val textProps: TextProps? = null,
    val stickerChar: String? = null
) {
    fun evalSpeedAt(localTime: Float): Float {
        val curve = speedCurve
        if (curve.isNullOrEmpty()) return speed
        val norm = (localTime / max(0.01f, duration)).coerceIn(0f, 1f)
        for (i in 0 until curve.size - 1) {
            val a = curve[i]
            val b = curve[i + 1]
            if (norm in a.t..b.t) {
                val frac = (norm - a.t) / max(0.0001f, b.t - a.t)
                return a.v + (b.v - a.v) * frac
            }
        }
        return curve.last().v
    }

    fun evalKeyframes(localTime: Float): Keyframe {
        val base = Keyframe(localTime, scale, offsetX, offsetY, rotation, opacity)
        if (keyframes.isEmpty()) return base
        val sorted = keyframes.sortedBy { it.time }
        if (localTime <= sorted.first().time) return sorted.first()
        if (localTime >= sorted.last().time) return sorted.last()
        for (i in 0 until sorted.size - 1) {
            val a = sorted[i]
            val b = sorted[i + 1]
            if (localTime in a.time..b.time) {
                val frac = (localTime - a.time) / max(0.001f, b.time - a.time)
                return Keyframe(
                    time = localTime,
                    scale = a.scale + (b.scale - a.scale) * frac,
                    x = a.x + (b.x - a.x) * frac,
                    y = a.y + (b.y - a.y) * frac,
                    rotation = a.rotation + (b.rotation - a.rotation) * frac,
                    opacity = a.opacity + (b.opacity - a.opacity) * frac
                )
            }
        }
        return base
    }
}

enum class EditorView { TIMELINE, EFFECTS, SPEED, EXPORT }

enum class ContextDrawerMode { NONE, VOLUME, TRANSFORM, TRANSITION, TEXT, STICKER, AUDIO, CANVAS }

data class ExportConfig(
    val resolution: Int = 1080,
    val fps: Int = 30,
    val bitrateMbps: Int = 12
)

sealed interface ExportStatus {
    data object Idle : ExportStatus
    data class Rendering(
        val progress: Float,
        val currentFrame: Int,
        val totalFrames: Int,
        val detail: String
    ) : ExportStatus

    data class Completed(
        val displayName: String,
        val contentUriString: String?,
        val fileSizeMb: Float,
        val mimeType: String
    ) : ExportStatus

    data class Failed(val message: String) : ExportStatus
}

data class ProjectSnapshot(
    val aspect: AspectRatioPreset,
    val clips: List<TimelineClip>,
    val selectedClipId: String?
)

data class EditorUiState(
    val activeView: EditorView = EditorView.TIMELINE,
    val aspect: AspectRatioPreset = AspectRatioPreset.RATIO_9_16,
    val playheadSec: Float = 0f,
    val isPlaying: Boolean = false,
    val pxPerSec: Float = 64f,
    val snapEnabled: Boolean = true,
    val showSafeGuides: Boolean = true,
    val selectedClipId: String? = "clip-v1-1",
    val clips: List<TimelineClip> = initialStarterClips(),
    val drawerMode: ContextDrawerMode = ContextDrawerMode.NONE,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val vuLeft: Float = 0.1f,
    val vuRight: Float = 0.12f,
    val isRecordingVoice: Boolean = false,
    val recordingSeconds: Float = 0f,
    val importerStatus: String = "Photo Picker & SAF Ready",
    val activeFxCategory: FxCategory = FxCategory.ALL,
    val fxSplitRatio: Float = 0.5f,
    val editingSpeedCurve: List<SpeedPoint> = SpeedCurvePreset.NORMAL.points,
    val uniformSpeed: Float = 1.0f,
    val exportConfig: ExportConfig = ExportConfig(),
    val exportStatus: ExportStatus = ExportStatus.Idle,
    val toastMessage: String? = null,
    val toastNonce: Long = 0L
) {
    val selectedClip: TimelineClip?
        get() = clips.firstOrNull { it.id == selectedClipId }

    val totalDurationSec: Float
        get() = max(4f, clips.maxOfOrNull { it.startTime + it.duration } ?: 4f)
}

fun initialStarterClips(): List<TimelineClip> = listOf(
    TimelineClip(
        id = "clip-v1-1",
        track = TrackType.VIDEO,
        type = ClipType.PROCEDURAL,
        variant = ProceduralVariant.CYBER_RAIN,
        name = "NeoTokyo_Rain.mp4",
        startTime = 0f,
        duration = 6f,
        sourceDuration = 15f,
        filterId = "cyber",
        adjustments = ColorAdjustments(
            brightness = 105,
            contrast = 118,
            saturation = 125,
            chroma = 25,
            glow = 35,
            vignette = 25
        ),
        keyframes = listOf(Keyframe(time = 0f, scale = 1f, x = 0f, y = 0f, rotation = 0f, opacity = 1f))
    ),
    TimelineClip(
        id = "clip-v1-2",
        track = TrackType.VIDEO,
        type = ClipType.PROCEDURAL,
        variant = ProceduralVariant.NEON_TUNNEL,
        name = "Cyber_Tunnel.mp4",
        startTime = 6f,
        duration = 6f,
        sourceDuration = 15f,
        transitionIn = TransitionType.CROSSFADE,
        transitionDuration = 0.8f,
        filterId = "none",
        adjustments = ColorAdjustments(
            brightness = 100,
            contrast = 100,
            saturation = 100,
            chroma = 0,
            glow = 20,
            vignette = 20
        )
    ),
    TimelineClip(
        id = "clip-t1-1",
        track = TrackType.TEXT,
        type = ClipType.TEXT,
        name = "NEON GLOW",
        startTime = 0.8f,
        duration = 4.5f,
        sourceDuration = 20f,
        offsetY = 0.28f,
        rotation = -2f,
        textProps = TextProps(
            content = "NEON GLOW",
            fontName = "Plus Jakarta Sans",
            textColor = 0xFF002022,
            bgColor = 0xFF00F0FF,
            hasBg = true,
            sizePx = 44f,
            animation = TextAnimation.POP
        )
    ),
    TimelineClip(
        id = "clip-a1-1",
        track = TrackType.AUDIO,
        type = ClipType.AUDIO,
        name = "Cyber_Overdrive.wav",
        mediaId = "demo-audio",
        startTime = 0f,
        duration = 12f,
        sourceDuration = 12f,
        volume = 0.85f
    )
)

fun formatTimecode(sec: Float): String {
    val clamped = max(0f, sec)
    val mins = (clamped / 60f).toInt()
    val rem = clamped % 60f
    return String.format("%02d:%04.1f", mins, rem)
}
