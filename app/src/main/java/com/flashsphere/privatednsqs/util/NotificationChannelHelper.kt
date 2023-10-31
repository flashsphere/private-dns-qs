package com.flashsphere.privatednsqs.util

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW
import com.flashsphere.privatednsqs.R

class NotificationChannelHelper(
    context: Context,
) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val ongoingChannelName = context.getString(R.string.channel_name_ongoing)
    private val channels = listOf(ongoingChannelName)

    fun setupNotificationChannels() {
        cleanupNotificationChannels()
        createOngoingNotificationChannel()
    }

    private fun cleanupNotificationChannels() {
        val channelList = notificationManager.notificationChannelsCompat
        for (channel in channelList) {
            if (!channels.contains(channel.id)) {
                notificationManager.deleteNotificationChannel(channel.id)
            }
        }
    }

    private fun createOngoingNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(ongoingChannelName, IMPORTANCE_LOW)
            .setName(ongoingChannelName)
            .setShowBadge(false)
            .setLightsEnabled(false)
            .setVibrationEnabled(false)
            .build()

        notificationManager.createNotificationChannel(channel)
    }
}
