package com.bassengine.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Biquad coefficients normalised by a0. The denominator is
 * 1 + a1*z^-1 + a2*z^-2.
 */
data class BiquadCoeffs(
    val b0: Float,
    val b1: Float,
    val b2: Float,
    val a1: Float,
    val a2: Float
) {
    fun magnitude(freq: Double, sampleRate: Int): Double {
        val w = 2.0 * PI * freq / sampleRate.toDouble()
        val cosW = cos(w)
        val sinW = sin(w)
        val cos2 = cos(2.0 * w)
        val sin2 = sin(2.0 * w)
        val numRe = b0 + b1 * cosW + b2 * cos2
        val numIm = -(b1 * sinW + b2 * sin2)
        val denRe = 1.0 + a1 * cosW + a2 * cos2
        val denIm = -(a1 * sinW + a2 * sin2)
        val num = sqrt(numRe * numRe + numIm * numIm)
        val den = sqrt(denRe * denRe + denIm * denIm)
        return num / den.coerceAtLeast(1e-12)
    }

    fun magnitudeDb(freq: Double, sampleRate: Int): Double =
        20.0 * log10(magnitude(freq, sampleRate).coerceAtLeast(1e-9))

    companion object {
        val IDENTITY = BiquadCoeffs(1f, 0f, 0f, 0f, 0f)
    }
}

/** Direct form I biquad with per-instance state. */
class Biquad {
    private var x1 = 0f
    private var x2 = 0f
    private var y1 = 0f
    private var y2 = 0f

    fun process(x: Float, c: BiquadCoeffs): Float {
        val y = c.b0 * x + c.b1 * x1 + c.b2 * x2 - c.a1 * y1 - c.a2 * y2
        x2 = x1
        x1 = x
        y2 = y1
        y1 = y
        return y
    }

    fun reset() {
        x1 = 0f
        x2 = 0f
        y1 = 0f
        y2 = 0f
    }
}

/**
 * Shared RBJ cookbook filter maths. The UI uses the same coefficients as the
 * audio thread, so the drawn response is the response that is actually heard.
 */
object DspMath {
    val FREQUENCIES = floatArrayOf(20f, 125f, 250f, 500f, 650f, 1000f, 2000f, 4000f, 8000f, 16000f)
    val BAND_Q = floatArrayOf(1.15f, 1.1f, 1.0f, 0.95f, 0.85f, 0.85f, 1.0f, 1.0f, 1.0f, 1.0f)
    const val BAND_COUNT = 10

    fun dbToLinear(db: Double): Double = 10.0.pow(db / 20.0)

    fun clampGain(db: Float): Float = db.coerceIn(-15f, 15f)

    fun peaking(sampleRate: Int, freq: Double, gainDb: Double, q: Double): BiquadCoeffs {
        if (abs(gainDb) < 0.001) return BiquadCoeffs.IDENTITY
        val f = freq.coerceIn(10.0, sampleRate / 2.0 - 10.0)
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * f / sampleRate
        val cosW = cos(w0)
        val alpha = sin(w0) / (2.0 * q.coerceAtLeast(0.1))
        val b0 = 1.0 + alpha * a
        val b1 = -2.0 * cosW
        val b2 = 1.0 - alpha * a
        val a0 = 1.0 + alpha / a
        val a1 = -2.0 * cosW
        val a2 = 1.0 - alpha / a
        return normalise(b0, b1, b2, a0, a1, a2)
    }

    fun lowShelf(sampleRate: Int, freq: Double, gainDb: Double, s: Double = 1.0): BiquadCoeffs {
        if (abs(gainDb) < 0.001) return BiquadCoeffs.IDENTITY
        val f = freq.coerceIn(10.0, sampleRate / 2.0 - 10.0)
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * f / sampleRate
        val cosW = cos(w0)
        val sinW = sin(w0)
        val alpha = sinW / 2.0 * sqrt((a + 1.0 / a) * (1.0 / s - 1.0) + 2.0)
        val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha
        val b0 = a * ((a + 1.0) - (a - 1.0) * cosW + twoSqrtAAlpha)
        val b1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cosW)
        val b2 = a * ((a + 1.0) - (a - 1.0) * cosW - twoSqrtAAlpha)
        val a0 = (a + 1.0) + (a - 1.0) * cosW + twoSqrtAAlpha
        val a1 = -2.0 * ((a - 1.0) + (a + 1.0) * cosW)
        val a2 = (a + 1.0) + (a - 1.0) * cosW - twoSqrtAAlpha
        return normalise(b0, b1, b2, a0, a1, a2)
    }

    fun highShelf(sampleRate: Int, freq: Double, gainDb: Double, s: Double = 1.0): BiquadCoeffs {
        if (abs(gainDb) < 0.001) return BiquadCoeffs.IDENTITY
        val f = freq.coerceIn(10.0, sampleRate / 2.0 - 10.0)
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * f / sampleRate
        val cosW = cos(w0)
        val sinW = sin(w0)
        val alpha = sinW / 2.0 * sqrt((a + 1.0 / a) * (1.0 / s - 1.0) + 2.0)
        val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha
        val b0 = a * ((a + 1.0) + (a - 1.0) * cosW + twoSqrtAAlpha)
        val b1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * cosW)
        val b2 = a * ((a + 1.0) + (a - 1.0) * cosW - twoSqrtAAlpha)
        val a0 = (a + 1.0) - (a - 1.0) * cosW + twoSqrtAAlpha
        val a1 = 2.0 * ((a - 1.0) - (a + 1.0) * cosW)
        val a2 = (a + 1.0) - (a - 1.0) * cosW - twoSqrtAAlpha
        return normalise(b0, b1, b2, a0, a1, a2)
    }

    fun lowPass(sampleRate: Int, freq: Double, q: Double = 0.707): BiquadCoeffs {
        val f = freq.coerceIn(10.0, sampleRate / 2.0 - 10.0)
        val w0 = 2.0 * PI * f / sampleRate
        val cosW = cos(w0)
        val alpha = sin(w0) / (2.0 * q.coerceAtLeast(0.1))
        val b0 = (1.0 - cosW) / 2.0
        val b1 = 1.0 - cosW
        val b2 = (1.0 - cosW) / 2.0
        val a0 = 1.0 + alpha
        val a1 = -2.0 * cosW
        val a2 = 1.0 - alpha
        return normalise(b0, b1, b2, a0, a1, a2)
    }

    fun highPass(sampleRate: Int, freq: Double, q: Double = 0.707): BiquadCoeffs {
        val f = freq.coerceIn(10.0, sampleRate / 2.0 - 10.0)
        val w0 = 2.0 * PI * f / sampleRate
        val cosW = cos(w0)
        val alpha = sin(w0) / (2.0 * q.coerceAtLeast(0.1))
        val b0 = (1.0 + cosW) / 2.0
        val b1 = -(1.0 + cosW)
        val b2 = (1.0 + cosW) / 2.0
        val a0 = 1.0 + alpha
        val a1 = -2.0 * cosW
        val a2 = 1.0 - alpha
        return normalise(b0, b1, b2, a0, a1, a2)
    }

    private fun normalise(b0: Double, b1: Double, b2: Double, a0: Double, a1: Double, a2: Double): BiquadCoeffs =
        BiquadCoeffs(
            (b0 / a0).toFloat(),
            (b1 / a0).toFloat(),
            (b2 / a0).toFloat(),
            (a1 / a0).toFloat(),
            (a2 / a0).toFloat()
        )

    /** Total magnitude of a cascade at [freq], in dB. */
    fun cascadeDb(filters: List<BiquadCoeffs>, freq: Double, sampleRate: Int): Double {
        var sum = 0.0
        for (f in filters) {
            if (f.b0 == 1f && f.b1 == 0f && f.b2 == 0f && f.a1 == 0f && f.a2 == 0f) continue
            sum += f.magnitudeDb(freq, sampleRate)
        }
        return sum
    }
}
