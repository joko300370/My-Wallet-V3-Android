package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.ExchangeRates
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.view_account_fiat_overview.view.*
import org.koin.core.KoinComponent
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible

class AccountInfoFiat @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), KoinComponent {

    private val exchangeRates: ExchangeRates by scopedInject()
    private val currencyPrefs: CurrencyPrefs by scopedInject()

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.view_account_fiat_overview, this, true)
    }

    var account: FiatAccount? = null
        private set

    fun updateAccount(account: FiatAccount, disposables: CompositeDisposable) {
        this.account = account
        updateView(account, disposables)
    }

    private fun updateView(account: FiatAccount, disposables: CompositeDisposable) {

        val userFiat = currencyPrefs.selectedFiatCurrency

        wallet_name.text = account.label
        icon.setIcon(account.fiatCurrency)
        asset_name.text = account.fiatCurrency

        disposables += account.balance
            .flatMap { balanceInAccountCurrency ->
                if (userFiat == account.fiatCurrency)
                    Single.just(balanceInAccountCurrency to balanceInAccountCurrency)
                else account.fiatBalance(userFiat, exchangeRates).map { balanceInSelectedCurrency ->
                    balanceInAccountCurrency to balanceInSelectedCurrency
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { (balanceInAccountCurrency, balanceInWalletCurrency) ->
                if (userFiat == account.fiatCurrency) {
                    wallet_balance_exchange_fiat.gone()
                    wallet_balance_fiat.text = balanceInAccountCurrency.toStringWithSymbol()
                } else {
                    wallet_balance_exchange_fiat.visible()
                    wallet_balance_fiat.text = balanceInWalletCurrency.toStringWithSymbol()
                    wallet_balance_exchange_fiat.text = balanceInAccountCurrency.toStringWithSymbol()
                }
            }
        }
}
