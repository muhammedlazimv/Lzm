package com.example.editor.engine

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import com.example.editor.model.TimelineClip
import com.example.editor.model.TrackType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AudioPlaybackEngine(
    private val mediaRegistry: MediaRegistry
) {
    private val sampleRate = MediaRegistry.DEFAULT_SAMPLE_RATE
    private var playbackJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private var recordJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private val recordedSamples = ArrayList<Short>(sampleRate * 10)
    private var recordStartPlayhead: Float = 0f

    @Volatile
    var lastPeakLeft: Float = 0.08f
        private set

    @Volatile
    var lastPeakRight: Float = 0.10f
        private set

    fun startPlayback(
        scope: CoroutineScope,
        startSec: Float,
        clipsProvider: () -> List<TimelineClip>,
        totalDurationProvider: () -> Float
    ) {
        stopPlayback()
        playbackJob = scope.launch(Dispatchers.IO) {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull()

            audioTrack = track
            runCatching { track?.play() }

            val chunkFrames = 1024
            val mixBuffer = ShortArray(chunkFrames)
            var currentTimeSec = startSec.toDouble()
            val secPerSample = 1.0 / sampleRate.toDouble()

            while (isActive) {
                val clips = clipsProvider()
                val totalDur = totalDurationProvider().toDouble()
                if (currentTimeSec >= totalDur) break

                var peak = 0
                for (i in 0 until chunkFrames) {
                    val t = currentTimeSec + i * secPerSample
                    var sampleSum = 0f
                    for (clip in clips) {
                        if (clip.track != TrackType.AUDIO || clip.muted) continue
                        if (t < clip.startTime || t > clip.startTime + clip.duration) continue
                        val asset = mediaRegistry.get(clip.mediaId)
                        val pcm = when (asset) {
                            is MediaAsset.AudioAsset -> asset.pcm
                            is MediaAsset.VideoAsset -> asset.audioPcm
                            else -> null
                        } ?: continue
                        val localT = (t - clip.startTime) * clip.speed.coerceIn(0.1f, 4f) + clip.trimStart
                        val sampleIdx = (localT * sampleRate).toInt()
                        if (sampleIdx in pcm.indices) {
                            sampleSum += pcm[sampleIdx] * clip.volume
                        }
                    }
                    val clamped = sampleSum.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    mixBuffer[i] = clamped.toShort()
                    val a = abs(clamped)
                    if (a > peak) peak = a
                }

                val normPeak = (peak.toFloat() / Short.MAX_VALUE).coerceIn(0.08f, 1f)
                lastPeakLeft = normPeak
                lastPeakRight = (normPeak * 0.92f + 0.05f).coerceIn(0.08f, 1f)

                runCatching {
                    track?.write(mixBuffer, 0, chunkFrames)
                }
                currentTimeSec += chunkFrames * secPerSample
            }

            lastPeakLeft = 0.08f
            lastPeakRight = 0.10f
            runCatching {
                track?.stop()
                track?.release()
            }
            audioTrack = null
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        }
        audioTrack = null
        lastPeakLeft = 0.08f
        lastPeakRight = 0.10f
    }

    @SuppressLint("MissingPermission")
    fun startVoiceRecording(
        scope: CoroutineScope,
        playheadSec: Float,
        onTickSeconds: (Float) -> Unit
    ): Boolean {
        stopVoiceRecording()
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val recorder = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2
            )
        }.getOrNull() ?: return false

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return false
        }

        audioRecord = recorder
        recordedSamples.clear()
        recordStartPlayhead = playheadSec

        return try {
            recorder.startRecording()
            recordJob = scope.launch(Dispatchers.IO) {
                val buf = ShortArray(1024)
                while (isActive) {
                    val read = recorder.read(buf, 0, buf.size)
                    if (read > 0) {
                        synchronized(recordedSamples) {
                            for (i in 0 until read) {
                                recordedSamples.add(buf[i])
                            }
                            onTickSeconds(recordedSamples.size.toFloat() / sampleRate)
                        }
                    }
                }
            }
            true
        } catch (_: Exception) {
            runCatching { recorder.release() }
            audioRecord = null
            false
        }
    }

    fun stopVoiceRecording(): Pair<MediaAsset.AudioAsset, Float>? {
        recordJob?.cancel()
        recordJob = null
        runCatching {
            audioRecord?.stop()
            audioRecord?.release()
        }
        audioRecord = null

        val pcm = synchronized(recordedSamples) {
            if (recordedSamples.size < sampleRate / 4) return null
            recordedSamples.toShortArray().also { recordedSamples.clear() }
        }

        val durSec = max(0.5f, pcm.size.toFloat() / sampleRate)
        val id = "voice-${System.currentTimeMillis()}"
        val asset = MediaAsset.AudioAsset(
            id = id,
            displayName = "Voiceover_Mic.wav",
            pcm = pcm,
            sampleRate = sampleRate,
            durationSec = durSec,
            waveform = mediaRegistry.computeWaveform(pcm, 36)
        )
        mediaRegistry.putAudio(asset)
        return asset to recordStartPlayhead
    }

    fun mixOfflinePcm(clips: List<TimelineClip>, totalDurationSec: Float): ShortArray {
        val totalSamples = max(sampleRate, (totalDurationSec * sampleRate).toInt())
        val out = ShortArray(totalSamples)
        val secPerSample = 1.0 / sampleRate
        val audioClips = clips.filter { it.track == TrackType.AUDIO && !it.muted }

        for (i in 0 until totalSamples) {
            val t = i * secPerSample
            var sum = 0f
            for (clip in audioClips) {
                if (t < clip.startTime || t > clip.startTime + clip.duration) continue
                val asset = mediaRegistry.get(clip.mediaId)
                val pcm = when (asset) {
                    is MediaAsset.AudioAsset -> asset.pcm
                    is MediaAsset.VideoAsset -> asset.audioPcm
                    else -> null
                } ?: continue
                val localT = (t - clip.startTime) * clip.speed.coerceIn(0.1f, 4f) + clip.trimStart
                val idx = (localT * sampleRate).toInt()
                if (idx in pcm.indices) {
                    sum += pcm[idx] * clip.volume
                }
            }
            out[i] = sum.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    fun release() {
        stopPlayback()
        stopVoiceRecording()
    }
}
