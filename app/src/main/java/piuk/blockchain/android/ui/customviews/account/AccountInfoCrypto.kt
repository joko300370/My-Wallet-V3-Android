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
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.InterestAccount
import piuk.blockchain.android.coincore.NonCustodialAccount
import piuk.blockchain.android.coincore.TradingAccount
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.setCoinIcon
import piuk.blockchain.androidcoreui.utils.extensions.gone
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
    private val coincore: Coincore by scopedInject()

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
        updateAccountDetails(account, disposables)

        when (account) {
            is InterestAccount -> setInterestAccountDetails(account, disposables)
            is TradingAccount -> asset_account_icon.setImageResource(R.drawable.ic_account_badge_custodial)
            is NonCustodialAccount -> asset_account_icon.gone()
            else -> asset_account_icon.gone()
        }
    }

    private fun setInterestAccountDetails(account: CryptoAccount, disposables: CompositeDisposable) {
        asset_account_icon.setImageResource(R.drawable.ic_account_badge_interest)

        disposables += coincore[account.asset].interestRate().observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    asset_name.text = resources.getString(R.string.dashboard_asset_balance_interest, it)
                },
                onError = {
                    asset_name.text = resources.getString(
                        R.string.dashboard_asset_actions_interest_dsc_failed)

                    Timber.e("AssetActions error loading Interest rate: $it")
                }
            )
    }

    private fun updateAccountDetails(account: CryptoAccount, disposables: CompositeDisposable) {
        val crypto = account.asset
        wallet_name.text = account.label
        icon.setCoinIcon(crypto)
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
