package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.TxConfirmation
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.databinding.ItemSendConfirmCountdownBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import java.util.concurrent.TimeUnit

class InvoiceCountdownTimerDelegate<in T> : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        return (items[position] as? TxConfirmationValue)?.confirmation == TxConfirmation.INVOICE_COUNTDOWN
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ViewHolder(
            ItemSendConfirmCountdownBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ViewHolder).bind(items[position] as TxConfirmationValue.BitPayCountdown)

    class ViewHolder(
        private val binding: ItemSendConfirmCountdownBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TxConfirmationValue.BitPayCountdown) {
            binding.confirmationItemLabel.setText(R.string.bitpay_remaining_time)
            updateCountdownElements(item.timeRemainingSecs)
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
            binding.confirmationItemValue.text = readableTime

            when {
                remaining > FIVE_MINUTES -> setColors(R.color.primary_grey_light)
                remaining > ONE_MINUTE -> setColors(R.color.secondary_yellow_medium)
                else -> setColors(R.color.secondary_red_light)
            }
        }

        private fun setColors(@ColorRes colourResId: Int) {
            val resolved = context.getResolvedColor(colourResId)
            binding.confirmationItemValue.setTextColor(resolved)
            binding.confirmationItemLabel.setTextColor(resolved)
        }

        companion object {
            private const val FIVE_MINUTES = 5 * 60
            private const val ONE_MINUTE = 60
        }
    }
}
