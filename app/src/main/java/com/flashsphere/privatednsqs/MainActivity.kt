package com.flashsphere.privatednsqs

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
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
import androidx.core.os.ExecutorCompat
import com.flashsphere.privatednsqs.ui.MainScreen
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
}