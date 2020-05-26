package com.blockchain.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.NotificationReceived
import piuk.blockchain.androidcoreui.utils.AndroidUtils

class NotificationsUtil(
    private val context: Context,
    private val notificationManager: NotificationManager,
    private val analytics: Analytics
) {

    fun triggerNotification(
        title: String,
        marquee: String,
        text: String,
        @DrawableRes icon: Int,
        pendingIntent: PendingIntent,
        id: Int
    ) {

        val builder = NotificationCompat.Builder(
            context,
            NOTIFICATION_CHANNEL_ID
        ).setSmallIcon(icon)
            .setColor(ContextCompat.getColor(context, R.color.primary_navy_medium))
            .setContentTitle(title)
            .setContentIntent(pendingIntent)
            .setWhen(System.currentTimeMillis())
            .setSound(Uri.parse("android.resource://${context.packageName}/${R.raw.beep}"))
            .setTicker(marquee)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(100))
            .setDefaults(Notification.DEFAULT_LIGHTS)
            .setContentText(text)

        if (AndroidUtils.is26orHigher()) {
            // TODO: Maybe pass in specific channel names here, such as "payments" and "contacts"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.app_name),
                importance
            ).apply {
                enableLights(true)
                lightColor = ContextCompat.getColor(context, R.color.primary_navy_medium)
                enableVibration(true)
                vibrationPattern = longArrayOf(100)
            }
            notificationManager.createNotificationChannel(notificationChannel)
        }

        notificationManager.notify(id, builder.build())
        analytics.logEvent(NotificationReceived)
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "group_01"
        const val INTENT_FROM_NOTIFICATION = "notification_pending_intent"
    }
}
