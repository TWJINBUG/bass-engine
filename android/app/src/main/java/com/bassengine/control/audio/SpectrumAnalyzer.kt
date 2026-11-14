package com.bassengine.control.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Microphone spectrum for the visualiser. It deliberately does not use
 * Visualizer on session 0, which would need privileged routing permissions.
 */
class SpectrumAnalyzer(private val onBands: (FloatArray) -> Unit) {
    companion object {
        const val SAMPLE_RATE = 44_100
        const val FFT_SIZE = 1024
        const val BANDS = 56
        private const val MIN_HZ = 40.0
        private const val MAX_HZ = 16_000.0
    }

    @Volatile
    private var running = false
    private var thread: Thread? = null
    private val smoothed = FloatArray(BANDS)
    private val handler = Handler(Looper.getMainLooper())
    private var lastPost = 0L

    fun start(): Boolean {
        if (running) return true
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return false
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer, FFT_SIZE * 2 * 4)
            )
        } catch (_: Throwable) {
            return false
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return false
        }
        running = true
        thread = Thread {
            try {
                record.startRecording()
                val buffer = ShortArray(2048)
                val window = FloatArray(FFT_SIZE) { i ->
                    (0.5 - 0.5 * cos(2.0 * PI * i / (FFT_SIZE - 1))).toFloat()
                }
                while (running) {
                    val n = record.read(buffer, 0, buffer.size)
                    if (n < FFT_SIZE) continue
                    val real = FloatArray(FFT_SIZE) { i -> buffer[i] / 32768f * window[i] }
                    val imag = FloatArray(FFT_SIZE)
                    Fft.transform(real, imag)
                    val mags = Fft.magnitude(real, imag)
                    val bands = FloatArray(BANDS)
                    for (b in 0 until BANDS) {
                        val f0 = MIN_HZ * (MAX_HZ / MIN_HZ).pow(b.toDouble() / BANDS)
                        val f1 = MIN_HZ * (MAX_HZ / MIN_HZ).pow((b + 1).toDouble() / BANDS)
                        val i0 = (f0 / SAMPLE_RATE * FFT_SIZE).toInt().coerceIn(1, FFT_SIZE / 2 - 2)
                        val i1 = (f1 / SAMPLE_RATE * FFT_SIZE).toInt().coerceIn(i0 + 1, FFT_SIZE / 2 - 1)
                        var peak = 0f
                        for (i in i0 until i1) {
                            if (mags[i] > peak) peak = mags[i]
                        }
                        val db = 20.0 * log10((peak / (FFT_SIZE / 2.0)).coerceAtLeast(1e-7))
                        val normalized = ((db + 72.0) / 62.0).coerceIn(0.0, 1.0).toFloat()
                        smoothed[b] = maxOf(normalized, smoothed[b] * 0.82f)
                        bands[b] = smoothed[b]
                    }
                    val now = System.currentTimeMillis()
                    if (now - lastPost > 33) {
                        lastPost = now
                        val copy = bands.copyOf()
                        handler.post { onBands(copy) }
                    }
                }
            } catch (_: Throwable) {
            } finally {
                try {
                    record.stop()
                } catch (_: Throwable) {
                }
                record.release()
            }
        }.apply { isDaemon = true; name = "BassEngineSpectrum" }
        thread?.start()
        return true
    }

    fun stop() {
        running = false
        thread?.join(300)
        thread = null
        smoothed.fill(0f)
    }
}
