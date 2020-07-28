package piuk.blockchain.android.ui.dashboard.assetdetails

import android.view.View
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.wallet.DefaultLabels
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.sheet_asset_actions.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.impl.CryptoInterestAccount
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.dashboard.DashboardModel
import piuk.blockchain.android.ui.dashboard.ReturnToPreviousStep
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.drawableResFilled
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import timber.log.Timber

class AssetActionsSheet : SlidingModalBottomDialog() {
    private lateinit var account: BlockchainAccount
    private val disposables = CompositeDisposable()
    private val prefs: CurrencyPrefs by scopedInject()
    private val exchangeRates: ExchangeRateDataManager by scopedInject()
    private val coincore: Coincore by scopedInject()
    private val labels: DefaultLabels by scopedInject()
    private val model: DashboardModel by scopedInject()
    private val uiScheduler = AndroidSchedulers.mainThread()

    override val layoutResource: Int
        get() = R.layout.sheet_asset_actions

    override fun initControls(view: View) {
        mapInfo(view, account)

        mapAccountIcon(view)

        disposables += Singles.zip(
            account.balance,
            account.fiatBalance(prefs.selectedFiatCurrency, exchangeRates)
        ).observeOn(uiScheduler).subscribeBy(
            onSuccess = { (balance, fiatBalance) ->
                view.asset_actions_crypto_value.text = balance.toStringWithSymbol()
                view.asset_actions_fiat_value.text = fiatBalance.toStringWithSymbol()
            },
            onError = {
                Timber.e("---- error with zips $it")
            }
        )

        account.actions.forEach {
            mapAction(it)
        }

        view.asset_actions_back.setOnClickListener {
            model.process(ReturnToPreviousStep)
        }
    }

    private fun mapAccountIcon(view: View) {

    }

    private fun mapInfo(view: View, account: BlockchainAccount) {
        when (account) {
            is CryptoInterestAccount -> {
                view.asset_actions_title.text = labels.getDefaultInterestWalletLabel(account.asset)
                view.asset_actions_asset_icon.setImageResource(account.asset.drawableResFilled())
                disposables += coincore[account.asset].interestRate().observeOn(uiScheduler)
                    .subscribeBy(
                        onSuccess = {
                            view.asset_actions_details.text =
                                getString(R.string.dashboard_asset_balance_interest, it)
                        },
                        onError = {
                            Timber.e("----- error loading interest rate $it")
                        }
                    )
            }
            is CustodialTradingAccount -> {
                view.asset_actions_title.text = getString(account.asset.assetName())
                view.asset_actions_details.text =
                    labels.getDefaultCustodialWalletLabel(account.asset)
                view.asset_actions_asset_icon.setImageResource(account.asset.drawableResFilled())

            }
            is CryptoNonCustodialAccount -> {
                view.asset_actions_title.text = getString(account.asset.assetName())
                view.asset_actions_details.text =
                    labels.getDefaultNonCustodialWalletLabel(account.asset)
                view.asset_actions_asset_icon.setImageResource(account.asset.drawableResFilled())
            }
        }
    }

    private fun mapAction(action: AssetAction) {
        when (action) {
            AssetAction.ViewActivity -> R.id.action_activity
            AssetAction.Send,
            AssetAction.NewSend -> R.id.action_send
            AssetAction.Receive -> R.id.action_receive
            AssetAction.Swap -> R.id.action_swap
        }
    }

    companion object {
        fun newInstance(blockchainAccount: BlockchainAccount): AssetActionsSheet {
            return AssetActionsSheet().apply {
                account = if (blockchainAccount is AccountGroup) {
                    blockchainAccount.accounts.first()
                } else {
                    blockchainAccount as CryptoAccount
                }
            }
        }
    }
}