package com.flashsphere.privatednsqs

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.os.BundleCompat
import androidx.core.os.ExecutorCompat
import com.flashsphere.privatednsqs.ui.MainScreen
import com.flashsphere.privatednsqs.ui.SnackbarMessage
import com.flashsphere.privatednsqs.ui.TileAddedMessage
import com.flashsphere.privatednsqs.ui.TileAlreadyAddedMessage
import com.flashsphere.privatednsqs.ui.TileNotAddedMessage

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MainScreen(viewModel = viewModel,
                showAppInfo = { showAppInfo() },
                requestAddTile = { requestAddTile() })
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
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }

    private fun requestAddTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val statusBarManager = getSystemService(StatusBarManager::class.java) as StatusBarManager
        statusBarManager.requestAddTileService(
            ComponentName(this, PrivateDnsTileService::class.java),
            getString(R.string.qt_default),
            Icon.createWithResource(this, R.drawable.ic_dnsauto),
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

        private const val PARAM_BUNDLE = "bundle"
        private const val PARAM_MESSAGE = "message"
    }
}