package com.jpwolfso.privdnsqt

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.ExecutorCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jpwolfso.privdnsqt.PrivateDnsConstants.PRIVATE_DNS_SPECIFIER
import com.jpwolfso.privdnsqt.SharedPreferencesHelper.Companion.SHARED_PREF_FIRST_RUN
import com.jpwolfso.privdnsqt.SharedPreferencesHelper.Companion.SHARED_PREF_REQUIRE_UNLOCK
import com.jpwolfso.privdnsqt.SharedPreferencesHelper.Companion.SHARED_PREF_TOGGLE_AUTO
import com.jpwolfso.privdnsqt.SharedPreferencesHelper.Companion.SHARED_PREF_TOGGLE_OFF
import com.jpwolfso.privdnsqt.SharedPreferencesHelper.Companion.SHARED_PREF_TOGGLE_ON

class PrivateDnsConfigActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferencesHelper
    private lateinit var checkoff: CheckBox
    private lateinit var checkauto: CheckBox
    private lateinit var checkon: CheckBox
    private lateinit var requireUnlock: CheckBox
    private lateinit var texthostname: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_private_dns_config)

        preferences = SharedPreferencesHelper(this)

        checkoff = findViewById(R.id.check_off)
        checkauto = findViewById(R.id.check_auto)
        checkon = findViewById(R.id.check_on)

        requireUnlock = findViewById(R.id.require_unlock)

        texthostname = findViewById(R.id.text_hostname)
        texthostname.maxLines = Integer.MAX_VALUE
        texthostname.setHorizontallyScrolling(false)

        val okbutton = findViewById<Button>(R.id.button_ok)

        if (!hasPermission() || preferences.getBoolean(SHARED_PREF_FIRST_RUN, true)) {
            showHelpMenu()
            preferences.update(SHARED_PREF_FIRST_RUN, false)
        }

        checkoff.isChecked = preferences.getBoolean(SHARED_PREF_TOGGLE_OFF, true)
        checkoff.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            preferences.update(SHARED_PREF_TOGGLE_OFF, isChecked)
        }

        checkauto.isChecked = preferences.getBoolean(SHARED_PREF_TOGGLE_AUTO, true)
        checkauto.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            preferences.update(SHARED_PREF_TOGGLE_AUTO, isChecked)
        }

        checkon.isChecked = preferences.getBoolean(SHARED_PREF_TOGGLE_ON, true)
        checkon.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            preferences.update(SHARED_PREF_TOGGLE_ON, isChecked)
            texthostname.isEnabled = isChecked
        }

        texthostname.setText(Settings.Global.getString(contentResolver, PRIVATE_DNS_SPECIFIER))
        texthostname.isEnabled = checkon.isChecked

        requireUnlock.isChecked = preferences.getBoolean(SHARED_PREF_REQUIRE_UNLOCK, false)
        requireUnlock.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            preferences.update(SHARED_PREF_REQUIRE_UNLOCK, isChecked)
        }

        okbutton.setOnClickListener {
            handleOkButton()
        }
    }

    private fun hasPermission(): Boolean {
        return checkCallingOrSelfPermission(WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_DENIED
    }

    private fun handleOkButton() {
        if (hasPermission()) {
            if (checkon.isChecked) {
                val hostname = texthostname.text.toString().trim()
                if (hostname.isEmpty()) {
                    texthostname.requestFocus()
                    showToast(R.string.toast_no_dns)
                    return
                } else {
                    Settings.Global.putString(contentResolver, PRIVATE_DNS_SPECIFIER, hostname)
                }
            }
            showToast(R.string.toast_changes_saved)
            finish()
        } else {
            showToast(R.string.toast_no_permission)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_overflow, menu)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            menu?.findItem(R.id.action_add_tile)?.isVisible = true
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_app_info -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
                return true
            }

            R.id.action_add_tile -> {
                requestAddTile()
                return true
            }

            R.id.action_help -> {
                showHelpMenu()
                return true
            }

            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun requestAddTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val statusBarManager = getSystemService(StatusBarManager::class.java)!!
        statusBarManager.requestAddTileService(
            ComponentName(this, PrivateDnsTileService::class.java),
            getString(R.string.qt_default),
            Icon.createWithResource(this, R.drawable.ic_dnsauto),
            ExecutorCompat.create(Handler(Looper.getMainLooper())),
        ) { resultCode ->
            val message = when (resultCode) {
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> {
                    getString(R.string.tile_added)
                }
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> {
                    getString(R.string.tile_already_added)
                }
                else -> {
                    getString(R.string.tile_not_added, resultCode)
                }
            }
            runOnUiThread {
                showToast(message)
            }
        }
    }

    private fun showHelpMenu() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.message_help)
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.more_details) { _, _ -> openUrl(HELP_URL) }
            .setView(R.layout.dialog_help)
            .show()
    }

    private fun openUrl(url: Uri) {
        startActivity(Intent(ACTION_VIEW, url))
    }

    private fun showToast(@StringRes resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        val HELP_URL: Uri by lazy { Uri.parse("https://private-dns-qs.web.app/help") }
    }
}