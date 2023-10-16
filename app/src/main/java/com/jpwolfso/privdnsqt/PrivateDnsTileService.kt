package com.jpwolfso.privdnsqt

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.StringRes
import com.jpwolfso.privdnsqt.PrivateDnsConstants.PRIVATE_DNS_MODE
import com.jpwolfso.privdnsqt.PrivateDnsConstants.PRIVATE_DNS_SPECIFIER
import com.jpwolfso.privdnsqt.SharedPreferencesHelper.Companion.SHARED_PREF_REQUIRE_UNLOCK
import com.jpwolfso.privdnsqt.SharedPreferencesHelper.Companion.SHARED_PREF_TOGGLE_AUTO
import com.jpwolfso.privdnsqt.SharedPreferencesHelper.Companion.SHARED_PREF_TOGGLE_OFF
import com.jpwolfso.privdnsqt.SharedPreferencesHelper.Companion.SHARED_PREF_TOGGLE_ON

class PrivateDnsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()

        val dnsmode = Settings.Global.getString(contentResolver, PRIVATE_DNS_MODE)
        val tile = this.qsTile

        if (DNS_MODE_OFF.equals(dnsmode, ignoreCase = true)) {
            refreshTile(tile, Tile.STATE_INACTIVE, getString(R.string.qt_off), R.drawable.ic_dnsoff)
        } else if (DNS_MODE_AUTO.equals(dnsmode, ignoreCase = true) || dnsmode == null) {
            refreshTile(tile, Tile.STATE_ACTIVE, getString(R.string.qt_auto), R.drawable.ic_dnsauto)
        } else if (DNS_MODE_ON.equals(dnsmode, ignoreCase = true)) {
            val dnsprovider = Settings.Global.getString(contentResolver, PRIVATE_DNS_SPECIFIER)
            if (dnsprovider != null) {
                refreshTile(tile, Tile.STATE_ACTIVE, dnsprovider, R.drawable.ic_dnson)
            } else {
                showToast(R.string.toast_no_permission)
            }
        }
    }

    override fun onClick() {
        super.onClick()

        val togglestates = SharedPreferencesHelper(this)
        val isLocked = this.isSecure && this.isLocked
        val requireUnlock = togglestates.getBoolean(SHARED_PREF_REQUIRE_UNLOCK, false)

        if (!isLocked || !requireUnlock) {
            toggle()
        } else {
            unlockAndRun {
                toggle()
            }
        }
    }

    private fun toggle() {
        val togglestates = SharedPreferencesHelper(this)

        val toggleoff = togglestates.getBoolean(SHARED_PREF_TOGGLE_OFF, true)
        val toggleauto = togglestates.getBoolean(SHARED_PREF_TOGGLE_AUTO, true)
        val toggleon = togglestates.getBoolean(SHARED_PREF_TOGGLE_ON, true)

        val dnsprovider = Settings.Global.getString(contentResolver, PRIVATE_DNS_SPECIFIER)

        if (!hasPermission()) {
            showToast(R.string.toast_no_permission)
            return
        }

        val dnsmode = Settings.Global.getString(contentResolver, PRIVATE_DNS_MODE)
        val tile = this.qsTile
        if (DNS_MODE_OFF.equals(dnsmode, ignoreCase = true)) {
            if (toggleauto) {
                changeTileState(tile, Tile.STATE_ACTIVE, getString(R.string.qt_auto), R.drawable.ic_dnsauto, DNS_MODE_AUTO)
            } else if (toggleon) {
                changeTileState(tile, Tile.STATE_ACTIVE, dnsprovider, R.drawable.ic_dnson, DNS_MODE_ON)
            }
        } else if (DNS_MODE_AUTO.equals(dnsmode, ignoreCase = true) || dnsmode == null) {
            if (dnsprovider != null) {
                if (toggleon) {
                    changeTileState(tile, Tile.STATE_ACTIVE, dnsprovider, R.drawable.ic_dnson, DNS_MODE_ON)
                } else if (toggleoff) {
                    changeTileState(tile, Tile.STATE_INACTIVE, getString(R.string.qt_off), R.drawable.ic_dnsoff, DNS_MODE_OFF)
                }
            } else {
                if (toggleoff) {
                    changeTileState(tile, Tile.STATE_INACTIVE, getString(R.string.qt_off), R.drawable.ic_dnsoff, DNS_MODE_OFF)
                }
            }
        } else if (DNS_MODE_ON.equals(dnsmode, ignoreCase = true)) {
            if (toggleoff) {
                changeTileState(tile, Tile.STATE_INACTIVE, getString(R.string.qt_off), R.drawable.ic_dnsoff, DNS_MODE_OFF)
            } else if (toggleauto) {
                changeTileState(tile, Tile.STATE_ACTIVE, getString(R.string.qt_auto), R.drawable.ic_dnsauto, DNS_MODE_AUTO)
            }
        }
    }

    private fun hasPermission(): Boolean {
        return checkCallingOrSelfPermission(WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_DENIED
    }

    private fun changeTileState(tile: Tile, state: Int, label: String?, icon: Int, dnsmode: String?) {
        tile.label = label
        tile.state = state
        tile.icon = Icon.createWithResource(this, icon)
        Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, dnsmode)
        tile.updateTile()
    }

    private fun refreshTile(tile: Tile, state: Int, label: String?, icon: Int) {
        tile.state = state
        tile.label = label
        tile.icon = Icon.createWithResource(this, icon)
        tile.updateTile()
    }

    private fun showToast(@StringRes resId: Int) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, resId, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val DNS_MODE_OFF = "off"
        const val DNS_MODE_AUTO = "opportunistic"
        const val DNS_MODE_ON = "hostname"
    }
}