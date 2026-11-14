package com.bassengine.control.ui

import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bassengine.control.data.ConfigRepository
import com.bassengine.control.data.LogEntry
import com.bassengine.control.data.LogRepository
import com.bassengine.control.audio.OutputRoute
import com.bassengine.control.audio.OutputRoutes
import com.bassengine.control.global.GlobalEngine
import com.bassengine.control.global.GlobalStatus
import com.bassengine.core.BassConfig
import com.bassengine.core.BassEngineConfig
import com.bassengine.core.ConfigCodec
import com.bassengine.core.CustomPreset
import com.bassengine.core.DEVICE_BLUETOOTH
import com.bassengine.core.DEVICE_HEADPHONE
import com.bassengine.core.DEVICE_SPEAKER
import com.bassengine.core.Presets
import com.bassengine.core.SurroundConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EngineViewModel(app: Application) : AndroidViewModel(app) {
    private val configRepo = ConfigRepository.get()
    private val logRepo = LogRepository.get()

    val config: StateFlow<BassEngineConfig> = configRepo.config
    val logs: StateFlow<List<LogEntry>> = logRepo.logs
    private val _global = MutableStateFlow(GlobalStatus())
    val global: StateFlow<GlobalStatus> = _global.asStateFlow()

    private val _route = MutableStateFlow(OutputRoutes.detect(app))
    val route: StateFlow<OutputRoute> = _route.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _global.value = GlobalEngine.readStatus(getApplication())
                _route.value = OutputRoutes.detect(getApplication())
                delay(1500)
            }
        }
    }

    fun update(transform: (BassEngineConfig) -> BassEngineConfig) {
        configRepo.update(transform)
    }

    fun setEngineOn(on: Boolean) {
        configRepo.update { it.copy(engineOn = on) }
        log(if (on) "引擎总开关：开启" else "引擎总开关：关闭")
    }

    fun selectPreset(id: String) {
        val cfg = configRepo.current()
        val bands = Presets.bandsFor(cfg, id)
        configRepo.update { withProfilePreset(it.copy(presetId = id, bands = bands), id) }
        log("预设切换 · ${Presets.nameFor(cfg, id)}")
    }

    fun syncActiveProfile() {
        val cfg = configRepo.current()
        val profile = cfg.device.profiles[activeDeviceKey()] ?: return
        if (profile.presetId != cfg.presetId) {
            configRepo.update {
                it.copy(
                    presetId = profile.presetId,
                    bands = Presets.bandsFor(it, profile.presetId)
                )
            }
        }
    }

    /** Keeps the active device profile in sync with a manual preset choice. */
    private fun withProfilePreset(cfg: BassEngineConfig, presetId: String): BassEngineConfig {
        val key = activeDeviceKey()
        val profiles = cfg.device.profiles.toMutableMap()
        val old = profiles[key] ?: com.bassengine.core.DeviceProfile()
        profiles[key] = old.copy(presetId = presetId)
        return cfg.copy(device = cfg.device.copy(profiles = profiles))
    }

    private fun activeDeviceKey(): String {
        return try {
            val am = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            var wired = false
            for (d in devices) {
                when (d.type) {
                    android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    android.media.AudioDeviceInfo.TYPE_BLE_HEADSET,
                    android.media.AudioDeviceInfo.TYPE_BLE_SPEAKER -> return DEVICE_BLUETOOTH
                    android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    android.media.AudioDeviceInfo.TYPE_USB_HEADSET -> wired = true
                }
            }
            if (wired) DEVICE_HEADPHONE else DEVICE_SPEAKER
        } catch (_: Throwable) {
            DEVICE_SPEAKER
        }
    }

    fun setBand(index: Int, value: Float) {
        configRepo.update { cfg ->
            val bands = cfg.bands.toMutableList()
            if (index in bands.indices) bands[index] = value.coerceIn(-15f, 15f)
            withProfilePreset(cfg.copy(bands = bands, presetId = Presets.CUSTOM), Presets.CUSTOM)
        }
    }

    fun resetBands() {
        val cfg = configRepo.current()
        val bands = Presets.byId(Presets.HIRES)!!.bands
        configRepo.update { withProfilePreset(it.copy(presetId = Presets.HIRES, bands = bands), Presets.HIRES) }
        log("EQ 曲线已重置为平直")
    }

    fun saveCustomPreset(name: String, color: Long) {
        val cfg = configRepo.current()
        val cleanName = name.trim().ifBlank { "自定义 ${cfg.customPresets.size + 1}" }
        val preset = CustomPreset(
            id = "custom_${System.currentTimeMillis()}",
            name = cleanName,
            color = color,
            bands = cfg.bands.toList()
        )
        configRepo.update {
            withProfilePreset(
                it.copy(
                    customPresets = it.customPresets + preset,
                    presetId = preset.id,
                    bands = preset.bands
                ),
                preset.id
            )
        }
        log("保存自定义预设 · $cleanName")
    }

    fun renameCustomPreset(id: String, name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        configRepo.update { cfg ->
            cfg.copy(customPresets = cfg.customPresets.map {
                if (it.id == id) it.copy(name = clean) else it
            })
        }
        log("重命名预设 · $clean")
    }

    fun recolorCustomPreset(id: String, color: Long) {
        configRepo.update { cfg ->
            cfg.copy(customPresets = cfg.customPresets.map {
                if (it.id == id) it.copy(color = color) else it
            })
        }
    }

    fun deleteCustomPreset(id: String) {
        val cfg = configRepo.current()
        val name = cfg.customPresets.firstOrNull { it.id == id }?.name ?: id
        configRepo.update {
            val fallback = it.presetId == id
            val nextPreset = if (fallback) Presets.SPACE else it.presetId
            withProfilePreset(
                it.copy(
                    customPresets = it.customPresets.filterNot { p -> p.id == id },
                    presetId = nextPreset,
                    bands = if (fallback) Presets.byId(Presets.SPACE)!!.bands else it.bands
                ),
                nextPreset
            )
        }
        log("删除预设 · $name")
    }

    fun updateBass(transform: (BassConfig) -> BassConfig) {
        configRepo.update { it.copy(bass = transform(it.bass)) }
    }

    fun updateSurround(transform: (SurroundConfig) -> SurroundConfig) {
        configRepo.update {
            val next = transform(it.surround)
            it.copy(surround = next.copy(intensity = next.intensity.coerceIn(40, 70)))
        }
    }

    fun setPreamp(value: Float, finished: Boolean) {
        updateBass { it.copy(preampDb = value.coerceIn(-10f, 10f)) }
        if (finished) log("预增益 · ${formatDb(value)}")
    }

    fun setBassQ(value: Float, finished: Boolean) {
        updateBass { it.copy(q = value.coerceIn(0.5f, 3.0f)) }
        if (finished) log("低音 Q 值 · ${String.format("%.2f", value)}")
    }

    fun setSurroundIntensity(value: Float) {
        configRepo.update { it.copy(surround = it.surround.copy(intensity = value.toInt().coerceIn(40, 70))) }
    }

    fun setTheme(theme: String) {
        configRepo.update { it.copy(theme = theme) }
        log("主题切换 · $theme")
    }

    fun setLimiter(on: Boolean) {
        configRepo.update { it.copy(limiter = on) }
        log("安全限幅 · ${if (on) "开启" else "关闭"}")
    }

    fun importConfig(text: String): Boolean {
        val parsed = ConfigCodec.parse(text) ?: return false
        configRepo.replace(parsed)
        log("导入配置 · 成功")
        return true
    }

    fun resetAll() {
        configRepo.reset()
        log("恢复默认配置")
    }

    fun log(message: String) = logRepo.add(message)

    fun clearLogs() = logRepo.clear()

    fun exportLogs(): String = logRepo.exportText()

    fun exportJson(): String = configRepo.json()

    fun shareCode(): String = ConfigCodec.shareCode(configRepo.current())

    private fun formatDb(value: Float): String = (if (value >= 0) "+" else "") + String.format("%.1f dB", value)
}
