package piuk.blockchain.android.ui.activity.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.utils.toFormattedDate
import info.blockchain.balance.CryptoCurrency
import io.reactivex.disposables.CompositeDisposable
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.CustodialTradingActivitySummaryItem
import piuk.blockchain.android.databinding.DialogActivitiesTxItemBinding
import piuk.blockchain.android.ui.activity.CryptoActivityType
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.setAssetIconColours
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import java.util.Date

class CustodialTradingActivityItemDelegate<in T>(
    private val currencyPrefs: CurrencyPrefs,
    private val assetResources: AssetResources,
    private val onItemClicked: (CryptoCurrency, String, CryptoActivityType) -> Unit // crypto, txID, type
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is CustodialTradingActivitySummaryItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CustodialTradingActivityItemViewHolder(
            DialogActivitiesTxItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as CustodialTradingActivityItemViewHolder).bind(
        items[position] as CustodialTradingActivitySummaryItem,
        currencyPrefs.selectedFiatCurrency,
        assetResources,
        onItemClicked
    )
}

private class CustodialTradingActivityItemViewHolder(
    private val binding: DialogActivitiesTxItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    private val disposables: CompositeDisposable = CompositeDisposable()

    fun bind(
        tx: CustodialTradingActivitySummaryItem,
        selectedFiatCurrency: String,
        assetResources: AssetResources,
        onAccountClicked: (CryptoCurrency, String, CryptoActivityType) -> Unit
    ) {
        disposables.clear()
        with(binding) {
            icon.setIcon(tx.status, tx.type)
            if (tx.status.isPending().not()) {
                icon.setAssetIconColours(
                    tintColor = assetResources.assetTint(tx.cryptoCurrency),
                    filterColor = assetResources.assetFilter(tx.cryptoCurrency)
                )
            } else {
                icon.background = null
                icon.setColorFilter(Color.TRANSPARENT)
            }

            txType.setTxLabel(tx.cryptoCurrency, tx.type)

            statusDate.setTxStatus(tx)
            setTextColours(tx.status)

            assetBalanceFiat.bindAndConvertFiatBalance(tx, disposables, selectedFiatCurrency)

            assetBalanceCrypto.text = tx.value.toStringWithSymbol()

            txRoot.setOnClickListener {
                onAccountClicked(tx.cryptoCurrency, tx.txId, CryptoActivityType.CUSTODIAL_TRADING)
            }
        }
    }

    private fun setTextColours(txStatus: OrderState) {
        with(binding) {
            if (txStatus == OrderState.FINISHED) {
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

private fun OrderState.isPending(): Boolean =
    this == OrderState.PENDING_CONFIRMATION ||
        this == OrderState.PENDING_EXECUTION ||
        this == OrderState.AWAITING_FUNDS

private fun ImageView.setIcon(status: OrderState, type: OrderType) =
    setImageResource(
        when (status) {
            OrderState.FINISHED -> if (type == OrderType.BUY) R.drawable.ic_tx_buy else R.drawable.ic_tx_sell
            OrderState.AWAITING_FUNDS,
            OrderState.PENDING_CONFIRMATION,
            OrderState.PENDING_EXECUTION -> R.drawable.ic_tx_confirming
            OrderState.UNINITIALISED, // should not see these next ones ATM
            OrderState.INITIALISED,
            OrderState.UNKNOWN,
            OrderState.CANCELED,
            OrderState.FAILED -> if (type == OrderType.BUY) R.drawable.ic_tx_buy else R.drawable.ic_tx_sell
        }
    )

private fun TextView.setTxLabel(cryptoCurrency: CryptoCurrency, type: OrderType) {
    text = context.resources.getString(
        if (type == OrderType.BUY) R.string.tx_title_buy else R.string.tx_title_sell, cryptoCurrency.displayTicker
    )
}

private fun TextView.setTxStatus(tx: CustodialTradingActivitySummaryItem) {
    text = when (tx.status) {
        OrderState.FINISHED -> Date(tx.timeStampMs).toFormattedDate()
        OrderState.UNINITIALISED -> context.getString(R.string.activity_state_uninitialised)
        OrderState.INITIALISED -> context.getString(R.string.activity_state_initialised)
        OrderState.AWAITING_FUNDS -> context.getString(R.string.activity_state_awaiting_funds)
        OrderState.PENDING_EXECUTION -> context.getString(R.string.activity_state_pending)
        OrderState.PENDING_CONFIRMATION -> context.getString(R.string.activity_state_pending)
        OrderState.UNKNOWN -> context.getString(R.string.activity_state_unknown)
        OrderState.CANCELED -> context.getString(R.string.activity_state_canceled)
        OrderState.FAILED -> context.getString(R.string.activity_state_failed)
    }
}
