package piuk.blockchain.android.ui.accounts

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.blockchain.preferences.CurrencyPrefs
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.util.setCoinIcon
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import kotlinx.android.synthetic.main.dialog_account_selector_item.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.isCustodial
import piuk.blockchain.android.util.assetName
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.toFiat
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.visible

class CryptoSingleAccountDelegate<in T>(
    private val disposables: CompositeDisposable,
    private val exchangeRates: ExchangeRateDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val onAccountClicked: (CryptoAccount) -> Unit
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is CryptoSingleAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CryptoSingleAccountViewHolder(parent.inflate(R.layout.dialog_account_selector_item))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as CryptoSingleAccountViewHolder).bind(
        items[position] as CryptoSingleAccount,
        disposables,
        exchangeRates,
        currencyPrefs.selectedFiatCurrency,
        onAccountClicked
    )
}

private class CryptoSingleAccountViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    internal fun bind(
        account: CryptoSingleAccount,
        disposables: CompositeDisposable,
        exchangeRates: ExchangeRateDataManager,
        currency: String,
        onAccountClicked: (CryptoAccount) -> Unit
    ) {
        with(itemView) {
            val crypto = account.cryptoCurrencies.first()
            icon.setCoinIcon(crypto)
            asset_spend_locked.goneIf(account.isCustodial().not())
            wallet_name.text = account.label

            asset_name.setText(crypto.assetName())

            wallet_balance_crypto.invisible()
            wallet_balance_fiat.invisible()

            disposables += account.balance
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy {
                    wallet_balance_crypto.text = it.toStringWithSymbol()
                    wallet_balance_fiat.text = it.toFiat(exchangeRates, currency).toStringWithSymbol()

                    wallet_balance_crypto.visible()
                    wallet_balance_fiat.visible()
                }

            setOnClickListener { onAccountClicked(account) }
        }
    }
}
