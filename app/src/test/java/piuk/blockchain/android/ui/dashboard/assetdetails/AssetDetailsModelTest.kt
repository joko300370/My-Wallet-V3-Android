package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.nabu.models.data.RecurringBuyState
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.amshove.kluent.`it returns`
import org.amshove.kluent.itReturns
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.coincore.btc.BtcAsset
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.exchangerate.TimeSpan

class AssetDetailsModelTest {
    private lateinit var model: AssetDetailsModel
    private val defaultState = AssetDetailsState(
        asset = BtcAsset(
            payloadManager = mock(),
            sendDataManager = mock(),
            feeDataManager = mock(),
            custodialManager = mock(),
            exchangeRates = mock(),
            historicRates = mock(),
            currencyPrefs = mock(),
            labels = mock(),
            pitLinking = mock(),
            crashLogger = mock(),
            walletPreferences = mock(),
            offlineAccounts = mock(),
            identity = mock(),
            coinsWebsocket = mock(),
            features = mock()
        )
    )

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() } `it returns` false
    }

    private val interactor: AssetDetailsInteractor = mock()

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = AssetDetailsModel(defaultState, Schedulers.io(), interactor, environmentConfig, mock())
    }

    @Test
    fun `load asset success`() {
        val assetDisplayMap = mapOf(
            AssetFilter.Custodial to AssetDisplayInfo(mock(), mock(), mock(), mock(), emptySet())
        )
        val recurringBuy = RecurringBuy(
            "123", RecurringBuyState.ACTIVE, RecurringBuyFrequency.BI_WEEKLY, mock(),
            PaymentMethodType.BANK_TRANSFER, "321", FiatValue.zero("EUR"), mock(), mock()
        )
        val recurringBuys: List<RecurringBuy> = listOf(
            recurringBuy
        )
        val expectedRecurringBuyMap = mapOf(
            "123" to recurringBuy
        )
        val price = "1000 BTC"
        val priceSeries = listOf(PriceDatum())
        val asset: CryptoAsset = mock {
            on { asset } itReturns CryptoCurrency.BTC
        }

        val timeSpan = TimeSpan.DAY

        whenever(interactor.loadAssetDetails(asset)).thenReturn(Single.just(assetDisplayMap))
        whenever(interactor.loadExchangeRate(asset)).thenReturn(Single.just(price))
        whenever(interactor.loadHistoricPrices(asset, timeSpan)).thenReturn(Single.just(priceSeries))
        whenever(interactor.loadRecurringBuysForAsset(asset.asset.networkTicker)).thenReturn(Single.just(recurringBuys))

        val testObserver = model.state.test()
        model.process(LoadAsset(asset))

        testObserver.assertValueAt(0, defaultState)
        testObserver.assertValueAt(1, defaultState.copy(asset = asset, assetDisplayMap = mapOf()))
        testObserver.assertValueAt(
            2, defaultState.copy(
                asset = asset,
                assetDisplayMap = mapOf(),
                chartLoading = true
            )
        )
        testObserver.assertValueAt(
            3, defaultState.copy(
                asset = asset,
                assetDisplayMap = mapOf(),
                chartData = priceSeries,
                chartLoading = false
            )
        )
        testObserver.assertValueAt(
            4, defaultState.copy(
                asset = asset,
                assetDisplayMap = mapOf(),
                chartData = priceSeries,
                chartLoading = false,
                assetFiatPrice = price
            )
        )
        testObserver.assertValueAt(
            5, defaultState.copy(
                asset = asset,
                chartData = priceSeries,
                chartLoading = false,
                assetFiatPrice = price,
                assetDisplayMap = assetDisplayMap
            )
        )
        testObserver.assertValueAt(
            6, defaultState.copy(
                asset = asset,
                chartData = priceSeries,
                chartLoading = false,
                assetFiatPrice = price,
                assetDisplayMap = assetDisplayMap,
                recurringBuys = expectedRecurringBuyMap
            )
        )

        verify(interactor).loadAssetDetails(asset)
        verify(interactor).loadExchangeRate(asset)
        verify(interactor).loadRecurringBuysForAsset(asset.asset.networkTicker)
        verify(interactor).loadHistoricPrices(asset, timeSpan)

        verifyNoMoreInteractions(interactor)
    }
}