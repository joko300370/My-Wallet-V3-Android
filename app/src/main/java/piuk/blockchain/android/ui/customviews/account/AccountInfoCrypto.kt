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
import kotlinx.android.synthetic.main.view_account_crypto_overview.view.*
import org.koin.core.KoinComponent
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.isCustodial
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.setCoinIcon
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.visible
import timber.log.Timber

class AccountInfoCrypto @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(ctx, attr, defStyle), KoinComponent {

    private val exchangeRates: ExchangeRates by scopedInject()
    private val currencyPrefs: CurrencyPrefs by scopedInject()

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.view_account_crypto_overview, this, true)
    }

    var account: CryptoAccount? = null
        private set

    fun updateAccount(account: CryptoAccount, disposables: CompositeDisposable) {
        this.account = account
        updateView(account, disposables)
    }

    private fun updateView(account: CryptoAccount, disposables: CompositeDisposable) {
        val crypto = account.asset
        icon.setCoinIcon(crypto)
        asset_account_icon.goneIf(account.isCustodial().not())
        wallet_name.text = account.label

        icon.visible()

        asset_name.setText(crypto.assetName())

        wallet_balance_crypto.invisible()
        wallet_balance_fiat.invisible()

        disposables += account.accountBalance
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    wallet_balance_crypto.text = it.toStringWithSymbol()
                    wallet_balance_fiat.text =
                        it.toFiat(
                            exchangeRates,
                            currencyPrefs.selectedFiatCurrency
                        ).toStringWithSymbol()

                    wallet_balance_crypto.visible()
                    wallet_balance_fiat.visible()
                },
                onError = {
                    Timber.e("Cannot get balance for ${account.label}")
                }
            )
        }
}
