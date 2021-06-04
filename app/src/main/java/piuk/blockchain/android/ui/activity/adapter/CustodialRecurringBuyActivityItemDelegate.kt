package piuk.blockchain.android.ui.activity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.RecurringBuyActivityState
import com.blockchain.utils.toFormattedDate
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.RecurringBuyActivitySummaryItem
import piuk.blockchain.android.databinding.DialogActivitiesTxItemBinding
import piuk.blockchain.android.ui.activity.CryptoActivityType
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.setAssetIconColours
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
            icon.setImageResource(R.drawable.ic_tx_recurring_buy)
            icon.setAssetIconColours(
                assetResources.assetTint(tx.cryptoCurrency),
                assetResources.assetFilter(tx.cryptoCurrency)
            )

            txType.text = context.resources.getString(R.string.tx_title_buy, tx.cryptoCurrency)
            statusDate.setTxStatus(tx)
            setTextColours(tx.state)

            assetBalanceFiat.text = tx.value.toStringWithSymbol()
            assetBalanceCrypto.text = tx.destinationValue.toStringWithSymbol()

            root.setOnClickListener {
                // TODO on Activity Details and Cancel
                onAccountClicked(tx.cryptoCurrency, tx.txId, CryptoActivityType.CUSTODIAL_TRADING)
            }
        }
    }

    private fun setTextColours(txStatus: RecurringBuyActivityState) {
        val context = binding.root.context
        with(binding) {
            when (txStatus) {
                RecurringBuyActivityState.COMPLETED -> {
                    txType.setTextColor(ContextCompat.getColor(context, R.color.black))
                    statusDate.setTextColor(ContextCompat.getColor(context, R.color.grey_600))
                    assetBalanceFiat.setTextColor(ContextCompat.getColor(context, R.color.grey_600))
                    assetBalanceCrypto.setTextColor(ContextCompat.getColor(context, R.color.black))
                }
                RecurringBuyActivityState.FAILED -> {
                    txType.setTextColor(ContextCompat.getColor(context, R.color.black))
                    statusDate.setTextColor(ContextCompat.getColor(context, R.color.grey_600))
                    assetBalanceFiat.setTextColor(ContextCompat.getColor(context, R.color.red_400))
                    assetBalanceCrypto.setTextColor(ContextCompat.getColor(context, R.color.black))
                }
                RecurringBuyActivityState.PENDING,
                RecurringBuyActivityState.UNKNOWN -> {
                    txType.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
                    statusDate.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
                    assetBalanceFiat.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
                    assetBalanceCrypto.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
                }
            }
        }
    }

    private fun TextView.setTxStatus(tx: RecurringBuyActivitySummaryItem) {
        text = when (tx.state) {
            RecurringBuyActivityState.COMPLETED -> Date(tx.timeStampMs).toFormattedDate()
            RecurringBuyActivityState.PENDING -> context.getString(R.string.recurring_buy_activity_pending)
            RecurringBuyActivityState.FAILED -> context.getString(R.string.recurring_buy_activity_failed)
            RecurringBuyActivityState.UNKNOWN -> ""
        }
    }
}
