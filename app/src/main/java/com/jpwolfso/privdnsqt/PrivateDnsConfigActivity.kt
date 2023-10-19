package com.jpwolfso.privdnsqt

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jpwolfso.privdnsqt.PrivateDnsConstants.PRIVATE_DNS_SPECIFIER
import com.jpwolfso.privdnsqt.SharedPreferencesHelper.Companion.SHARED_PREF_FIRST_RUN
import com.jpwolfso.privdnsqt.SharedPreferencesHelper.Companion.SHARED_PREF_REQUIRE_UNLOCK
import com.jpwolfso.privdnsqt.SharedPreferencesHelper.Companion.SHARED_PREF_TOGGLE_AUTO
import com.jpwolfso.privdnsqt.SharedPreferencesHelper.Companion.SHARED_PREF_TOGGLE_OFF
import com.jpwolfso.privdnsqt.SharedPreferencesHelper.Companion.SHARED_PREF_TOGGLE_ON

class PrivateDnsConfigActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_private_dns_config)

        val togglestates = SharedPreferencesHelper(this)

        val checkoff = findViewById<CheckBox>(R.id.check_off)
        val checkauto = findViewById<CheckBox>(R.id.check_auto)
        val checkon = findViewById<CheckBox>(R.id.check_on)

        val requireUnlock = findViewById<CheckBox>(R.id.require_unlock)

        val texthostname = findViewById<EditText>(R.id.text_hostname)

        val okbutton = findViewById<Button>(R.id.button_ok)

        if (!hasPermission() || togglestates.getBoolean(SHARED_PREF_FIRST_RUN, true)) {
            showHelpMenu()
            togglestates.update(SHARED_PREF_FIRST_RUN, false)
        }

        if (togglestates.getBoolean(SHARED_PREF_TOGGLE_OFF, true)) {
            checkoff.isChecked = true
        }

        if (togglestates.getBoolean(SHARED_PREF_TOGGLE_AUTO, true)) {
            checkauto.isChecked = true
        }

        if (togglestates.getBoolean(SHARED_PREF_TOGGLE_ON, true)) {
            checkon.isChecked = true
            texthostname.isEnabled = true
        } else {
            texthostname.isEnabled = false
        }

        if (togglestates.getBoolean(SHARED_PREF_REQUIRE_UNLOCK, false)) {
            requireUnlock.isChecked = true
        }

        val dnsprovider = Settings.Global.getString(contentResolver, PRIVATE_DNS_SPECIFIER)
        if (dnsprovider != null) {
            texthostname.setText(dnsprovider)
        }

        checkoff.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            togglestates.update(SHARED_PREF_TOGGLE_OFF, isChecked)
        }

        checkauto.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            togglestates.update(SHARED_PREF_TOGGLE_AUTO, isChecked)
        }

        checkon.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            togglestates.update(SHARED_PREF_TOGGLE_ON, isChecked)
            texthostname.isEnabled = isChecked
        }

        requireUnlock.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            togglestates.update(SHARED_PREF_REQUIRE_UNLOCK, isChecked)
        }

        okbutton.setOnClickListener {
            if (hasPermission()) {
                if (checkon.isChecked) {
                    val hostname = texthostname.text.toString().trim()
                    if (hostname.isEmpty()) {
                        showToast(R.string.toast_no_dns)
                        return@setOnClickListener
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
    }

    private fun hasPermission(): Boolean {
        return checkCallingOrSelfPermission(WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_DENIED
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_overflow, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_appinfo -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
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

    private fun showHelpMenu() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.message_help)
            .setPositiveButton(android.R.string.ok, null)
            .setView(R.layout.dialog_help)
            .show()
    }

    private fun showToast(@StringRes resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }
}