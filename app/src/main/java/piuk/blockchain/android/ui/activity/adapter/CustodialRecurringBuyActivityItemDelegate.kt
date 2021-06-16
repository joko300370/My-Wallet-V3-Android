package piuk.blockchain.android.ui.activity.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.RecurringBuyErrorState
import com.blockchain.nabu.datamanagers.RecurringBuyTransactionState
import com.blockchain.utils.toFormattedDate
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.RecurringBuyActivitySummaryItem
import piuk.blockchain.android.databinding.DialogActivitiesTxItemBinding
import piuk.blockchain.android.ui.activity.CryptoActivityType
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.setAssetIconColours
import piuk.blockchain.android.util.setTransactionHasFailed
import java.util.Date

class CustodialRecurringBuyActivityItemDelegate(
    private val assetResources: AssetResources,
    private val onItemClicked: (CryptoCurrency, String, CryptoActivityType) -> Unit
) : AdapterDelegate<ActivitySummaryItem> {

    override fun isForViewType(items: List<ActivitySummaryItem>, position: Int): Boolean =
        items[position] is RecurringBuyActivitySummaryItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CustodialRecurringBuyActivityViewHolder(
            DialogActivitiesTxItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<ActivitySummaryItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as CustodialRecurringBuyActivityViewHolder).bind(
        items[position] as RecurringBuyActivitySummaryItem,
        assetResources,
        onItemClicked
    )
}

private class CustodialRecurringBuyActivityViewHolder(
    val binding: DialogActivitiesTxItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        tx: RecurringBuyActivitySummaryItem,
        assetResources: AssetResources,
        onAccountClicked: (CryptoCurrency, String, CryptoActivityType) -> Unit
    ) {
        val context = binding.root.context
        with(binding) {
            when (tx.state) {
                RecurringBuyTransactionState.PENDING,
                RecurringBuyTransactionState.COMPLETED -> {
                    icon.setImageResource(R.drawable.ic_tx_recurring_buy)
                    icon.setAssetIconColours(
                        assetResources.assetTint(tx.cryptoCurrency),
                        assetResources.assetFilter(tx.cryptoCurrency)
                    )
                }
                else -> icon.setTransactionHasFailed()
            }

            txType.text = context.resources.getString(R.string.tx_title_buy, tx.cryptoCurrency)
            statusDate.setTxStatus(tx)
            setTextColours(tx.state)

            tx.setFiatAndCryptoText()

            root.setOnClickListener {
                onAccountClicked(tx.cryptoCurrency, tx.txId, CryptoActivityType.RECURRING_BUY)
            }
        }
    }

    private fun setTextColours(txStatus: RecurringBuyTransactionState) {
        val context = binding.root.context
        with(binding) {
            when (txStatus) {
                RecurringBuyTransactionState.COMPLETED -> {
                    txType.setTextColor(ContextCompat.getColor(context, R.color.black))
                    statusDate.setTextColor(ContextCompat.getColor(context, R.color.grey_600))
                    assetBalanceFiat.setTextColor(ContextCompat.getColor(context, R.color.grey_600))
                    assetBalanceCrypto.setTextColor(ContextCompat.getColor(context, R.color.black))
                }
                RecurringBuyTransactionState.FAILED -> {
                    txType.setTextColor(ContextCompat.getColor(context, R.color.black))
                    statusDate.setTextColor(ContextCompat.getColor(context, R.color.red_600))
                    assetBalanceCrypto.setTextColor(ContextCompat.getColor(context, R.color.grey_600))
                    assetBalanceFiat.gone()
                }
                RecurringBuyTransactionState.PENDING,
                RecurringBuyTransactionState.UNKNOWN -> {
                    txType.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
                    statusDate.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
                    assetBalanceCrypto.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
                    assetBalanceFiat.gone()
                }
            }
        }
    }

    private fun RecurringBuyActivitySummaryItem.setFiatAndCryptoText() {
        with(binding) {
            when (state) {
                RecurringBuyTransactionState.COMPLETED -> {
                    assetBalanceFiat.text = value.toStringWithSymbol()
                    assetBalanceCrypto.text = destinationMoney.toStringWithSymbol()
                }
                RecurringBuyTransactionState.FAILED,
                RecurringBuyTransactionState.PENDING,
                RecurringBuyTransactionState.UNKNOWN -> {
                    assetBalanceCrypto.text = value.toStringWithSymbol()
                }
            }
        }
    }

    private fun TextView.setTxStatus(tx: RecurringBuyActivitySummaryItem) {
        text = when (tx.state) {
            RecurringBuyTransactionState.COMPLETED -> Date(tx.timeStampMs).toFormattedDate()
            RecurringBuyTransactionState.PENDING -> context.getString(R.string.recurring_buy_activity_pending)
            RecurringBuyTransactionState.FAILED -> tx.failureReason?.toShortErrorMessage(context)
            RecurringBuyTransactionState.UNKNOWN -> ""
        }
    }

    private fun RecurringBuyErrorState.toShortErrorMessage(context: Context): String =
        when (this) {
            RecurringBuyErrorState.INSUFFICIENT_FUNDS -> context.getString(
                R.string.recurring_buy_insufficient_funds_short_error
            )
            RecurringBuyErrorState.INTERNAL_SERVER_ERROR,
            RecurringBuyErrorState.TRADING_LIMITS_EXCEED,
            RecurringBuyErrorState.BLOCKED_BENEFICIARY_ID,
            RecurringBuyErrorState.UNKNOWN -> context.getString(R.string.recurring_buy_short_error)
        }
}
