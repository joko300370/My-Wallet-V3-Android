package piuk.blockchain.android.simplebuy

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.datamanagers.ApprovalErrorStatus
import com.blockchain.nabu.datamanagers.BuySellLimits
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.BuySellPair
import com.blockchain.nabu.datamanagers.BuySellPairs
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.TransferLimits
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.responses.simplebuy.EverypayPaymentAttrs
import com.blockchain.nabu.models.responses.simplebuy.PaymentAttributes
import com.blockchain.preferences.RatingPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.google.gson.Gson
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
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
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import java.util.Date

class SimpleBuyModelTest {

    private lateinit var model: SimpleBuyModel
    private val defaultState = SimpleBuyState(
        selectedCryptoCurrency = CryptoCurrency.BTC,
        amount = FiatValue.fromMinor("USD", 1000),
        fiatCurrency = "USD",
        selectedPaymentMethod = SelectedPaymentMethod(
            id = "123-321",
            paymentMethodType = PaymentMethodType.PAYMENT_CARD,
            isEligible = true
        ),
        id = "123"
    )
    private val gson = Gson()
    private val interactor: SimpleBuyInteractor = mock()
    private val prefs: SimpleBuyPrefs = mock {
        on { simpleBuyState() } `it returns` gson.toJson(defaultState)
    }
    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() } `it returns` false
    }

    private val ratingPrefs: RatingPrefs = mock {
        on { hasSeenRatingDialog } `it returns` true
        on { preRatingActionCompletedTimes } `it returns` 0
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
                ),
                ratingPrefs = ratingPrefs,
                environmentConfig = environmentConfig,
                crashLogger = mock()
            )
    }

    @Test
    fun `interactor fetched limits and pairs should be applied to state`() {
        whenever(interactor.fetchBuyLimitsAndSupportedCryptoCurrencies("USD"))
            .thenReturn(
                Single.just(
                    BuySellPairs(
                        listOf(
                            BuySellPair(
                                pair = "BTC-USD",
                                buyLimits = BuySellLimits(100, 5024558),
                                sellLimits = BuySellLimits(100, 5024558)
                            ),
                            BuySellPair(
                                pair = "BTC-EUR", buyLimits = BuySellLimits(1006, 10000),
                                sellLimits = BuySellLimits(100, 5024558)
                            ),
                            BuySellPair(
                                pair = "ETH-EUR", buyLimits = BuySellLimits(1005, 10000),
                                sellLimits = BuySellLimits(100, 5024558)
                            ),
                            BuySellPair(
                                pair = "BCH-EUR", buyLimits = BuySellLimits(1001, 10000),
                                sellLimits = BuySellLimits(100, 5024558)
                            )
                        )
                    ) to TransferLimits("USD")
                )
            )
        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.FetchBuyLimits("USD", CryptoCurrency.BTC))

        testObserver.assertValueAt(0, defaultState)
        testObserver.assertValueAt(
            1, defaultState.copy(
                supportedPairsAndLimits = listOf(
                    BuySellPair(
                        "BTC-USD", BuySellLimits(min = 100, max = 5024558),
                        sellLimits = BuySellLimits(100, 5024558)
                    )
                ),
                fiatCurrency = "USD",
                transferLimits = TransferLimits("USD"),
                selectedCryptoCurrency = CryptoCurrency.BTC
            )
        )
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
        whenever(
            interactor.createOrder(
                cryptoCurrency = anyOrNull(),
                amount = anyOrNull(),
                paymentMethodId = anyOrNull(),
                paymentMethod = any(),
                isPending = any()
            )
        ).thenReturn(
            Single.just(
                SimpleBuyIntent.OrderCreated(
                    BuySellOrder(
                        id = "testId",
                        expires = date,
                        state = OrderState.AWAITING_FUNDS,
                        crypto = CryptoValue.ZeroBtc,
                        orderValue = CryptoValue.ZeroBtc,
                        paymentMethodId = "213",
                        updated = Date(),
                        paymentMethodType = PaymentMethodType.FUNDS,
                        fiat = FiatValue.zero("USD"),
                        pair = "USD-BTC",
                        type = OrderType.BUY,
                        depositPaymentId = ""
                    )
                )
            )
        )

        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.CancelOrderIfAnyAndCreatePendingOne)

        testObserver.assertValueAt(0, defaultState)
        testObserver.assertValueAt(1, defaultState.copy(isLoading = true))
        testObserver.assertValueAt(
            2,
            defaultState.copy(
                orderState = OrderState.AWAITING_FUNDS,
                id = "testId",
                orderValue = CryptoValue.ZeroBtc,
                expirationDate = date
            )
        )
    }

    @Test
    fun `update kyc state shall make interactor poll for kyc state and update the state accordingly`() {
        whenever(interactor.pollForKycState())
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
            .thenReturn(
                Single.just(
                    BuySellOrder(
                        id = id,
                        pair = "EUR-BTC",
                        fiat = FiatValue.fromMinor("EUR", 10000),
                        crypto = CryptoValue.ZeroBtc,
                        state = OrderState.AWAITING_FUNDS,
                        paymentMethodId = "123-123",
                        expires = Date(),
                        price = price,
                        paymentMethodType = PaymentMethodType.PAYMENT_CARD,
                        attributes = PaymentAttributes(
                            EverypayPaymentAttrs(
                                paymentLink = paymentLink,
                                paymentState = EverypayPaymentAttrs.WAITING_3DS
                            ),
                            null,
                            null
                        ),
                        type = OrderType.BUY,
                        depositPaymentId = ""
                    )
                )
            )

        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.MakePayment("testId"))

        testObserver.assertValueAt(0, defaultState)
        testObserver.assertValueAt(1, defaultState.copy(isLoading = true))
        testObserver.assertValueAt(2, defaultState.copy(orderExchangePrice = price))
        testObserver.assertValueAt(
            3, defaultState.copy(
                orderExchangePrice = price,
                everypayAuthOptions = EverypayAuthOptions(
                    paymentLink, EverypayCardActivator.redirectUrl
                )
            )
        )
        testObserver.assertValueAt(4, defaultState.copy(orderExchangePrice = price))
    }

    @Test
    fun `polling order status with approval error should propagate`() {
        whenever(interactor.pollForOrderStatus(any())).thenReturn(
            Single.just(
                BuySellOrder(
                    id = "testId",
                    expires = Date(),
                    state = OrderState.CANCELED,
                    crypto = CryptoValue.ZeroBtc,
                    orderValue = CryptoValue.ZeroBtc,
                    paymentMethodId = "213",
                    updated = Date(),
                    paymentMethodType = PaymentMethodType.BANK_TRANSFER,
                    fiat = FiatValue.zero("USD"),
                    pair = "USD-BTC",
                    type = OrderType.BUY,
                    depositPaymentId = "",
                    approvalErrorStatus = ApprovalErrorStatus.REJECTED
                )
            )
        )

        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.CheckOrderStatus)

        testObserver.assertValueAt(0, defaultState)
        testObserver.assertValueAt(
            1, defaultState.copy(
                isLoading = true
            )
        )
        testObserver.assertValueAt(
            2, defaultState.copy(
                errorState = ErrorState.ApprovedBankRejected
            )
        )
    }

    @Test
    fun `predefined should be filtered properly based on the buy limits`() {
        whenever(interactor.fetchBuyLimitsAndSupportedCryptoCurrencies("USD"))
            .thenReturn(
                Single.just(
                    BuySellPairs(
                        listOf(
                            BuySellPair(
                                pair = "BTC-USD", buyLimits = BuySellLimits(100, 3000),
                                sellLimits = BuySellLimits(100, 5024558)
                            ),
                            BuySellPair(
                                pair = "BTC-EUR", buyLimits = BuySellLimits(1006, 10000),
                                sellLimits = BuySellLimits(100, 5024558)
                            ),
                            BuySellPair(
                                pair = "ETH-EUR", buyLimits = BuySellLimits(1005, 10000),
                                sellLimits = BuySellLimits(100, 5024558)
                            ),
                            BuySellPair(
                                pair = "BCH-EUR", buyLimits = BuySellLimits(1001, 10000),
                                sellLimits = BuySellLimits(100, 5024558)
                            )
                        )
                    ) to TransferLimits("USD")
                )
            )

        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.FetchBuyLimits("USD", CryptoCurrency.BTC))

        testObserver.assertValueAt(0, defaultState)
        testObserver.assertValueAt(
            1, defaultState.copy(
                supportedPairsAndLimits = listOf(
                    BuySellPair(
                        pair = "BTC-USD", buyLimits = BuySellLimits(100, 3000),
                        sellLimits = BuySellLimits(100, 5024558)
                    )
                ),
                selectedCryptoCurrency = CryptoCurrency.BTC,
                transferLimits = TransferLimits("USD")
            )
        )
    }

    @Test
    fun `WHEN eligiblePaymentMethods and getRecurringBuyEligibility success THEN observe state`() {
        val paymentMethodsUpdated = mock<SimpleBuyIntent.PaymentMethodsUpdated>()
        whenever(interactor.eligiblePaymentMethods("USD", "123-321"))
            .thenReturn(Single.just(paymentMethodsUpdated))

        val paymentMethodType = mock<PaymentMethodType>()
        whenever(interactor.getRecurringBuyEligibility())
            .thenReturn(Single.just(listOf(paymentMethodType)))

        verifyNoMoreInteractions(interactor)

        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.FetchSuggestedPaymentMethod("USD", "123-321"))

        testObserver.assertValueAt(0, defaultState)

        val state1 = defaultState.copy(
            paymentOptions = PaymentOptions(),
            selectedPaymentMethod = null
        )
        testObserver.assertValueAt(1, state1)

        val state2 = state1.copy(
            recurringBuyEligiblePaymentMethods = listOf(paymentMethodType),
            isLoading = false,
            selectedPaymentMethod = null,
            paymentOptions = PaymentOptions()
        )
        testObserver.assertValueAt(2, state2)
    }

    @Test
    fun `WHEN eligiblePaymentMethods fails THEN observe state`() {
        whenever(interactor.eligiblePaymentMethods("USD", "123-321"))
            .thenReturn(Single.error(Throwable()))

        verifyNoMoreInteractions(interactor)

        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.FetchSuggestedPaymentMethod("USD", "123-321"))

        testObserver.assertValueAt(0, defaultState)

        val state1 = defaultState.copy(
            paymentOptions = PaymentOptions(),
            selectedPaymentMethod = null
        )
        testObserver.assertValueAt(1, state1)

        testObserver.assertValueAt(
            2,
            state1.copy(
                errorState = ErrorState.GenericError,
                isLoading = false,
                confirmationActionRequested = false
            )
        )
    }

    @Test
    fun `WHEN eligiblePaymentMethods success and getRecurringBuyEligibility fails THEN observe state`() {
        whenever(interactor.eligiblePaymentMethods("USD", "123-321"))
            .thenReturn(Single.just(mock()))

        whenever(interactor.getRecurringBuyEligibility())
            .thenReturn(Single.error(Throwable()))

        verifyNoMoreInteractions(interactor)

        val testObserver = model.state.test()
        model.process(SimpleBuyIntent.FetchSuggestedPaymentMethod("USD", "123-321"))

        testObserver.assertValueAt(0, defaultState)

        val state1 = defaultState.copy(
            paymentOptions = PaymentOptions(),
            selectedPaymentMethod = null
        )
        testObserver.assertValueAt(1, state1)
    }
}