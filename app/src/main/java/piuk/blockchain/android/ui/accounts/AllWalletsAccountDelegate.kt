package piuk.blockchain.android.ui.accounts

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import com.blockchain.preferences.CurrencyPrefs
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import kotlinx.android.synthetic.main.dialog_account_selector_item.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.impl.AllWalletsAccount
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.visible

class AllWalletsAccountDelegate<in T>(
    private val disposables: CompositeDisposable,
    private val exchangeRates: ExchangeRateDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val onAccountClicked: (CryptoAccount) -> Unit
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is AllWalletsAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AllWalletsAccountViewHolder(parent.inflate(R.layout.dialog_account_selector_item))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AllWalletsAccountViewHolder).bind(
        items[position] as AllWalletsAccount,
        disposables,
        exchangeRates,
        currencyPrefs.selectedFiatCurrency,
        onAccountClicked
    )
}

private class AllWalletsAccountViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    internal fun bind(
        account: AllWalletsAccount,
        disposables: CompositeDisposable,
        exchangeRates: ExchangeRateDataManager,
        currency: String,
        onAccountClicked: (CryptoAccount) -> Unit
    ) {
        with(itemView) {
            icon.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_all_wallets_blue))
            asset_spend_locked.gone()
            wallet_name.text = account.label

            asset_name.text = context.getString(R.string.activity_wallet_total_balance)

            wallet_balance_fiat.invisible()
            wallet_balance_crypto.text = currency

            disposables += account.fiatBalance(currency, exchangeRates)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy {
                    wallet_balance_fiat.text = it.toStringWithSymbol()
                    wallet_balance_fiat.visible()
                }
            setOnClickListener { onAccountClicked(account) }
        }
    }
}
