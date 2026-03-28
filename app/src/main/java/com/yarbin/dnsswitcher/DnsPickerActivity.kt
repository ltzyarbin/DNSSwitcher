package com.yarbin.dnsswitcher

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DnsPickerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)

        val servers = loadServers()
        val activeServer = getActiveServer()
        val currentMode = try {
            Settings.Global.getString(contentResolver, "private_dns_mode")
        } catch (_: SecurityException) {
            null
        }
        val checkedIndex = if (currentMode != null && currentMode != "off") servers.indexOf(activeServer) else -1

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.picker_title)
            .setSingleChoiceItems(servers.toTypedArray(), checkedIndex) { dialog, which ->
                val server = servers[which]
                getSharedPreferences("dns_prefs", Context.MODE_PRIVATE)
                    .edit().putString("active_server", server).apply()
                try {
                    Settings.Global.putString(contentResolver, "private_dns_mode", "hostname")
                    Settings.Global.putString(contentResolver, "private_dns_specifier", server)
                    Toast.makeText(this, getString(R.string.picker_dns_set, server), Toast.LENGTH_SHORT).show()
                } catch (_: SecurityException) {
                    Toast.makeText(this, R.string.permission_error, Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
                finish()
            }
            .setNeutralButton(R.string.picker_disable) { _, _ ->
                try {
                    Settings.Global.putString(contentResolver, "private_dns_mode", "off")
                    Toast.makeText(this, R.string.picker_dns_disabled, Toast.LENGTH_SHORT).show()
                } catch (_: SecurityException) {
                    Toast.makeText(this, R.string.permission_error, Toast.LENGTH_LONG).show()
                }
                finish()
            }
            .setNegativeButton(R.string.cancel_button) { _, _ ->
                finish()
            }
            .setOnCancelListener {
                finish()
            }
            .show()
    }

    private fun loadServers(): List<String> {
        val prefs = getSharedPreferences("dns_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("servers", "") ?: ""
        return if (saved.isEmpty()) listOf("dns.cloudflare.com", "dns.google")
        else saved.split(",")
    }

    private fun getActiveServer(): String? {
        return getSharedPreferences("dns_prefs", Context.MODE_PRIVATE)
            .getString("active_server", null)
    }
}
