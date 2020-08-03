package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.testutils.rxInit
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Single
import junit.framework.Assert.assertEquals
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.androidcore.data.charts.TimeSpan
import java.util.Locale

class AssetDetailsInteractorTest {

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    private lateinit var interactor: AssetDetailsInteractor

    private val totalGroup: AccountGroup = mock()
    private val nonCustodialGroup: AccountGroup = mock()
    private val custodialGroup: AccountGroup = mock()
    private val interestGroup: AccountGroup = mock()
    private val interestRate: Double = 5.0
    private val interestEnabled: Boolean = true
    private val asset: CryptoAsset = mock()
    private val featureFlagMock: FeatureFlag = mock()

    @Before
    fun setUp() {
        interactor = AssetDetailsInteractor(featureFlagMock, mock(), mock())

        whenever(asset.accountGroup(AssetFilter.All)).thenReturn(Single.just(totalGroup))
        whenever(asset.accountGroup(AssetFilter.NonCustodial)).thenReturn(
            Single.just(nonCustodialGroup))
        whenever(asset.accountGroup(AssetFilter.Custodial)).thenReturn(Single.just(custodialGroup))
        whenever(asset.accountGroup(AssetFilter.Interest)).thenReturn(Single.just(interestGroup))
        whenever(featureFlagMock.enabled).thenReturn(Single.just(interestEnabled))

        Locale.setDefault(Locale.US)
    }

    @Test
    fun `cryptoBalance,fiatBalance & interestBalance return the right values`() {

        val price = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 56478.99.toBigDecimal())

        val walletCrypto = CryptoValue(CryptoCurrency.BTC, 548621.toBigInteger())
        val custodialCrypto = CryptoValue.ZeroBtc
        val interestCrypto = CryptoValue.ZeroBtc
        val totalCrypto = walletCrypto + custodialCrypto + interestCrypto

        val walletFiat = FiatValue.fromMinor("USD", 30985)
        val custodialFiat = FiatValue.fromMinor("USD", 0)
        val interestFiat = FiatValue.fromMinor("USD", 0)
        val totalFiat = walletFiat + custodialFiat + interestFiat

        val expectedResult = mapOf(
            AssetFilter.All to AssetDisplayInfo(totalGroup, totalCrypto, totalFiat, emptySet(),
                AssetDetailsInteractor.NOT_USED),
            AssetFilter.NonCustodial to AssetDisplayInfo(nonCustodialGroup, walletCrypto,
                walletFiat, emptySet(),
                AssetDetailsInteractor.NOT_USED),
            AssetFilter.Custodial to AssetDisplayInfo(custodialGroup, custodialCrypto,
                custodialFiat, emptySet(),
                AssetDetailsInteractor.NOT_USED),
            AssetFilter.Interest to AssetDisplayInfo(interestGroup, interestCrypto, interestFiat,
                emptySet(),
                interestRate)
        )

        whenever(asset.exchangeRate()).thenReturn(Single.just(price))

        whenever(totalGroup.balance).thenReturn(Single.just(totalCrypto))
        whenever(nonCustodialGroup.balance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.balance).thenReturn(Single.just(custodialCrypto))
        whenever(interestGroup.balance).thenReturn(Single.just(interestCrypto))
        whenever(asset.interestRate()).thenReturn(Single.just(interestRate))

        whenever(custodialGroup.accounts).thenReturn(listOf(mock()))
        whenever(nonCustodialGroup.accounts).thenReturn(listOf(mock()))
        whenever(custodialGroup.isFunded).thenReturn(true)
        whenever(nonCustodialGroup.isFunded).thenReturn(true)

        whenever(interestGroup.accounts).thenReturn(listOf(mock()))
        whenever(interestGroup.isFunded).thenReturn(true)

        val v = interactor.loadAssetDetails(asset)
            .test()
            .values()

        // Using assertResult(expectedResult) instead of fetching the values and checking them results in
        // an 'AssertionException Not completed' result. I have no clue why; changing the matchers to not use all
        // three possible enum values changes the failure into an expected 'Failed, not equal' result (hence the
        // doAnswer() nonsense instead of eq() etc - I tried many things)
        // All very strange.
        assertEquals(expectedResult, v[0])
    }

    @Test
    fun `custodial not show if unfunded`() {

        val price = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 56478.99.toBigDecimal())

        val walletCrypto = CryptoValue(CryptoCurrency.BTC, 548621.toBigInteger())
        val custodialCrypto = CryptoValue.ZeroBtc
        val interestCrypto = CryptoValue.ZeroBtc
        val totalCrypto = walletCrypto + custodialCrypto + interestCrypto

        val walletFiat = FiatValue.fromMinor("USD", 30985)
        val custodialFiat = FiatValue.fromMinor("USD", 0)
        val interestFiat = FiatValue.fromMinor("USD", 0)
        val totalFiat = walletFiat + custodialFiat + interestFiat

        val expectedResult = mapOf(
            AssetFilter.All to AssetDisplayInfo(totalGroup, totalCrypto, totalFiat, emptySet()),
            AssetFilter.NonCustodial to AssetDisplayInfo(nonCustodialGroup, walletCrypto,
                walletFiat, emptySet())
        )

        whenever(asset.exchangeRate()).thenReturn(Single.just(price))

        whenever(totalGroup.balance).thenReturn(Single.just(totalCrypto))
        whenever(nonCustodialGroup.balance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.balance).thenReturn(Single.just(custodialCrypto))
        whenever(interestGroup.balance).thenReturn(Single.just(interestCrypto))
        whenever(asset.interestRate()).thenReturn(Single.just(interestRate))

        whenever(nonCustodialGroup.accounts).thenReturn(listOf(mock()))
        whenever(nonCustodialGroup.isFunded).thenReturn(true)

        whenever(custodialGroup.isFunded).thenReturn(false)
        whenever(interestGroup.isFunded).thenReturn(false)

        val v = interactor.loadAssetDetails(asset)
            .test()
            .values()

        // Using assertResult(expectedResult) instead of fetching the values and checking them results in
        // an 'AssertionException Not completed' result. I have no clue why; changing the matchers to not use all
        // three possible enum values changes the failure into an expected 'Failed, not equal' result (hence the
        // doAnswer() nonsense instead of eq() etc - I tried many things)
        // All very strange.
        assertEquals(expectedResult, v[0])
    }

    @Test
    fun `interest doesn't show if unfunded`() {

        val price = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 56478.99.toBigDecimal())

        val walletCrypto = CryptoValue(CryptoCurrency.BTC, 548621.toBigInteger())
        val custodialCrypto = CryptoValue.ZeroBtc
        val interestCrypto = CryptoValue.ZeroBtc
        val totalCrypto = walletCrypto + custodialCrypto + interestCrypto

        val walletFiat = FiatValue.fromMinor("USD", 30985)
        val custodialFiat = FiatValue.fromMinor("USD", 0)
        val interestFiat = FiatValue.fromMinor("USD", 0)
        val totalFiat = walletFiat + custodialFiat + interestFiat

        val expectedResult = mapOf(
            AssetFilter.All to AssetDisplayInfo(totalGroup, totalCrypto, totalFiat, emptySet(),
                AssetDetailsInteractor.NOT_USED),
            AssetFilter.NonCustodial to AssetDisplayInfo(nonCustodialGroup, walletCrypto,
                walletFiat, emptySet(),
                AssetDetailsInteractor.NOT_USED),
            AssetFilter.Custodial to AssetDisplayInfo(custodialGroup, custodialCrypto,
                custodialFiat, emptySet(),
                AssetDetailsInteractor.NOT_USED)
        )

        whenever(asset.exchangeRate()).thenReturn(Single.just(price))

        whenever(totalGroup.balance).thenReturn(Single.just(totalCrypto))
        whenever(nonCustodialGroup.balance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.balance).thenReturn(Single.just(custodialCrypto))
        whenever(interestGroup.balance).thenReturn(Single.just(interestCrypto))
        whenever(asset.interestRate()).thenReturn(Single.just(interestRate))

        whenever(custodialGroup.accounts).thenReturn(listOf(mock()))
        whenever(nonCustodialGroup.accounts).thenReturn(listOf(mock()))
        whenever(nonCustodialGroup.isFunded).thenReturn(true)
        whenever(custodialGroup.isFunded).thenReturn(true)
        whenever(interestGroup.isFunded).thenReturn(false)

        val v = interactor.loadAssetDetails(asset)
            .test()
            .values()

        // Using assertResult(expectedResult) instead of fetching the values and checking them results in
        // an 'AssertionException Not completed' result. I have no clue why; changing the matchers to not use all
        // three possible enum values changes the failure into an expected 'Failed, not equal' result (hence the
        // doAnswer() nonsense instead of eq() etc - I tried many things)
        // All very strange.
        assertEquals(expectedResult, v[0])
    }

    @Test
    fun `cryptoBalance, fiatBalance & interestBalance are never returned if exchange rate fails`() {
        whenever(asset.exchangeRate()).thenReturn(Single.error(Throwable()))

        val walletCrypto = CryptoValue(CryptoCurrency.BTC, 548621.toBigInteger())
        val custodialCrypto = CryptoValue.ZeroBtc
        val interestCrypto = CryptoValue.ZeroBtc
        val totalCrypto = walletCrypto + custodialCrypto + interestCrypto

        whenever(totalGroup.balance).thenReturn(Single.just(totalCrypto))
        whenever(nonCustodialGroup.balance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.balance).thenReturn(Single.just(custodialCrypto))
        whenever(interestGroup.balance).thenReturn(Single.just(interestCrypto))
        whenever(asset.interestRate()).thenReturn(Single.just(interestRate))

        val testObserver = interactor.loadAssetDetails(asset)
            .test()

        testObserver.assertNoValues()
    }

    @Test
    fun `cryptoBalance & fiatBalance never return if interest fails`() {
        val walletCrypto = CryptoValue(CryptoCurrency.BTC, 548621.toBigInteger())
        val custodialCrypto = CryptoValue.ZeroBtc
        val totalCrypto = walletCrypto + custodialCrypto

        val price = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 5647899.toBigDecimal())

        whenever(asset.exchangeRate()).thenReturn(Single.just(price))
        whenever(asset.accountGroup(AssetFilter.Interest)).thenReturn(Single.error(Throwable()))

        whenever(totalGroup.balance).thenReturn(Single.just(totalCrypto))
        whenever(nonCustodialGroup.balance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.balance).thenReturn(Single.just(custodialCrypto))
        whenever(asset.interestRate()).thenReturn(Single.just(interestRate))

        val testObserver = interactor.loadAssetDetails(asset)
            .test()
        testObserver.assertNoValues()
    }

    @Test
    fun `cryptoBalance, fiatBalance & interestBalance are never returned if totalBalance fails`() {
        val walletCrypto = CryptoValue(CryptoCurrency.BTC, 548621.toBigInteger())
        val custodialCrypto = CryptoValue.ZeroBtc
        val interestCrypto = CryptoValue.ZeroBtc

        val price = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 5647899.toBigDecimal())
        whenever(asset.exchangeRate()).thenReturn(Single.just(price))

        whenever(totalGroup.balance).thenReturn(Single.error(Throwable()))
        whenever(nonCustodialGroup.balance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.balance).thenReturn(Single.just(custodialCrypto))
        whenever(interestGroup.balance).thenReturn(Single.just(interestCrypto))
        whenever(asset.interestRate()).thenReturn(Single.just(interestRate))

        val testObserver = interactor.loadAssetDetails(asset)
            .test()
        testObserver.assertNoValues()
    }

    @Test
    fun `exchange rate is the right one`() {
        val price = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 56478.99.toBigDecimal())

        whenever(asset.exchangeRate()).thenReturn(Single.just(price))

        val testObserver = interactor.loadExchangeRate(asset).test()
        testObserver.assertValue("$56,478.99").assertValueCount(1)
    }

    @Test
    fun `historic prices are returned properly`() {
        whenever(asset.historicRateSeries(TimeSpan.DAY, TimeInterval.FIFTEEN_MINUTES))
            .thenReturn(Single.just(listOf(
                PriceDatum(5556, 2.toDouble(), volume24h = 0.toDouble()),
                PriceDatum(587, 22.toDouble(), volume24h = 0.toDouble()),
                PriceDatum(6981, 23.toDouble(), volume24h = 4.toDouble())
            )))

        val testObserver = interactor.loadHistoricPrices(asset, TimeSpan.DAY).test()

        testObserver.assertValue(listOf(
            PriceDatum(5556, 2.toDouble(), volume24h = 0.toDouble()),
            PriceDatum(587, 22.toDouble(), volume24h = 0.toDouble()),
            PriceDatum(6981, 23.toDouble(), volume24h = 4.toDouble())
        )).assertValueCount(1)
    }

    @Test
    fun `when historic prices api returns error, empty list should be returned`() {
        whenever(asset.historicRateSeries(TimeSpan.DAY, TimeInterval.FIFTEEN_MINUTES))
            .thenReturn(Single.error(Throwable()))

        val testObserver = interactor.loadHistoricPrices(asset, TimeSpan.DAY).test()

        testObserver.assertValue(emptyList()).assertValueCount(1)
    }
}