package com.flashsphere.privatednsqs.ui

import android.content.Context
import android.os.Parcelable
import com.flashsphere.privatednsqs.R
import kotlinx.parcelize.Parcelize

interface SnackbarMessage : Parcelable {
    fun getMessage(context: Context): String
}

@Parcelize
object NoPermissionMessage : SnackbarMessage {
    override fun getMessage(context: Context): String {
        return context.getString(R.string.toast_no_permission)
    }
}

@Parcelize
object NoDnsHostnameMessage : SnackbarMessage {
    override fun getMessage(context: Context): String {
        return context.getString(R.string.toast_no_dns)
    }
}

@Parcelize
object ChangesSavedMessage : SnackbarMessage {
    override fun getMessage(context: Context): String {
        return context.getString(R.string.toast_changes_saved)
    }
}

@Parcelize
object TileAddedMessage : SnackbarMessage {
    override fun getMessage(context: Context): String {
        return context.getString(R.string.tile_added)
    }
}

@Parcelize
object TileAlreadyAddedMessage : SnackbarMessage {
    override fun getMessage(context: Context): String {
        return context.getString(R.string.tile_already_added)
    }
}

@Parcelize
class TileNotAddedMessage(private val resultCode: Int) : SnackbarMessage {
    override fun getMessage(context: Context): String {
        return context.getString(R.string.tile_not_added, resultCode)
    }
}