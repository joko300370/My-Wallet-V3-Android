package piuk.blockchain.android.ui.interest

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRates
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_interest_details_sheet.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.drawableResFilled
import piuk.blockchain.androidcoreui.utils.extensions.gone
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class InterestSummarySheet : SlidingModalBottomDialog() {
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
            "Host fragment is not a InterestSummarySheet.Host")
    }

    private lateinit var account: SingleAccount
    private lateinit var cryptoCurrency: CryptoCurrency

    override val layoutResource: Int
        get() = R.layout.dialog_interest_details_sheet

    private val disposables = CompositeDisposable()
    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val exchangeRates: ExchangeRates by scopedInject()
    private val currencyPrefs: CurrencyPrefs by scopedInject()
    private val coincore: Coincore by scopedInject()

    private val listAdapter: InterestSummaryAdapter by lazy { InterestSummaryAdapter() }

    override fun initControls(view: View) {
        view.interest_details_list.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            addItemDecoration(BlockchainListDividerDecor(requireContext()))
            adapter = listAdapter
        }

        view.apply {
            interest_details_title.text = account.label
            interest_details_sheet_header.text = getString(cryptoCurrency.assetName())
            interest_details_label.text = getString(cryptoCurrency.assetName())
            interest_details_asset_icon.setImageResource(cryptoCurrency.drawableResFilled())

            interest_details_activity_cta.setOnClickListener {
                host.gotoActivityFor(account as BlockchainAccount)
            }

            if (account.actions.contains(AssetAction.Deposit)) {
                interest_details_deposit_cta.text =
                    getString(R.string.tx_title_deposit, cryptoCurrency.displayTicker)
                interest_details_deposit_cta.setOnClickListener {
                    // TODO how do we select accounts from here? For now choose default non-custodial
                    coincore[cryptoCurrency].accountGroup(AssetFilter.NonCustodial).subscribe {
                        val defaultAccount = it.accounts.first { acc -> acc.isDefault }
                        host.goToDeposit(defaultAccount, account, AssetAction.Deposit)
                    }
                }
            } else {
                interest_details_deposit_cta.gone()
            }
        }

        val compositeData = CompositeInterestDetails()
        disposables += custodialWalletManager.getInterestAccountDetails(cryptoCurrency)
            .flatMap { details ->
                compositeData.totalInterest = details.totalInterest
                compositeData.pendingInterest = details.pendingInterest
                compositeData.balance = details.balance

                custodialWalletManager.getInterestLimits(cryptoCurrency).toSingle().flatMap { limits ->
                    val lockDurationInSeconds = limits.interestLockUpDuration
                    val lockupDurationDays =
                        lockDurationInSeconds / SECONDS_TO_DAYS // seconds -> days conversion
                    compositeData.lockupDuration = lockupDurationDays

                    custodialWalletManager.getInterestAccountRates(cryptoCurrency).map { interestRate ->
                        compositeData.interestRate = interestRate

                        compositeData
                    }
                }
            }.observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    compositeToView(it, view)
                },
                onError = {
                    Timber.e("Error loading interest summary details: $it")
                }
            )
    }

    private fun compositeToView(composite: CompositeInterestDetails, view: View) {
        val itemList = mutableListOf<InterestSummaryInfoItem>()
        itemList.apply {
            add(InterestSummaryInfoItem(getString(R.string.interest_summary_total),
                composite.totalInterest?.let { it.toStringWithSymbol() }
                    ?: getString(R.string.interest_summary_total_fail)))

            // TODO this will be returned by the API sometime soon, for now show 1st of next month
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, 1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val sdf = SimpleDateFormat("MMM d, YYYY", Locale.getDefault())
            val formattedDate = sdf.format(calendar.time)
            add(InterestSummaryInfoItem(getString(R.string.interest_summary_next_payment),
                formattedDate))

            add(InterestSummaryInfoItem(getString(R.string.interest_summary_accrued),
                composite.pendingInterest?.let {
                    it.toStringWithSymbol()
                } ?: getString(R.string.interest_summary_accrued_fail)))

            add(InterestSummaryInfoItem(getString(R.string.interest_summary_hold_period),
                composite.lockupDuration?.let {
                    getString(R.string.interest_summary_hold_period_days, it)
                } ?: getString(R.string.interest_summary_hold_period_fail)))

            add(InterestSummaryInfoItem(getString(R.string.interest_summary_rate),
                composite.interestRate?.let { "$it%" } ?: getString(
                    R.string.interest_summary_rate_fail)))
        }

        composite.balance?.let {
            view.apply {
                interest_details_crypto_value.text = it.toStringWithSymbol()
                interest_details_fiat_value.text =
                    it.toFiat(exchangeRates, currencyPrefs.selectedFiatCurrency)
                        .toStringWithSymbol()
            }
        }

        listAdapter.items = itemList
    }

    companion object {
        private const val SECONDS_TO_DAYS = 86400.0
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
        var balance: CryptoValue? = null,
        var totalInterest: CryptoValue? = null,
        var pendingInterest: CryptoValue? = null,
        var accruedInterest: CryptoValue? = null,
        var nextInterestPayment: Date? = null,
        var lockupDuration: Double? = null,
        var interestRate: Double? = null
    )
}