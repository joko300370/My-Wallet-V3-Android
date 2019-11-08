package piuk.blockchain.android.ui.dashboard

import com.blockchain.android.testutils.rxInit
import com.blockchain.balance.TotalBalance
import com.blockchain.kyc.status.KycTiersQueries
import com.blockchain.lockbox.data.LockboxDataManager
import com.blockchain.swap.nabu.CurrentTier
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.usdPax
import com.blockchain.testutils.lumens
import com.blockchain.testutils.ether
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.campaign.BlockstackCampaignRegistration
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementList
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.FiatExchangeRates
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.PersistentPrefs
import java.util.Locale

class DashboardPresenterTest {

    private lateinit var subject: DashboardPresenter
    private val prefs: PersistentPrefs = mock()
    private val currentTier: CurrentTier = mock()
    private val exchangeRateFactory: ExchangeRateDataManager = mock()
    private val bchDataManager: BchDataManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val transactionListDataManager: TransactionListDataManager = mock()
    private val stringUtils: StringUtils = mock()
    private val accessState: AccessState = mock()
    private val buyDataManager: BuyDataManager = mock()
    private val rxBus: RxBus = mock()
    private val swipeToReceiveHelper: SwipeToReceiveHelper = mock()
    private val view: DashboardView = mock()
    private val pitLinking: PitLinking = mock()
    private val currencyFormatManager: CurrencyFormatManager = mock()
    private val kycTiersQueries: KycTiersQueries = mock {
        on { isKycResubmissionRequired() } `it returns` Single.just(false)
    }
    private val lockboxDataManager: LockboxDataManager = mock()

    private val announcementList: AnnouncementList = mock()
    private val stxRegistration: BlockstackCampaignRegistration = mock()

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        whenever(prefs.selectedFiatCurrency).thenReturn("USD")

        subject = DashboardPresenter(
            AsyncDashboardDataCalculator(
                FiatExchangeRates(exchangeRateFactory, prefs),
                BalanceUpdater(
                    bchDataManager,
                    payloadDataManager
                ),
                transactionListDataManager
            ),
            prefs,
            exchangeRateFactory,
            stringUtils,
            rxBus,
            swipeToReceiveHelper,
            lockboxDataManager,
            currentTier,
            pitLinking,
            announcementList,
            stxRegistration
        )

        subject.initView(view)

        whenever(view.locale).thenReturn(Locale.US)
        whenever(currentTier.usersCurrentTier()).thenReturn(Single.just(1))
        whenever(prefs.pitToWalletLinkId).thenReturn("")
        whenever(pitLinking.isPitLinked()).thenReturn(Single.just(false))
    }

    @Test
    fun `onViewReady onboarding complete, no announcements`() {
        // Arrange
        whenever(stringUtils.getString(any())).thenReturn("")

        // updatePrices()
        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        whenever(currencyFormatManager.getFormattedFiatValueWithSymbol(any())).thenReturn("$2.00")

        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BTC),
                any()
            )
        ).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.ETHER), any())).thenReturn(
            4000.00
        )
        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BCH),
                any()
            )
        ).thenReturn(3000.00)

        // getOnboardingStatusObservable()
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefs.isOnboardingComplete).thenReturn(true)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // doOnSuccess { updateAllBalances() }
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        givenBalance(210.bitcoin())
        givenBalance(200.bitcoinCash())
        givenBalance(220.ether())
        givenBalance(100.lumens())
        givenBalance(50.usdPax())
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BTC, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.ETHER, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BCH, "USD")).thenReturn(2.0)

        // PieChartsState
        whenever(currencyFormatManager.getFiatSymbol(any(), any())).thenReturn("$")
        whenever(currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(any(), any()))
            .thenReturn("$2.00")

        whenever(currencyFormatManager.getFormattedValueWithUnit(any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedEthShortValueWithUnit(any(), any()))
            .thenReturn("$2.00")

        // Native Buy/Sell not available
        whenever(buyDataManager.isCoinifyAllowed).thenReturn(Observable.just(false))

        // No Lockbox, not available
        whenever(lockboxDataManager.hasLockbox()).thenReturn(Single.just(false))
        whenever(lockboxDataManager.isLockboxAvailable()).thenReturn(Single.just(false))

        // Act
        subject.onViewReady()

        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(view, atLeastOnce()).scrollToTop()

        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BTC), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.ETHER), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BCH), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.XLM), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.PAX), any())
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyBalanceQueries()
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        verify(view, atLeastOnce()).startWebsocketService()

        verify(swipeToReceiveHelper).storeAll()

        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(transactionListDataManager)
        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady onboarding not complete`() {
        // Arrange
        whenever(stringUtils.getString(any())).thenReturn("")

        // updatePrices()
        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        whenever(currencyFormatManager.getFormattedFiatValueWithSymbol(any())).thenReturn("$2.00")

        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BTC),
                any()
            )
        ).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.ETHER), any())).thenReturn(
            4000.00
        )
        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BCH),
                any()
            )
        ).thenReturn(3000.00)

        // getOnboardingStatusObservable()
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefs.isOnboardingComplete).thenReturn(false)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // doOnSuccess { updateAllBalances() }
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        givenBalance(210.bitcoin())
        givenBalance(200.bitcoinCash())
        givenBalance(220.ether())
        givenBalance(100.lumens())
        givenBalance(50.usdPax())
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BTC, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.ETHER, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BCH, "USD")).thenReturn(2.0)

        // PieChartsState
        whenever(currencyFormatManager.getFiatSymbol(any(), any())).thenReturn("$")
        whenever(currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(any(), any()))
            .thenReturn("$2.00")

        whenever(currencyFormatManager.getFormattedValueWithUnit(any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedEthShortValueWithUnit(any(), any()))
            .thenReturn("$2.00")

        // storeSwipeToReceiveAddresses()
        whenever(bchDataManager.getWalletTransactions(any(), any()))
            .thenReturn(Observable.empty())

        // No Native Buy/Sell announcement
        whenever(buyDataManager.isCoinifyAllowed).thenReturn(Observable.just(false))

        // No Lockbox, not available
        whenever(lockboxDataManager.hasLockbox()).thenReturn(Single.just(false))
        whenever(lockboxDataManager.isLockboxAvailable()).thenReturn(Single.just(false))

        // Act
        subject.onViewReady()

        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(view, atLeastOnce()).scrollToTop()
        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BTC), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.ETHER), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BCH), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.XLM), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.PAX), any())
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyBalanceQueries()
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        // no announcements allowed while onboarding hasn't been completed
        verify(swipeToReceiveHelper).storeAll()

        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(transactionListDataManager)
        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady onboarding complete native buy sell announcement`() {
        // Arrange
        whenever(stringUtils.getString(any())).thenReturn("")

        // updatePrices()
        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        whenever(currencyFormatManager.getFormattedFiatValueWithSymbol(any())).thenReturn("$2.00")

        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BTC),
                any()
            )
        ).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.ETHER), any())).thenReturn(
            4000.00
        )
        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BCH),
                any()
            )
        ).thenReturn(3000.00)

        // getOnboardingStatusObservable()
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefs.isOnboardingComplete).thenReturn(true)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // doOnSuccess { updateAllBalances() }
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        givenBalance(210.bitcoin())
        givenBalance(200.bitcoinCash())
        givenBalance(220.ether())
        givenBalance(100.lumens())
        givenBalance(50.usdPax())
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BTC, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.ETHER, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BCH, "USD")).thenReturn(2.0)

        // PieChartsState
        whenever(currencyFormatManager.getFiatSymbol(any(), any())).thenReturn("$")
        whenever(currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(any(), any()))
            .thenReturn("$2.00")

        whenever(currencyFormatManager.getFormattedValueWithUnit(any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedEthShortValueWithUnit(any(), any()))
            .thenReturn("$2.00")

        // storeSwipeToReceiveAddresses()
        whenever(bchDataManager.getWalletTransactions(any(), any()))
            .thenReturn(Observable.empty())

        // checkLatestAnnouncements()
        // No Native Buy/Sell announcement
        whenever(buyDataManager.isCoinifyAllowed).thenReturn(Observable.just(true))

        // No Lockbox, not available
        whenever(lockboxDataManager.hasLockbox()).thenReturn(Single.just(false))
        whenever(lockboxDataManager.isLockboxAvailable()).thenReturn(Single.just(false))

        // Act
        subject.onViewReady()

        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BTC), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.ETHER), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BCH), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.XLM), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.PAX), any())
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyBalanceQueries()
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).scrollToTop()

        verify(swipeToReceiveHelper).storeAll()

        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(transactionListDataManager)
        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady onboarding complete kyc announcement`() {
        // Arrange
        whenever(stringUtils.getString(any())).thenReturn("")

        // updatePrices()
        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        whenever(currencyFormatManager.getFormattedFiatValueWithSymbol(any())).thenReturn("$2.00")

        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.BTC), any()))
            .thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.ETHER), any()))
            .thenReturn(4000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.BCH), any()))
            .thenReturn(3000.00)

        // getOnboardingStatusObservable()
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefs.isOnboardingComplete).thenReturn(true)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // doOnSuccess { updateAllBalances() }
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        givenBalance(210.bitcoin())
        givenBalance(200.bitcoinCash())
        givenBalance(220.ether())
        givenBalance(100.lumens())
        givenBalance(50.usdPax())
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BTC, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.ETHER, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BCH, "USD")).thenReturn(2.0)

        // PieChartsState
        whenever(currencyFormatManager.getFiatSymbol(any(), any())).thenReturn("$")
        whenever(currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(any(), any()))
            .thenReturn("$2.00")

        whenever(currencyFormatManager.getFormattedValueWithUnit(any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedEthShortValueWithUnit(any(), any()))
            .thenReturn("$2.00")

        // storeSwipeToReceiveAddresses()
        whenever(bchDataManager.getWalletTransactions(any(), any()))
            .thenReturn(Observable.empty())

        // No Native Buy/Sell announcement
        whenever(buyDataManager.isCoinifyAllowed).thenReturn(Observable.just(true))

        whenever(kycTiersQueries.isKycInProgress()).thenReturn(Single.just(true))
        // No Lockbox, not available
        whenever(lockboxDataManager.hasLockbox()).thenReturn(Single.just(false))
        whenever(lockboxDataManager.isLockboxAvailable()).thenReturn(Single.just(false))

        // Act
        subject.onViewReady()

        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BTC), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.ETHER), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BCH), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.XLM), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.PAX), any())
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyBalanceQueries()
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        // KYC
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).scrollToTop()

        verify(swipeToReceiveHelper).storeAll()

        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(transactionListDataManager)
        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady onboarding complete with no native buy sell announcement`() {
        // Arrange
        whenever(stringUtils.getString(any())).thenReturn("")

        // updatePrices()
        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        whenever(currencyFormatManager.getFormattedFiatValueWithSymbol(any())).thenReturn("$2.00")

        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BTC),
                any()
            )
        ).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.ETHER), any())).thenReturn(
            4000.00
        )
        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BCH),
                any()
            )
        ).thenReturn(3000.00)

        // getOnboardingStatusObservable()
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefs.isOnboardingComplete).thenReturn(true)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // doOnSuccess { updateAllBalances() }
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        givenBalance(210.bitcoin())
        givenBalance(200.bitcoinCash())
        givenBalance(220.ether())
        givenBalance(100.lumens())
        givenBalance(50.usdPax())
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BTC, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.ETHER, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BCH, "USD")).thenReturn(2.0)

        // PieChartsState
        whenever(currencyFormatManager.getFiatSymbol(any(), any())).thenReturn("$")
        whenever(currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(any(), any()))
            .thenReturn("$2.00")

        whenever(currencyFormatManager.getFormattedValueWithUnit(any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedEthShortValueWithUnit(any(), any()))
            .thenReturn("$2.00")

        // storeSwipeToReceiveAddresses()
        whenever(bchDataManager.getWalletTransactions(any(), any()))
            .thenReturn(Observable.empty())

        // checkLatestAnnouncements()
        // No Native Buy/Sell announcement
        whenever(buyDataManager.isCoinifyAllowed).thenReturn(Observable.just(false))

        // No Lockbox, not available
        whenever(lockboxDataManager.hasLockbox()).thenReturn(Single.just(false))
        whenever(lockboxDataManager.isLockboxAvailable()).thenReturn(Single.just(false))

        // Act
        subject.onViewReady()

        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(view, atLeastOnce()).scrollToTop()
        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BTC), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.ETHER), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BCH), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.XLM), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.PAX), any())
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyBalanceQueries()
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        verify(swipeToReceiveHelper).storeAll()

        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(transactionListDataManager)
        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun updateBalances() {
        // Arrange
        whenever(stringUtils.getString(any())).thenReturn("")

        // updatePrices()
        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        whenever(currencyFormatManager.getFormattedFiatValueWithSymbol(any())).thenReturn("$2.00")

        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BTC),
                any()
            )
        ).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.ETHER), any())).thenReturn(
            4000.00
        )
        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BCH),
                any()
            )
        ).thenReturn(3000.00)

        // getOnboardingStatusObservable()
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefs.isOnboardingComplete).thenReturn(true)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // doOnSuccess { updateAllBalances() }
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        givenBalance(210.bitcoin())
        givenBalance(200.bitcoinCash())
        givenBalance(220.ether())
        givenBalance(100.lumens())
        givenBalance(50.usdPax())
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BTC, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.ETHER, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BCH, "USD")).thenReturn(2.0)

        // PieChartsState
        whenever(currencyFormatManager.getFiatSymbol(any(), any())).thenReturn("$")
        whenever(currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(any(), any()))
            .thenReturn("$2.00")

        whenever(currencyFormatManager.getFormattedValueWithUnit(any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedEthShortValueWithUnit(any(), any()))
            .thenReturn("$2.00")

        // storeSwipeToReceiveAddresses()
        whenever(bchDataManager.getWalletTransactions(any(), any()))
            .thenReturn(Observable.empty())
        // No Lockbox, not available
        whenever(lockboxDataManager.hasLockbox()).thenReturn(Single.just(false))
        whenever(lockboxDataManager.isLockboxAvailable()).thenReturn(Single.just(false))

        // Act
        subject.updateBalances()

        // Assert
        verify(view, atLeastOnce()).scrollToTop()

        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BTC), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.ETHER), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BCH), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.XLM), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.PAX), any())
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyBalanceQueries()
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(transactionListDataManager)
        verifyNoMoreInteractions(exchangeRateFactory)
    }

    private fun givenBalance(
        cryptoValue: CryptoValue
    ) {
        whenever(transactionListDataManager.totalBalance(cryptoValue.currency)).thenReturn(
            Single.just(
                TotalBalance.Balance(
                    spendable = cryptoValue,
                    watchOnly = cryptoValue.toZero(),
                    coldStorage = cryptoValue.toZero()
                )
            )
        )
    }

    private fun verifyBalanceQueries() {
        verify(transactionListDataManager).totalBalance(CryptoCurrency.BTC)
        verify(transactionListDataManager).totalBalance(CryptoCurrency.BCH)
        verify(transactionListDataManager).totalBalance(CryptoCurrency.ETHER)
        verify(transactionListDataManager).totalBalance(CryptoCurrency.XLM)
        verify(transactionListDataManager).totalBalance(CryptoCurrency.PAX)
    }

    @Test
    fun onViewDestroyed() {
        // Arrange

        // Act
        subject.onViewDestroyed()
        // Assert
        verify(rxBus).unregister(eq(MetadataEvent::class.java), anyOrNull())
    }

    @Test
    fun `should propagate the correct fiat and crypto currency to the view`() {
        // Arrange
        mockDependencies()

        // Act
        subject.onViewReady()
        subject.startSwapOrKyc(CryptoCurrency.ETHER)
        // Assert
        verify(view).startSwap("USD", CryptoCurrency.ETHER)
    }

    @Test
    fun `should go to swap if tier is higher or equal to 1`() {
        // Arrange
        mockDependencies()
        whenever(currentTier.usersCurrentTier()).thenReturn(Single.just(1))

        // Act
        subject.onViewReady()
        subject.startSwapOrKyc(CryptoCurrency.ETHER)
        // Assert
        verify(view).startSwap("USD", CryptoCurrency.ETHER)
        verify(view, never()).startKycFlow(CampaignType.Swap)
    }

    @Test
    fun `should go to kyc if tier is zero`() {
        // Arrange
        mockDependencies()
        whenever(currentTier.usersCurrentTier()).thenReturn(Single.just(0))
        // Act
        subject.onViewReady()
        subject.startSwapOrKyc(CryptoCurrency.ETHER)
        // Assert
        verify(view, never()).startSwap(any(), any())
        verify(view).startKycFlow(CampaignType.Swap)
    }

    private fun mockDependencies() {
        whenever(stringUtils.getString(any())).thenReturn("")

        // updatePrices()
        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        whenever(currencyFormatManager.getFormattedFiatValueWithSymbol(any())).thenReturn("$2.00")

        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.BTC), any()))
            .thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.ETHER), any()))
            .thenReturn(4000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.BCH), any()))
            .thenReturn(3000.00)

        // getOnboardingStatusObservable()
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefs.isOnboardingComplete).thenReturn(true)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // doOnSuccess { updateAllBalances() }
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        givenBalance(210.bitcoin())
        givenBalance(200.bitcoinCash())
        givenBalance(220.ether())
        givenBalance(100.lumens())
        givenBalance(50.usdPax())
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BTC, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.ETHER, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BCH, "USD")).thenReturn(2.0)
        // PieChartsState
        whenever(currencyFormatManager.getFiatSymbol(any(), any())).thenReturn("$")
        whenever(currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(any(), any()))
            .thenReturn("$2.00")

        whenever(currencyFormatManager.getFormattedValueWithUnit(any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedEthShortValueWithUnit(any(), any()))
            .thenReturn("$2.00")

        // storeSwipeToReceiveAddresses()
        whenever(bchDataManager.getWalletTransactions(any(), any()))
            .thenReturn(Observable.empty())

        // checkLatestAnnouncements()
        // No Native Buy/Sell announcement
        whenever(buyDataManager.isCoinifyAllowed).thenReturn(Observable.just(true))

        whenever(kycTiersQueries.isKycInProgress()).thenReturn(Single.just(true))
        // No Lockbox, not available
        whenever(lockboxDataManager.hasLockbox()).thenReturn(Single.just(false))
        whenever(lockboxDataManager.isLockboxAvailable()).thenReturn(Single.just(false))
    }
}