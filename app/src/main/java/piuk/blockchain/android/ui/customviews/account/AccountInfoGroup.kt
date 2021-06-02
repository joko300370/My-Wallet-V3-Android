package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.ExchangeRates
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.core.KoinComponent
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.impl.AllWalletsAccount
import piuk.blockchain.android.databinding.ViewAccountGroupOverviewBinding
import piuk.blockchain.android.util.invisible
import piuk.blockchain.android.util.visible
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedDrawable
import timber.log.Timber

class AccountInfoGroup @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), KoinComponent {

    private val binding: ViewAccountGroupOverviewBinding =
        ViewAccountGroupOverviewBinding.inflate(LayoutInflater.from(context), this, true)

    private val exchangeRates: ExchangeRates by scopedInject()
    private val currencyPrefs: CurrencyPrefs by scopedInject()

    private val disposables = CompositeDisposable()

    fun updateAccount(account: AccountGroup) {
        disposables.clear()
        updateView(account)
    }

    private fun updateView(account: AccountGroup) {
        // Only supports AllWallets at this time
        require(account is AllWalletsAccount)

        disposables.clear()

        val currency = currencyPrefs.selectedFiatCurrency
        with(binding) {
            icon.setImageDrawable(context.getResolvedDrawable(R.drawable.ic_all_wallets_blue))

            walletName.text = account.label

            assetSubtitle.text = context.getString(R.string.activity_wallet_total_balance)

            walletBalanceFiat.invisible()
            walletCurrency.text = currency

            disposables += account.fiatBalance(currency, exchangeRates)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        walletBalanceFiat.text = it.toStringWithSymbol()
                        walletBalanceFiat.visible()
                    },
                    onError = {
                        Timber.e("Cannot get balance for ${account.label}")
                    }
                )
        }
    }

    fun dispose() {
        disposables.clear()
    }
}
