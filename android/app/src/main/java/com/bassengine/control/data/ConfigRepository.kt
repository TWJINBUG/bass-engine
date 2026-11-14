package com.bassengine.control.data

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import com.bassengine.control.tiles.BassTileService
import com.bassengine.control.tiles.EngineTileService
import com.bassengine.control.tiles.PresetTileService
import com.bassengine.core.BassEngineConfig
import com.bassengine.control.global.GlobalEngine
import com.bassengine.core.ConfigCodec
import com.bassengine.core.defaultConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConfigRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val lock = Any()

    private val _config = MutableStateFlow(load())
    val config: StateFlow<BassEngineConfig> = _config.asStateFlow()

    init {
        GlobalEngine.publish(appContext, _config.value)
    }

    fun current(): BassEngineConfig = _config.value

    fun update(transform: (BassEngineConfig) -> BassEngineConfig) {
        synchronized(lock) {
            val next = transform(_config.value).normalized().copy(updatedAt = System.currentTimeMillis())
            _config.value = next
            prefs.edit().putString(KEY_CONFIG, ConfigCodec.encode(next)).apply()
            GlobalEngine.publish(appContext, next)
        }
        requestTileRefresh()
    }

    fun replace(next: BassEngineConfig) {
        synchronized(lock) {
            val normalized = next.normalized().copy(updatedAt = System.currentTimeMillis())
            _config.value = normalized
            prefs.edit().putString(KEY_CONFIG, ConfigCodec.encode(normalized)).apply()
            GlobalEngine.publish(appContext, normalized)
        }
        requestTileRefresh()
    }

    fun reset() {
        replace(defaultConfig())
    }

    fun json(): String = ConfigCodec.encode(_config.value)

    private fun load(): BassEngineConfig {
        val raw = prefs.getString(KEY_CONFIG, null) ?: return defaultConfig()
        return ConfigCodec.decode(raw)
    }

    private fun requestTileRefresh() {
        for (cls in listOf(
            EngineTileService::class.java,
            PresetTileService::class.java,
            BassTileService::class.java
        )) {
            try {
                TileService.requestListeningState(appContext, ComponentName(appContext, cls))
            } catch (_: Throwable) {
            }
        }
    }

    companion object {
        private const val PREFS = "bass_engine"
        private const val KEY_CONFIG = "config_json"

        @Volatile
        private var instance: ConfigRepository? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) instance = ConfigRepository(context)
                }
            }
        }

        fun get(): ConfigRepository = instance
            ?: throw IllegalStateException("ConfigRepository is not initialised")
    }
}
