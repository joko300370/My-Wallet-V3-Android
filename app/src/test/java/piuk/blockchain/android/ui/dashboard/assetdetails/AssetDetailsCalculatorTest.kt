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
import io.reactivex.Maybe
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

class AssetDetailsCalculatorTest {

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    private lateinit var calculator: AssetDetailsCalculator

    private val token: CryptoAsset = mock()
    private val totalGroup: AccountGroup = mock()
    private val nonCustodialGroup: AccountGroup = mock()
    private val custodialGroup: AccountGroup = mock()
    private val interestGroup: AccountGroup = mock()
    private val interestRate: Double = 5.0
    private val interestEnabled: Boolean = true

    @Before
    fun setUp() {
        val featureFlagMock: FeatureFlag = mock()
        calculator = AssetDetailsCalculator(featureFlagMock)

        whenever(token.accountGroup(AssetFilter.All)).thenReturn(Maybe.just(totalGroup))
        whenever(token.accountGroup(AssetFilter.NonCustodial)).thenReturn(Maybe.just(nonCustodialGroup))
        whenever(token.accountGroup(AssetFilter.Custodial)).thenReturn(Maybe.just(custodialGroup))
        whenever(token.accountGroup(AssetFilter.Interest)).thenReturn(Maybe.just(interestGroup))
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
                AssetDetailsCalculator.NOT_USED),
            AssetFilter.NonCustodial to AssetDisplayInfo(nonCustodialGroup, walletCrypto, walletFiat, emptySet(),
                AssetDetailsCalculator.NOT_USED),
            AssetFilter.Custodial to AssetDisplayInfo(custodialGroup, custodialCrypto, custodialFiat, emptySet(),
                AssetDetailsCalculator.NOT_USED),
            AssetFilter.Interest to AssetDisplayInfo(interestGroup, interestCrypto, interestFiat, emptySet(),
                interestRate)
        )

        whenever(token.exchangeRate()).thenReturn(Single.just(price))

        whenever(totalGroup.balance).thenReturn(Single.just(totalCrypto))
        whenever(nonCustodialGroup.balance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.balance).thenReturn(Single.just(custodialCrypto))
        whenever(interestGroup.balance).thenReturn(Single.just(interestCrypto))
        whenever(token.interestRate()).thenReturn(Single.just(interestRate))

        whenever(custodialGroup.accounts).thenReturn(listOf(mock()))
        whenever(nonCustodialGroup.accounts).thenReturn(listOf(mock()))
        whenever(custodialGroup.isFunded).thenReturn(true)
        whenever(nonCustodialGroup.isFunded).thenReturn(true)

        whenever(interestGroup.accounts).thenReturn(listOf(mock()))
        whenever(interestGroup.isFunded).thenReturn(true)

        calculator.token.accept(token)

        val v = calculator.assetDisplayDetails
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
            AssetFilter.NonCustodial to AssetDisplayInfo(nonCustodialGroup, walletCrypto, walletFiat, emptySet())
        )

        whenever(token.exchangeRate()).thenReturn(Single.just(price))

        whenever(totalGroup.balance).thenReturn(Single.just(totalCrypto))
        whenever(nonCustodialGroup.balance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.balance).thenReturn(Single.just(custodialCrypto))
        whenever(interestGroup.balance).thenReturn(Single.just(interestCrypto))
        whenever(token.interestRate()).thenReturn(Single.just(interestRate))

        whenever(nonCustodialGroup.accounts).thenReturn(listOf(mock()))
        whenever(nonCustodialGroup.isFunded).thenReturn(true)

        whenever(custodialGroup.isFunded).thenReturn(false)
        whenever(interestGroup.isFunded).thenReturn(false)

        calculator.token.accept(token)

        val v = calculator.assetDisplayDetails
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
                AssetDetailsCalculator.NOT_USED),
            AssetFilter.NonCustodial to AssetDisplayInfo(nonCustodialGroup, walletCrypto, walletFiat, emptySet(),
                AssetDetailsCalculator.NOT_USED),
            AssetFilter.Custodial to AssetDisplayInfo(custodialGroup, custodialCrypto, custodialFiat, emptySet(),
                AssetDetailsCalculator.NOT_USED)
        )

        whenever(token.exchangeRate()).thenReturn(Single.just(price))

        whenever(totalGroup.balance).thenReturn(Single.just(totalCrypto))
        whenever(nonCustodialGroup.balance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.balance).thenReturn(Single.just(custodialCrypto))
        whenever(interestGroup.balance).thenReturn(Single.just(interestCrypto))
        whenever(token.interestRate()).thenReturn(Single.just(interestRate))

        whenever(custodialGroup.accounts).thenReturn(listOf(mock()))
        whenever(nonCustodialGroup.accounts).thenReturn(listOf(mock()))
        whenever(nonCustodialGroup.isFunded).thenReturn(true)
        whenever(custodialGroup.isFunded).thenReturn(true)
        whenever(interestGroup.isFunded).thenReturn(false)

        calculator.token.accept(token)

        val v = calculator.assetDisplayDetails
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
        val token: CryptoAsset = mock()

        whenever(token.exchangeRate()).thenReturn(Single.error(Throwable()))

        val walletCrypto = CryptoValue(CryptoCurrency.BTC, 548621.toBigInteger())
        val custodialCrypto = CryptoValue.ZeroBtc
        val interestCrypto = CryptoValue.ZeroBtc
        val totalCrypto = walletCrypto + custodialCrypto + interestCrypto

        whenever(totalGroup.balance).thenReturn(Single.just(totalCrypto))
        whenever(nonCustodialGroup.balance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.balance).thenReturn(Single.just(custodialCrypto))
        whenever(interestGroup.balance).thenReturn(Single.just(interestCrypto))
        whenever(token.interestRate()).thenReturn(Single.just(interestRate))

        calculator.token.accept(token)

        val testObserver = calculator.assetDisplayDetails.test()
        testObserver.assertNoValues()
    }

    @Test
    fun`cryptoBalance & fiatBalance never return if interest fails`() {
        val token: CryptoAsset = mock()

        val walletCrypto = CryptoValue(CryptoCurrency.BTC, 548621.toBigInteger())
        val custodialCrypto = CryptoValue.ZeroBtc
        val totalCrypto = walletCrypto + custodialCrypto

        val price = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 5647899.toBigDecimal())

        whenever(token.exchangeRate()).thenReturn(Single.just(price))
        whenever(token.accountGroup(AssetFilter.Interest)).thenReturn(Maybe.error(Throwable()))

        whenever(totalGroup.balance).thenReturn(Single.just(totalCrypto))
        whenever(nonCustodialGroup.balance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.balance).thenReturn(Single.just(custodialCrypto))
        whenever(token.interestRate()).thenReturn(Single.just(interestRate))

        calculator.token.accept(token)

        val testObserver = calculator.assetDisplayDetails.test()
        testObserver.assertNoValues()
    }

    @Test
    fun `cryptoBalance, fiatBalance & interestBalance are never returned if totalBalance fails`() {
        val token: CryptoAsset = mock()

        val walletCrypto = CryptoValue(CryptoCurrency.BTC, 548621.toBigInteger())
        val custodialCrypto = CryptoValue.ZeroBtc
        val interestCrypto = CryptoValue.ZeroBtc

        val price = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 5647899.toBigDecimal())
        whenever(token.exchangeRate()).thenReturn(Single.just(price))

        whenever(totalGroup.balance).thenReturn(Single.error(Throwable()))
        whenever(nonCustodialGroup.balance).thenReturn(Single.just(walletCrypto))
        whenever(custodialGroup.balance).thenReturn(Single.just(custodialCrypto))
        whenever(interestGroup.balance).thenReturn(Single.just(interestCrypto))
        whenever(token.interestRate()).thenReturn(Single.just(interestRate))

        calculator.token.accept(token)

        val testObserver = calculator.assetDisplayDetails.test()
        testObserver.assertNoValues()
    }

    @Test
    fun `exchange rate is the right one`() {
        val token: CryptoAsset = mock()

        val price = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 56478.99.toBigDecimal())

        whenever(token.exchangeRate()).thenReturn(Single.just(price))
        calculator.token.accept(token)

        val testObserver = calculator.exchangeRate.test()
        testObserver.assertValue("$56,478.99").assertValueCount(1)
    }

    @Test
    fun `historic prices are returned properly`() {
        val token: CryptoAsset = mock()

        whenever(token.historicRateSeries(TimeSpan.DAY, TimeInterval.FIFTEEN_MINUTES))
            .thenReturn(Single.just(listOf(
                PriceDatum(5556, 2.toDouble(), volume24h = 0.toDouble()),
                PriceDatum(587, 22.toDouble(), volume24h = 0.toDouble()),
                PriceDatum(6981, 23.toDouble(), volume24h = 4.toDouble())
            )))

        calculator.token.accept(token)

        val loadingTestObserver = calculator.chartLoading.test()
        val testObserver = calculator.historicPrices.test()

        testObserver.assertValue(listOf(
            PriceDatum(5556, 2.toDouble(), volume24h = 0.toDouble()),
            PriceDatum(587, 22.toDouble(), volume24h = 0.toDouble()),
            PriceDatum(6981, 23.toDouble(), volume24h = 4.toDouble())
        )).assertValueCount(1)

        loadingTestObserver.assertValueAt(0, false)
            .assertValueAt(1, true)
            .assertValueAt(2, false)
            .assertValueCount(3)
    }

    @Test
    fun `when historic prices api returns error, empty list should be returned`() {
        val token: CryptoAsset = mock()
        whenever(token.historicRateSeries(TimeSpan.DAY, TimeInterval.FIFTEEN_MINUTES))
            .thenReturn(Single.error(Throwable()))

        calculator.token.accept(token)

        val loadingTestObserver = calculator.chartLoading.test()
        val testObserver = calculator.historicPrices.test()

        testObserver.assertValue(emptyList()).assertValueCount(1)

        loadingTestObserver.assertValueAt(0, false)
            .assertValueAt(1, true)
            .assertValueAt(2, false)
            .assertValueCount(3)
    }
}