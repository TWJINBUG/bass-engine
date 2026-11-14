package com.bassengine.core

const val THEME_DARK = "dark"
const val THEME_MIDNIGHT = "midnight"
const val THEME_NEON = "neon"
const val THEME_TITANIUM = "titanium"

const val SURROUND_WIDE = "wide"
const val SURROUND_CINEMA = "cinema"
const val SURROUND_NEAR = "near"

const val DEVICE_AUTO = "auto"
const val DEVICE_SPEAKER = "speaker"
const val DEVICE_HEADPHONE = "headphone"
const val DEVICE_BLUETOOTH = "bluetooth"

const val SCHEDULE_SLEEP = "sleep"
const val SCHEDULE_OFF = "off"

data class BassConfig(
    val enabled: Boolean = true,
    val boost: Boolean = true,
    val virtualBass: Boolean = false,
    val adaptive: Boolean = true,
    val subImpact: Boolean = false,
    val subwoofer: Boolean = false,
    val preampDb: Float = 0.0f,
    val q: Float = 2.2f
)

data class SurroundConfig(
    val mode: String = SURROUND_WIDE,
    val air: Boolean = true,
    val focus: Boolean = true,
    val widthDetail: Boolean = true,
    val vocal: Boolean = true,
    val loudness: Boolean = true,
    val intensity: Int = 45
)

data class ScheduleConfig(
    val enabled: Boolean = false,
    val start: String = "23:00",
    val end: String = "07:00",
    val action: String = SCHEDULE_SLEEP
)

data class DeviceProfile(
    val presetId: String = Presets.HARMAN,
    val bassEnabled: Boolean = true,
    val surroundMode: String = SURROUND_WIDE,
    val intensity: Int = 50
)

data class DeviceConfig(
    val mode: String = DEVICE_AUTO,
    val lockedModel: String? = null,
    val profiles: Map<String, DeviceProfile> = mapOf(
        DEVICE_SPEAKER to DeviceProfile(Presets.HARMAN, true, SURROUND_WIDE, 45),
        DEVICE_HEADPHONE to DeviceProfile(Presets.HARMAN, true, SURROUND_NEAR, 50),
        DEVICE_BLUETOOTH to DeviceProfile(Presets.CINEMA, true, SURROUND_WIDE, 55)
    )
)

data class AppRule(
    val packageName: String,
    val presetId: String
)

data class CustomPreset(
    val id: String,
    val name: String,
    val color: Long,
    val bands: List<Float>,
    val createdAt: Long = System.currentTimeMillis()
)

data class BassEngineConfig(
    val schema: Int = 1,
    val engineOn: Boolean = true,
    val presetId: String = Presets.HARMAN,
    val bands: List<Float> = Presets.byId(Presets.HARMAN)!!.bands,
    val bass: BassConfig = BassConfig(),
    val surround: SurroundConfig = SurroundConfig(),
    val limiter: Boolean = true,
    val theme: String = THEME_DARK,
    val customPresets: List<CustomPreset> = emptyList(),
    val device: DeviceConfig = DeviceConfig(),
    val appRules: List<AppRule> = emptyList(),
    val schedule: ScheduleConfig = ScheduleConfig(),
    val lastTab: Int = 0,
    val updatedAt: Long = 0L
) {
    fun normalized(): BassEngineConfig {
        val safeBands = when {
            bands.size == DspMath.BAND_COUNT -> bands.map { DspMath.clampGain(it) }
            else -> Presets.byId(Presets.SPACE)!!.bands
        }
        val safeTheme = theme.takeIf { it in setOf(THEME_DARK, THEME_MIDNIGHT, THEME_NEON, THEME_TITANIUM) } ?: THEME_DARK
        val safeMode = surround.mode.takeIf { it in setOf(SURROUND_WIDE, SURROUND_CINEMA, SURROUND_NEAR) } ?: SURROUND_WIDE
        return copy(
            bands = safeBands,
            theme = safeTheme,
            surround = surround.copy(
                mode = safeMode,
                intensity = surround.intensity.coerceIn(40, 70)
            ),
            bass = bass.copy(
                preampDb = bass.preampDb.coerceIn(-10f, 10f),
                q = bass.q.coerceIn(0.5f, 3.0f)
            ),
            lastTab = lastTab.coerceIn(0, 4)
        )
    }
}

fun defaultConfig(): BassEngineConfig = BassEngineConfig().normalized()
