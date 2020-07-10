package piuk.blockchain.android.ui.accounts

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_fiat_account_selector.view.*
import kotlinx.android.synthetic.main.layout_fiat_account_selector.view.asset_name
import kotlinx.android.synthetic.main.layout_fiat_account_selector.view.icon
import kotlinx.android.synthetic.main.layout_fiat_account_selector.view.wallet_balance_fiat
import kotlinx.android.synthetic.main.layout_fiat_account_selector.view.wallet_name
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible

class FiatAccountDelegate<in T>(
    private val disposables: CompositeDisposable,
    private val selectedFiatCurrency: String,
    private val exchangeRates: ExchangeRateDataManager,
    private val onAccountClicked: (FiatAccount) -> Unit
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is FiatAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FiatAccountViewHolder(
            parent.inflate(R.layout.layout_fiat_account_selector),
            selectedFiatCurrency,
            disposables,
            exchangeRates
        )

    override fun onBindViewHolder(items: List<T>, position: Int, holder: RecyclerView.ViewHolder) {
        (holder as FiatAccountViewHolder).bind(items[position] as FiatAccount, onAccountClicked)
    }
}

private class FiatAccountViewHolder(
    itemView: View,
    private val selectedFiatCurrency: String,
    private val disposables: CompositeDisposable,
    private val exchangeRates: ExchangeRateDataManager
) : RecyclerView.ViewHolder(itemView) {

    internal fun bind(
        account: FiatAccount,
        onAccountClicked: (FiatAccount) -> Unit
    ) {
        with(itemView) {
            wallet_name.text = account.label
            icon.setIcon(account.fiatCurrency)
            asset_name.text = account.fiatCurrency

            disposables += account.balance.flatMap { balanceInAccountCurrency ->
                if (selectedFiatCurrency == account.fiatCurrency)
                    Single.just(balanceInAccountCurrency to balanceInAccountCurrency)
                else account.fiatBalance(selectedFiatCurrency, exchangeRates).map { balanceInSelectedCurrency ->
                    balanceInAccountCurrency to balanceInSelectedCurrency
                }
            }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { (balanceInAccountCurrency, balanceInWalletFiatCurrency) ->
                    if (selectedFiatCurrency == account.fiatCurrency) {
                        wallet_balance_exchange_fiat.gone()
                        wallet_balance_fiat.text = balanceInAccountCurrency.toStringWithSymbol()
                    } else {
                        wallet_balance_exchange_fiat.visible()
                        wallet_balance_fiat.text = balanceInWalletFiatCurrency.toStringWithSymbol()
                        wallet_balance_exchange_fiat.text = balanceInAccountCurrency.toStringWithSymbol()
                    }
                }
            setOnClickListener { onAccountClicked(account) }
        }
    }
}
