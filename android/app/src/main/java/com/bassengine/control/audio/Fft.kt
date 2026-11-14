package com.bassengine.control.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

object Fft {
    fun transform(real: FloatArray, imag: FloatArray) {
        val n = real.size
        if (n == 0 || n and (n - 1) != 0 || imag.size != n) return

        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
        }

        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wLenReal = cos(ang).toFloat()
            val wLenImag = kotlin.math.sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var wReal = 1f
                var wImag = 0f
                for (k in 0 until len / 2) {
                    val uReal = real[i + k]
                    val uImag = imag[i + k]
                    val vReal = real[i + k + len / 2] * wReal - imag[i + k + len / 2] * wImag
                    val vImag = real[i + k + len / 2] * wImag + imag[i + k + len / 2] * wReal
                    real[i + k] = uReal + vReal
                    imag[i + k] = uImag + vImag
                    real[i + k + len / 2] = uReal - vReal
                    imag[i + k + len / 2] = uImag - vImag
                    val nextWReal = wReal * wLenReal - wImag * wLenImag
                    wImag = wReal * wLenImag + wImag * wLenReal
                    wReal = nextWReal
                }
                i += len
            }
            len = len shl 1
        }
    }

    fun magnitude(real: FloatArray, imag: FloatArray): FloatArray {
        val out = FloatArray(real.size / 2)
        for (i in out.indices) {
            out[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }
        return out
    }
}
