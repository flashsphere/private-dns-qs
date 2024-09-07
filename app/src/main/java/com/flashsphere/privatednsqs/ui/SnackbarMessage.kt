package com.flashsphere.privatednsqs.ui

import android.content.Context
import com.flashsphere.privatednsqs.R

interface SnackbarMessage {
    fun getMessage(context: Context): String
}

object NoPermissionMessage : SnackbarMessage {
    override fun getMessage(context: Context): String {
        return context.getString(R.string.toast_no_permission)
    }
}

object NoDnsHostnameMessage : SnackbarMessage {
    override fun getMessage(context: Context): String {
        return context.getString(R.string.toast_no_dns)
    }
}

object ChangesSavedMessage : SnackbarMessage {
    override fun getMessage(context: Context): String {
        return context.getString(R.string.toast_changes_saved)
    }
}

object TileAddedMessage : SnackbarMessage {
    override fun getMessage(context: Context): String {
        return context.getString(R.string.tile_added)
    }
}

object TileAlreadyAddedMessage : SnackbarMessage {
    override fun getMessage(context: Context): String {
        return context.getString(R.string.tile_already_added)
    }
}

class TileNotAddedMessage(private val resultCode: Int) : SnackbarMessage {
    override fun getMessage(context: Context): String {
        return context.getString(R.string.tile_not_added, resultCode)
    }
}