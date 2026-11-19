package com.example.editor.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.editor.engine.AudioPlaybackEngine
import com.example.editor.engine.MediaAsset
import com.example.editor.engine.MediaRegistry
import com.example.editor.engine.VideoExportEngine
import com.example.editor.model.AspectRatioPreset
import com.example.editor.model.ClipType
import com.example.editor.model.ColorAdjustments
import com.example.editor.model.ContextDrawerMode
import com.example.editor.model.EditorUiState
import com.example.editor.model.EditorView
import com.example.editor.model.ExportStatus
import com.example.editor.model.FX_PRESETS
import com.example.editor.model.FxCategory
import com.example.editor.model.Keyframe
import com.example.editor.model.ProjectSnapshot
import com.example.editor.model.SpeedCurvePreset
import com.example.editor.model.SpeedPoint
import com.example.editor.model.TextProps
import com.example.editor.model.TimelineClip
import com.example.editor.model.TrackType
import com.example.editor.model.TransitionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    val mediaRegistry = MediaRegistry()
    private val audioEngine = AudioPlaybackEngine(mediaRegistry)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<ProjectSnapshot>()
    private val redoStack = ArrayDeque<ProjectSnapshot>()

    private var playbackJob: Job? = null
    private var exportJob: Job? = null
    private var toastJob: Job? = null

    private fun pushHistory() {
        val st = _uiState.value
        undoStack.addLast(ProjectSnapshot(st.aspect, st.clips, st.selectedClipId))
        if (undoStack.size > 30) undoStack.removeFirst()
        redoStack.clear()
        _uiState.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = false) }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val cur = _uiState.value
        redoStack.addLast(ProjectSnapshot(cur.aspect, cur.clips, cur.selectedClipId))
        val prev = undoStack.removeLast()
        _uiState.update {
            it.copy(
                aspect = prev.aspect,
                clips = prev.clips,
                selectedClipId = prev.selectedClipId,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        showToast("Undid last action")
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val cur = _uiState.value
        undoStack.addLast(ProjectSnapshot(cur.aspect, cur.clips, cur.selectedClipId))
        val next = redoStack.removeLast()
        _uiState.update {
            it.copy(
                aspect = next.aspect,
                clips = next.clips,
                selectedClipId = next.selectedClipId,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        showToast("Redid action")
    }

    fun showToast(message: String) {
        val nonce = System.nanoTime()
        _uiState.update { it.copy(toastMessage = message, toastNonce = nonce) }
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(2200)
            _uiState.update { if (it.toastNonce == nonce) it.copy(toastMessage = null) else it }
        }
    }

    fun switchView(view: EditorView) {
        _uiState.update { state ->
            val updatedCurve = if (view == EditorView.SPEED) {
                val clip = state.selectedClip
                clip?.speedCurve ?: List(5) { idx ->
                    SpeedPoint(idx * 0.25f, clip?.speed ?: 1f)
                }
            } else {
                state.editingSpeedCurve
            }
            val updatedUniform = if (view == EditorView.SPEED) {
                state.selectedClip?.speed ?: 1f
            } else state.uniformSpeed

            state.copy(
                activeView = view,
                drawerMode = if (view != EditorView.TIMELINE) ContextDrawerMode.NONE else state.drawerMode,
                editingSpeedCurve = updatedCurve,
                uniformSpeed = updatedUniform
            )
        }
    }

    fun openDrawer(mode: ContextDrawerMode) {
        _uiState.update {
            it.copy(
                activeView = EditorView.TIMELINE,
                drawerMode = if (it.drawerMode == mode) ContextDrawerMode.NONE else mode
            )
        }
    }

    fun closeDrawer() {
        _uiState.update { it.copy(drawerMode = ContextDrawerMode.NONE) }
    }

    fun cycleAspectRatio() {
        val presets = AspectRatioPreset.entries
        val cur = _uiState.value.aspect
        val next = presets[(presets.indexOf(cur) + 1) % presets.size]
        setAspectRatio(next)
    }

    fun setAspectRatio(preset: AspectRatioPreset) {
        pushHistory()
        _uiState.update { it.copy(aspect = preset) }
        showToast("Aspect Ratio: ${preset.label}")
    }

    fun toggleSafeGuides() {
        _uiState.update { it.copy(showSafeGuides = !it.showSafeGuides) }
    }

    fun toggleSnap() {
        _uiState.update {
            val next = !it.snapEnabled
            it.copy(snapEnabled = next)
        }
    }

    fun zoomIn() {
        _uiState.update { it.copy(pxPerSec = min(160f, it.pxPerSec + 16f)) }
    }

    fun zoomOut() {
        _uiState.update { it.copy(pxPerSec = max(28f, it.pxPerSec - 16f)) }
    }

    fun selectClip(clipId: String) {
        _uiState.update { it.copy(selectedClipId = clipId) }
    }

    fun seekTo(sec: Float) {
        val total = _uiState.value.totalDurationSec
        val clamped = sec.coerceIn(0f, total)
        _uiState.update { it.copy(playheadSec = clamped) }
        if (_uiState.value.isPlaying) {
            audioEngine.startPlayback(
                scope = viewModelScope,
                startSec = clamped,
                clipsProvider = { _uiState.value.clips },
                totalDurationProvider = { _uiState.value.totalDurationSec }
            )
        }
    }

    fun jumpToStart() = seekTo(0f)

    fun jumpToEnd() {
        stopPlayback()
        seekTo(_uiState.value.totalDurationSec)
    }

    fun stepBy(deltaSec: Float) {
        seekTo(_uiState.value.playheadSec + deltaSec)
    }

    fun togglePlay() {
        if (_uiState.value.isPlaying) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        val state = _uiState.value
        val startAt = if (state.playheadSec >= state.totalDurationSec - 0.05f) 0f else state.playheadSec
        _uiState.update { it.copy(isPlaying = true, playheadSec = startAt) }

        audioEngine.startPlayback(
            scope = viewModelScope,
            startSec = startAt,
            clipsProvider = { _uiState.value.clips },
            totalDurationProvider = { _uiState.value.totalDurationSec }
        )

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            var lastNs = System.nanoTime()
            while (isActive && _uiState.value.isPlaying) {
                delay(16L)
                val now = System.nanoTime()
                val dt = ((now - lastNs) / 1_000_000_000f).coerceIn(0f, 0.1f)
                lastNs = now

                val cur = _uiState.value
                val next = cur.playheadSec + dt
                if (next >= cur.totalDurationSec) {
                    stopPlayback()
                    _uiState.update { it.copy(playheadSec = 0f) }
                    break
                } else {
                    _uiState.update {
                        it.copy(
                            playheadSec = next,
                            vuLeft = audioEngine.lastPeakLeft,
                            vuRight = audioEngine.lastPeakRight
                        )
                    }
                }
            }
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        audioEngine.stopPlayback()
        _uiState.update { it.copy(isPlaying = false, vuLeft = 0.08f, vuRight = 0.10f) }
    }

    fun beginClipGesture() {
        pushHistory()
    }

    fun moveClipBy(clipId: String, deltaSec: Float) {
        _uiState.update { state ->
            val updated = state.clips.map { clip ->
                if (clip.id != clipId) clip
                else {
                    var nextStart = max(0f, clip.startTime + deltaSec)
                    if (state.snapEnabled && abs(nextStart - state.playheadSec) < 0.18f) {
                        nextStart = state.playheadSec
                    }
                    clip.copy(startTime = round(nextStart * 20f) / 20f)
                }
            }
            state.copy(clips = updated, selectedClipId = clipId)
        }
    }

    fun trimClipLeft(clipId: String, deltaSec: Float) {
        _uiState.update { state ->
            val updated = state.clips.map { clip ->
                if (clip.id != clipId) clip
                else {
                    val safeDelta = deltaSec.coerceIn(-clip.startTime, clip.duration - 0.35f)
                    clip.copy(
                        startTime = max(0f, clip.startTime + safeDelta),
                        duration = max(0.35f, clip.duration - safeDelta),
                        trimStart = max(0f, clip.trimStart + safeDelta)
                    )
                }
            }
            state.copy(clips = updated, selectedClipId = clipId)
        }
    }

    fun trimClipRight(clipId: String, deltaSec: Float) {
        _uiState.update { state ->
            val updated = state.clips.map { clip ->
                if (clip.id != clipId) clip
                else clip.copy(duration = max(0.35f, clip.duration + deltaSec))
            }
            state.copy(clips = updated, selectedClipId = clipId)
        }
    }

    fun splitSelectedClip() {
        val state = _uiState.value
        val clip = state.selectedClip
        if (clip == null) {
            showToast("Select a clip first")
            return
        }
        val t = state.playheadSec
        if (t <= clip.startTime + 0.15f || t >= clip.startTime + clip.duration - 0.15f) {
            showToast("Move playhead inside selected clip")
            return
        }
        pushHistory()
        val firstDur = t - clip.startTime
        val secondDur = clip.duration - firstDur
        val firstPart = clip.copy(duration = firstDur)
        val secondPart = clip.copy(
            id = "clip-${System.currentTimeMillis()}",
            startTime = t,
            duration = secondDur,
            trimStart = clip.trimStart + firstDur * clip.speed,
            transitionIn = TransitionType.NONE
        )
        val newList = state.clips.map { if (it.id == clip.id) firstPart else it } + secondPart
        _uiState.update { it.copy(clips = newList, selectedClipId = secondPart.id) }
        showToast("Clip split at playhead")
    }

    fun duplicateSelectedClip() {
        val clip = _uiState.value.selectedClip ?: return
        pushHistory()
        val clone = clip.copy(
            id = "clip-${System.currentTimeMillis()}",
            startTime = clip.startTime + clip.duration
        )
        _uiState.update { it.copy(clips = it.clips + clone, selectedClipId = clone.id) }
        showToast("Clip duplicated")
    }

    fun deleteSelectedClip() {
        val clip = _uiState.value.selectedClip ?: return
        pushHistory()
        val remaining = _uiState.value.clips.filterNot { it.id == clip.id }
        _uiState.update {
            it.copy(
                clips = remaining,
                selectedClipId = remaining.firstOrNull()?.id,
                drawerMode = ContextDrawerMode.NONE
            )
        }
        showToast("Clip removed")
    }

    fun addKeyframeAtPlayhead() {
        val state = _uiState.value
        val clip = state.selectedClip
        if (clip == null || clip.track == TrackType.AUDIO) {
            showToast("Select a Video, Overlay, or Text clip")
            return
        }
        pushHistory()
        val localT = (state.playheadSec - clip.startTime).coerceIn(0f, clip.duration)
        val kf = Keyframe(
            time = localT,
            scale = clip.scale,
            x = clip.offsetX,
            y = clip.offsetY,
            rotation = clip.rotation,
            opacity = clip.opacity
        )
        updateClip(clip.id) { it.copy(keyframes = it.keyframes + kf) }
        showToast("Keyframe ◆ added at ${String.format("%.1f", localT)}s")
    }

    fun extractAudioFromSelectedClip() {
        val clip = _uiState.value.selectedClip
        if (clip == null || (clip.track != TrackType.VIDEO && clip.track != TrackType.OVERLAY)) {
            showToast("Select a video clip first")
            return
        }
        pushHistory()
        val audioMediaId = clip.mediaId ?: MediaRegistry.DEMO_AUDIO_ID
        val extracted = TimelineClip(
            id = "clip-a-${System.currentTimeMillis()}",
            track = TrackType.AUDIO,
            type = ClipType.AUDIO,
            name = "${clip.name.substringBeforeLast('.')}_Audio.wav",
            mediaId = audioMediaId,
            startTime = clip.startTime,
            duration = clip.duration,
            trimStart = clip.trimStart,
            sourceDuration = clip.sourceDuration,
            speed = clip.speed,
            volume = 1f,
            muted = false
        )
        _uiState.update { state ->
            val updated = state.clips.map { if (it.id == clip.id) it.copy(muted = true) else it } + extracted
            state.copy(clips = updated, selectedClipId = extracted.id)
        }
        showToast("Audio extracted to Track A1")
    }

    fun quickRotateSelected() {
        val clip = _uiState.value.selectedClip ?: return
        pushHistory()
        val nextRot = (clip.rotation + 90f) % 360f
        updateClip(clip.id) { it.copy(rotation = nextRot) }
        showToast("Rotated ${nextRot.toInt()}°")
    }

    fun updateSelectedVolume(volume: Float) {
        val id = _uiState.value.selectedClipId ?: return
        updateClip(id) { it.copy(volume = volume.coerceIn(0f, 2f)) }
    }

    fun toggleSelectedMute() {
        val clip = _uiState.value.selectedClip ?: return
        pushHistory()
        updateClip(clip.id) { it.copy(muted = !it.muted) }
    }

    fun updateSelectedTransform(
        scale: Float? = null,
        rotation: Float? = null,
        offsetX: Float? = null,
        offsetY: Float? = null,
        flipH: Boolean? = null
    ) {
        val id = _uiState.value.selectedClipId ?: return
        updateClip(id) { c ->
            c.copy(
                scale = scale ?: c.scale,
                rotation = rotation ?: c.rotation,
                offsetX = offsetX ?: c.offsetX,
                offsetY = offsetY ?: c.offsetY,
                flipH = flipH ?: c.flipH
            )
        }
    }

    fun resetSelectedTransform() {
        val id = _uiState.value.selectedClipId ?: return
        pushHistory()
        updateClip(id) { it.copy(scale = 1f, rotation = 0f, offsetX = 0f, offsetY = 0f, flipH = false) }
        showToast("Transform reset")
    }

    fun setSelectedTransition(transition: TransitionType) {
        val id = _uiState.value.selectedClipId ?: return
        pushHistory()
        updateClip(id) { it.copy(transitionIn = transition) }
        showToast("Transition: ${transition.label}")
    }

    fun saveTextOverlay(props: TextProps) {
        val state = _uiState.value
        val sel = state.selectedClip
        pushHistory()
        if (sel != null && sel.type == ClipType.TEXT) {
            updateClip(sel.id) { it.copy(name = props.content.ifBlank { "TITLE" }, textProps = props) }
            showToast("Text overlay updated")
        } else {
            val newClip = TimelineClip(
                id = "clip-t-${System.currentTimeMillis()}",
                track = TrackType.TEXT,
                type = ClipType.TEXT,
                name = props.content.ifBlank { "TITLE" },
                startTime = state.playheadSec,
                duration = 4f,
                sourceDuration = 20f,
                offsetY = 0.22f,
                textProps = props
            )
            _uiState.update { it.copy(clips = it.clips + newClip, selectedClipId = newClip.id) }
            showToast("Added text to Track T1")
        }
    }

    fun addSticker(emoji: String) {
        pushHistory()
        val newClip = TimelineClip(
            id = "clip-st-${System.currentTimeMillis()}",
            track = TrackType.TEXT,
            type = ClipType.STICKER,
            name = emoji,
            stickerChar = emoji,
            startTime = _uiState.value.playheadSec,
            duration = 3.5f,
            sourceDuration = 20f,
            offsetY = -0.15f
        )
        _uiState.update { it.copy(clips = it.clips + newClip, selectedClipId = newClip.id) }
        showToast("Added $emoji sticker")
    }

    // ---- Effects & Filters ----
    fun setFxCategory(category: FxCategory) {
        _uiState.update { it.copy(activeFxCategory = category) }
    }

    fun setFxSplitRatio(ratio: Float) {
        _uiState.update { it.copy(fxSplitRatio = ratio.coerceIn(0.08f, 0.92f)) }
    }

    private fun targetVisualClip(): TimelineClip? {
        val st = _uiState.value
        val sel = st.selectedClip
        return if (sel != null && (sel.track == TrackType.VIDEO || sel.track == TrackType.OVERLAY)) {
            sel
        } else {
            st.clips.firstOrNull { it.track == TrackType.VIDEO }
        }
    }

    fun applyFxPreset(presetId: String) {
        val clip = targetVisualClip() ?: return
        val preset = FX_PRESETS.firstOrNull { it.id == presetId } ?: return
        pushHistory()
        updateClip(clip.id) { it.copy(filterId = preset.id, adjustments = preset.adjustments) }
    }

    fun updateFxAdjustments(transform: (ColorAdjustments) -> ColorAdjustments) {
        val clip = targetVisualClip() ?: return
        updateClip(clip.id) { it.copy(adjustments = transform(it.adjustments)) }
    }

    fun resetFxParameters() {
        val clip = targetVisualClip() ?: return
        pushHistory()
        updateClip(clip.id) { it.copy(filterId = "none", adjustments = ColorAdjustments()) }
        showToast("Filters reset")
    }

    fun applyFxToWholeTrack() {
        val src = targetVisualClip() ?: return
        pushHistory()
        _uiState.update { state ->
            val updated = state.clips.map { clip ->
                if (clip.track == TrackType.VIDEO) {
                    clip.copy(filterId = src.filterId, adjustments = src.adjustments)
                } else clip
            }
            state.copy(clips = updated, activeView = EditorView.TIMELINE)
        }
        showToast("Effects applied to entire V1 track")
    }

    // ---- Speed Curve Control ----
    fun setUniformSpeed(speed: Float) {
        val clamped = (round(speed * 10f) / 10f).coerceIn(0.1f, 10f)
        _uiState.update { state ->
            state.copy(
                uniformSpeed = clamped,
                editingSpeedCurve = state.editingSpeedCurve.map { it.copy(v = clamped) }
            )
        }
    }

    fun updateSpeedCurveNode(index: Int, speedValue: Float) {
        val clamped = (round(speedValue * 10f) / 10f).coerceIn(0.1f, 10f)
        _uiState.update { state ->
            val updated = state.editingSpeedCurve.mapIndexed { i, pt ->
                if (i == index) pt.copy(v = clamped) else pt
            }
            state.copy(editingSpeedCurve = updated)
        }
    }

    fun applySpeedCurvePreset(preset: SpeedCurvePreset) {
        _uiState.update {
            it.copy(
                editingSpeedCurve = preset.points,
                uniformSpeed = preset.points[2].v
            )
        }
    }

    fun resetSpeedCurve() {
        applySpeedCurvePreset(SpeedCurvePreset.NORMAL)
    }

    fun commitSpeedCurveToClip() {
        val clip = _uiState.value.selectedClip ?: _uiState.value.clips.firstOrNull { it.track == TrackType.VIDEO } ?: return
        val curve = _uiState.value.editingSpeedCurve
        pushHistory()
        val allSame = curve.all { abs(it.v - curve.first().v) < 0.05f }
        updateClip(clip.id) {
            if (allSame) {
                it.copy(speed = curve.first().v, speedCurve = null)
            } else {
                it.copy(speed = curve[curve.size / 2].v, speedCurve = curve)
            }
        }
        _uiState.update { it.copy(activeView = EditorView.TIMELINE) }
        showToast("Speed curve applied")
    }

    // ---- Local Media Import (Photo Picker & SAF) ----
    fun importMediaUris(uris: List<Uri>, asOverlay: Boolean) {
        if (uris.isEmpty()) return
        _uiState.update { it.copy(importerStatus = "Importing ${uris.size} file(s)...") }
        viewModelScope.launch {
            var importedCount = 0
            for (uri in uris) {
                val asset = mediaRegistry.importUri(getApplication(), uri) ?: continue
                importedCount++
                pushHistory()
                when (asset) {
                    is MediaAsset.ImageAsset -> {
                        val tr = if (asOverlay) TrackType.OVERLAY else TrackType.VIDEO
                        val start = if (tr == TrackType.OVERLAY) {
                            _uiState.value.playheadSec
                        } else {
                            _uiState.value.clips.filter { it.track == tr }
                                .maxOfOrNull { it.startTime + it.duration } ?: 0f
                        }
                        val clip = TimelineClip(
                            id = "clip-img-${System.currentTimeMillis()}-$importedCount",
                            track = tr,
                            type = ClipType.IMAGE,
                            name = asset.displayName,
                            mediaId = asset.id,
                            startTime = start,
                            duration = 4f,
                            sourceDuration = 60f,
                            transitionIn = TransitionType.CROSSFADE
                        )
                        _uiState.update { it.copy(clips = it.clips + clip, selectedClipId = clip.id) }
                    }

                    is MediaAsset.VideoAsset -> {
                        val tr = if (asOverlay) TrackType.OVERLAY else TrackType.VIDEO
                        val start = if (tr == TrackType.OVERLAY) {
                            _uiState.value.playheadSec
                        } else {
                            _uiState.value.clips.filter { it.track == tr }
                                .maxOfOrNull { it.startTime + it.duration } ?: 0f
                        }
                        val clip = TimelineClip(
                            id = "clip-vid-${System.currentTimeMillis()}-$importedCount",
                            track = tr,
                            type = ClipType.VIDEO,
                            name = asset.displayName,
                            mediaId = asset.id,
                            startTime = start,
                            duration = asset.durationSec,
                            sourceDuration = asset.durationSec
                        )
                        _uiState.update { it.copy(clips = it.clips + clip, selectedClipId = clip.id) }
                    }

                    is MediaAsset.AudioAsset -> {
                        val clip = TimelineClip(
                            id = "clip-aud-${System.currentTimeMillis()}-$importedCount",
                            track = TrackType.AUDIO,
                            type = ClipType.AUDIO,
                            name = asset.displayName,
                            mediaId = asset.id,
                            startTime = _uiState.value.playheadSec,
                            duration = min(30f, asset.durationSec),
                            sourceDuration = asset.durationSec
                        )
                        _uiState.update { it.copy(clips = it.clips + clip, selectedClipId = clip.id) }
                    }
                }
            }
            val label = if (importedCount > 0) "Imported $importedCount media item(s)" else "Unsupported media format"
            _uiState.update { it.copy(importerStatus = label) }
            showToast(label)
        }
    }

    // ---- Voiceover Recording ----
    fun toggleVoiceRecording() {
        if (_uiState.value.isRecordingVoice) {
            val result = audioEngine.stopVoiceRecording()
            _uiState.update { it.copy(isRecordingVoice = false, recordingSeconds = 0f) }
            if (result != null) {
                val (asset, startPlayhead) = result
                pushHistory()
                val clip = TimelineClip(
                    id = "clip-vo-${System.currentTimeMillis()}",
                    track = TrackType.AUDIO,
                    type = ClipType.AUDIO,
                    name = asset.displayName,
                    mediaId = asset.id,
                    startTime = startPlayhead,
                    duration = asset.durationSec,
                    sourceDuration = asset.durationSec,
                    volume = 1.2f
                )
                _uiState.update { it.copy(clips = it.clips + clip, selectedClipId = clip.id) }
                showToast("Voiceover added to Track A1")
            } else {
                showToast("Voiceover too short")
            }
        } else {
            stopPlayback()
            val started = audioEngine.startVoiceRecording(
                scope = viewModelScope,
                playheadSec = _uiState.value.playheadSec,
                onTickSeconds = { secs ->
                    _uiState.update { it.copy(recordingSeconds = secs) }
                }
            )
            if (started) {
                _uiState.update { it.copy(isRecordingVoice = true, recordingSeconds = 0f) }
                showToast("Recording voiceover...")
            } else {
                showToast("Microphone unavailable")
            }
        }
    }

    // ---- Export Config & Real MediaCodec/WAV Export ----
    fun setExportResolution(res: Int) {
        _uiState.update { it.copy(exportConfig = it.exportConfig.copy(resolution = res)) }
    }

    fun setExportFps(fps: Int) {
        _uiState.update { it.copy(exportConfig = it.exportConfig.copy(fps = fps)) }
    }

    fun setExportBitrate(mbps: Int) {
        _uiState.update { it.copy(exportConfig = it.exportConfig.copy(bitrateMbps = mbps.coerceIn(4, 35))) }
    }

    fun startVideoExport() {
        if (_uiState.value.exportStatus is ExportStatus.Rendering) return
        stopPlayback()
        val state = _uiState.value
        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    exportStatus = ExportStatus.Rendering(
                        progress = 0f,
                        currentFrame = 0,
                        totalFrames = (state.totalDurationSec * state.exportConfig.fps).toInt().coerceAtLeast(1),
                        detail = "Initializing H.264 encoder..."
                    )
                )
            }
            try {
                val res = VideoExportEngine.exportVideoMp4(
                    context = getApplication(),
                    aspect = state.aspect,
                    config = state.exportConfig,
                    clips = state.clips,
                    totalDurationSec = state.totalDurationSec,
                    mediaRegistry = mediaRegistry,
                    onProgress = { progress, frame, total, detail ->
                        _uiState.update {
                            it.copy(
                                exportStatus = ExportStatus.Rendering(progress, frame, total, detail)
                            )
                        }
                    }
                )
                _uiState.update {
                    it.copy(
                        exportStatus = ExportStatus.Completed(
                            displayName = res.displayName,
                            contentUriString = res.contentUri?.toString(),
                            fileSizeMb = res.fileSizeMb,
                            mimeType = res.mimeType
                        )
                    )
                }
                showToast("Saved ${res.displayName} to Movies/NeonCut")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(exportStatus = ExportStatus.Failed(e.localizedMessage ?: "Export failed"))
                }
                showToast("Export failed: ${e.localizedMessage ?: "Codec error"}")
            }
        }
    }

    fun exportAudioMixOnly() {
        stopPlayback()
        val state = _uiState.value
        viewModelScope.launch {
            showToast("Mixing audio tracks...")
            try {
                val res = VideoExportEngine.exportAudioMixWav(
                    context = getApplication(),
                    audioEngine = audioEngine,
                    clips = state.clips,
                    totalDurationSec = state.totalDurationSec
                )
                showToast("Saved ${res.displayName} (${String.format("%.1f", res.fileSizeMb)} MB)")
            } catch (e: Exception) {
                showToast("Audio export failed")
            }
        }
    }

    private fun updateClip(clipId: String, transform: (TimelineClip) -> TimelineClip) {
        _uiState.update { state ->
            state.copy(clips = state.clips.map { if (it.id == clipId) transform(it) else it })
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        exportJob?.cancel()
        audioEngine.release()
    }
}
