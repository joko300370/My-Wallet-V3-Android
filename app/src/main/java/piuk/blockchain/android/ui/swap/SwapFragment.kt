package piuk.blockchain.android.ui.swap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.SwapLimits
import com.blockchain.swap.nabu.datamanagers.SwapOrder
import com.blockchain.swap.nabu.datamanagers.SwapPair
import com.blockchain.swap.nabu.datamanagers.repositories.SwapPairsRepository
import com.blockchain.swap.nabu.models.nabu.KycTierLevel
import com.blockchain.swap.nabu.models.nabu.KycTiers
import com.blockchain.swap.nabu.service.TierService
import info.blockchain.balance.Money
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import kotlinx.android.synthetic.main.fragment_swap.*
import kotlinx.android.synthetic.main.pending_swaps_layout.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.accounts.CellDecorator
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.NonCustodialAccount
import piuk.blockchain.android.coincore.TradingAccount
import piuk.blockchain.android.ui.customviews.ButtonOptions
import piuk.blockchain.android.ui.customviews.KycBenefitsBottomSheet
import piuk.blockchain.android.ui.customviews.TrendingPair
import piuk.blockchain.android.ui.customviews.TrendingPairsProvider
import piuk.blockchain.android.ui.customviews.VerifyIdentityBenefit
import piuk.blockchain.android.ui.customviews.account.AccountSelectSheet
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.swapold.SwapSourceAccountSelectSheetDecorator
import piuk.blockchain.android.ui.transactionflow.DialogFlow
import piuk.blockchain.android.ui.transactionflow.TransactionFlow
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf
import timber.log.Timber

class SwapFragment : Fragment(), DialogFlow.FlowHost, AccountSelectSheet.SelectionHost, KycBenefitsBottomSheet.Host {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_swap)

    private val kycTierService: TierService by scopedInject()
    private val coincore: Coincore by scopedInject()
    private val exchangeRateDataManager: ExchangeRateDataManager by scopedInject()
    private val trendingPairsProvider: TrendingPairsProvider by scopedInject()
    private val walletManager: CustodialWalletManager by scopedInject()
    private val swapPairsRepository: SwapPairsRepository by scopedInject()
    private val currencyPrefs: CurrencyPrefs by inject()
    private val walletPrefs: WalletStatus by inject()

    private val compositeDisposable = CompositeDisposable()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swap_error.setDetails {
            loadSwapOrKyc()
        }

        swap_cta.apply {
            setOnClickListener {
                val fragment = AccountSelectSheet.newInstance(
                    this@SwapFragment, getAccountList(),
                    R.string.swap_account_select,
                    ::statusDecorator,
                    R.string.which_wallet_to_swap
                )
                childFragmentManager.beginTransaction().add(fragment, TAG).commit()
            }
            gone()
        }
        loadSwapOrKyc()
    }

    private fun statusDecorator(account: BlockchainAccount): CellDecorator =
        SwapSourceAccountSelectSheetDecorator(account)

    private fun getAccountList(): Single<List<BlockchainAccount>> =
        coincore.allWallets().zipWith(swapPairsRepository.getSwapAvailablePairs()).map { (accountGroup, pairs) ->
            accountGroup.accounts.filter { account ->
                (account is TradingAccount || account is NonCustodialAccount) &&
                        (account as? CryptoAccount)?.isAvailableToSwapFrom(pairs) ?: false
            }
        }

    override fun onAccountSelected(account: BlockchainAccount) {
        require(account is CryptoAccount)
        TransactionFlow(
            sourceAccount = account,
            action = AssetAction.Swap
        ).apply {
            startFlow(
                fragmentManager = childFragmentManager,
                host = this@SwapFragment
            )
        }
    }

    override fun verificationCtaClicked() {
        walletPrefs.setSeenSwapPromo()
        KycNavHostActivity.start(requireActivity(), CampaignType.Swap)
    }

    override fun onSheetClosed() {
        walletPrefs.setSeenSwapPromo()
    }

    private fun loadSwapOrKyc() {
        compositeDisposable +=
            Singles.zip(
                kycTierService.tiers(),
                trendingPairsProvider.getTrendingPairs(),
                walletManager.getSwapLimits(currencyPrefs.selectedFiatCurrency),
                walletManager.getSwapTrades()
            ) { tiers: KycTiers, pairs: List<TrendingPair>, limits: SwapLimits, orders: List<SwapOrder> ->
                SwapComposite(
                    tiers,
                    pairs,
                    limits,
                    orders
                )
            }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    showLoadingUi()
                }
                .subscribeBy(
                    onSuccess = { composite ->
                        showSwapUi(composite.orders)

                        if (composite.tiers.isVerified()) {
                            swap_view_switcher.displayedChild = SWAP_VIEW
                            swap_header.toggleBottomSeparator(false)

                            val onPairClicked = onTrendingPairClicked()

                            swap_trending.initialise(
                                pairs = composite.pairs,
                                onSwapPairClicked = onPairClicked
                            )

                            if (!composite.tiers.isApprovedFor(KycTierLevel.GOLD)) {
                                showKycUpsellIfEligible(composite.limits)
                            }
                        } else {
                            swap_view_switcher.displayedChild = KYC_VIEW
                            initKycView()
                        }
                    },
                    onError = {
                        showErrorUi()

                        ToastCustom.makeText(
                            requireContext(),
                            getString(R.string.transfer_wallets_load_error),
                            ToastCustom.LENGTH_SHORT,
                            ToastCustom.TYPE_ERROR
                        )

                        Timber.e("Error loading swap kyc service $it")
                    })
    }

    private fun showKycUpsellIfEligible(limits: SwapLimits) {
        val usedUpLimitPercent = (limits.maxLimit / limits.maxOrder).toFloat() * 100
        if (usedUpLimitPercent >= KYC_UPSELL_PERCENTAGE && !walletPrefs.hasSeenSwapPromo) {
            val fragment = KycBenefitsBottomSheet.newInstance(
                KycBenefitsBottomSheet.BenefitsDetails(
                    title = getString(R.string.swap_kyc_upsell_title),
                    description = getString(R.string.swap_kyc_upsell_desc),
                    listOfBenefits = listOf(
                        VerifyIdentityBenefit(
                            getString(R.string.swap_kyc_upsell_1_title),
                            getString(R.string.swap_kyc_upsell_1_desc)),
                        VerifyIdentityBenefit(
                            getString(R.string.swap_kyc_upsell_2_title),
                            getString(R.string.swap_kyc_upsell_2_desc)),
                        VerifyIdentityBenefit(
                            getString(R.string.swap_kyc_upsell_3_title),
                            getString(R.string.swap_kyc_upsell_3_desc))
                    )
                )
            )
            childFragmentManager.beginTransaction().add(fragment, TAG).commit()
        }
    }

    private fun onTrendingPairClicked(): (TrendingPair) -> Unit = { pair ->
        TransactionFlow(
            sourceAccount = pair.sourceAccount,
            target = pair.destinationAccount,
            action = AssetAction.Swap
        ).apply {
            startFlow(
                fragmentManager = childFragmentManager,
                host = this@SwapFragment
            )
        }
    }

    private fun initKycView() {
        swap_kyc_benefits.initWithBenefits(
            listOf(
                VerifyIdentityBenefit(
                    getString(R.string.swap_kyc_1_title),
                    getString(R.string.swap_kyc_1_label)),
                VerifyIdentityBenefit(
                    getString(R.string.swap_kyc_2_title),
                    getString(R.string.swap_kyc_2_label)),
                VerifyIdentityBenefit(
                    getString(R.string.swap_kyc_3_title),
                    getString(R.string.swap_kyc_3_label))
            ),
            getString(R.string.swap_kyc_title),
            getString(R.string.swap_kyc_desc),
            R.drawable.ic_swap_blue_circle,
            ButtonOptions(visible = true, text = getString(R.string.swap_kyc_cta)) {
                KycNavHostActivity.start(requireActivity(), CampaignType.Swap)
            },
            ButtonOptions(visible = false),
            showSheetIndicator = false
        )
    }

    private fun showErrorUi() {
        swap_loading_indicator.gone()
        swap_error.visible()
    }

    private fun showSwapUi(orders: List<SwapOrder>) {
        val pendingOrders = orders.filter { it.state.isPending }
        val hasPendingOrder = pendingOrders.isNotEmpty()
        swap_loading_indicator.gone()
        swap_view_switcher.visible()
        swap_error.gone()
        swap_cta.visible()
        swap_trending.visibleIf { !hasPendingOrder }
        pending_swaps.visibleIf { hasPendingOrder }
        pending_swaps.pending_list.apply {
            adapter =
                PendingSwapsAdapter(
                    pendingOrders) { money: Money ->
                    money.toFiat(exchangeRateDataManager, currencyPrefs.selectedFiatCurrency)
                }
            layoutManager = LinearLayoutManager(activity)
            addItemDecoration(
                DividerItemDecoration(
                    context,
                    DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    private fun showLoadingUi() {
        swap_loading_indicator.visible()
        swap_view_switcher.gone()
        swap_error.gone()
    }

    companion object {
        private const val KYC_UPSELL_PERCENTAGE = 90
        private const val SWAP_VIEW = 0
        private const val KYC_VIEW = 1
        private const val TAG = "BOTTOM_SHEET"
        fun newInstance(): SwapFragment = SwapFragment()
    }

    override fun onFlowFinished() {
        loadSwapOrKyc()
    }

    private data class SwapComposite(
        val tiers: KycTiers,
        val pairs: List<TrendingPair>,
        val limits: SwapLimits,
        val orders: List<SwapOrder>
    )
}

private fun CryptoAccount.isAvailableToSwapFrom(pairs: List<SwapPair>): Boolean =
    pairs.any { it.source == this.asset }
