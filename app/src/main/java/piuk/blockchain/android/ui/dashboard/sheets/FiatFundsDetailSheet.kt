package piuk.blockchain.android.ui.dashboard.sheets

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.LaunchOrigin
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.ExchangeRates
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.NullFiatAccount
import piuk.blockchain.android.databinding.DialogSheetFiatFundsDetailBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsAnalytics
import piuk.blockchain.android.ui.dashboard.assetdetails.fiatAssetAction
import piuk.blockchain.android.ui.transactionflow.analytics.DepositAnalytics
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf
import timber.log.Timber

class FiatFundsDetailSheet : SlidingModalBottomDialog<DialogSheetFiatFundsDetailBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun gotoActivityFor(account: BlockchainAccount)
        fun showFundsKyc()
        fun startBankTransferWithdrawal(fiatAccount: FiatAccount)
        fun startDepositFlow(fiatAccount: FiatAccount)
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a FiatFundsDetailSheet.Host"
        )
    }

    private val prefs: CurrencyPrefs by scopedInject()
    private val exchangeRates: ExchangeRates by scopedInject()
    private val disposables = CompositeDisposable()

    private var account: FiatAccount = NullFiatAccount

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetFiatFundsDetailBinding =
        DialogSheetFiatFundsDetailBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetFiatFundsDetailBinding) {
        val ticker = account.fiatCurrency
        binding.apply {
            with(fundDetails) {
                fundsTitle.setStringFromTicker(requireContext(), ticker)
                fundsFiatTicker.text = ticker
                fundsIcon.setIcon(ticker)
                fundsBalance.gone()
                fundsUserFiatBalance.gone()
            }
            disposables += Singles.zip(
                account.accountBalance,
                account.fiatBalance(prefs.selectedFiatCurrency, exchangeRates),
                account.actions
            ).observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { (fiatBalance, userFiatBalance, actions) ->
                        fundDetails.fundsUserFiatBalance.visibleIf { prefs.selectedFiatCurrency != ticker }
                        fundDetails.fundsUserFiatBalance.text = userFiatBalance.toStringWithSymbol()
                        fundDetails.fundsBalance.text = fiatBalance.toStringWithSymbol()
                        fundDetails.fundsBalance.visibleIf { fiatBalance.isZero || fiatBalance.isPositive }
                        fundsWithdrawHolder.visibleIf { actions.contains(AssetAction.Withdraw) }
                        fundsDepositHolder.visibleIf { actions.contains(AssetAction.FiatDeposit) }
                        fundsActivityHolder.visibleIf { actions.contains(AssetAction.ViewActivity) }
                    },
                    onError = {
                        Timber.e("Error getting fiat funds balances: $it")
                        showErrorToast()
                    }
                )

            fundsDepositHolder.setOnClickListener {
                analytics.logEvent(fiatAssetAction(AssetDetailsAnalytics.FIAT_DEPOSIT_CLICKED, account.fiatCurrency))
                analytics.logEvent(DepositAnalytics.DepositClicked(LaunchOrigin.CURRENCY_PAGE))
                dismiss()
                host.startDepositFlow(account)
            }
            fundsWithdrawHolder.setOnClickListener {
                analytics.logEvent(fiatAssetAction(AssetDetailsAnalytics.FIAT_WITHDRAW_CLICKED, account.fiatCurrency))
                handleWithdrawalChecks()
            }

            fundsActivityHolder.setOnClickListener {
                analytics.logEvent(fiatAssetAction(AssetDetailsAnalytics.FIAT_ACTIVITY_CLICKED, account.fiatCurrency))
                dismiss()
                host.gotoActivityFor(account)
            }
        }
    }

    private fun handleWithdrawalChecks() {
        disposables += account.canWithdrawFunds()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                binding.fundsSheetProgress.visible()
            }.doFinally {
                binding.fundsSheetProgress.gone()
            }.subscribeBy(
                onSuccess = {
                    if (it) {
                        dismiss()
                        host.startBankTransferWithdrawal(fiatAccount = account)
                    } else {
                        ToastCustom.makeText(
                            requireContext(), getString(R.string.fiat_funds_detail_pending_withdrawal),
                            Toast.LENGTH_LONG, ToastCustom.TYPE_ERROR
                        )
                    }
                },
                onError = {
                    Timber.e("Error getting transactions for withdrawal $it")
                    ToastCustom.makeText(
                        requireContext(), getString(R.string.common_error),
                        Toast.LENGTH_LONG, ToastCustom.TYPE_ERROR
                    )
                }
            )
    }

    private fun showErrorToast() {
        ToastCustom.makeText(
            requireContext(), getString(R.string.common_error), Toast.LENGTH_SHORT,
            ToastCustom.TYPE_ERROR
        )
    }

    companion object {
        fun newInstance(fiatAccount: FiatAccount): FiatFundsDetailSheet {
            return FiatFundsDetailSheet().apply {
                account = fiatAccount
            }
        }
    }

    private fun TextView.setStringFromTicker(context: Context, ticker: String) {
        text = context.getString(
            when (ticker) {
                "EUR" -> R.string.euros
                "GBP" -> R.string.pounds
                "USD" -> R.string.us_dollars
                else -> R.string.empty
            }
        )
    }
}