package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_send_confirm_countdown.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.androidcoreui.utils.extensions.context
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import java.util.concurrent.TimeUnit
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import timber.log.Timber

class InvoiceCountdownTimerDelegate<in T>(
    private val model: TransactionModel,
    private val disposable: CompositeDisposable
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        return (items[position] as? TxOptionValue)?.option == TxOption.INVOICE_COUNTDOWN
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ViewHolder(parent.inflate(R.layout.item_send_confirm_countdown), model, disposable)

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ViewHolder).bind(items[position] as TxOptionValue.BitPayCountdown)

    class ViewHolder(
        val view: View,
        val model: TransactionModel,
        val disposable: CompositeDisposable
    ) : RecyclerView.ViewHolder(view), LayoutContainer {

        override val containerView: View?
            get() = itemView

        fun bind(item: TxOptionValue.BitPayCountdown) {
            itemView.confirmation_item_label.setText(R.string.bitpay_remaining_time)

            val remaining = (item.expireTime - System.currentTimeMillis()) / 1000

            startCountdownTimer(remaining)
            updateCountdownElements(remaining)
        }

        private fun startCountdownTimer(remainingTime: Long) {
            var remaining = remainingTime
            if (remaining <= TIMEOUT_STOP) {
                handleCountdownComplete()
            } else {
                disposable += Observable.interval(1, TimeUnit.SECONDS)
                    .doOnEach { remaining-- }
                    .map { remaining }
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext { updateCountdownElements(remaining) }
                    .takeUntil { it <= TIMEOUT_STOP }
                    .doOnComplete { handleCountdownComplete() }
                    .subscribe()
            }
        }

        private fun updateCountdownElements(remaining: Long) {
            val readableTime = if (remaining >= 0) {
                String.format(
                    "%2d:%02d",
                    TimeUnit.SECONDS.toMinutes(remaining),
                    TimeUnit.SECONDS.toSeconds(remaining) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(remaining))
                )
            } else {
                "--:--"
            }
            itemView.confirmation_item_value.text = readableTime

            when {
                remaining > FIVE_MINUTES -> setColors(R.color.primary_grey_light)
                remaining > ONE_MINUTE -> setColors(R.color.secondary_yellow_medium)
                else -> setColors(R.color.secondary_red_light)
            }
        }

        private fun setColors(@ColorRes colourResId: Int) {
            val resolved = context.getResolvedColor(colourResId)
            itemView.confirmation_item_value.setTextColor(resolved)
            itemView.confirmation_item_label.setTextColor(resolved)
        }

        private fun handleCountdownComplete() {
            Timber.d("BitPay Invoice Countdown expired")
            model.process(
                TransactionIntent.ModifyTxOption(TxOptionValue.BitPayCountdown(isExpired = true))
            )
        }

        companion object {
            private const val TIMEOUT_STOP = 2
            private const val FIVE_MINUTES = 5 * 60
            private const val ONE_MINUTE = 60
        }
    }
}
