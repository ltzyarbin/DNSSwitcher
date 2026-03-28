package com.yarbin.dnsswitcher

import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class DnsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val currentMode = Settings.Global.getString(contentResolver, "private_dns_mode")
        if (currentMode == "off") {
            val server = loadActiveServer()
            enableDns(server)
        } else {
            disableDns()
        }
        updateTile()
    }

    private fun loadActiveServer(): String {
        val prefs = getSharedPreferences("dns_prefs", MODE_PRIVATE)
        return prefs.getString("active_server", "dns.cloudflare.com") ?: "dns.cloudflare.com"
    }

    private fun enableDns(server: String) {
        Settings.Global.putString(contentResolver, "private_dns_mode", "hostname")
        Settings.Global.putString(contentResolver, "private_dns_specifier", server)
    }

    private fun disableDns() {
        Settings.Global.putString(contentResolver, "private_dns_mode", "off")
    }

    private fun updateTile() {
        val currentMode = Settings.Global.getString(contentResolver, "private_dns_mode")
        val currentServer = Settings.Global.getString(contentResolver, "private_dns_specifier")
        qsTile?.let { tile ->
            tile.label = if (currentMode == "off") getString(R.string.tile_off) else currentServer ?: getString(R.string.app_name)
            tile.icon = android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_dns_tile)
            tile.state = if (currentMode == "off") Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            tile.updateTile()
        }
    }
}