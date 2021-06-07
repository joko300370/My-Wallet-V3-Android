package piuk.blockchain.android.ui.activity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.utils.toFormattedDate
import info.blockchain.balance.CryptoCurrency
import io.reactivex.disposables.CompositeDisposable
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.CustodialSendActivitySummaryItem
import piuk.blockchain.android.databinding.DialogActivitiesTxItemBinding
import piuk.blockchain.android.ui.activity.CryptoActivityType
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.setAssetIconColours
import piuk.blockchain.android.util.setTransactionIsConfirming
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import java.util.Date

class CustodialSendActivityItemDelegate(
    private val prefs: CurrencyPrefs,
    private val assetResources: AssetResources,
    private val onItemClicked: (CryptoCurrency, String, CryptoActivityType) -> Unit // crypto, txID, type
) : AdapterDelegate<ActivitySummaryItem> {

    override fun isForViewType(items: List<ActivitySummaryItem>, position: Int): Boolean =
        items[position] is CustodialSendActivitySummaryItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CustodialSendActivityItemViewHolder(
            DialogActivitiesTxItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<ActivitySummaryItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as CustodialSendActivityItemViewHolder).bind(
        items[position] as CustodialSendActivitySummaryItem,
        prefs.selectedFiatCurrency,
        assetResources,
        onItemClicked
    )
}

private class CustodialSendActivityItemViewHolder(
    private val binding: DialogActivitiesTxItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    private val disposables: CompositeDisposable = CompositeDisposable()

    fun bind(
        tx: CustodialSendActivitySummaryItem,
        selectedFiatCurrency: String,
        assetResources: AssetResources,
        onAccountClicked: (CryptoCurrency, String, CryptoActivityType) -> Unit
    ) {
        disposables.clear()
        with(binding) {
            if (tx.isConfirmed) {
                icon.setImageResource(R.drawable.ic_tx_sent)
                icon.setAssetIconColours(
                    tintColor = assetResources.assetTint(tx.cryptoCurrency),
                    filterColor = assetResources.assetFilter(tx.cryptoCurrency)
                )
            } else {
                icon.setTransactionIsConfirming()
            }

            setTextColours(tx.isConfirmed)

            statusDate.text = Date(tx.timeStampMs).toFormattedDate()

            txType.text = context.getString(R.string.tx_title_send, tx.cryptoCurrency.displayTicker)

            assetBalanceFiat.gone()
            assetBalanceCrypto.text = tx.value.toStringWithSymbol()
            assetBalanceFiat.bindAndConvertFiatBalance(tx, disposables, selectedFiatCurrency)

            binding.root.setOnClickListener {
                onAccountClicked(
                    tx.cryptoCurrency, tx.txId, CryptoActivityType.CUSTODIAL_SEND
                )
            }
        }
    }

    private fun setTextColours(isConfirmed: Boolean) {
        with(binding) {
            if (isConfirmed) {
                txType.setTextColor(context.getResolvedColor(R.color.black))
                statusDate.setTextColor(context.getResolvedColor(R.color.grey_600))
                assetBalanceFiat.setTextColor(context.getResolvedColor(R.color.grey_600))
                assetBalanceCrypto.setTextColor(context.getResolvedColor(R.color.black))
            } else {
                txType.setTextColor(context.getResolvedColor(R.color.grey_400))
                statusDate.setTextColor(context.getResolvedColor(R.color.grey_400))
                assetBalanceFiat.setTextColor(context.getResolvedColor(R.color.grey_400))
                assetBalanceCrypto.setTextColor(context.getResolvedColor(R.color.grey_400))
            }
        }
    }
}