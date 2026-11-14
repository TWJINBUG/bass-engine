package com.bassengine.control.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

class TestTonePlayer {
    enum class Mode { PINK, SWEEP }

    @Volatile
    private var running = false
    private var thread: Thread? = null
    private var track: AudioTrack? = null

    fun start(mode: Mode): Boolean {
        stop()
        val sampleRate = 48_000
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return false
        val audioTrack = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuffer, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Throwable) {
            return false
        }
        track = audioTrack
        running = true
        thread = Thread {
            try {
                audioTrack.play()
                val buffer = ShortArray(2048)
                var phase = 0.0
                var sweepT = 0.0
                var b0 = 0f; var b1 = 0f; var b2 = 0f; var b3 = 0f; var b4 = 0f; var b5 = 0f; var b6 = 0f
                while (running) {
                    for (i in buffer.indices step 2) {
                        val sample: Float = if (mode == Mode.PINK) {
                            val white = Random.nextFloat() * 2f - 1f
                            b0 = 0.99886f * b0 + white * 0.0555179f
                            b1 = 0.99332f * b1 + white * 0.0750759f
                            b2 = 0.96900f * b2 + white * 0.1538520f
                            b3 = 0.86650f * b3 + white * 0.3104856f
                            b4 = 0.55000f * b4 + white * 0.5329522f
                            b5 = -0.7616f * b5 - white * 0.0168980f
                            val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362f
                            b6 = white * 0.115926f
                            (pink * 0.045f).coerceIn(-0.22f, 0.22f)
                        } else {
                            val f = 20.0 * (1000.0).pow((sweepT % 8.0) / 8.0)
                            phase += 2.0 * PI * f / sampleRate
                            if (phase > 2.0 * PI) phase -= 2.0 * PI
                            (sin(phase) * 0.16 * exp(-sweepT % 8.0 / 40.0)).toFloat()
                        }
                        val v = (sample * 32767).toInt().coerceIn(-32768, 32767).toShort()
                        buffer[i] = v
                        buffer[i + 1] = v
                        sweepT += 1.0 / sampleRate
                    }
                    val written = audioTrack.write(buffer, 0, buffer.size)
                    if (written < 0) break
                }
            } catch (_: Throwable) {
            } finally {
                runCatching { audioTrack.stop() }
                audioTrack.release()
            }
        }.apply { isDaemon = true; name = "BassEngineTestTone" }
        thread?.start()
        return true
    }

    fun stop() {
        running = false
        thread?.join(400)
        thread = null
        runCatching { track?.pause() }
        runCatching { track?.flush() }
        track = null
    }

    fun isPlaying(): Boolean = running
}
