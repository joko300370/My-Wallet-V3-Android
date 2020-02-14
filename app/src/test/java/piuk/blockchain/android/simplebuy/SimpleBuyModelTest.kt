package piuk.blockchain.android.simplebuy

import com.blockchain.android.testutils.rxInit
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.BuyLimits
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPair
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPairs
import com.google.gson.Gson
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.amshove.kluent.`it returns`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date

class SimpleBuyModelTest {

    private lateinit var model: SimpleBuyModel
    private val defaultState = SimpleBuyState()
    private val gson = Gson()
    private val interactor: SimpleBuyInteractor = mock()
    private val prefs: SimpleBuyPrefs = mock {
        on { simpleBuyState() } `it returns` gson.toJson(defaultState)
    }

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model =
            SimpleBuyModel(
                prefs = prefs,
                initialState = defaultState,
                gson = gson,
                scheduler = Schedulers.io(),
                interactor = interactor
            )
    }

    @Test
    fun `interactor fetched limits and pairs should be applied to state`() {
        whenever(interactor.fetchBuyLimitsAndSupportedCryptoCurrencies("USD"))
            .thenReturn(Single.just(SimpleBuyIntent.UpdatedBuyLimitsAndSupportedCryptoCurrencies(
                SimpleBuyPairs(listOf(
                    SimpleBuyPair(pair = "BTC-USD", buyLimits = BuyLimits(100, 5024558)),
                    SimpleBuyPair(pair = "BTC-EUR", buyLimits = BuyLimits(1006, 10000)),
                    SimpleBuyPair(pair = "ETH-EUR", buyLimits = BuyLimits(1005, 10000)),
                    SimpleBuyPair(pair = "BCH-EUR", buyLimits = BuyLimits(1001, 10000))
                )))))
        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.FetchBuyLimits("USD"))

        testObserver.assertValueAt(0, SimpleBuyState())
        testObserver.assertValueAt(1, SimpleBuyState(supportedPairsAndLimits = listOf(
            SimpleBuyPair("BTC-USD", BuyLimits(min = 100, max = 5024558))),
            currency = "USD",
            selectedCryptoCurrency = CryptoCurrency.BTC
        ))
    }

    @Test
    fun `cancel order should make the order to cancel if interactor doesnt return an error`() {
        whenever(interactor.cancelOrder())
            .thenReturn(Single.just(SimpleBuyIntent.OrderCanceled))
        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.CancelOrder)

        testObserver.assertValueAt(0, SimpleBuyState())
        testObserver.assertValueAt(1, SimpleBuyState(orderState = OrderState.CANCELED))
    }

    @Test
    fun `confirm order should make the order to confirm if interactor doesnt return an error`() {
        val date = Date()
        whenever(interactor.createOrder(anyOrNull(), anyOrNull()))
            .thenReturn(Single.just(SimpleBuyIntent.OrderCreated("testId", date, OrderState.AWAITING_FUNDS)))
        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.ConfirmOrder)

        testObserver.assertValueAt(0, SimpleBuyState())
        testObserver.assertValueAt(1, SimpleBuyState(confirmationActionRequested = true))
        testObserver.assertValueAt(2,
            SimpleBuyState(confirmationActionRequested = true,
                orderState = OrderState.AWAITING_FUNDS,
                id = "testId",
                expirationDate = date))
    }

    @Test
    fun `update kyc state shall make interactor poll for kyc state and update the state accordingly`() {
        whenever(interactor.pollForKycState())
            .thenReturn(Single.just(SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_AND_ELIGIBLE)))
        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.FetchKycState)

        testObserver.assertValueAt(0, SimpleBuyState())
        testObserver.assertValueAt(1, SimpleBuyState(kycVerificationState = KycState.PENDING))
        testObserver.assertValueAt(2, SimpleBuyState(kycVerificationState = KycState.VERIFIED_AND_ELIGIBLE))
    }

    @Test
    fun `predefined shoulb be filtered properly based on the buy limits`() {
        whenever(interactor.fetchBuyLimitsAndSupportedCryptoCurrencies("USD"))
            .thenReturn(Single.just(SimpleBuyIntent.UpdatedBuyLimitsAndSupportedCryptoCurrencies(
                SimpleBuyPairs(listOf(
                    SimpleBuyPair(pair = "BTC-USD", buyLimits = BuyLimits(100, 3000)),
                    SimpleBuyPair(pair = "BTC-EUR", buyLimits = BuyLimits(1006, 10000)),
                    SimpleBuyPair(pair = "ETH-EUR", buyLimits = BuyLimits(1005, 10000)),
                    SimpleBuyPair(pair = "BCH-EUR", buyLimits = BuyLimits(1001, 10000))
                )))))

        whenever(interactor.fetchPredefinedAmounts("USD"))
            .thenReturn(Single.just(SimpleBuyIntent.UpdatedPredefinedAmounts(listOf(
                FiatValue.fromMinor("USD", 100000),
                FiatValue.fromMinor("USD", 5000),
                FiatValue.fromMinor("USD", 1000),
                FiatValue.fromMinor("USD", 500)))))

        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.FetchPredefinedAmounts("USD"))
        model.process(SimpleBuyIntent.FetchBuyLimits("USD"))

        testObserver.assertValueAt(0, SimpleBuyState())
        testObserver.assertValueAt(1, SimpleBuyState(predefinedAmounts = listOf(
            FiatValue.fromMinor("USD", 100000),
            FiatValue.fromMinor("USD", 5000),
            FiatValue.fromMinor("USD", 1000),
            FiatValue.fromMinor("USD", 500))))

        testObserver.assertValueAt(2, SimpleBuyState(
            supportedPairsAndLimits = listOf(
                SimpleBuyPair(pair = "BTC-USD", buyLimits = BuyLimits(100, 3000))
            ),
            selectedCryptoCurrency = CryptoCurrency.BTC,
            predefinedAmounts = listOf(
                FiatValue.fromMinor("USD", 1000),
                FiatValue.fromMinor("USD", 500))))
    }
}