package com.bassengine.core

data class Preset(
    val id: String,
    val name: String,
    val subtitle: String,
    val color: Long,
    val bands: List<Float>,
    val builtIn: Boolean = true
) {
    init {
        require(bands.size == DspMath.BAND_COUNT) { "A preset needs exactly 10 bands" }
    }
}

object Presets {
    const val VOCAL = "vocal"
    const val SPACE = "space3d"
    const val GAME = "game"
    const val CINEMA = "cinema"
    const val SLEEP = "sleep"
    const val BW = "bw"
    const val HIRES = "hires"
    const val HARMAN = "harman"
    const val ONEPLUS15 = "oneplus15"
    const val CUSTOM = "custom"

    val all: List<Preset> = listOf(
        Preset(
            VOCAL, "人声 VOCAL", "中频靠前，齿音不刺，适合歌词/播客", 0xFF4DD0E1,
            listOf(-2.5f, -2f, -1f, 1f, 2f, 3f, 2.5f, 1f, 0f, -1f)
        ),
        Preset(
            SPACE, "3D SPACE", "温和的宽场与空气感，不靠夸张增益", 0xFF7C4DFF,
            listOf(3f, 2.5f, 1f, 0f, -1f, 0f, 1.5f, 2.5f, 3f, 2f)
        ),
        Preset(
            GAME, "游戏 GAME", "脚步与提示音更清楚，低频不糊", 0xFF00E5A0,
            listOf(1f, 1f, 0f, 0f, 1f, 2f, 3.5f, 3.5f, 2f, 1f)
        ),
        Preset(
            CINEMA, "影院 CINEMA", "对白稳、包围感足，适合追剧", 0xFFFF8A65,
            listOf(4f, 3f, 1.5f, 0f, -0.5f, 0f, 1.5f, 2.5f, 1.5f, 0f)
        ),
        Preset(
            SLEEP, "助眠 SLEEP", "削掉刺激频段，声音更远更软", 0xFF90A4AE,
            listOf(1f, 0.5f, 0f, -1f, -1.5f, -2f, -1.5f, -2.5f, -3f, -3.5f)
        ),
        Preset(
            BW, "宝华 B&W", "温暖中频、丝滑高频的英伦声", 0xFFFFD166,
            listOf(2.5f, 2f, 1f, 0f, -1f, 0f, 1f, 1.5f, 2f, 1f)
        ),
        Preset(
            HIRES, "Hi-Res HIFI", "平直高解析，不做额外染色", 0xFF64B5F6,
            listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        ),
        Preset(
            HARMAN, "哈曼卡顿 HARMAN", "哈曼风格低频下潜 + 人声甜润，耐听不轰头", 0xFFE040FB,
            listOf(5f, 4.5f, 3f, 1f, 0f, 0f, 1f, 2.5f, 2f, 1f)
        ),
        Preset(
            ONEPLUS15, "一加 15 特调", "按 PLK110 双扬声器收低频、提中高频", 0xFFFF5252,
            listOf(4f, 3.5f, 2f, 0.5f, -1f, 0f, 1.5f, 2f, 2.5f, 1.5f)
        )
    )

    fun byId(id: String): Preset? = all.firstOrNull { it.id == id }

    fun bandsFor(config: BassEngineConfig, id: String): List<Float> {
        byId(id)?.let { return it.bands }
        config.customPresets.firstOrNull { it.id == id }?.let { return it.bands }
        return config.bands.takeIf { it.size == DspMath.BAND_COUNT } ?: byId(SPACE)!!.bands
    }

    fun nameFor(config: BassEngineConfig, id: String): String =
        byId(id)?.name ?: config.customPresets.firstOrNull { it.id == id }?.name ?: "自定义"

    fun colorFor(config: BassEngineConfig, id: String): Long =
        byId(id)?.color ?: config.customPresets.firstOrNull { it.id == id }?.color ?: 0xFF7C4DFF
}
