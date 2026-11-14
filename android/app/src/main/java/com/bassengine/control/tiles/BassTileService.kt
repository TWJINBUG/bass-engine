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

class BassTileService : TileService() {
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
        val on = !repo.current().bass.enabled
        repo.update { it.copy(bass = it.bass.copy(enabled = on)) }
        runCatching { LogRepository.get().add(if (on) "磁贴 · 低频开" else "磁贴 · 低频关") }
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val cfg = config(this).current()
        val on = cfg.engineOn && cfg.bass.enabled
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "BASS 低频"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                !cfg.engineOn -> "引擎已关闭"
                cfg.bass.enabled -> "低频增强 ON"
                else -> "低频增强 OFF"
            }
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_bass)
        tile.updateTile()
    }
}
