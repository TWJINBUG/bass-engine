package com.bassengine.control.ui

import java.util.Locale
import kotlin.math.abs

fun formatDb(value: Float, digits: Int = 1): String {
    val sign = if (value >= 0) "+" else ""
    return sign + String.format(Locale.US, "%.${digits}f dB", value)
}

fun formatDbShort(value: Float): String {
    val sign = if (value >= 0) "+" else ""
    return sign + String.format(Locale.US, "%.1f", value)
}

fun formatCount(value: Long): String = when {
    abs(value) >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
    abs(value) >= 1_000 -> String.format(Locale.US, "%.1fk", value / 1_000.0)
    else -> value.toString()
}

fun formatUptime(ms: Long): String {
    if (ms <= 0) return "0m"
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

fun formatPercent(value: Int): String = "$value%"

fun freqLabel(hz: Float): String = when {
    hz >= 1000f -> {
        val k = hz / 1000f
        if (k == k.toInt().toFloat()) "${k.toInt()}k" else String.format(Locale.US, "%.1fk", k)
    }
    else -> hz.toInt().toString()
}

fun bassTotalDb(preamp: Float, boost: Boolean, adaptive: Boolean, subImpact: Boolean, subwoofer: Boolean): Float {
    var total = preamp
    if (boost) total += 4.5f
    if (adaptive) total += 2.5f
    if (subImpact) total += 5.0f
    if (subwoofer) total += 7.0f
    return total
}
