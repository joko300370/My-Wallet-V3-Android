package piuk.blockchain.android.ui.interest

import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.notifications.analytics.InterestAnalytics
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRates
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.databinding.DialogSheetInterestDetailsBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.util.secondsToDays
import piuk.blockchain.android.util.gone
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InterestSummarySheet : SlidingModalBottomDialog<DialogSheetInterestDetailsBinding>() {
    interface Host : SlidingModalBottomDialog.Host {
        fun gotoActivityFor(account: BlockchainAccount)
        fun goToDeposit(
            fromAccount: SingleAccount,
            toAccount: SingleAccount,
            action: AssetAction
        )
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a InterestSummarySheet.Host"
        )
    }

    private lateinit var account: SingleAccount
    private lateinit var cryptoCurrency: CryptoCurrency

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetInterestDetailsBinding =
        DialogSheetInterestDetailsBinding.inflate(inflater, container, false)

    private val disposables = CompositeDisposable()
    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val exchangeRates: ExchangeRates by scopedInject()
    private val currencyPrefs: CurrencyPrefs by scopedInject()
    private val coincore: Coincore by scopedInject()
    private val assetResources: AssetResources by scopedInject()

    private val listAdapter: InterestSummaryAdapter by lazy { InterestSummaryAdapter() }

    override fun initControls(binding: DialogSheetInterestDetailsBinding) {
        binding.interestDetailsList.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            addItemDecoration(BlockchainListDividerDecor(requireContext()))
            adapter = listAdapter
        }

        binding.apply {
            interestDetailsTitle.text = account.label
            interestDetailsSheetHeader.text = getString(assetResources.assetNameRes(cryptoCurrency))
            interestDetailsLabel.text = getString(assetResources.assetNameRes(cryptoCurrency))
            interestDetailsAssetIcon.setImageResource(assetResources.drawableResFilled(cryptoCurrency))

            interestDetailsActivityCta.setOnClickListener {
                host.gotoActivityFor(account as BlockchainAccount)
            }
            disposables += account.actions
                .map { it.contains(AssetAction.InterestDeposit) }
                .onErrorReturn { false }
                .subscribeBy {
                    if (it) {
                        interestDetailsDepositCta.text =
                            getString(R.string.tx_title_deposit, cryptoCurrency.displayTicker)
                        interestDetailsDepositCta.setOnClickListener {
                            // TODO how do we select accounts from here? For now choose default non-custodial
                            disposables += coincore[cryptoCurrency].accountGroup(AssetFilter.NonCustodial).subscribe {
                                val defaultAccount = it.accounts.first { acc -> acc.isDefault }
                                analytics.logEvent(InterestAnalytics.INTEREST_SUMMARY_DEPOSIT_CTA)
                                host.goToDeposit(defaultAccount, account, AssetAction.InterestDeposit)
                            }
                        }
                    } else {
                        interestDetailsDepositCta.gone()
                    }
                }
        }

        disposables += Singles.zip(
            custodialWalletManager.getInterestAccountDetails(cryptoCurrency),
            custodialWalletManager.getInterestLimits(cryptoCurrency).toSingle(),
            custodialWalletManager.getInterestAccountRates(cryptoCurrency)
        ) { details, limits, interestRate ->
            CompositeInterestDetails(
                totalInterest = details.totalInterest,
                pendingInterest = details.pendingInterest,
                balance = details.balance,
                lockupDuration = limits.interestLockUpDuration.secondsToDays(),
                interestRate = interestRate,
                nextInterestPayment = limits.nextInterestPayment
            )
        }.observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    compositeToView(it)
                },
                onError = {
                    Timber.e("Error loading interest summary details: $it")
                }
            )
    }

    private fun compositeToView(composite: CompositeInterestDetails) {
        val itemList = mutableListOf<InterestSummaryInfoItem>()
        itemList.apply {
            add(
                InterestSummaryInfoItem(
                    getString(R.string.interest_summary_total),
                    composite.totalInterest.toStringWithSymbol()
                )
            )

            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val formattedDate = sdf.format(composite.nextInterestPayment)
            add(InterestSummaryInfoItem(getString(R.string.interest_summary_next_payment), formattedDate))

            add(
                InterestSummaryInfoItem(
                    getString(R.string.interest_summary_accrued),
                    composite.pendingInterest.toStringWithSymbol()
                )
            )

            add(
                InterestSummaryInfoItem(
                    getString(R.string.interest_summary_hold_period),
                    getString(R.string.interest_summary_hold_period_days, composite.lockupDuration)
                )
            )

            add(InterestSummaryInfoItem(getString(R.string.interest_summary_rate), "${composite.interestRate}%"))
        }

        composite.balance.run {
            binding.apply {
                interestDetailsCryptoValue.text = toStringWithSymbol()
                interestDetailsFiatValue.text = toFiat(exchangeRates, currencyPrefs.selectedFiatCurrency)
                    .toStringWithSymbol()
            }
        }

        listAdapter.items = itemList
    }

    companion object {
        fun newInstance(
            singleAccount: SingleAccount,
            selectedAsset: CryptoCurrency
        ): InterestSummarySheet =
            InterestSummarySheet().apply {
                account = singleAccount
                cryptoCurrency = selectedAsset
            }
    }

    data class InterestSummaryInfoItem(
        val title: String,
        val label: String
    )

    private data class CompositeInterestDetails(
        val balance: CryptoValue,
        val totalInterest: CryptoValue,
        val pendingInterest: CryptoValue,
        var nextInterestPayment: Date,
        val lockupDuration: Int,
        val interestRate: Double
    )

    override fun dismiss() {
        super.dismiss()
        disposables.clear()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        disposables.clear()
    }
}