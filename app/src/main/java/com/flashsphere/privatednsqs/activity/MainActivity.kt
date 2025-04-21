package com.flashsphere.privatednsqs.activity

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.os.ExecutorCompat
import com.flashsphere.privatednsqs.PrivateDnsConstants.HELP_URL
import com.flashsphere.privatednsqs.R
import com.flashsphere.privatednsqs.service.PrivateDnsTileService
import com.flashsphere.privatednsqs.ui.MainScreen
import com.flashsphere.privatednsqs.ui.SnackbarMessage
import com.flashsphere.privatednsqs.ui.TileAddedMessage
import com.flashsphere.privatednsqs.ui.TileAlreadyAddedMessage
import com.flashsphere.privatednsqs.ui.TileNotAddedMessage
import com.flashsphere.privatednsqs.viewmodel.MainViewModel

class MainActivity : BaseActivity() {
    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    private var toast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MainScreen(
                viewModel = viewModel,
                showAppInfo = this::showAppInfo,
                showMoreInfo = this::showMoreInfo,
                requestAddTile = this::requestAddTile,
            )
        }

        showMessageFromIntent(savedInstanceState, intent)
    }

    private fun showMessageFromIntent(savedInstanceState: Bundle?, intent: Intent?) {
        if (savedInstanceState != null) return
        if (intent == null) return
        val bundle = intent.getBundleExtra(PARAM_BUNDLE) ?: return
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

    companion object {
        fun getPendingIntent(context: Context, message: SnackbarMessage): PendingIntent =
            PendingIntent.getActivity(context, R.id.start_main_activity_request_code,
                getIntent(context, message), FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)

        fun getIntent(context: Context, message: SnackbarMessage): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_CLEAR_TASK or FLAG_ACTIVITY_NEW_TASK)
                .putExtra(PARAM_BUNDLE, Bundle().also {
                    it.putParcelable(PARAM_MESSAGE, message)
                })

        fun startActivity(context: Context, message: SnackbarMessage) {
            context.startActivity(getIntent(context, message))
        }

        private const val PARAM_BUNDLE = "bundle"
        private const val PARAM_MESSAGE = "message"
    }
}