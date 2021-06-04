package piuk.blockchain.android.ui.activity.adapter

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.TransactionState
import com.blockchain.nabu.datamanagers.TransactionType
import com.blockchain.utils.toFormattedDate
import kotlinx.android.synthetic.main.layout_fiat_activity_item.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.FiatActivitySummaryItem
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.inflate

import java.util.Date

class CustodialFiatActivityItemDelegate<in T>(
    private val onItemClicked: (String, String) -> Unit
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is FiatActivitySummaryItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FiatActivityItemViewHolder(parent.inflate(R.layout.layout_fiat_activity_item))

    override fun onBindViewHolder(items: List<T>, position: Int, holder: RecyclerView.ViewHolder) {
        (holder as FiatActivityItemViewHolder).bind(
            items[position] as FiatActivitySummaryItem,
            onItemClicked
        )
    }
}

private class FiatActivityItemViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    fun bind(
        tx: FiatActivitySummaryItem,
        onAccountClicked: (String, String) -> Unit
    ) {
        with(itemView) {

            if (tx.state.isPending()) {
                renderPending()
            } else {
                renderComplete(tx)
            }

            tx_type.setTxLabel(tx.currency, tx.type)

            status_date.text = Date(tx.timeStampMs).toFormattedDate()

            asset_balance_fiat.text = tx.value.toStringWithSymbol()

            setOnClickListener { onAccountClicked(tx.currency, tx.txId) }
        }
    }

    private fun View.renderComplete(tx: FiatActivitySummaryItem) {
        icon.apply {
            setImageResource(
                if (tx.type == TransactionType.DEPOSIT)
                    R.drawable.ic_tx_buy else
                    R.drawable.ic_tx_sell
            )
            setBackgroundResource(R.drawable.bkgd_tx_circle)
            background.setTint(ContextCompat.getColor(context, R.color.green_500_fade_15))
            setColorFilter(ContextCompat.getColor(context, R.color.green_500))
        }

        tx_type.setTextColor(ContextCompat.getColor(context, R.color.black))
        status_date.setTextColor(ContextCompat.getColor(context, R.color.grey_600))
        asset_balance_fiat.setTextColor(ContextCompat.getColor(context, R.color.grey_600))
    }

    private fun View.renderPending() {
        tx_type.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
        status_date.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
        asset_balance_fiat.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
        icon.apply {
            setImageResource(R.drawable.ic_tx_confirming)
            background = null
            setColorFilter(Color.TRANSPARENT)
        }
    }

    private fun TransactionState.isPending() =
        this == TransactionState.PENDING
}

private fun AppCompatTextView.setTxLabel(currency: String, type: TransactionType) {
    text = when (type) {
        TransactionType.DEPOSIT -> context.getString(R.string.tx_title_deposit, currency)
        else -> context.getString(R.string.tx_title_withdraw, currency)
    }
}
