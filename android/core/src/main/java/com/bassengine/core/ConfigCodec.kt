package com.bassengine.core

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

object ConfigCodec {
    const val SHARE_PREFIX = "BASS7:"

    fun encode(config: BassEngineConfig): String {
        val c = config.normalized()
        val root = JSONObject()
        root.put("schema", c.schema)
        root.put("engineOn", c.engineOn)
        root.put("presetId", c.presetId)
        root.put("bands", floats(c.bands))
        root.put("limiter", c.limiter)
        root.put("theme", c.theme)
        root.put("lastTab", c.lastTab)
        root.put("updatedAt", c.updatedAt)

        root.put("bass", JSONObject().apply {
            put("enabled", c.bass.enabled)
            put("boost", c.bass.boost)
            put("virtualBass", c.bass.virtualBass)
            put("adaptive", c.bass.adaptive)
            put("subImpact", c.bass.subImpact)
            put("subwoofer", c.bass.subwoofer)
            put("preampDb", c.bass.preampDb.toDouble())
            put("q", c.bass.q.toDouble())
        })

        root.put("surround", JSONObject().apply {
            put("mode", c.surround.mode)
            put("air", c.surround.air)
            put("focus", c.surround.focus)
            put("widthDetail", c.surround.widthDetail)
            put("vocal", c.surround.vocal)
            put("loudness", c.surround.loudness)
            put("intensity", c.surround.intensity)
        })

        root.put("schedule", JSONObject().apply {
            put("enabled", c.schedule.enabled)
            put("start", c.schedule.start)
            put("end", c.schedule.end)
            put("action", c.schedule.action)
        })

        val custom = JSONArray()
        c.customPresets.forEach { p ->
            custom.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("color", p.color)
                put("bands", floats(p.bands))
                put("createdAt", p.createdAt)
            })
        }
        root.put("customPresets", custom)

        val profiles = JSONObject()
        c.device.profiles.forEach { (key, p) ->
            profiles.put(key, JSONObject().apply {
                put("presetId", p.presetId)
                put("bassEnabled", p.bassEnabled)
                put("surroundMode", p.surroundMode)
                put("intensity", p.intensity)
            })
        }
        root.put("device", JSONObject().apply {
            put("mode", c.device.mode)
            put("lockedModel", c.device.lockedModel ?: JSONObject.NULL)
            put("profiles", profiles)
        })

        val rules = JSONArray()
        c.appRules.forEach { r ->
            rules.put(JSONObject().apply {
                put("package", r.packageName)
                put("presetId", r.presetId)
            })
        }
        root.put("appRules", rules)
        return root.toString()
    }

    fun decode(raw: String?): BassEngineConfig {
        if (raw.isNullOrBlank()) return defaultConfig()
        return try {
            val root = JSONObject(raw)
            val bands = readBands(root.optJSONArray("bands"))
            val bassObj = root.optJSONObject("bass")
            val surroundObj = root.optJSONObject("surround")
            val scheduleObj = root.optJSONObject("schedule")
            val deviceObj = root.optJSONObject("device")

            val profiles = mutableMapOf<String, DeviceProfile>()
            val defaults = DeviceConfig().profiles
            val profileObj = deviceObj?.optJSONObject("profiles")
            for (key in listOf(DEVICE_SPEAKER, DEVICE_HEADPHONE, DEVICE_BLUETOOTH)) {
                val fallback = defaults[key] ?: DeviceProfile()
                val p = profileObj?.optJSONObject(key)
                profiles[key] = if (p == null) fallback else DeviceProfile(
                    presetId = p.optString("presetId", fallback.presetId),
                    bassEnabled = p.optBoolean("bassEnabled", fallback.bassEnabled),
                    surroundMode = p.optString("surroundMode", fallback.surroundMode),
                    intensity = p.optInt("intensity", fallback.intensity).coerceIn(40, 70)
                )
            }

            val custom = mutableListOf<CustomPreset>()
            val customArr = root.optJSONArray("customPresets")
            if (customArr != null) {
                for (i in 0 until customArr.length()) {
                    val p = customArr.optJSONObject(i) ?: continue
                    val cb = readBands(p.optJSONArray("bands"))
                    if (cb.size == DspMath.BAND_COUNT) {
                        custom += CustomPreset(
                            id = p.optString("id", "custom_$i"),
                            name = p.optString("name", "自定义 ${i + 1}"),
                            color = p.optLong("color", 0xFF7C4DFF),
                            bands = cb,
                            createdAt = p.optLong("createdAt", 0L)
                        )
                    }
                }
            }

            val rules = mutableListOf<AppRule>()
            val ruleArr = root.optJSONArray("appRules")
            if (ruleArr != null) {
                for (i in 0 until ruleArr.length()) {
                    val r = ruleArr.optJSONObject(i) ?: continue
                    val pkg = r.optString("package", "")
                    if (pkg.isNotBlank()) {
                        rules += AppRule(pkg, r.optString("presetId", Presets.SPACE))
                    }
                }
            }

            BassEngineConfig(
                schema = root.optInt("schema", 1),
                engineOn = root.optBoolean("engineOn", true),
                presetId = root.optString("presetId", Presets.SPACE),
                bands = bands,
                bass = BassConfig(
                    enabled = bassObj?.optBoolean("enabled", true) ?: true,
                    boost = bassObj?.optBoolean("boost", true) ?: true,
                    virtualBass = bassObj?.optBoolean("virtualBass", false) ?: false,
                    adaptive = bassObj?.optBoolean("adaptive", true) ?: true,
                    subImpact = bassObj?.optBoolean("subImpact", false) ?: false,
                    subwoofer = bassObj?.optBoolean("subwoofer", false) ?: false,
                    preampDb = (bassObj?.optDouble("preampDb", 2.0) ?: 2.0).toFloat().coerceIn(-10f, 10f),
                    q = (bassObj?.optDouble("q", 2.8) ?: 2.8).toFloat().coerceIn(0.5f, 3.0f)
                ),
                surround = SurroundConfig(
                    mode = surroundObj?.optString("mode", SURROUND_WIDE) ?: SURROUND_WIDE,
                    air = surroundObj?.optBoolean("air", true) ?: true,
                    focus = surroundObj?.optBoolean("focus", true) ?: true,
                    widthDetail = surroundObj?.optBoolean("widthDetail", true) ?: true,
                    vocal = surroundObj?.optBoolean("vocal", true) ?: true,
                    loudness = surroundObj?.optBoolean("loudness", true) ?: true,
                    intensity = (surroundObj?.optInt("intensity", 50) ?: 50).coerceIn(40, 70)
                ),
                limiter = root.optBoolean("limiter", true),
                theme = root.optString("theme", THEME_DARK),
                customPresets = custom,
                device = DeviceConfig(
                    mode = deviceObj?.optString("mode", DEVICE_AUTO) ?: DEVICE_AUTO,
                    lockedModel = deviceObj?.optString("lockedModel", "")?.takeIf { it.isNotBlank() },
                    profiles = profiles
                ),
                appRules = rules,
                schedule = ScheduleConfig(
                    enabled = scheduleObj?.optBoolean("enabled", false) ?: false,
                    start = scheduleObj?.optString("start", "23:00") ?: "23:00",
                    end = scheduleObj?.optString("end", "07:00") ?: "07:00",
                    action = scheduleObj?.optString("action", SCHEDULE_SLEEP) ?: SCHEDULE_SLEEP
                ),
                lastTab = (root.optInt("lastTab", 0)).coerceIn(0, 4),
                updatedAt = root.optLong("updatedAt", 0L)
            ).normalized()
        } catch (_: Throwable) {
            defaultConfig()
        }
    }

    fun shareCode(config: BassEngineConfig): String {
        val json = encode(config)
        return SHARE_PREFIX + Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    fun parse(input: String): BassEngineConfig? {
        val text = input.trim()
        if (text.isEmpty()) return null
        val json = if (text.startsWith(SHARE_PREFIX)) {
            try {
                String(Base64.decode(text.removePrefix(SHARE_PREFIX), Base64.DEFAULT), Charsets.UTF_8)
            } catch (_: Throwable) {
                return null
            }
        } else {
            text
        }
        if (!json.trimStart().startsWith("{")) return null
        return try {
            decode(json)
        } catch (_: Throwable) {
            null
        }
    }

    private fun floats(values: List<Float>): JSONArray {
        val arr = JSONArray()
        values.forEach { arr.put(it.toDouble()) }
        return arr
    }

    private fun readBands(arr: JSONArray?): List<Float> {
        if (arr == null) return Presets.byId(Presets.SPACE)!!.bands
        val out = ArrayList<Float>(DspMath.BAND_COUNT)
        for (i in 0 until minOf(arr.length(), DspMath.BAND_COUNT)) {
            out += arr.optDouble(i, 0.0).toFloat().coerceIn(-15f, 15f)
        }
        while (out.size < DspMath.BAND_COUNT) out += 0f
        return out
    }
}
