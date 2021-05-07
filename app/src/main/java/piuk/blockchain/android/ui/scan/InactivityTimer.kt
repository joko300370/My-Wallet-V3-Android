package piuk.blockchain.android.ui.scan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import java.util.concurrent.TimeUnit

internal class InactivityTimer(private val activity: AppCompatActivity) {

    private var disposable: Disposable? = null

    fun onActivityEvent() {
        stop()
        start()
    }

    fun onActivityPaused() {
        stop()
        activity.unregisterReceiver(powerStatusReceiver)
    }

    fun onActivityResumed() {
        activity.registerReceiver(powerStatusReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onActivityEvent()
    }

    fun stop() {
        disposable?.dispose()
        disposable = null
    }

    private fun start() {
        require(disposable == null)

        disposable = Single.timer(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .subscribeBy(
                onSuccess = { activity.finish() }
            )
    }

    private val powerStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (Intent.ACTION_BATTERY_CHANGED == intent?.action) {
                // 0 indicates that we're on battery
                if (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) <= 0) {
                    onActivityEvent()
                } else {
                    stop()
                }
            }
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5 * 60 * 1000L
    }
}