package com.bassengine.control.global

import android.content.Context
import com.bassengine.core.BassEngineConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class GlobalStatus(
    val running: Boolean = false,
    val attached: Boolean = false,
    val effectId: Int = -1,
    val error: String = "",
    val updated: Long = 0L,
    val pid: Int = -1
) {
    fun fresh(now: Long = System.currentTimeMillis()): Boolean = now - updated < 8_000L
    fun active(now: Long = System.currentTimeMillis()): Boolean = running && attached && fresh(now)
}

object GlobalEngine {
    private const val CONFIG_NAME = "global_config.json"
    private const val STATUS_NAME = "global_status.json"

    fun configFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, CONFIG_NAME)

    fun statusFile(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, STATUS_NAME)

    fun publish(context: Context, config: BassEngineConfig) {
        try {
            val root = JSONObject().apply {
                put("enabled", config.engineOn)
                put("preampDb", config.bass.preampDb.toDouble())
                put("presetId", config.presetId)
                put("updated", System.currentTimeMillis())
                put("bands", JSONArray().apply {
                    config.bands.forEach { put(it.toDouble()) }
                })
                put("bass", JSONObject().apply {
                    put("enabled", config.bass.enabled)
                    put("boost", config.bass.boost)
                    put("adaptive", config.bass.adaptive)
                    put("virtualBass", config.bass.virtualBass)
                    put("subImpact", config.bass.subImpact)
                    put("subwoofer", config.bass.subwoofer)
                    put("q", config.bass.q.toDouble())
                })
                put("surround", JSONObject().apply {
                    put("mode", config.surround.mode)
                    put("air", config.surround.air)
                    put("focus", config.surround.focus)
                    put("vocal", config.surround.vocal)
                    put("loudness", config.surround.loudness)
                    put("intensity", config.surround.intensity)
                })
            }
            val target = configFile(context)
            val tmp = File(target.parentFile, "$CONFIG_NAME.tmp")
            tmp.writeText(root.toString())
            if (!tmp.renameTo(target)) {
                target.writeText(root.toString())
                tmp.delete()
            }
        } catch (_: Throwable) {
        }
    }

    fun readStatus(context: Context): GlobalStatus {
        return try {
            val file = statusFile(context)
            if (!file.exists()) return GlobalStatus()
            val o = JSONObject(file.readText())
            GlobalStatus(
                running = o.optBoolean("running", false),
                attached = o.optBoolean("attached", false),
                effectId = o.optInt("effectId", -1),
                error = o.optString("error", ""),
                updated = o.optLong("updated", 0L),
                pid = o.optInt("pid", -1)
            )
        } catch (_: Throwable) {
            GlobalStatus()
        }
    }
}
