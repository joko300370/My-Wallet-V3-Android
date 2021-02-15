package piuk.blockchain.android.ui.dashboard.sheets

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.service.TierService
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
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visibleIf
import timber.log.Timber

class FiatFundsDetailSheet : SlidingModalBottomDialog<DialogSheetFiatFundsDetailBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun depositFiat(account: FiatAccount)
        fun gotoActivityFor(account: BlockchainAccount)
        fun withdrawFiat(currency: String)
        fun showFundsKyc()
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a FiatFundsDetailSheet.Host"
        )
    }

    private val prefs: CurrencyPrefs by scopedInject()
    private val exchangeRates: ExchangeRates by scopedInject()
    private val tierService: TierService by scopedInject()
    private val disposables = CompositeDisposable()

    private var account: FiatAccount = NullFiatAccount

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetFiatFundsDetailBinding =
        DialogSheetFiatFundsDetailBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetFiatFundsDetailBinding) {
        val ticker = account.fiatCurrency
        binding.apply {
            fundDetails.fundsTitle.setStringFromTicker(requireContext(), ticker)
            fundDetails.fundsFiatTicker.text = ticker
            fundDetails.fundsIcon.setIcon(ticker)

            fundDetails.fundsBalance.gone()
            fundDetails.fundsUserFiatBalance.gone()

            disposables += Singles.zip(
                account.accountBalance,
                account.fiatBalance(prefs.selectedFiatCurrency, exchangeRates),
                account.actions
            ).observeOn(AndroidSchedulers.mainThread()).subscribeBy(
                onSuccess = { (fiatBalance, userFiatBalance, actions) ->
                    fundDetails.fundsUserFiatBalance.visibleIf { prefs.selectedFiatCurrency != ticker }
                    fundDetails.fundsUserFiatBalance.text = userFiatBalance.toStringWithSymbol()

                    fundDetails.fundsBalance.text = fiatBalance.toStringWithSymbol()
                    fundDetails.fundsBalance.visibleIf { fiatBalance.isZero || fiatBalance.isPositive }
                    fundsWithdrawHolder.visibleIf { actions.contains(AssetAction.Withdraw) }
                    fundsDepositHolder.visibleIf { actions.contains(AssetAction.Deposit) }
                    fundsActivityHolder.visibleIf { actions.contains(AssetAction.ViewActivity) }
                },
                onError = {
                    Timber.e("Error getting fiat funds balances: $it")
                    showErrorToast()
                }
            )

            disposables += tierService.tiers()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { tiers ->
                        fundsDepositHolder.setOnClickListener {
                            dismiss()
                            if (!tiers.isApprovedFor(KycTierLevel.GOLD)) {
                                host.showFundsKyc()
                            } else {
                                host.depositFiat(account)
                            }
                        }
                    },
                    onError = {
                        Timber.e("Error getting fiat funds tiers: $it")
                        showErrorToast()
                    }
                )

            fundsWithdrawHolder.setOnClickListener {
                dismiss()
                host.withdrawFiat(account.fiatCurrency)
            }

            fundsActivityHolder.setOnClickListener {
                dismiss()
                host.gotoActivityFor(account)
            }
        }
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