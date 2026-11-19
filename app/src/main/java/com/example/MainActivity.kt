package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.editor.model.ContextDrawerMode
import com.example.editor.model.EditorView
import com.example.editor.ui.BottomToolBar
import com.example.editor.ui.EditorHeader
import com.example.editor.ui.EffectsScreen
import com.example.editor.ui.ExportScreen
import com.example.editor.ui.SpeedCurveScreen
import com.example.editor.ui.TimelineScreen
import com.example.editor.viewmodel.EditorViewModel
import com.example.ui.theme.LabelCapsStyle
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryContainerCyan

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            MyApplicationTheme {
                NeonCutApp()
            }
        }
    }
}

@Composable
fun NeonCutApp(
    viewModel: EditorViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var importAsOverlay by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker for Images & Videos
    val visualMediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 8)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importMediaUris(uris, asOverlay = importAsOverlay)
        }
    }

    // Zero-permission Storage Access Framework Picker for Audio BGM
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importMediaUris(listOf(uri), asOverlay = false)
        }
    }

    // Runtime Permission Launcher for Microphone Voiceover
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleVoiceRecording()
        } else {
            viewModel.showToast("Microphone permission required for voiceover")
        }
    }

    val requestVoiceoverToggle: () -> Unit = {
        if (state.isRecordingVoice) {
            viewModel.toggleVoiceRecording()
        } else {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                viewModel.toggleVoiceRecording()
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Handle system back on sub-screens or open drawers
    BackHandler(enabled = state.activeView != EditorView.TIMELINE || state.drawerMode != ContextDrawerMode.NONE) {
        if (state.activeView != EditorView.TIMELINE) {
            viewModel.switchView(EditorView.TIMELINE)
        } else if (state.drawerMode != ContextDrawerMode.NONE) {
            viewModel.closeDrawer()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                EditorHeader(
                    state = state,
                    onBack = {
                        if (state.activeView != EditorView.TIMELINE) {
                            viewModel.switchView(EditorView.TIMELINE)
                        } else if (state.drawerMode != ContextDrawerMode.NONE) {
                            viewModel.closeDrawer()
                        }
                    },
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    onCycleAspect = viewModel::cycleAspectRatio,
                    onOpenExport = { viewModel.switchView(EditorView.EXPORT) }
                )
            },
            bottomBar = {
                BottomToolBar(
                    state = state,
                    onSelectEdit = {
                        viewModel.switchView(EditorView.TIMELINE)
                        viewModel.closeDrawer()
                    },
                    onSelectAudio = { viewModel.openDrawer(ContextDrawerMode.AUDIO) },
                    onSelectText = { viewModel.openDrawer(ContextDrawerMode.TEXT) },
                    onSelectStickers = { viewModel.openDrawer(ContextDrawerMode.STICKER) },
                    onSelectOverlay = {
                        importAsOverlay = true
                        visualMediaPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    onSelectEffects = { viewModel.switchView(EditorView.EFFECTS) },
                    onSelectSpeed = { viewModel.switchView(EditorView.SPEED) },
                    onSelectCanvas = { viewModel.openDrawer(ContextDrawerMode.CANVAS) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                ) {
                    when (state.activeView) {
                        EditorView.TIMELINE -> TimelineScreen(
                            state = state,
                            mediaRegistry = viewModel.mediaRegistry,
                            onTogglePlay = viewModel::togglePlay,
                            onJumpStart = viewModel::jumpToStart,
                            onJumpEnd = viewModel::jumpToEnd,
                            onStepBack = { viewModel.stepBy(-0.5f) },
                            onStepForward = { viewModel.stepBy(0.5f) },
                            onSeek = viewModel::seekTo,
                            onQuickRotate = viewModel::quickRotateSelected,
                            onToggleGuides = viewModel::toggleSafeGuides,
                            onSplit = viewModel::splitSelectedClip,
                            onZoomIn = viewModel::zoomIn,
                            onZoomOut = viewModel::zoomOut,
                            onAddKeyframe = viewModel::addKeyframeAtPlayhead,
                            onToggleSnap = viewModel::toggleSnap,
                            onSelectClip = viewModel::selectClip,
                            onBeginClipGesture = viewModel::beginClipGesture,
                            onMoveClipBy = viewModel::moveClipBy,
                            onTrimLeft = viewModel::trimClipLeft,
                            onTrimRight = viewModel::trimClipRight,
                            onOpenTransitionForClip = { id ->
                                viewModel.selectClip(id)
                                viewModel.openDrawer(ContextDrawerMode.TRANSITION)
                            },
                            onImportMedia = {
                                importAsOverlay = false
                                visualMediaPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            onRibbonAction = { action ->
                                when (action) {
                                    "split" -> viewModel.splitSelectedClip()
                                    "speed" -> viewModel.switchView(EditorView.SPEED)
                                    "volume" -> viewModel.openDrawer(ContextDrawerMode.VOLUME)
                                    "transform" -> viewModel.openDrawer(ContextDrawerMode.TRANSFORM)
                                    "extract-audio" -> viewModel.extractAudioFromSelectedClip()
                                    "duplicate" -> viewModel.duplicateSelectedClip()
                                    "transition" -> viewModel.openDrawer(ContextDrawerMode.TRANSITION)
                                    "delete" -> viewModel.deleteSelectedClip()
                                }
                            },
                            onCloseDrawer = viewModel::closeDrawer,
                            onVolumeChange = viewModel::updateSelectedVolume,
                            onToggleMute = viewModel::toggleSelectedMute,
                            onTransformChange = viewModel::updateSelectedTransform,
                            onResetTransform = viewModel::resetSelectedTransform,
                            onTransitionSelect = viewModel::setSelectedTransition,
                            onSaveText = viewModel::saveTextOverlay,
                            onAddSticker = viewModel::addSticker,
                            onImportAudio = { audioPicker.launch(arrayOf("audio/*")) },
                            onToggleVoiceRecord = requestVoiceoverToggle,
                            onSelectAspect = viewModel::setAspectRatio
                        )

                        EditorView.EFFECTS -> EffectsScreen(
                            state = state,
                            mediaRegistry = viewModel.mediaRegistry,
                            onSplitRatioChange = viewModel::setFxSplitRatio,
                            onUpdateAdjustments = viewModel::updateFxAdjustments,
                            onResetFx = viewModel::resetFxParameters,
                            onSelectCategory = viewModel::setFxCategory,
                            onSelectPreset = viewModel::applyFxPreset,
                            onApplyToClip = {
                                viewModel.showToast("Effects applied to clip")
                                viewModel.switchView(EditorView.TIMELINE)
                            },
                            onApplyToTrack = viewModel::applyFxToWholeTrack
                        )

                        EditorView.SPEED -> SpeedCurveScreen(
                            state = state,
                            mediaRegistry = viewModel.mediaRegistry,
                            onTogglePlay = viewModel::togglePlay,
                            onUniformSpeedChange = viewModel::setUniformSpeed,
                            onCurveNodeChange = viewModel::updateSpeedCurveNode,
                            onSelectPreset = viewModel::applySpeedCurvePreset,
                            onReset = viewModel::resetSpeedCurve,
                            onApply = viewModel::commitSpeedCurveToClip
                        )

                        EditorView.EXPORT -> ExportScreen(
                            state = state,
                            mediaRegistry = viewModel.mediaRegistry,
                            onSelectResolution = viewModel::setExportResolution,
                            onSelectFps = viewModel::setExportFps,
                            onBitrateChange = viewModel::setExportBitrate,
                            onStartRender = viewModel::startVideoExport,
                            onShareExported = { uriStr, mime ->
                                runCatching {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = mime
                                        putExtra(Intent.EXTRA_STREAM, Uri.parse(uriStr))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Exported Video"))
                                }.onFailure {
                                    viewModel.showToast("Video saved in Movies/NeonCut")
                                }
                            },
                            onExportAudioOnly = viewModel::exportAudioMixOnly,
                            onKeepEditing = { viewModel.switchView(EditorView.TIMELINE) }
                        )
                    }
                }
            }
        }

        // Global Floating Neon Toast Pill
        AnimatedVisibility(
            visible = state.toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 64.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f))
                    .border(1.dp, PrimaryContainerCyan.copy(alpha = 0.4f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = PrimaryContainerCyan,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = state.toastMessage.orEmpty(),
                    style = LabelCapsStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
