package piuk.blockchain.android.data.notifications

import android.app.LauncherActivity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.koin.mwaFeatureFlag
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.notifications.NotificationsUtil
import piuk.blockchain.android.R
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.models.NotificationPayload
import com.blockchain.remoteconfig.FeatureFlag
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.reactivex.Maybe
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import kotlinx.serialization.encodeToString
import org.koin.android.ext.android.inject
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.auth.newlogin.AuthNewLoginSheet
import piuk.blockchain.android.ui.auth.newlogin.SecureChannelManager
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcoreui.ApplicationLifeCycle
import timber.log.Timber

class FcmCallbackService : FirebaseMessagingService() {

    private val notificationManager: NotificationManager by inject()
    private val notificationTokenManager: NotificationTokenManager by scopedInject()
    private val rxBus: RxBus by inject()
    private val accessState: AccessState by scopedInject()
    private val analytics: Analytics by inject()
    private val secureChannelManager: SecureChannelManager by scopedInject()
    private val internalFlags: InternalFeatureFlagApi by inject()
    private val mwaFF: FeatureFlag by inject(mwaFeatureFlag)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Timber.d("Message data payload: %s", remoteMessage.data)

            // Parse data, emit events
            val payload = NotificationPayload(remoteMessage.data)
            rxBus.emitEvent(NotificationPayload::class.java, payload)
            sendNotification(
                payload = payload,
                foreground = ApplicationLifeCycle.getInstance().isForeground && accessState.isLoggedIn
            )
        } else {
            // If there is no data field, provide this default behaviour
            NotificationsUtil(applicationContext, notificationManager, analytics).triggerNotification(
                remoteMessage.notification?.title ?: "",
                remoteMessage.notification?.title ?: "",
                remoteMessage.notification?.body ?: "",
                R.mipmap.ic_launcher,
                // Don't want to launch an activity
                PendingIntent.getActivity(applicationContext, 0, Intent(), PendingIntent.FLAG_UPDATE_CURRENT),
                ID_BACKGROUND_NOTIFICATION_2FA,
                remoteMessage.notification?.channelId
            )
        }
    }

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        notificationTokenManager.storeAndUpdateToken(newToken)
    }

    /**
     * Redirects the user to the [LauncherActivity] if [foreground] is set to true, otherwise to the [MainActivity]
     * unless it is a new device login, in which case [MainActivity] is going to load the
     * [piuk.blockchain.android.ui.auth.newlogin.AuthNewLoginSheet] .
     */
    private fun sendNotification(payload: NotificationPayload, foreground: Boolean) {
        val compositeDisposable = createIntentForNotification(payload, foreground)
            .zipWith(mwaFF.enabled.toMaybe())
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onSuccess = { result ->
                    val notifyIntent = result.first
                    val isModernAuthEnabled = result.second
                    val intent = PendingIntent.getActivity(
                        applicationContext,
                        0,
                        notifyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    val notificationId = if (foreground) ID_FOREGROUND_NOTIFICATION else ID_BACKGROUND_NOTIFICATION

                    if (isSecureChannelMessage(payload) && isModernAuthEnabled) {
                        if (foreground) {
                            startActivity(notifyIntent)
                        } else {
                            NotificationsUtil(applicationContext, notificationManager, analytics).triggerNotification(
                                getString(R.string.secure_channel_notif_title),
                                getString(R.string.secure_channel_notif_title),
                                getString(R.string.secure_channel_notif_summary),
                                R.mipmap.ic_launcher,
                                intent,
                                notificationId
                            )
                        }
                    } else {
                        triggerHeadsUpNotification(
                            payload,
                            intent,
                            notificationId
                        )
                    }
                },
                onError = {}
            )
    }

    private fun createIntentForNotification(payload: NotificationPayload, foreground: Boolean): Maybe<Intent> {
        return when {
            isSecureChannelMessage(payload) -> createSecureChannelIntent(payload.payload, foreground)
            foreground -> Maybe.just(
                Intent(applicationContext, MainActivity::class.java).apply {
                    putExtra(NotificationsUtil.INTENT_FROM_NOTIFICATION, true)
                }
            )
            else -> Maybe.just(
                Intent(applicationContext, LauncherActivity::class.java).apply {
                    putExtra(NotificationsUtil.INTENT_FROM_NOTIFICATION, true)
                }
            )
        }
    }

    private fun isSecureChannelMessage(payload: NotificationPayload) =
        payload.type == NotificationPayload.NotificationType.SECURE_CHANNEL_MESSAGE

    private fun createSecureChannelIntent(payload: MutableMap<String, String>, foreground: Boolean): Maybe<Intent> {
        if (internalFlags.isFeatureEnabled(GatedFeature.MODERN_AUTH_PAIRING)) {
            val pubKeyHash = payload[NotificationPayload.PUB_KEY_HASH]
                ?: return Maybe.empty()
            val messageRawEncrypted = payload[NotificationPayload.DATA_MESSAGE]
                ?: return Maybe.empty()

            val message = secureChannelManager.decryptMessage(pubKeyHash, messageRawEncrypted)
                ?: return Maybe.empty()

            return Maybe.just(
                Intent(applicationContext, MainActivity::class.java).apply {
                    putExtra(MainActivity.LAUNCH_AUTH_FLOW, true)
                    putExtra(AuthNewLoginSheet.PUB_KEY_HASH, pubKeyHash)
                    putExtra(AuthNewLoginSheet.MESSAGE, SecureChannelManager.jsonBuilder.encodeToString(message))

                    putExtra(AuthNewLoginSheet.ORIGIN_IP, payload[NotificationPayload.ORIGIN_IP])
                    putExtra(AuthNewLoginSheet.ORIGIN_LOCATION, payload[NotificationPayload.ORIGIN_COUNTRY])
                    putExtra(AuthNewLoginSheet.ORIGIN_BROWSER, payload[NotificationPayload.ORIGIN_BROWSER])
                    putExtra(AuthNewLoginSheet.FORCE_PIN, !foreground)
                    if (foreground) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            )
        } else {
            return Maybe.empty()
        }
    }

    /**
     * Triggers a notification with the "Heads Up" feature on >21, with the "beep" sound and a short
     * vibration enabled.
     *
     * @param payload A [NotificationPayload] object from the Notification Service
     * @param pendingIntent The [PendingIntent] that you wish to be called when the
     * notification is selected
     * @param notificationId The ID of the notification
     */
    private fun triggerHeadsUpNotification(
        payload: NotificationPayload,
        pendingIntent: PendingIntent,
        notificationId: Int
    ) {

        NotificationsUtil(applicationContext, notificationManager, analytics).triggerNotification(
            payload.title ?: "",
            payload.title ?: "",
            payload.body ?: "",
            R.mipmap.ic_launcher,
            pendingIntent,
            notificationId
        )
    }

    companion object {

        const val ID_BACKGROUND_NOTIFICATION = 1337
        const val ID_FOREGROUND_NOTIFICATION = 1338
        const val ID_BACKGROUND_NOTIFICATION_2FA = 1339
    }
}
