package piuk.blockchain.android.ui.home

import com.blockchain.android.testutils.rxInit
import com.blockchain.swap.nabu.models.nabu.NabuUser
import com.blockchain.swap.nabu.models.nabu.TierLevels
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import piuk.blockchain.android.campaign.SunriverCampaignRegistration
import com.blockchain.lockbox.data.LockboxDataManager
import com.blockchain.logging.CrashLogger
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.deeplink.DeepLinkProcessor
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs

class MainPresenterTest {

    private lateinit var subject: MainPresenter

    private val view: MainView = mock()
    private val prefs: PersistentPrefs = mock()
    private val accessState: AccessState = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val credentialsWiper: CredentialsWiper = mock()
    private val exchangeRateFactory: ExchangeRateDataManager = mock()
    private val environmentSettings: EnvironmentConfig = mock()
    private val kycStatusHelper: KycStatusHelper = mock()
    private val lockboxDataManager: LockboxDataManager = mock()
    private val deepLinkProcessor: DeepLinkProcessor = mock()
    private val sunriverCampaignRegistration: SunriverCampaignRegistration = mock()
    private val xlmDataManager: XlmDataManager = mock()
    private val pitLinking: PitLinking = mock()
    private val featureFlag: FeatureFlag = mock()
    private val simpleBuySync: SimpleBuySyncFactory = mock()
    private val crashLogger: CrashLogger = mock()
    private val nabuDatamanager: NabuDataManager = mock()

    private val nabuToken: NabuToken = mock {
        on { fetchNabuToken() } `it returns` Single.just(NabuOfflineTokenResponse(
            "",
            ""))
    }

    private val userTierZero: NabuUser = mock {
        on { tiers } `it returns` TierLevels(0, 0, 0)
    }

    private val userTierOne: NabuUser = mock {
        on { tiers } `it returns` TierLevels(1, 1, 2)
    }

    private val userTierTwo: NabuUser = mock {
        on { tiers } `it returns` TierLevels(2, 2, 2)
    }

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = MainPresenter(
            prefs = prefs,
            accessState = accessState,
            credentialsWiper = credentialsWiper,
            payloadDataManager = payloadDataManager,
            exchangeRateFactory = exchangeRateFactory,
            environmentSettings = environmentSettings,
            kycStatusHelper = kycStatusHelper,
            lockboxDataManager = lockboxDataManager,
            deepLinkProcessor = deepLinkProcessor,
            sunriverCampaignRegistration = sunriverCampaignRegistration,
            xlmDataManager = xlmDataManager,
            pitFeatureFlag = featureFlag,
            simpleBuySync = simpleBuySync,
            pitLinking = pitLinking,
            nabuToken = nabuToken,
            nabuDataManager = nabuDatamanager,
            crashLogger = crashLogger,
            simpleBuyAvailability = mock(),
            cacheCredentialsWiper = mock(),
            analytics = mock(),
            appUtil = mock()
        )

        subject.attachView(view)
    }

    @Test
    fun `should go to kyc if tier is zero and intro completed`() {
        // Arrange
        whenever(prefs.selectedFiatCurrency).thenReturn("USD")
        whenever(prefs.swapIntroCompleted).thenReturn(true)
        whenever(nabuDatamanager.getUser(any())).thenReturn(Single.just(userTierZero))

        // Act
        subject.onViewReady()
        subject.startSwapOrKyc(toCurrency = CryptoCurrency.ETHER, fromCurrency = CryptoCurrency.BTC)

        // Assert
        verify(view, never()).launchSwap(any(), any(), any())
        verify(view, never()).launchSwapIntro()
        verify(view).launchPendingVerificationScreen(CampaignType.Swap)
    }

    @Test
    fun `should go to swap if tier is equal to 1`() {
        // Arrange
        whenever(prefs.selectedFiatCurrency).thenReturn("USD")
        whenever(nabuDatamanager.getUser(any())).thenReturn(Single.just(userTierOne))
        // Act
        subject.onViewReady()
        subject.startSwapOrKyc(toCurrency = CryptoCurrency.ETHER, fromCurrency = CryptoCurrency.BTC)

        // Assert
        verify(view).launchSwap(defCurrency = "USD",
            toCryptoCurrency = CryptoCurrency.ETHER,
            fromCryptoCurrency = CryptoCurrency.BTC)
        verify(view, never()).launchKyc(CampaignType.Swap)
        verify(view, never()).launchSwapIntro()
    }

    @Test
    fun `should go to swap if tier is higher that 1`() {
        // Arrange
        whenever(prefs.selectedFiatCurrency).thenReturn("USD")
        whenever(nabuDatamanager.getUser(any())).thenReturn(Single.just(userTierTwo))

        // Act
        subject.onViewReady()
        subject.startSwapOrKyc(toCurrency = CryptoCurrency.ETHER, fromCurrency = CryptoCurrency.BTC)

        // Assert
        verify(view).launchSwap("USD",
            toCryptoCurrency = CryptoCurrency.ETHER,
            fromCryptoCurrency = CryptoCurrency.BTC
        )
        verify(view, never()).launchKyc(CampaignType.Swap)
        verify(view, never()).launchSwapIntro()
    }

    @Test
    fun `should go to swap intro if tier is 0 and intro hasn't completed`() {
        // Arrange
        whenever(prefs.selectedFiatCurrency).thenReturn("USD")
        whenever(prefs.swapIntroCompleted).thenReturn(false)
        whenever(nabuDatamanager.getUser(any())).thenReturn(Single.just(userTierZero))

        // Act
        subject.onViewReady()
        subject.startSwapOrKyc(toCurrency = CryptoCurrency.ETHER, fromCurrency = CryptoCurrency.BTC)

        // Assert
        verify(view, never()).launchSwap("USD", CryptoCurrency.ETHER)
        verify(view, never()).launchKyc(CampaignType.Swap)
        verify(view).launchSwapIntro()
    }
}
