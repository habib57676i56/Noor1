package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.sin

/**
 * Lightweight, self-contained audible mistake chime player for Progressive Page/Surah Memorization.
 * Synthesizes a clean, unmistakable two-tone descending alert (520Hz -> 360Hz, 180ms total)
 * using AudioTrack (USAGE_ASSISTANCE_SONIFICATION) so it does not require bundled external sound files
 * and safely avoids interfering with single-session QuranAudioPlayer playback.
 */
class RecitationMistakeSoundPlayer {

    private val isMuted = AtomicBoolean(false)
    private val _playedCountForTest = AtomicInteger(0)
    val playedCountForTest: Int
        get() = _playedCountForTest.get()

    @Volatile
    private var lastTriggerMs: Long = 0L

    fun setMuted(muted: Boolean) {
        isMuted.set(muted)
    }

    fun isMuted(): Boolean = isMuted.get()

    fun playMistakeAlert() {
        if (isMuted.get()) return
        val now = System.currentTimeMillis()
        // Prevent rapid back-to-back buzz if multiple wrong words hit within 220ms
        if (now - lastTriggerMs < 220L) return
        lastTriggerMs = now
        _playedCountForTest.incrementAndGet()

        Thread {
            val playedViaTrack = runCatching { playSynthesizedDoubleBeep() }.getOrDefault(false)
            if (!playedViaTrack) {
                runCatching {
                    val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                    tg.startTone(ToneGenerator.TONE_PROP_NACK, 180)
                    Thread.sleep(210L)
                    tg.release()
                }
            }
        }.apply {
            name = "noor-mistake-chime"
            isDaemon = true
            start()
        }
    }

    private fun playSynthesizedDoubleBeep(): Boolean {
        val sampleRate = 22050
        val totalSamples = (sampleRate * 0.19).toInt()
        val half = totalSamples / 2
        val pcm = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val freq = if (i < half) 520.0 else 360.0
            val localPos = if (i < half) i else (i - half)
            val segLen = if (i < half) half else (totalSamples - half)
            val env = when {
                localPos < 160 -> localPos / 160.0
                localPos > segLen - 220 -> ((segLen - localPos).coerceAtLeast(0)) / 220.0
                else -> 1.0
            }
            val sample = sin(2.0 * PI * freq * i / sampleRate) * env * 0.55
            pcm[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }

        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) return false

        val bufferBytes = maxOf(minBuf, pcm.size * 2)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        return try {
            if (track.state != AudioTrack.STATE_INITIALIZED) {
                track.release()
                false
            } else {
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(210L)
                track.stop()
                track.release()
                true
            }
        } catch (_: Throwable) {
            runCatching { track.release() }
            false
        }
    }

    fun release() {
        lastTriggerMs = 0L
    }
}
