package com.bassengine.core

/**
 * Builds the same shelf/peaking cascade that the DSP applies, so the curve in
 * the UI is a real magnitude response rather than a decorative spline.
 */
object ResponseMath {
    fun filters(resolved: ResolvedConfig, sampleRate: Int = 48_000): List<BiquadCoeffs> {
        val out = ArrayList<BiquadCoeffs>(20)
        resolved.bands.forEachIndexed { i, gain ->
            out += DspMath.peaking(
                sampleRate,
                DspMath.FREQUENCIES[i].toDouble(),
                gain.toDouble(),
                DspMath.BAND_Q[i].toDouble()
            )
        }
        val bass = resolved.bass
        val surround = resolved.surround
        if (bass.enabled) {
            var lowGain = if (bass.boost) 3.5 + (bass.q - 2.2) * 1.0 else 0.0
            if (bass.adaptive) lowGain += 1.25
            if (bass.subwoofer) lowGain += 4.0
            out += DspMath.lowShelf(sampleRate, 70.0, lowGain.coerceIn(-6.0, 14.0), bass.q.coerceIn(0.5f, 2.0f).toDouble())
            if (bass.subImpact) out += DspMath.lowShelf(sampleRate, 32.0, 10.0, 0.9)
            if (bass.subwoofer) {
                out += DspMath.highShelf(sampleRate, 9_000.0, 5.5, 1.0)
                out += DspMath.peaking(sampleRate, 800.0, -4.0, 0.8)
            }
        }
        if (surround.air) out += DspMath.highShelf(sampleRate, 10_000.0, 3.0, 0.9)
        if (surround.vocal) out += DspMath.peaking(sampleRate, 1_100.0, 3.0, 0.9)
        if (surround.focus) out += DspMath.peaking(sampleRate, 2_800.0, 2.0, 1.1)
        if (surround.loudness) {
            out += DspMath.lowShelf(sampleRate, 55.0, 2.5, 0.8)
            out += DspMath.highShelf(sampleRate, 11_000.0, 1.8, 0.8)
        }
        when (surround.mode) {
            SURROUND_CINEMA -> out += DspMath.lowShelf(sampleRate, 45.0, 2.0, 1.0)
            SURROUND_NEAR -> out += DspMath.peaking(sampleRate, 1_600.0, 2.0, 1.0)
            else -> out += DspMath.highShelf(sampleRate, 8_000.0, 1.0, 1.0)
        }
        return out
    }

    fun responseDb(resolved: ResolvedConfig, freq: Double, sampleRate: Int = 48_000): Double =
        DspMath.cascadeDb(filters(resolved, sampleRate), freq, sampleRate) + resolved.bass.preampDb
}
