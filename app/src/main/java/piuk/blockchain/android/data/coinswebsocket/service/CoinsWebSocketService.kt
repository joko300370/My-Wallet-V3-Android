package piuk.blockchain.android.data.coinswebsocket.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.NotificationsUtil
import com.blockchain.notifications.analytics.Analytics
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.data.coinswebsocket.strategy.CoinsWebSocketStrategy
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.util.lifecycle.AppState
import piuk.blockchain.android.util.lifecycle.LifecycleInterestedComponent
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

class CoinsWebSocketService : Service(), MessagesSocketHandler {

    private val binder = LocalBinder()
    private val compositeDisposable = CompositeDisposable()
    private val notificationManager: NotificationManager by inject()
    private val coinsWebSocketStrategy: CoinsWebSocketStrategy by scopedInject()
    private val lifecycleInterestedComponent: LifecycleInterestedComponent by inject()
    private val rxBus: RxBus by inject()
    private val analytics: Analytics by inject()

    override fun onCreate() {
        super.onCreate()
        coinsWebSocketStrategy.setMessagesHandler(this)
        coinsWebSocketStrategy.open()
        compositeDisposable += lifecycleInterestedComponent.appStateUpdated.subscribe {
            if (it == AppState.FOREGROUNDED) {
                coinsWebSocketStrategy.open()
            } else {
                coinsWebSocketStrategy.close()
            }
        }
    }

    override fun showToast(message: Int) {
        ToastCustom.makeText(
            this,
            getString(message),
            ToastCustom.LENGTH_SHORT,
            ToastCustom.TYPE_GENERAL)
    }

    override fun onBind(intent: Intent?): IBinder? = binder

    override fun triggerNotification(title: String, marquee: String, text: String) {
        val notifyIntent = Intent(applicationContext, MainActivity::class.java)
        notifyIntent.putExtra(NotificationsUtil.INTENT_FROM_NOTIFICATION, true)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT)

        NotificationsUtil(applicationContext, notificationManager, analytics).triggerNotification(
            title,
            marquee,
            text,
            R.drawable.ic_launcher_round,
            pendingIntent,
            1000)
    }

    override fun sendBroadcast(event: ActionEvent) {
        rxBus.emitEvent(ActionEvent::class.java, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        coinsWebSocketStrategy.close()
        compositeDisposable.clear()
    }

    private inner class LocalBinder internal constructor() // Empty constructor
        : Binder() {
        // Necessary for implementing bound Android Service
        val service: CoinsWebSocketService
            get() = this@CoinsWebSocketService
    }
}