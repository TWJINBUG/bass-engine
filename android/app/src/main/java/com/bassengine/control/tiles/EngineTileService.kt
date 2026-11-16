package com.bassengine.control.tiles

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.bassengine.control.R
import com.bassengine.control.data.ConfigRepository
import com.bassengine.control.data.LogRepository

private fun config(context: Context): ConfigRepository {
    ConfigRepository.init(context)
    return ConfigRepository.get()
}

private fun log(message: String) {
    runCatching { LogRepository.get().add(message) }
}

class EngineTileService : TileService() {
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
        val on = !repo.current().engineOn
        repo.update { it.copy(engineOn = on) }
        log(if (on) "磁贴 · 引擎开启" else "磁贴 · 引擎关闭")
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val on = config(this).current().engineOn
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "BASS 总开关"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (on) "ON · 全局 DSP" else "OFF · 已旁路"
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_engine)
        tile.updateTile()
    }
}
