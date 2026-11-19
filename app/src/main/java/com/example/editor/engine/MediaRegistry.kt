package com.example.editor.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

sealed interface MediaAsset {
    val id: String
    val displayName: String

    data class ImageAsset(
        override val id: String,
        override val displayName: String,
        val bitmap: Bitmap
    ) : MediaAsset

    data class VideoAsset(
        override val id: String,
        override val displayName: String,
        val uri: Uri,
        val durationSec: Float,
        val frames: List<Bitmap>,
        val audioPcm: ShortArray? = null,
        val sampleRate: Int = 44100,
        val waveform: List<Float>? = null
    ) : MediaAsset {
        fun frameAt(timeSec: Float): Bitmap? {
            if (frames.isEmpty()) return null
            val norm = (timeSec / max(0.1f, durationSec)).coerceIn(0f, 0.999f)
            val index = (norm * frames.size).toInt().coerceIn(0, frames.lastIndex)
            return frames[index]
        }
    }

    data class AudioAsset(
        override val id: String,
        override val displayName: String,
        val pcm: ShortArray,
        val sampleRate: Int = 44100,
        val durationSec: Float,
        val waveform: List<Float>
    ) : MediaAsset
}

class MediaRegistry {
    companion object {
        const val DEMO_AUDIO_ID = "demo-audio"
        const val DEFAULT_SAMPLE_RATE = 44100
    }

    private val assets = ConcurrentHashMap<String, MediaAsset>()

    init {
        assets[DEMO_AUDIO_ID] = buildDemoSynthAudio()
    }

    fun get(mediaId: String?): MediaAsset? = mediaId?.let { assets[it] }

    fun putAudio(asset: MediaAsset.AudioAsset) {
        assets[asset.id] = asset
    }

    fun getWaveform(mediaId: String?): List<Float> {
        return when (val asset = get(mediaId)) {
            is MediaAsset.AudioAsset -> asset.waveform
            is MediaAsset.VideoAsset -> asset.waveform ?: defaultWave()
            else -> defaultWave()
        }
    }

    private fun defaultWave(): List<Float> =
        List(36) { i -> 0.25f + 0.65f * abs(sin(i * 0.65f)) }

    private fun buildDemoSynthAudio(): MediaAsset.AudioAsset {
        val sr = DEFAULT_SAMPLE_RATE
        val durationSec = 12f
        val len = (sr * durationSec).toInt()
        val pcm = ShortArray(len)
        val bassNotes = floatArrayOf(55f, 55f, 65.4f, 73.4f)
        val arpNotes = floatArrayOf(220f, 277.18f, 329.63f, 440f)

        for (i in 0 until len) {
            val t = i.toFloat() / sr
            val beat = (t * 2f) % 1f
            val kick = if (beat < 0.15f) {
                sin(2.0 * PI * (120.0 - beat * 500.0) * beat) * exp(-beat * 18.0)
            } else 0.0
            val bassFreq = bassNotes[t.toInt() % bassNotes.size]
            val bass = sin(2.0 * PI * bassFreq * t) * 0.22
            val arpFreq = arpNotes[(t * 4f).toInt() % arpNotes.size]
            val arp = sin(2.0 * PI * arpFreq * t) * 0.10 * exp(-((t * 4f) % 1f) * 4.0)
            val mixed = (kick * 0.55 + bass + arp).coerceIn(-0.9, 0.9)
            pcm[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        val waveform = computeWaveform(pcm, 48)
        return MediaAsset.AudioAsset(
            id = DEMO_AUDIO_ID,
            displayName = "Cyber_Overdrive.wav",
            pcm = pcm,
            sampleRate = sr,
            durationSec = durationSec,
            waveform = waveform
        )
    }

    fun computeWaveform(pcm: ShortArray, bars: Int = 44): List<Float> {
        if (pcm.isEmpty()) return defaultWave()
        val step = max(1, pcm.size / bars)
        return List(bars) { b ->
            var peak = 0
            val start = b * step
            val end = min(pcm.size, start + step)
            for (i in start until end) {
                val v = abs(pcm[i].toInt())
                if (v > peak) peak = v
            }
            ((peak.toFloat() / Short.MAX_VALUE) * 1.15f).coerceIn(0.12f, 1f)
        }
    }

    suspend fun importUri(context: Context, uri: Uri): MediaAsset? = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri).orEmpty()
        val name = queryDisplayName(context, uri) ?: "Imported_${System.currentTimeMillis() % 10000}"
        val id = "media-${System.currentTimeMillis()}-${(100..999).random()}"

        when {
            mime.startsWith("image/") -> {
                val bmp = decodeScaledBitmap(context, uri, 1080) ?: return@withContext null
                val asset = MediaAsset.ImageAsset(id, name, bmp)
                assets[id] = asset
                asset
            }

            mime.startsWith("video/") -> {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 5000L
                    val durSec = (durMs / 1000f).coerceIn(1f, 60f)
                    val frameCount = min(36, max(10, (durSec * 3).toInt()))
                    val frames = mutableListOf<Bitmap>()
                    for (i in 0 until frameCount) {
                        val timeUs = ((i.toFloat() / frameCount) * durMs * 1000L).toLong()
                        val raw = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        if (raw != null) {
                            frames.add(scaleBitmapDown(raw, 720))
                            if (raw != frames.last()) raw.recycle()
                        }
                    }
                    val decodedAudio = runCatching { decodeAudioTrackToPcm(context, uri) }.getOrNull()
                    val asset = MediaAsset.VideoAsset(
                        id = id,
                        displayName = name,
                        uri = uri,
                        durationSec = durSec,
                        frames = frames,
                        audioPcm = decodedAudio?.first,
                        sampleRate = decodedAudio?.second ?: DEFAULT_SAMPLE_RATE,
                        waveform = decodedAudio?.first?.let { computeWaveform(it, 40) }
                    )
                    assets[id] = asset
                    asset
                } catch (_: Exception) {
                    null
                } finally {
                    runCatching { retriever.release() }
                }
            }

            mime.startsWith("audio/") -> {
                val decoded = runCatching { decodeAudioTrackToPcm(context, uri) }.getOrNull()
                    ?: return@withContext null
                val (pcm, sr) = decoded
                val durSec = (pcm.size.toFloat() / max(1, sr)).coerceIn(0.5f, 120f)
                val asset = MediaAsset.AudioAsset(
                    id = id,
                    displayName = name,
                    pcm = pcm,
                    sampleRate = sr,
                    durationSec = durSec,
                    waveform = computeWaveform(pcm, 44)
                )
                assets[id] = asset
                asset
            }

            else -> null
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
        }.getOrNull()
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        var sample = 1
        while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        }
    }

    private fun scaleBitmapDown(src: Bitmap, maxDim: Int): Bitmap {
        val ratio = min(maxDim.toFloat() / max(1, src.width), maxDim.toFloat() / max(1, src.height))
        if (ratio >= 1f) return src
        return Bitmap.createScaledBitmap(
            src,
            max(1, (src.width * ratio).toInt()),
            max(1, (src.height * ratio).toInt()),
            true
        )
    }

    private fun decodeAudioTrackToPcm(context: Context, uri: Uri): Pair<ShortArray, Int>? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val m = f.getString(MediaFormat.KEY_MIME).orEmpty()
                if (m.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }
            if (audioTrackIndex < 0 || format == null) return null
            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 1

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val maxSamples = sampleRate * 30 // cap decode at 30s for fast responsive import
            val outputSamples = ArrayList<Short>(min(maxSamples, sampleRate * 10))
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone && outputSamples.size < maxSamples) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx) ?: continue
                        val sampleSize = extractor.readSampleData(inBuf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, 10_000)
                if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)
                    if (outBuf != null && info.size > 0) {
                        val chunk = ByteArray(info.size)
                        outBuf.position(info.offset)
                        outBuf.get(chunk)
                        val shortBuf = ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        val totalShorts = shortBuf.remaining()
                        var idx = 0
                        while (idx < totalShorts && outputSamples.size < maxSamples) {
                            if (channels >= 2 && idx + 1 < totalShorts) {
                                val mono = ((shortBuf.get(idx).toInt() + shortBuf.get(idx + 1).toInt()) / 2).toShort()
                                outputSamples.add(mono)
                                idx += channels
                            } else {
                                outputSamples.add(shortBuf.get(idx))
                                idx++
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                }
            }

            if (outputSamples.isEmpty()) return null
            val resampled = resampleTo44100(outputSamples.toShortArray(), sampleRate)
            return resampled to DEFAULT_SAMPLE_RATE
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun resampleTo44100(input: ShortArray, srcRate: Int): ShortArray {
        if (srcRate == DEFAULT_SAMPLE_RATE || srcRate <= 0) return input
        val ratio = srcRate.toDouble() / DEFAULT_SAMPLE_RATE.toDouble()
        val outLen = max(1, (input.size / ratio).toInt())
        val out = ShortArray(outLen)
        for (i in 0 until outLen) {
            val srcPos = (i * ratio).toInt().coerceIn(0, input.lastIndex)
            out[i] = input[srcPos]
        }
        return out
    }
}
