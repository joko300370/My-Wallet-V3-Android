package piuk.blockchain.android.ui.activity.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.utils.toFormattedDate
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.TradeActivitySummaryItem
import piuk.blockchain.android.databinding.DialogActivitiesTxItemBinding
import piuk.blockchain.android.ui.activity.CryptoActivityType
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.setAssetIconColours
import piuk.blockchain.android.util.visible
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import java.util.Date

class SwapActivityItemDelegate<in T>(
    private val assetResources: AssetResources,
    private val onItemClicked: (CryptoCurrency, String, CryptoActivityType) -> Unit // crypto, txID, type
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        (items[position] as? TradeActivitySummaryItem)?.let {
            it.currencyPair is CurrencyPair.CryptoCurrencyPair
        } ?: false

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        SwapActivityItemViewHolder(
            DialogActivitiesTxItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as SwapActivityItemViewHolder).bind(
        items[position] as TradeActivitySummaryItem,
        assetResources,
        onItemClicked
    )
}

private class SwapActivityItemViewHolder(
    private val binding: DialogActivitiesTxItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        tx: TradeActivitySummaryItem,
        assetResources: AssetResources,
        onAccountClicked: (CryptoCurrency, String, CryptoActivityType) -> Unit
    ) {
        with(binding) {

            statusDate.text = Date(tx.timeStampMs).toFormattedDate()
            (tx.currencyPair as? CurrencyPair.CryptoCurrencyPair)?.let {
                txType.text = context.resources.getString(
                    R.string.tx_title_swap,
                    it.source.displayTicker,
                    it.destination.displayTicker
                )
                if (tx.state.isPending) {
                    icon.setIsConfirming()
                } else {
                    icon.setImageResource(R.drawable.ic_tx_swap)
                    icon.setAssetIconColours(
                        tintColor = assetResources.assetTint(it.source),
                        filterColor = assetResources.assetFilter(it.source)
                    )
                }
                txRoot.setOnClickListener { onAccountClicked(tx.currencyPair.source, tx.txId, CryptoActivityType.SWAP) }
            }

            setTextColours(tx.state.isPending)

            assetBalanceCrypto.text = tx.value.toStringWithSymbol()
            assetBalanceFiat.text = tx.fiatValue.toStringWithSymbol()
            assetBalanceFiat.visible()
        }
    }

    private fun setTextColours(isPending: Boolean) {
        with(binding) {
            if (!isPending) {
                txType.setTextColor(context.getResolvedColor(R.color.black))
                statusDate.setTextColor(context.getResolvedColor(R.color.grey_600))
                assetBalanceFiat.setTextColor(context.getResolvedColor(R.color.grey_600))
                assetBalanceCrypto.setTextColor(context.getResolvedColor(R.color.black))
            } else {
                txType.setTextColor(context.getResolvedColor(R.color.grey_400))
                statusDate.setTextColor(context.getResolvedColor(R.color.grey_400))
                assetBalanceFiat.setTextColor(context.getResolvedColor(R.color.grey_400))
                assetBalanceFiat.setTextColor(context.getResolvedColor(R.color.grey_400))
            }
        }
    }
}

private fun ImageView.setIsConfirming() =
    apply {
        setImageDrawable(
            AppCompatResources.getDrawable(
                context,
                R.drawable.ic_tx_confirming
            )
        )
        background = null
        setColorFilter(Color.TRANSPARENT)
    }
