package com.example.editor.engine

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.editor.model.AspectRatioPreset
import com.example.editor.model.ExportConfig
import com.example.editor.model.TimelineClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class ExportResult(
    val displayName: String,
    val contentUri: Uri?,
    val fileSizeMb: Float,
    val mimeType: String
)

object VideoExportEngine {

    suspend fun exportVideoMp4(
        context: Context,
        aspect: AspectRatioPreset,
        config: ExportConfig,
        clips: List<TimelineClip>,
        totalDurationSec: Float,
        mediaRegistry: MediaRegistry,
        onProgress: (progress: Float, currentFrame: Int, totalFrames: Int, detail: String) -> Unit
    ): ExportResult = withContext(Dispatchers.IO) {
        // Compute safe even codec dimensions clamped so emulators & budget devices always succeed
        val scale = when (config.resolution) {
            720 -> 0.67f
            2160 -> 1.35f
            else -> 1.0f
        }
        val rawW = (aspect.exportWidth * scale).toInt().coerceIn(360, 1280)
        val rawH = (aspect.exportHeight * scale).toInt().coerceIn(360, 1280)
        val width = (rawW / 16) * 16
        val height = (rawH / 16) * 16
        val fps = config.fps.coerceIn(24, 60)
        val bitrate = (config.bitrateMbps * 1_000_000).coerceIn(2_000_000, 20_000_000)
        val totalFrames = max(1, ceil(totalDurationSec * fps).toInt())

        val tempFile = File(context.cacheDir, "neoncut_render_${System.currentTimeMillis()}.mp4")
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false

        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = MediaCodec.BufferInfo()
            var trackIndex = -1

            fun drainEncoder(endOfStream: Boolean) {
                if (endOfStream) {
                    encoder.signalEndOfInputStream()
                }
                while (true) {
                    val outIdx = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                    if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        if (!endOfStream) break
                    } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    } else if (outIdx >= 0) {
                        val encoded = encoder.getOutputBuffer(outIdx)
                        if (encoded != null && bufferInfo.size > 0 && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            if (muxerStarted) {
                                encoded.position(bufferInfo.offset)
                                encoded.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(trackIndex, encoded, bufferInfo)
                            }
                        }
                        encoder.releaseOutputBuffer(outIdx, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    }
                }
            }

            for (frameIdx in 0 until totalFrames) {
                if (!coroutineContext.isActive) break
                val timeSec = min(totalDurationSec, frameIdx.toFloat() / fps)
                val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    inputSurface.lockHardwareCanvas()
                } else {
                    inputSurface.lockCanvas(null)
                }
                try {
                    CanvasCompositor.renderComposite(
                        canvas = canvas,
                        width = width.toFloat(),
                        height = height.toFloat(),
                        clips = clips,
                        mediaRegistry = mediaRegistry,
                        timeSec = timeSec,
                        skipFx = false
                    )
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }
                drainEncoder(false)

                if (frameIdx % 2 == 0 || frameIdx == totalFrames - 1) {
                    val progress = (frameIdx + 1).toFloat() / totalFrames
                    onProgress(
                        progress,
                        frameIdx + 1,
                        totalFrames,
                        "Encoding ${width}x${height} @ ${fps}fps"
                    )
                }
            }

            drainEncoder(true)
            inputSurface.release()
        } finally {
            runCatching { encoder?.stop() }
            runCatching { encoder?.release() }
            runCatching {
                if (muxerStarted) muxer?.stop()
                muxer?.release()
            }
        }

        val displayName = "NeonCut_${System.currentTimeMillis() / 1000}.mp4"
        val savedUri = saveFileToMediaStore(
            context = context,
            sourceFile = tempFile,
            displayName = displayName,
            mimeType = "video/mp4",
            isVideo = true
        )
        val sizeMb = max(0.1f, tempFile.length().toFloat() / (1024f * 1024f))
        runCatching { tempFile.delete() }

        ExportResult(
            displayName = displayName,
            contentUri = savedUri,
            fileSizeMb = sizeMb,
            mimeType = "video/mp4"
        )
    }

    suspend fun exportAudioMixWav(
        context: Context,
        audioEngine: AudioPlaybackEngine,
        clips: List<TimelineClip>,
        totalDurationSec: Float
    ): ExportResult = withContext(Dispatchers.IO) {
        val pcm = audioEngine.mixOfflinePcm(clips, totalDurationSec)
        val tempFile = File(context.cacheDir, "neoncut_audio_${System.currentTimeMillis()}.wav")
        tempFile.outputStream().use { out ->
            writeWavStream(out, pcm, MediaRegistry.DEFAULT_SAMPLE_RATE)
        }
        val displayName = "NeonCut_Mix_${System.currentTimeMillis() / 1000}.wav"
        val savedUri = saveFileToMediaStore(
            context = context,
            sourceFile = tempFile,
            displayName = displayName,
            mimeType = "audio/wav",
            isVideo = false
        )
        val sizeMb = max(0.05f, tempFile.length().toFloat() / (1024f * 1024f))
        runCatching { tempFile.delete() }

        ExportResult(
            displayName = displayName,
            contentUri = savedUri,
            fileSizeMb = sizeMb,
            mimeType = "audio/wav"
        )
    }

    private fun writeWavStream(out: OutputStream, pcm: ShortArray, sampleRate: Int) {
        val dataLen = pcm.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(36 + dataLen)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1) // PCM
        header.putShort(1) // Mono
        header.putInt(sampleRate)
        header.putInt(sampleRate * 2)
        header.putShort(2)
        header.putShort(16)
        header.put("data".toByteArray())
        header.putInt(dataLen)
        out.write(header.array())

        val dataBuf = ByteBuffer.allocate(dataLen).order(ByteOrder.LITTLE_ENDIAN)
        for (s in pcm) dataBuf.putShort(s)
        out.write(dataBuf.array())
    }

    private fun saveFileToMediaStore(
        context: Context,
        sourceFile: File,
        displayName: String,
        mimeType: String,
        isVideo: Boolean
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = if (isVideo) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            val relPath = if (isVideo) "${Environment.DIRECTORY_MOVIES}/NeonCut" else "${Environment.DIRECTORY_DOWNLOADS}/NeonCut"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(collection, values) ?: return null
            context.contentResolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            uri
        } else {
            val dir = context.getExternalFilesDir(if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val dest = File(dir, displayName)
            sourceFile.copyTo(dest, overwrite = true)
            Uri.fromFile(dest)
        }
    }
}
