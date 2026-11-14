package com.bassengine.core

data class ResolvedConfig(
    val bands: List<Float>,
    val bass: BassConfig,
    val surround: SurroundConfig,
    val engineOn: Boolean,
    val presetId: String,
    val reason: String
)

/**
 * Resolves the user's base configuration into the curve for one output route:
 * app rules, device profiles and the sleep schedule are folded in, so callers
 * do not need to combine them by hand.
 */
object ConfigResolver {
    fun resolve(
        config: BassEngineConfig,
        packageName: String,
        deviceType: String,
        nowMinutes: Int
    ): ResolvedConfig {
        var bands = config.bands.toList()
        var bass = config.bass
        var surround = config.surround
        var presetId = config.presetId
        var engineOn = config.engineOn
        val reasons = mutableListOf<String>()

        val profileKey = when (config.device.mode) {
            DEVICE_SPEAKER, DEVICE_HEADPHONE, DEVICE_BLUETOOTH -> config.device.mode
            else -> deviceType
        }
        val profile = config.device.profiles[profileKey]
        if (profile != null) {
            bands = Presets.bandsFor(config, profile.presetId)
            presetId = profile.presetId
            bass = bass.copy(enabled = bass.enabled && profile.bassEnabled)
            surround = surround.copy(
                mode = profile.surroundMode,
                intensity = profile.intensity.coerceIn(40, 70)
            )
            reasons += when (profileKey) {
                DEVICE_HEADPHONE -> "耳机档案"
                DEVICE_BLUETOOTH -> "蓝牙档案"
                else -> "扬声器档案"
            }
        }

        config.appRules.firstOrNull { it.packageName == packageName }?.let { rule ->
            bands = Presets.bandsFor(config, rule.presetId)
            presetId = rule.presetId
            reasons += "应用规则 ${Presets.nameFor(config, rule.presetId)}"
        }

        if (config.schedule.enabled && inWindow(nowMinutes, config.schedule.start, config.schedule.end)) {
            when (config.schedule.action) {
                SCHEDULE_OFF -> {
                    engineOn = false
                    reasons += "定时关闭"
                }
                else -> {
                    bands = Presets.byId(Presets.SLEEP)!!.bands
                    presetId = Presets.SLEEP
                    surround = surround.copy(mode = SURROUND_NEAR, intensity = 40)
                    reasons += "定时助眠"
                }
            }
        }

        return ResolvedConfig(
            bands = bands,
            bass = bass,
            surround = surround,
            engineOn = engineOn,
            presetId = presetId,
            reason = reasons.joinToString(" · ").ifBlank { "基础配置" }
        )
    }

    fun inWindow(nowMinutes: Int, start: String, end: String): Boolean {
        val s = parseMinutes(start) ?: return false
        val e = parseMinutes(end) ?: return false
        if (s == e) return true
        return if (s < e) nowMinutes in s until e else nowMinutes >= s || nowMinutes < e
    }

    fun parseMinutes(value: String): Int? {
        val parts = value.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }
}
