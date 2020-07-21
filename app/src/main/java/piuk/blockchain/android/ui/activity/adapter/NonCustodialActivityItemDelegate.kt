package piuk.blockchain.android.ui.activity.adapter

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_activities_tx_item.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.extensions.toFormattedDate
import piuk.blockchain.android.util.setAssetIconColours
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible
import timber.log.Timber
import java.util.Date

class NonCustodialActivityItemDelegate<in T>(
    private val disposables: CompositeDisposable,
    private val currencyPrefs: CurrencyPrefs,
    private val onItemClicked: (CryptoCurrency, String, Boolean) -> Unit // crypto, txID, isCustodial
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is NonCustodialActivitySummaryItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        NonCustodialActivityItemViewHolder(parent.inflate(R.layout.dialog_activities_tx_item))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as NonCustodialActivityItemViewHolder).bind(
        items[position] as NonCustodialActivitySummaryItem,
        disposables,
        currencyPrefs.selectedFiatCurrency,
        onItemClicked
    )
}

private class NonCustodialActivityItemViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    internal fun bind(
        tx: NonCustodialActivitySummaryItem,
        disposables: CompositeDisposable,
        fiatCurrency: String,
        onAccountClicked: (CryptoCurrency, String, Boolean) -> Unit
    ) {
        with(itemView) {
            if (tx.isConfirmed) {
                icon.setDirectionIcon(tx.direction, tx.isFeeTransaction)
                icon.setAssetIconColours(tx.cryptoCurrency, context)
                status_date.text = Date(tx.timeStampMs).toFormattedDate()
            } else {
                icon.setIsConfirming()
            }

            tx_type.setTxLabel(tx.cryptoCurrency, tx.direction, tx.isFeeTransaction)

            setTextColours(tx.isConfirmed)

            asset_balance_fiat.gone()
            asset_balance_crypto.text = tx.value.toStringWithSymbol()
            disposables += tx.totalFiatWhenExecuted(fiatCurrency)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        asset_balance_fiat.text = it.toStringWithSymbol()
                        asset_balance_fiat.visible()
                    },
                    onError = {
                        Timber.e("Cannot convert to fiat")
                    }
                )

            setOnClickListener { onAccountClicked(tx.cryptoCurrency, tx.txId, false) }
        }
    }

    private fun setTextColours(isConfirmed: Boolean) {
        with(itemView) {
            if (isConfirmed) {
                tx_type.setTextColor(ContextCompat.getColor(context, R.color.black))
                status_date.setTextColor(ContextCompat.getColor(context, R.color.grey_600))
                asset_balance_fiat.setTextColor(ContextCompat.getColor(context, R.color.black))
                asset_balance_crypto.setTextColor(ContextCompat.getColor(context, R.color.grey_600))
            } else {
                tx_type.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
                status_date.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
                asset_balance_fiat.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
                asset_balance_crypto.setTextColor(ContextCompat.getColor(context, R.color.grey_400))
            }
        }
    }
}

private fun ImageView.setDirectionIcon(
    direction: TransactionSummary.Direction,
    isFeeTransaction: Boolean
) {
    setImageResource(
        if (isFeeTransaction) {
            R.drawable.ic_tx_sent
        } else {
            when (direction) {
                TransactionSummary.Direction.TRANSFERRED -> R.drawable.ic_tx_transfer
                TransactionSummary.Direction.RECEIVED -> R.drawable.ic_tx_receive
                TransactionSummary.Direction.SENT -> R.drawable.ic_tx_sent
                TransactionSummary.Direction.BUY -> R.drawable.ic_tx_buy
                TransactionSummary.Direction.SELL -> R.drawable.ic_tx_sell
                TransactionSummary.Direction.SWAP -> R.drawable.ic_tx_swap
            }
        }
    )
}

private fun ImageView.setIsConfirming() =
    icon.apply {
        setImageDrawable(
            AppCompatResources.getDrawable(
                context,
                R.drawable.ic_tx_confirming
            )
        )
        background = null
        setColorFilter(Color.TRANSPARENT)
    }

private fun TextView.setTxLabel(
    cryptoCurrency: CryptoCurrency,
    direction: TransactionSummary.Direction,
    isFeeTransaction: Boolean
) {
    val resId = if (isFeeTransaction) {
        R.string.tx_title_fee
    } else {
        when (direction) {
            TransactionSummary.Direction.TRANSFERRED -> R.string.tx_title_transfer
            TransactionSummary.Direction.RECEIVED -> R.string.tx_title_receive
            TransactionSummary.Direction.SENT -> R.string.tx_title_send
            TransactionSummary.Direction.BUY -> R.string.tx_title_buy
            TransactionSummary.Direction.SELL -> R.string.tx_title_sell
            TransactionSummary.Direction.SWAP -> R.string.tx_title_swap
        }
    }

    text = context.resources.getString(resId, cryptoCurrency.displayTicker)
}
