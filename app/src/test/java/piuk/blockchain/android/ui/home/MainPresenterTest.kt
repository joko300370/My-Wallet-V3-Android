package piuk.blockchain.android.ui.home

import com.blockchain.android.testutils.rxInit
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.Tiers
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import piuk.blockchain.android.ui.kyc.sunriver.SunriverCampaignHelper
import com.blockchain.lockbox.data.LockboxDataManager
import com.blockchain.logging.CrashLogger
import com.blockchain.remoteconfig.ABTestExperiment
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.models.NabuOfflineTokenResponse
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.sunriver.XlmDataManager
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.payload.PayloadManagerWiper
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.deeplink.DeepLinkProcessor
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import com.blockchain.swap.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.utils.AppUtil

class MainPresenterTest {

    private lateinit var subject: MainPresenter

    private val view: MainView = mock()

    private val prefs: PersistentPrefs = mock()
    private val appUtil: AppUtil = mock()
    private val accessState: AccessState = mock()
    private val payloadManagerWiper: PayloadManagerWiper = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val buyDataManager: BuyDataManager = mock()
    private val dynamicFeeCache: DynamicFeeCache = mock()
    private val exchangeRateFactory: ExchangeRateDataManager = mock()
    private val rxBus: RxBus = mock()
    private val feeDataManager: FeeDataManager = mock()
    private val ethDataManager: EthDataManager = mock()
    private val paxAccount: Erc20Account = mock()
    private val bchDataManager: BchDataManager = mock()
    private val currencyState: CurrencyState = mock()
    private val metadataManager: MetadataManager = mock()
    private val stringUtils: StringUtils = mock()
    private val shapeShiftDataManager: ShapeShiftDataManager = mock()
    private val environmentSettings: EnvironmentConfig = mock()
    private val coinifyDataManager: CoinifyDataManager = mock()
    private val exchangeService: ExchangeService = mock()
    private val kycStatusHelper: KycStatusHelper = mock()
    private val lockboxDataManager: LockboxDataManager = mock()
    private val deepLinkProcessor: DeepLinkProcessor = mock()
    private val sunriverCampaignHelper: SunriverCampaignHelper = mock()
    private val xlmDataManager: XlmDataManager = mock()
    private val pitLinking: PitLinking = mock()
    private val featureFlag: FeatureFlag = mock()
    private val crashLogger: CrashLogger = mock()
    private val nabuDatamanager: NabuDataManager = mock()
    private val abTesting: ABTestExperiment = mock()

    private val nabuToken: NabuToken = mock {
        on { fetchNabuToken() } `it returns` Single.just(NabuOfflineTokenResponse("", ""))
    }

    private val userTierZero: NabuUser = mock {
        on { tiers } `it returns` Tiers(0, 0, 0)
    }

    private val userTierOne: NabuUser = mock {
        on { tiers } `it returns` Tiers(1, 1, 2)
    }

    private val userTierTwo: NabuUser = mock {
        on { tiers } `it returns` Tiers(2, 2, 2)
    }

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = MainPresenter(
            prefs,
            appUtil,
            accessState,
            payloadManagerWiper,
            payloadDataManager,
            buyDataManager,
            dynamicFeeCache,
            exchangeRateFactory,
            rxBus,
            feeDataManager,
            ethDataManager,
            bchDataManager,
            currencyState,
            metadataManager,
            stringUtils,
            shapeShiftDataManager,
            environmentSettings,
            coinifyDataManager,
            exchangeService,
            kycStatusHelper,
            lockboxDataManager,
            deepLinkProcessor,
            sunriverCampaignHelper,
            xlmDataManager,
            paxAccount,
            featureFlag,
            abTesting,
            pitLinking,
            nabuToken,
            nabuDatamanager,
            crashLogger
        )

        subject.initView(view)
    }

    @Test
    fun `should go to kyc if tier is zero and intro completed`() {
        // Arrange
        whenever(prefs.selectedFiatCurrency).thenReturn("USD")
        whenever(prefs.swapIntroCompleted).thenReturn(true)
        whenever(nabuDatamanager.getUser(any())).thenReturn(Single.just(userTierZero))

        // Act
        subject.onViewReady()
        subject.startSwapOrKyc(CryptoCurrency.ETHER)

        // Assert
        verify(view, never()).launchSwap(any(), any())
        verify(view, never()).launchSwapIntro()
        verify(view).launchKyc(CampaignType.Swap)
    }

    @Test
    fun `should go to swap if tier is equal to 1`() {
        // Arrange
        whenever(prefs.selectedFiatCurrency).thenReturn("USD")
        whenever(nabuDatamanager.getUser(any())).thenReturn(Single.just(userTierOne))
        // Act
        subject.onViewReady()
        subject.startSwapOrKyc(CryptoCurrency.ETHER)

        // Assert
        verify(view).launchSwap("USD", CryptoCurrency.ETHER)
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
        subject.startSwapOrKyc(CryptoCurrency.ETHER)

        // Assert
        verify(view).launchSwap("USD", CryptoCurrency.ETHER)
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
        subject.startSwapOrKyc(CryptoCurrency.ETHER)

        // Assert
        verify(view, never()).launchSwap("USD", CryptoCurrency.ETHER)
        verify(view, never()).launchKyc(CampaignType.Swap)
        verify(view).launchSwapIntro()
    }
}
