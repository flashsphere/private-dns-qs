package com.flashsphere.privatednsqs.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.flashsphere.privatednsqs.ui.AppShortcutsScreen
import com.flashsphere.privatednsqs.viewmodel.AppShortcutsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppShortcutsActivity : BaseActivity() {
    private val viewModel: AppShortcutsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.enableEdgeToEdge(window)
        super.onCreate(savedInstanceState)

        setContent {
            AppShortcutsScreen(
                viewModel = viewModel,
                onBack = this::finish,
            )
        }
    }

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, AppShortcutsActivity::class.java))
        }
    }
}
