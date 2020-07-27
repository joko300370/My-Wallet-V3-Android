package piuk.blockchain.android.ui.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.CustodialBalanceSendClicked
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import info.blockchain.balance.CryptoCurrency
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_dashboard.*
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.campaign.blockstackCampaignName
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.android.simplebuy.SimpleBuyCancelOrderBottomSheet
import piuk.blockchain.android.ui.airdrops.AirdropStatusSheet
import piuk.blockchain.android.ui.dashboard.adapter.DashboardDelegateAdapter
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementList
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailSheet
import piuk.blockchain.android.ui.dashboard.sheets.BankDetailsBottomSheet
import piuk.blockchain.android.ui.dashboard.sheets.CustodyWalletIntroSheet
import piuk.blockchain.android.ui.dashboard.sheets.FiatFundsDetailSheet
import piuk.blockchain.android.ui.dashboard.sheets.FiatFundsNoKycDetailsSheet
import piuk.blockchain.android.ui.dashboard.sheets.ForceBackupForSendSheet
import piuk.blockchain.android.ui.dashboard.sheets.LinkBankAccountDetailsBottomSheet
import piuk.blockchain.android.ui.dashboard.transfer.BasicTransferToWallet
import piuk.blockchain.android.ui.home.HomeScreenMviFragment
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.transfer.send.flow.DialogFlow
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import timber.log.Timber

class EmptyDashboardItem : DashboardItem

private typealias RefreshFn = () -> Unit

class DashboardFragment : HomeScreenMviFragment<DashboardModel, DashboardIntent, DashboardState>(),
    AssetDetailSheet.Host,
    ForceBackupForSendSheet.Host,
    BasicTransferToWallet.Host,
    BankDetailsBottomSheet.Host,
    SimpleBuyCancelOrderBottomSheet.Host,
    FiatFundsDetailSheet.Host,
    FiatFundsNoKycDetailsSheet.Host,
    DialogFlow.FlowHost {

    override val model: DashboardModel by scopedInject()
    private val announcements: AnnouncementList by scopedInject()
    private val analyticsReporter: BalanceAnalyticsReporter by scopedInject()

    private val theAdapter: DashboardDelegateAdapter by lazy {
        DashboardDelegateAdapter(
            prefs = get(),
            onCardClicked = { onAssetClicked(it) },
            analytics = get(),
            onFundsItemClicked = { onFundsClicked(it) }
        )
    }

    private lateinit var theLayoutManager: RecyclerView.LayoutManager

    private val displayList = mutableListOf<DashboardItem>()

    private val compositeDisposable = CompositeDisposable()
    private val rxBus: RxBus by inject()

    private val actionEvent by unsafeLazy {
        rxBus.register(ActionEvent::class.java)
    }

    private var state: DashboardState? =
        null // Hold the 'current' display state, to enable optimising of state updates

    @UiThread
    override fun render(newState: DashboardState) {
        try {
            doRender(newState)
        } catch (e: Throwable) {
            Timber.e("Error rendering: $e")
        }
    }

    private fun doRender(newState: DashboardState) {

        swipe.isRefreshing = false

        if (displayList.isEmpty()) {
            createDisplayList(newState)
        } else {
            updateDisplayList(newState)
        }

        // Update/show bottom sheet
        if (this.state?.showAssetSheetFor != newState.showAssetSheetFor) {
            showAssetSheet(newState.showAssetSheetFor)
        } else {
            if (this.state?.showDashboardSheet != newState.showDashboardSheet) {
                showPromoSheet(newState)
            }
        }

        // Update/show dialog flow
        if (state?.activeFlow != newState.activeFlow) {
            state?.activeFlow?.finishFlow()
            newState.activeFlow?.startFlow(childFragmentManager, this)
        }

        // Update/show announcement
        if (this.state?.announcement != newState.announcement) {
            showAnnouncement(newState.announcement)
        }

        updateAnalytics(this.state, newState)

        this.state = newState
    }

    private fun createDisplayList(newState: DashboardState) {
        with(displayList) {
            add(IDX_CARD_ANNOUNCE, EmptyDashboardItem()) // Placeholder for announcements
            add(IDX_CARD_BALANCE, newState)
            add(IDX_FUNDS_BALANCE, EmptyDashboardItem()) // Placeholder for funds
            add(IDX_CARD_BTC, newState.assets[CryptoCurrency.BTC])
            add(IDX_CARD_ETH, newState.assets[CryptoCurrency.ETHER])
            add(IDX_CARD_BCH, newState.assets[CryptoCurrency.BCH])
            add(IDX_CARD_XLM, newState.assets[CryptoCurrency.XLM])
            add(IDX_CARD_ALGO, newState.assets[CryptoCurrency.ALGO])
            add(IDX_CARD_PAX, newState.assets[CryptoCurrency.PAX])
            add(IDX_CARD_USDT, newState.assets[CryptoCurrency.USDT])
        }
        theAdapter.notifyDataSetChanged()
    }

    private fun updateDisplayList(newState: DashboardState) {
        with(displayList) {

            val modList = mutableListOf<RefreshFn?>()

            modList.add(handleUpdatedAssetState(IDX_CARD_BTC, newState.assets[CryptoCurrency.BTC]))
            modList.add(handleUpdatedAssetState(IDX_CARD_ETH, newState.assets[CryptoCurrency.ETHER]))
            modList.add(handleUpdatedAssetState(IDX_CARD_BCH, newState.assets[CryptoCurrency.BCH]))
            modList.add(handleUpdatedAssetState(IDX_CARD_XLM, newState.assets[CryptoCurrency.XLM]))
            modList.add(handleUpdatedAssetState(IDX_CARD_ALGO, newState.assets[CryptoCurrency.ALGO]))
            modList.add(handleUpdatedAssetState(IDX_CARD_PAX, newState.assets[CryptoCurrency.PAX]))
            modList.add(handleUpdatedAssetState(IDX_CARD_USDT, newState.assets[CryptoCurrency.USDT]))

            modList.removeAll { it == null }

            if (newState.fiatAssets?.fiatAccounts?.isNotEmpty() == true) {
                set(IDX_FUNDS_BALANCE, newState.fiatAssets)
                modList.add { theAdapter.notifyItemChanged(IDX_FUNDS_BALANCE) }
            }

            if (modList.isNotEmpty()) {
                set(IDX_CARD_BALANCE, newState)
                modList.add { theAdapter.notifyItemChanged(IDX_CARD_BALANCE) }
            }

            modList.forEach { it?.invoke() }
        }
    }

    private fun handleUpdatedAssetState(index: Int, newState: CryptoAssetState): RefreshFn? {
        if (displayList[index] != newState) {
            displayList[index] = newState
            return { theAdapter.notifyItemChanged(index) }
        } else {
            return null
        }
    }

    private fun showAssetSheet(sheetFor: CryptoCurrency?) {
        if (sheetFor != null) {
            model.process(LaunchAssetDetailsFlow(sheetFor))
            //showBottomSheet(AssetDetailSheet.newInstance(sheetFor))
        } else {
            // Nothing, unless we need to remove the sheet? TODO
        }
    }

    private fun showPromoSheet(state: DashboardState) {
        showBottomSheet(
            when (state.showDashboardSheet) {
                DashboardSheet.STX_AIRDROP_COMPLETE -> AirdropStatusSheet.newInstance(
                    blockstackCampaignName)
                DashboardSheet.CUSTODY_INTRO -> CustodyWalletIntroSheet.newInstance()
                DashboardSheet.SIMPLE_BUY_PAYMENT -> BankDetailsBottomSheet.newInstance()
                DashboardSheet.BACKUP_BEFORE_SEND -> ForceBackupForSendSheet.newInstance()
                DashboardSheet.BASIC_WALLET_TRANSFER -> BasicTransferToWallet.newInstance(
                    state.transferFundsCurrency!!)
                DashboardSheet.SIMPLE_BUY_CANCEL_ORDER -> {
                    analytics.logEvent(SimpleBuyAnalytics.BANK_DETAILS_CANCEL_PROMPT)
                    SimpleBuyCancelOrderBottomSheet.newInstance(true)
                }
                DashboardSheet.FIAT_FUNDS_DETAILS -> FiatFundsDetailSheet.newInstance(state.selectedFiatAccount!!)
                DashboardSheet.LINK_OR_DEPOSIT -> {
                    state.selectedFiatAccount?.let {
                        LinkBankAccountDetailsBottomSheet.newInstance(it)
                    } ?: LinkBankAccountDetailsBottomSheet.newInstance()
                }
                DashboardSheet.FIAT_FUNDS_NO_KYC -> FiatFundsNoKycDetailsSheet.newInstance()
                null -> null
            }
        )
    }

    private fun showAnnouncement(card: AnnouncementCard?) {
        displayList[IDX_CARD_ANNOUNCE] = card ?: EmptyDashboardItem()
        theAdapter.notifyItemChanged(IDX_CARD_ANNOUNCE)
    }

    private fun updateAnalytics(oldState: DashboardState?, newState: DashboardState) {
        analyticsReporter.updateFiatTotal(newState.fiatBalance)

        newState.assets.forEach { (cc, s) ->

            val newBalance = s.balance
            if (newBalance != null && newBalance != oldState?.assets?.get(cc)?.balance) {
                // IF we have the full set, this will fire
                analyticsReporter.gotAssetBalance(cc, newBalance)
            }
        }
    }

    override fun onBackPressed(): Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_dashboard)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        analytics.logEvent(AnalyticsEvents.Dashboard)

        setupSwipeRefresh()
        setupRecycler()
    }

    private fun setupRecycler() {
        theLayoutManager = SafeLayoutManager(requireContext())

        recycler_view.apply {
            layoutManager = theLayoutManager
            adapter = theAdapter
        }
        theAdapter.items = displayList
    }

    private fun setupToolbar() {
        activity.supportActionBar?.let {
            activity.setupToolbar(it, R.string.dashboard_title)
        }
    }

    private fun setupSwipeRefresh() {

        swipe.setOnRefreshListener { model.process(RefreshAllIntent) }

        // Configure the refreshing colors
        swipe.setColorSchemeResources(
            R.color.blue_800,
            R.color.blue_600,
            R.color.blue_400,
            R.color.blue_200
        )
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()
        compositeDisposable += actionEvent.subscribe {
            model.process(RefreshAllIntent)
        }

        (activity as? MainActivity)?.let {
            compositeDisposable += it.refreshAnnouncements.observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (announcements.enable()) {
                        announcements.checkLatest(announcementHost, compositeDisposable)
                    }
                }
        }

        announcements.checkLatest(announcementHost, compositeDisposable)

        model.process(RefreshAllIntent)
    }

    override fun onPause() {
        compositeDisposable.clear()
        rxBus.unregister(ActionEvent::class.java, actionEvent)
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            MainActivity.SETTINGS_EDIT,
            MainActivity.ACCOUNT_EDIT -> model.process(RefreshAllIntent)
            BACKUP_FUNDS_REQUEST_CODE -> startTransferFunds()
        }
    }

    private fun onAssetClicked(cryptoCurrency: CryptoCurrency) {
        model.process(ShowCryptoAssetDetails(cryptoCurrency))
    }

    private fun onFundsClicked(fiatAccount: FiatAccount) {
        model.process(ShowFiatAssetDetails(fiatAccount))
    }

    private val announcementHost = object : AnnouncementHost {

        override val disposables: CompositeDisposable
            get() = compositeDisposable

        override fun showAnnouncementCard(card: AnnouncementCard) {
            model.process(ShowAnnouncement(card))
        }

        override fun dismissAnnouncementCard() {
            model.process(ClearAnnouncement)
        }

        override fun startKyc(campaignType: CampaignType) = navigator().launchKyc(campaignType)

        override fun startSwap(swapTarget: CryptoCurrency) = navigator().launchSwapOrKyc(
            targetCurrency = swapTarget)

        override fun startPitLinking() = navigator().launchThePitLinking()

        override fun startFundsBackup() = navigator().launchBackupFunds()

        override fun startSetup2Fa() = navigator().launchSetup2Fa()

        override fun startVerifyEmail() = navigator().launchVerifyEmail()

        override fun startEnableFingerprintLogin() = navigator().launchSetupFingerprintLogin()

        override fun startIntroTourGuide() = navigator().launchIntroTour()

        override fun startTransferCrypto() = navigator().launchTransfer()

        override fun startStxReceivedDetail() =
            model.process(ShowDashboardSheet(DashboardSheet.STX_AIRDROP_COMPLETE))

        override fun startSimpleBuyPaymentDetail() =
            model.process(ShowDashboardSheet(DashboardSheet.SIMPLE_BUY_PAYMENT))

        override fun finishSimpleBuySignup() {
            navigator().resumeSimpleBuyKyc()
        }

        override fun startSimpleBuy() {
            navigator().startSimpleBuy()
        }

        override fun showFiatFundsKyc() {
            model.process(ShowDashboardSheet(DashboardSheet.FIAT_FUNDS_NO_KYC))
        }

        override fun showBankLinking() {
            model.process(ShowBankLinkingSheet())
        }
    }

    override fun startWarnCancelSimpleBuyOrder() {
        analytics.logEvent(SimpleBuyAnalytics.CHECKOUT_SUMMARY_PRESS_CANCEL)
        model.process(ShowDashboardSheet(DashboardSheet.SIMPLE_BUY_CANCEL_ORDER))
    }

    override fun cancelOrderConfirmAction(cancelOrder: Boolean, orderId: String?) {
        if (cancelOrder && orderId != null) {
            analytics.logEvent(SimpleBuyAnalytics.BANK_DETAILS_CANCEL_CONFIRMED)
            model.process(CancelSimpleBuyOrder(orderId))
        } else {
            analytics.logEvent(SimpleBuyAnalytics.BANK_DETAILS_CANCEL_GO_BACK)
            model.process(ShowDashboardSheet(DashboardSheet.SIMPLE_BUY_PAYMENT))
        }
    }

    override fun depositFiat(account: FiatAccount) {
        model.process(ShowBankLinkingSheet(account))
    }

    override fun fiatFundsVerifyIdentityCta() {
        navigator().launchKyc(CampaignType.FiatFunds)
    }

    // AssetDetailSheet.Host
    override fun onSheetClosed() {
        model.process(ClearBottomSheet)
    }

    override fun onFlowFinished() {
        model.process(ClearBottomSheet)
    }

    override fun launchNewSendFor(account: SingleAccount) =
        model.process(LaunchSendFlow(account))

    override fun gotoSendFor(account: SingleAccount) {
        when (account) {
            is CryptoNonCustodialAccount -> navigator().gotoSendFor(account)
            is CustodialTradingAccount -> {
                analytics.logEvent(CustodialBalanceSendClicked(account.asset))
                model.process(StartCustodialTransfer(account.asset))
            }
            else -> throw IllegalStateException("The Send action is invalid for account: ${account.label}")
        }.exhaustive
    }

    override fun goToReceiveFor(account: SingleAccount) =
        when (account) {
            is CryptoNonCustodialAccount -> navigator().gotoReceiveFor(account)
            else -> throw IllegalStateException("The Send action is invalid for account: ${account.label}")
        }.exhaustive

    override fun gotoActivityFor(account: BlockchainAccount) =
        navigator().gotoActivityFor(account)

    override fun gotoSwap(account: SingleAccount) =
        when (account) {
            is CryptoNonCustodialAccount -> navigator().launchSwapOrKyc(
                fromCryptoCurrency = account.asset,
                targetCurrency = account.asset.defaultSwapTo
            )
            else -> throw IllegalStateException("The Swap action is invalid for account: ${account.label}")
        }.exhaustive

    override fun startBackupForTransfer() {
        navigator().launchBackupFunds(this, BACKUP_FUNDS_REQUEST_CODE)
    }

    override fun startTransferFunds() {
        model.process(TransferFunds)
    }

    override fun abortTransferFunds() {
        model.process(ClearBottomSheet)
    }

    companion object {
        fun newInstance() = DashboardFragment()

        private const val IDX_CARD_ANNOUNCE = 0
        private const val IDX_CARD_BALANCE = 1
        private const val IDX_FUNDS_BALANCE = 2
        private const val IDX_CARD_BTC = 3
        private const val IDX_CARD_ETH = 4
        private const val IDX_CARD_BCH = 5
        private const val IDX_CARD_XLM = 6
        private const val IDX_CARD_ALGO = 7
        private const val IDX_CARD_PAX = 8
        private const val IDX_CARD_USDT = 9

        private const val BACKUP_FUNDS_REQUEST_CODE = 8265
    }
}

/**
 * supportsPredictiveItemAnimations = false to avoid crashes when computing changes.
 */
private class SafeLayoutManager(context: Context) : LinearLayoutManager(context) {
    override fun supportsPredictiveItemAnimations() = false
}
