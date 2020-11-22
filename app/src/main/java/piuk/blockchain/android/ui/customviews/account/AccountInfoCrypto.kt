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
import piuk.blockchain.android.accounts.CellDecorator
import piuk.blockchain.android.accounts.addViewToBottomWithConstraints
import piuk.blockchain.android.accounts.removePossibleBottomView
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
    private val compositeDisposable = CompositeDisposable()

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.view_account_crypto_overview, this, true)
    }

    fun updateAccount(
        account: CryptoAccount,
        onAccountClicked: (CryptoAccount) -> Unit,
        cellDecorator: CellDecorator
    ) {
        compositeDisposable.clear()
        updateView(account, onAccountClicked, cellDecorator)
    }

    private fun updateView(
        account: CryptoAccount,
        onAccountClicked: (CryptoAccount) -> Unit,
        cellDecorator: CellDecorator
    ) {
        updateAccountDetails(account, onAccountClicked, cellDecorator)

        when (account) {
            is InterestAccount -> setInterestAccountDetails(account)
            is TradingAccount -> asset_account_icon.setImageResource(R.drawable.ic_account_badge_custodial)
            is NonCustodialAccount -> asset_account_icon.gone()
            else -> asset_account_icon.gone()
        }
    }

    private fun setInterestAccountDetails(
        account: CryptoAccount
    ) {
        asset_account_icon.setImageResource(R.drawable.ic_account_badge_interest)

        compositeDisposable += coincore[account.asset].interestRate().observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { asset_subtitle.text = resources.getString(R.string.empty) }
            .subscribeBy(
                onSuccess = {
                    asset_subtitle.text = resources.getString(R.string.dashboard_asset_balance_interest, it)
                },
                onError = {
                    asset_subtitle.text = resources.getString(
                        R.string.dashboard_asset_actions_interest_dsc_failed)

                    Timber.e("AssetActions error loading Interest rate: $it")
                }
            )
    }

    private fun updateAccountDetails(
        account: CryptoAccount,
        onAccountClicked: (CryptoAccount) -> Unit,
        cellDecorator: CellDecorator
    ) {
        val crypto = account.asset
        wallet_name.text = account.label
        icon.setCoinIcon(crypto)
        icon.visible()

        asset_subtitle.setText(crypto.assetName())

        wallet_balance_crypto.invisible()
        wallet_balance_fiat.invisible()

        compositeDisposable += account.accountBalance
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { accountBalance ->
                    wallet_balance_crypto.text = accountBalance.toStringWithSymbol()
                    wallet_balance_fiat.text =
                        accountBalance.toFiat(
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
        compositeDisposable += cellDecorator.view(container.context)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                container.addViewToBottomWithConstraints(
                    view = it,
                    bottomOfView = asset_subtitle,
                    startOfView = asset_subtitle,
                    endOfView = wallet_balance_crypto
                )
            }

        container.alpha = 1f
        setOnClickListener { println("account clicked not setted") }
        compositeDisposable += cellDecorator.isEnabled().observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { isEnabled ->
                    if (isEnabled) {
                        setOnClickListener {
                            println("account clicked with assettt ${account.asset}")
                            onAccountClicked(account)
                        }
                        container.alpha = 1f
                    } else {
                        container.alpha = .6f
                        setOnClickListener { println("account clicked not setted") }
                    }
                }
            )

        container.removePossibleBottomView()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        compositeDisposable.clear()
    }
}
