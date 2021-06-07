package piuk.blockchain.android.ui.activity.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.TransactionState
import com.blockchain.nabu.datamanagers.TransactionType
import com.blockchain.utils.toFormattedDate
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.FiatActivitySummaryItem
import piuk.blockchain.android.databinding.LayoutFiatActivityItemBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.setTransactionHasFailed
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import java.util.Date

class CustodialFiatActivityItemDelegate<in T>(
    private val onItemClicked: (String, String) -> Unit
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is FiatActivitySummaryItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FiatActivityItemViewHolder(
            LayoutFiatActivityItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(items: List<T>, position: Int, holder: RecyclerView.ViewHolder) {
        (holder as FiatActivityItemViewHolder).bind(
            items[position] as FiatActivitySummaryItem,
            onItemClicked
        )
    }
}

private class FiatActivityItemViewHolder(
    private val binding: LayoutFiatActivityItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        tx: FiatActivitySummaryItem,
        onAccountClicked: (String, String) -> Unit
    ) {
        with(binding) {
            when {
                tx.state.isPending() -> renderPending()
                tx.state.hasFailed() -> renderFailed()
                tx.state.hasCompleted() -> renderComplete(tx)
                else -> throw IllegalArgumentException("TransactionState not valid")
            }

            txType.setTxLabel(tx.currency, tx.type)

            statusDate.text = Date(tx.timeStampMs).toFormattedDate()

            assetBalanceFiat.text = tx.value.toStringWithSymbol()

            txRoot.setOnClickListener { onAccountClicked(tx.currency, tx.txId) }
        }
    }

    private fun LayoutFiatActivityItemBinding.renderComplete(tx: FiatActivitySummaryItem) {
        icon.apply {
            setImageResource(
                if (tx.type == TransactionType.DEPOSIT)
                    R.drawable.ic_tx_buy else
                    R.drawable.ic_tx_sell
            )
            setBackgroundResource(R.drawable.bkgd_tx_circle)
            background.setTint(context.getResolvedColor(R.color.green_500_fade_15))
            setColorFilter(context.getResolvedColor(R.color.green_500))
        }

        txType.setTextColor(context.getResolvedColor(R.color.black))
        statusDate.setTextColor(context.getResolvedColor(R.color.grey_600))
        assetBalanceFiat.setTextColor(context.getResolvedColor(R.color.grey_600))
    }

    private fun LayoutFiatActivityItemBinding.renderPending() {
        txType.setTextColor(context.getResolvedColor(R.color.grey_400))
        statusDate.setTextColor(context.getResolvedColor(R.color.grey_400))
        assetBalanceFiat.setTextColor(context.getResolvedColor(R.color.grey_400))
        icon.apply {
            setImageResource(R.drawable.ic_tx_confirming)
            background = null
            setColorFilter(Color.TRANSPARENT)
        }
    }

    private fun LayoutFiatActivityItemBinding.renderFailed() {
        txType.setTextColor(ContextCompat.getColor(context, R.color.black))
        statusDate.setTextColor(ContextCompat.getColor(context, R.color.grey_600))
        assetBalanceFiat.setTextColor(ContextCompat.getColor(context, R.color.grey_600))
        icon.setTransactionHasFailed()
    }

    private fun TransactionState.isPending() =
        this == TransactionState.PENDING

    private fun TransactionState.hasFailed() =
        this == TransactionState.FAILED

    private fun TransactionState.hasCompleted() =
        this == TransactionState.COMPLETED
}

private fun AppCompatTextView.setTxLabel(currency: String, type: TransactionType) {
    text = when (type) {
        TransactionType.DEPOSIT -> context.getString(R.string.tx_title_deposit, currency)
        else -> context.getString(R.string.tx_title_withdraw, currency)
    }
}
