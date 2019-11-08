package piuk.blockchain.android.ui.dashboard

import android.annotation.SuppressLint
import android.support.annotation.DrawableRes
import android.support.annotation.VisibleForTesting
import com.blockchain.balance.drawableResFilled
import com.blockchain.lockbox.data.LockboxDataManager
import com.blockchain.swap.nabu.CurrentTier
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.ui.charts.models.ArbitraryPrecisionFiatValue
import piuk.blockchain.android.ui.charts.models.toStringWithSymbol
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementList
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.utils.logging.BalanceLoadedEvent
import piuk.blockchain.androidcoreui.utils.logging.Logging
import timber.log.Timber

class DashboardPresenter(
    private val dashboardBalanceCalculator: DashboardData,
    private val prefs: PersistentPrefs,
    private val exchangeRateFactory: ExchangeRateDataManager,
    private val stringUtils: StringUtils,
    private val rxBus: RxBus,
    private val swipeToReceiveHelper: SwipeToReceiveHelper,
    private val lockboxDataManager: LockboxDataManager,
    private val currentTier: CurrentTier,
    private val pitLinking: PitLinking,
    private val announcements: AnnouncementList
) : BasePresenter<DashboardView>(), AnnouncementHost {

    private val currencies = DashboardConfig.currencies

    private val displayList by unsafeLazy {
        (listOf(
            stringUtils.getString(R.string.dashboard_balances),
            PieChartsState.Loading,
            stringUtils.getString(R.string.dashboard_price_charts)
        ) + currencies.map {
            AssetPriceCardState.Loading(it)
        }).toMutableList()
    }

    private val metadataObservable by unsafeLazy {
        rxBus.register(
            MetadataEvent::class.java
        )
    }

    private val balanceUpdateDisposable = CompositeDisposable()

    override fun onViewReady() {
        with(view) {
            notifyItemAdded(displayList, 0)
            scrollToTop()
        }
        updatePrices()

        val observable = when (firstRun) {
            true -> metadataObservable
            false -> Observable.just(MetadataEvent.SETUP_COMPLETE)
                .applySchedulers()
                // If data is present, update with cached data
                // Data updates run anyway but this makes the UI nicer to look at whilst loading
                .doOnNext {
                    cachedData?.run { view.updatePieChartState(this) }
                }
        }

        firstRun = false

        compositeDisposable += balanceUpdateDisposable

        // Triggers various updates to the page once all metadata is loaded
        compositeDisposable += observable
            // Clears subscription after single event
            .firstOrError()
            .doOnSuccess { updateAllBalances() }
            .doOnSuccess { storeSwipeToReceiveAddresses() }
            .doOnSuccess { updatePitAddressesForThePit() }
            .subscribe(
                { /* No-op */ },
                { Timber.e(it) }
            )
    }

    override fun onViewResumed() {
        announcements.checkLatest(this, compositeDisposable)
    }

    private fun updatePitAddressesForThePit() {
        // Wallet pit linking - update receive addresses in for the pit
        compositeDisposable += pitLinking.isPitLinked()
            .subscribeBy(
                onSuccess = { if (it) pitLinking.sendWalletAddressToThePit() },
                onError = { /* Ignore */ }
            )
    }

    private fun storeSwipeToReceiveAddresses() {
        compositeDisposable += swipeToReceiveHelper.storeAll()
            .subscribe(
                { /* No-op */ },
                { Timber.e(it) }
            )
    }

    override fun onViewDestroyed() {
        rxBus.unregister(MetadataEvent::class.java, metadataObservable)
        super.onViewDestroyed()
    }

    fun updateBalances() {

        with(view) {
            scrollToTop()
        }

        updatePrices()
        updateAllBalances()
    }

    @SuppressLint("CheckResult")
    private fun updatePrices() {
        exchangeRateFactory.updateTickers()
            .observeOn(AndroidSchedulers.mainThread())
            .addToCompositeDisposable(this)
            .doOnError { Timber.e(it) }
            .subscribe(
                {
                    handleAssetPriceUpdate(
                        currencies.filter { it.hasFeature(CryptoCurrency.PRICE_CHARTING) }
                            .map { AssetPriceCardState.Data(getPriceString(it), it, it.drawableResFilled()) })
                },
                {
                    handleAssetPriceUpdate(currencies.map { AssetPriceCardState.Error(it) })
                }
            )
    }

    private fun handleAssetPriceUpdate(list: List<AssetPriceCardState>) {
        displayList.removeAll { it is AssetPriceCardState }
        displayList.addAll(list)

        val firstPosition = displayList.indexOfFirst { it is AssetPriceCardState }

        val positions = (firstPosition until firstPosition + list.size).toList()

        view.notifyItemUpdated(displayList, positions)
    }

    private val balanceFilter = BehaviorSubject.create<BalanceFilter>().apply {
        onNext(BalanceFilter.Total)
    }

    fun setBalanceFilter(balanceFilter: BalanceFilter) {
        this.balanceFilter.onNext(balanceFilter)
    }

    private fun updateAllBalances() {
        balanceUpdateDisposable.clear()
        val data = dashboardBalanceCalculator.getPieChartData(balanceFilter.distinctUntilChanged())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                Logging.logCustom(
                    BalanceLoadedEvent(
                        hasBtcBalance = !it.bitcoin.displayable.isZero,
                        hasBchBalance = !it.bitcoinCash.displayable.isZero,
                        hasEthBalance = !it.ether.displayable.isZero,
                        hasXlmBalance = !it.lumen.displayable.isZero,
                        hasPaxBalance = !it.usdPax.displayable.isZero
                    )
                )
                cachedData = it
            }
            .doOnNext { view.startWebsocketService() }

        balanceUpdateDisposable += Observables.combineLatest(data, shouldDisplayLockboxMessage().cache().toObservable())
            .map { (data, hasLockbox) -> data.copy(hasLockbox = hasLockbox) }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { if (!it.isZero) prefs.setWalletFunded() }
            .subscribe(
                view::updatePieChartState,
                Timber::e
            )
    }

    private fun shouldDisplayLockboxMessage(): Single<Boolean> = Single.zip(
        lockboxDataManager.isLockboxAvailable(),
        lockboxDataManager.hasLockbox(),
        BiFunction { available: Boolean, hasLockbox: Boolean -> available && hasLockbox }
    )

    override val disposables: CompositeDisposable = compositeDisposable

    override fun clearAllAnnouncements() {
        displayList.removeAll { it is AnnouncementCard }
    }

    override fun showAnnouncementCard(card: AnnouncementCard) {
        displayList.add(0, card)
        with(view) {
            notifyItemAdded(displayList, 0)
            scrollToTop()
        }
    }

    override fun dismissAnnouncementCard(prefsKey: String) {
        displayList.filterIsInstance<AnnouncementCard>()
            .forEachIndexed { index, any ->
                if (any.dismissKey == prefsKey) {
                    displayList.remove(any)
                    with(view) {
                        notifyItemRemoved(displayList, index)
                        scrollToTop()
                    }
                }
            }
    }

    override fun startKyc(campaignType: CampaignType) {
        view.startKycFlow(campaignType)
    }

    override fun startSwapOrKyc(swapTarget: CryptoCurrency?) {
        val currency = swapTarget ?: CryptoCurrency.ETHER

        compositeDisposable += currentTier.usersCurrentTier()
            .subscribe { tier ->
                if (tier > 0) {
                    view.startSwap(prefs.selectedFiatCurrency, currency)
                } else {
                    view.startKycFlow(CampaignType.Swap)
                }
            }
    }

    override fun startBuySell() {
        view.startBuySell()
    }

    override fun startPitLinking() {
        view.startPitLinkingFlow()
    }

    override fun startIntroTourGuide() {
        view.startIntroTour()
    }

    override fun startFundsBackup() {
        view.startBackupWallet()
    }

    override fun startSetup2Fa() {
        view.startSetup2Fa()
    }

    override fun startVerifyEmail() {
        view.startVerifyEmail()
    }

    override fun startEnableFingerprintLogin() {
        view.startEnableFingerprintLogin()
    }

    override fun startTransferCrypto() {
        view.startTransferCrypto()
    }

    private fun getPriceString(cryptoCurrency: CryptoCurrency): String {
        val fiat = getFiatCurrency()
        return getLastPrice(cryptoCurrency, fiat).run {
            ArbitraryPrecisionFiatValue.fromMajor(fiat, this.toBigDecimal())
                .toStringWithSymbol()
        }
    }

    private fun getFiatCurrency() = prefs.selectedFiatCurrency

    private fun getLastPrice(cryptoCurrency: CryptoCurrency, fiat: String) =
        exchangeRateFactory.getLastPrice(cryptoCurrency, fiat)

    companion object {

        /**
         * This field stores whether or not the presenter has been run for the first time across
         * all instances. This allows the page to load without a metadata set-up event, which won't
         * be present if the the page is being returned to.
         */
        @VisibleForTesting
        var firstRun = true

        /**
         * This is intended to be a temporary solution to caching data on this page. In future,
         * I intend to organise the MainActivity fragment backstack so that the DashboardFragment
         * is never killed intentionally. However, this could introduce a lot of bugs so this will
         * do for now.
         */
        private var cachedData: PieChartsState.Data? = null

        @JvmStatic
        fun onLogout() {
            firstRun = true
            cachedData = null
        }
    }
}

sealed class AssetPriceCardState(val currency: CryptoCurrency) {

    data class Data(
        val priceString: String,
        val cryptoCurrency: CryptoCurrency,
        @DrawableRes val icon: Int
    ) : AssetPriceCardState(cryptoCurrency)

    class Loading(val cryptoCurrency: CryptoCurrency) : AssetPriceCardState(cryptoCurrency)
    class Error(val cryptoCurrency: CryptoCurrency) : AssetPriceCardState(cryptoCurrency)
}
