package com.bassengine.control.tiles

import android.content.Context
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.bassengine.control.R
import com.bassengine.control.data.ConfigRepository
import com.bassengine.control.data.LogRepository
import com.bassengine.core.DEVICE_BLUETOOTH
import com.bassengine.core.DEVICE_HEADPHONE
import com.bassengine.core.DEVICE_SPEAKER
import com.bassengine.core.Presets

private fun config(context: Context): ConfigRepository {
    ConfigRepository.init(context)
    return ConfigRepository.get()
}

class PresetTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        val repo = config(this)
        val cfg = repo.current()
        val ids = Presets.all.map { it.id } + cfg.customPresets.map { it.id }
        val index = ids.indexOf(cfg.presetId).let { if (it < 0) 0 else it }
        val nextId = ids[(index + 1) % ids.size]
        repo.update { cfg ->
            val key = activeDeviceKey(this)
            val profiles = cfg.device.profiles.toMutableMap()
            val old = profiles[key] ?: com.bassengine.core.DeviceProfile()
            profiles[key] = old.copy(presetId = nextId)
            cfg.copy(
                presetId = nextId,
                bands = Presets.bandsFor(cfg, nextId),
                device = cfg.device.copy(profiles = profiles)
            )
        }
        runCatching { LogRepository.get().add("磁贴 · 预设 ${Presets.nameFor(cfg, nextId)}") }
        refresh()
    }

    private fun activeDeviceKey(context: Context): String {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
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

    private fun refresh() {
        val tile = qsTile ?: return
        val cfg = config(this).current()
        val name = Presets.nameFor(cfg, cfg.presetId)
        tile.state = if (cfg.engineOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = name.take(12)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = "点按切换预设"
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_preset)
        tile.updateTile()
    }
}
