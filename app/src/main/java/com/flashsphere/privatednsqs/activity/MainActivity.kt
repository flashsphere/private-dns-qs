package com.flashsphere.privatednsqs.activity

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.os.ExecutorCompat
import com.flashsphere.privatednsqs.PrivateDnsConstants.HELP_URL
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.service.PrivateDnsTileService
import com.flashsphere.privatednsqs.shizuku.ShizukuUtils.grantWriteSecureSettingsPermission
import com.flashsphere.privatednsqs.shizuku.ShizukuUtils.isShizukuAvailable
import com.flashsphere.privatednsqs.ui.BackupFailed
import com.flashsphere.privatednsqs.ui.MainScreen
import com.flashsphere.privatednsqs.ui.RestoreFailed
import com.flashsphere.privatednsqs.ui.SnackbarMessage
import com.flashsphere.privatednsqs.ui.TileAddedMessage
import com.flashsphere.privatednsqs.ui.TileAlreadyAddedMessage
import com.flashsphere.privatednsqs.ui.TileNotAddedMessage
import com.flashsphere.privatednsqs.ui.WriteSecureSettingPermissionGrantedUsingShizuku
import com.flashsphere.privatednsqs.viewmodel.MainViewModel
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : BaseActivity(), OnRequestPermissionResultListener {
    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    private var toast: Toast? = null

    private val selectBackupDestLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument("text/plain")
        ) { uri ->
            uri?.let { viewModel.backup(contentResolver, it) }
        }

    private val openBackupLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let { viewModel.restore(contentResolver, it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(this)

        setContent {
            MainScreen(
                viewModel = viewModel,
                showAppInfo = this::showAppInfo,
                showMoreInfo = this::showMoreInfo,
                requestAddTile = this::requestAddTile,
                backupConfig = this::backupConfig,
                restoreConfig = this::restoreConfig,
            )
        }

        showMessageFromIntent(savedInstanceState, intent)
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(this)
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()

        if (!viewModel.hasPermission() && isShizukuAvailable() && checkShizukuPermission(R.id.shizuku_request_code)) {
            grantWriteSecureSettingsPermission()
        }
    }

    private fun showMessageFromIntent(savedInstanceState: Bundle?, intent: Intent?) {
        if (savedInstanceState != null) return
        if (intent == null) return
        val bundle = intent.getBundleExtra(PARAM_BUNDLE) ?: return
        bundle.classLoader = SnackbarMessage::class.java.classLoader
        val message = BundleCompat.getParcelable(bundle, PARAM_MESSAGE, SnackbarMessage::class.java) ?: return
        viewModel.showSnackbarMessage(message)
    }

    private fun showAppInfo() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = "package:$packageName".toUri()
        runCatching { startActivity(intent) }
            .onFailure {
                cancelToast()
                val message = getString(R.string.toast_cannot_open_app_info, it.toString())
                Toast.makeText(this, message, Toast.LENGTH_LONG)?.apply {
                    toast = this
                    show()
                }
            }
    }

    private fun showMoreInfo() {
        runCatching { startActivity(Intent(ACTION_VIEW, HELP_URL)) }
            .onFailure {
                cancelToast()
                val message = getString(R.string.toast_cannot_open_more_info, it.toString())
                Toast.makeText(this, message, Toast.LENGTH_LONG)?.apply {
                    toast = this
                    show()
                }
            }
    }

    private fun cancelToast() {
        toast?.cancel()
        toast = null
    }

    private fun requestAddTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val statusBarManager = getSystemService(StatusBarManager::class.java) as StatusBarManager
        statusBarManager.requestAddTileService(
            ComponentName(this, PrivateDnsTileService::class.java),
            getString(R.string.tile_name),
            Icon.createWithResource(this, R.drawable.ic_dns_auto),
            ExecutorCompat.create(Handler(Looper.getMainLooper())),
        ) { resultCode ->
            val message = when (resultCode) {
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> {
                    TileAddedMessage
                }
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> {
                    TileAlreadyAddedMessage
                }
                else -> {
                    TileNotAddedMessage(resultCode)
                }
            }
            runOnUiThread {
                viewModel.showSnackbarMessage(message)
            }
        }
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode != R.id.shizuku_request_code ||
            grantResult != PackageManager.PERMISSION_GRANTED) return

        grantWriteSecureSettingsPermission()
    }

    private fun grantWriteSecureSettingsPermission() {
        if (grantWriteSecureSettingsPermission(this)) {
            viewModel.openHelpDialog(false)
            viewModel.showSnackbarMessage(WriteSecureSettingPermissionGrantedUsingShizuku)
        }
    }

    private fun backupConfig() {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        val timestamp = LocalDateTime.now().format(formatter)
        runCatching {
            selectBackupDestLauncher.launch("private-dns-qs-${timestamp}.txt")
        }.onFailure {
            Timber.e(it)
            viewModel.showSnackbarMessage(BackupFailed)
        }
    }

    private fun restoreConfig() {
        runCatching {
            openBackupLauncher.launch(arrayOf("text/plain"))
        }.onFailure {
            Timber.e(it)
            viewModel.showSnackbarMessage(RestoreFailed)
        }
    }

    private fun checkShizukuPermission(code: Int): Boolean {
        runCatching {
            if (Shizuku.isPreV11()) {
                return false
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                return true
            }
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                return false
            }
            Shizuku.requestPermission(code)
        }.onFailure {
            Timber.w(it)
        }
        return false
    }

    companion object {
        fun getIntent(context: Context, message: SnackbarMessage? = null): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_CLEAR_TASK or FLAG_ACTIVITY_NEW_TASK)
                .also { intent ->
                    if (message != null) {
                        val bundle = Bundle().also {
                            it.putParcelable(PARAM_MESSAGE, message)
                        }
                        intent.putExtra(PARAM_BUNDLE, bundle)
                    }
                }

        fun startActivity(context: Context, message: SnackbarMessage? = null) {
            context.startActivity(getIntent(context, message))
        }

        private const val PARAM_BUNDLE = "bundle"
        private const val PARAM_MESSAGE = "message"
    }
}