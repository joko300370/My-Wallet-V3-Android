package piuk.blockchain.android.simplebuy

import com.blockchain.android.testutils.rxInit
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.swap.nabu.datamanagers.BuyLimits
import com.blockchain.swap.nabu.datamanagers.BuyOrder
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPair
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPairs
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.swap.nabu.models.simplebuy.CardPaymentAttributes
import com.blockchain.swap.nabu.models.simplebuy.EverypayPaymentAttrs
import com.google.gson.Gson
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.amshove.kluent.`it returns`
import org.amshove.kluent.any
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.cards.EverypayAuthOptions
import piuk.blockchain.android.cards.partners.EverypayCardActivator
import java.util.Date

class SimpleBuyModelTest {

    private lateinit var model: SimpleBuyModel
    private val defaultState = SimpleBuyState(
        selectedCryptoCurrency = CryptoCurrency.BTC,
        amount = FiatValue.fromMinor("USD", 1000),
        fiatCurrency = "USD",
        selectedPaymentMethod = SelectedPaymentMethod(
            id = "123-321",
            paymentMethodType = PaymentMethodType.PAYMENT_CARD
        ))
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
                interactor = interactor,
                cardActivators = listOf(
                    mock()
                )
            )
    }

    @Test
    fun `interactor fetched limits and pairs should be applied to state`() {
        whenever(interactor.fetchBuyLimitsAndSupportedCryptoCurrencies("USD"))
            .thenReturn(Single.just(
                SimpleBuyPairs(listOf(
                    SimpleBuyPair(pair = "BTC-USD", buyLimits = BuyLimits(100, 5024558)),
                    SimpleBuyPair(pair = "BTC-EUR", buyLimits = BuyLimits(1006, 10000)),
                    SimpleBuyPair(pair = "ETH-EUR", buyLimits = BuyLimits(1005, 10000)),
                    SimpleBuyPair(pair = "BCH-EUR", buyLimits = BuyLimits(1001, 10000))
                ))))
        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.FetchBuyLimits("USD"))

        testObserver.assertValueAt(0, defaultState)
        testObserver.assertValueAt(1, defaultState.copy(supportedPairsAndLimits = listOf(
            SimpleBuyPair("BTC-USD", BuyLimits(min = 100, max = 5024558))),
            fiatCurrency = "USD",
            selectedCryptoCurrency = CryptoCurrency.BTC
        ))
    }

    @Test
    fun `cancel order should make the order to cancel if interactor doesnt return an error`() {
        whenever(interactor.cancelOrder(any()))
            .thenReturn(Completable.complete())
        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.CancelOrder)

        testObserver.assertValueAt(0, defaultState)
        testObserver.assertValueAt(1, SimpleBuyState(orderState = OrderState.CANCELED))
    }

    @Test
    fun `confirm order should make the order to confirm if interactor doesnt return an error`() {
        val date = Date()
        whenever(interactor.createOrder(
            cryptoCurrency = anyOrNull(),
            amount = anyOrNull(),
            paymentMethodId = anyOrNull(),
            paymentMethod = any(),
            isPending = any()
        )).thenReturn(Single.just(SimpleBuyIntent.OrderCreated(
            BuyOrder(
                id = "testId",
                expires = date,
                state = OrderState.AWAITING_FUNDS,
                crypto = CryptoValue.ZeroBtc,
                paymentMethodId = "213",
                updated = Date(),
                paymentMethodType = PaymentMethodType.BANK_ACCOUNT,
                fiat = FiatValue.zero("USD"),
                pair = "USD-BTC"
            ))))

        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.CancelOrderIfAnyAndCreatePendingOne)

        testObserver.assertValueAt(0, defaultState)
        testObserver.assertValueAt(1, defaultState.copy(isLoading = true))
        testObserver.assertValueAt(2,
            defaultState.copy(
                orderState = OrderState.AWAITING_FUNDS,
                id = "testId",
                expirationDate = date))
    }

    @Test
    fun `update kyc state shall make interactor poll for kyc state and update the state accordingly`() {
        whenever(interactor.pollForKycState("USD"))
            .thenReturn(Single.just(SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_AND_ELIGIBLE)))
        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.FetchKycState)

        testObserver.assertValueAt(0, defaultState)
        testObserver.assertValueAt(1, defaultState.copy(kycVerificationState = KycState.PENDING))
        testObserver.assertValueAt(2, defaultState.copy(kycVerificationState = KycState.VERIFIED_AND_ELIGIBLE))
    }

    @Test
    fun `make card payment should update price and payment attributes`() {
        val price = FiatValue.fromMinor(
            "EUR",
            1000.toLong()
        )

        val paymentLink = "http://example.com"
        val id = "testId"
        whenever(interactor.fetchOrder(id))
            .thenReturn(Single.just(
                BuyOrder(
                    id = id,
                    pair = "EUR-BTC",
                    fiat = FiatValue.fromMinor("EUR", 10000),
                    crypto = CryptoValue.ZeroBtc,
                    state = OrderState.AWAITING_FUNDS,
                    paymentMethodId = "123-123",
                    expires = Date(),
                    price = price,
                    paymentMethodType = PaymentMethodType.PAYMENT_CARD,
                    attributes = CardPaymentAttributes(
                        EverypayPaymentAttrs(paymentLink = paymentLink,
                            paymentState = EverypayPaymentAttrs.WAITING_3DS)
                    )
                )
            ))

        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.MakePayment("testId"))

        testObserver.assertValueAt(0, defaultState)
        testObserver.assertValueAt(1, defaultState.copy(isLoading = true))
        testObserver.assertValueAt(2, defaultState.copy(orderExchangePrice = price))
        testObserver.assertValueAt(3, defaultState.copy(orderExchangePrice = price,
            everypayAuthOptions = EverypayAuthOptions(
                paymentLink, EverypayCardActivator.redirectUrl
            )))
        testObserver.assertValueAt(4, defaultState.copy(orderExchangePrice = price))
    }

    @Test
    fun `predefined should be filtered properly based on the buy limits`() {
        whenever(interactor.fetchBuyLimitsAndSupportedCryptoCurrencies("USD"))
            .thenReturn(Single.just(
                SimpleBuyPairs(listOf(
                    SimpleBuyPair(pair = "BTC-USD", buyLimits = BuyLimits(100, 3000)),
                    SimpleBuyPair(pair = "BTC-EUR", buyLimits = BuyLimits(1006, 10000)),
                    SimpleBuyPair(pair = "ETH-EUR", buyLimits = BuyLimits(1005, 10000)),
                    SimpleBuyPair(pair = "BCH-EUR", buyLimits = BuyLimits(1001, 10000))
                ))))

        whenever(interactor.fetchPredefinedAmounts("USD"))
            .thenReturn(Single.just(SimpleBuyIntent.UpdatedPredefinedAmounts(listOf(
                FiatValue.fromMinor("USD", 100000),
                FiatValue.fromMinor("USD", 5000),
                FiatValue.fromMinor("USD", 1000),
                FiatValue.fromMinor("USD", 500)))))

        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.FetchPredefinedAmounts("USD"))
        model.process(SimpleBuyIntent.FetchBuyLimits("USD"))

        testObserver.assertValueAt(0, defaultState)
        testObserver.assertValueAt(1, defaultState.copy(predefinedAmounts = listOf(
            FiatValue.fromMinor("USD", 100000),
            FiatValue.fromMinor("USD", 5000),
            FiatValue.fromMinor("USD", 1000),
            FiatValue.fromMinor("USD", 500))))

        testObserver.assertValueAt(2, defaultState.copy(
            supportedPairsAndLimits = listOf(
                SimpleBuyPair(pair = "BTC-USD", buyLimits = BuyLimits(100, 3000))
            ),
            selectedCryptoCurrency = CryptoCurrency.BTC,
            predefinedAmounts = listOf(
                FiatValue.fromMinor("USD", 1000),
                FiatValue.fromMinor("USD", 500))))
    }
}