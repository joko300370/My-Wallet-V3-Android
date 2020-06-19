package piuk.blockchain.android.simplebuy

import android.annotation.SuppressLint
import com.blockchain.android.testutils.rxInit
import com.blockchain.swap.nabu.datamanagers.BuyOrder
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import com.blockchain.swap.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Single
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.test.assertEquals

@Suppress("UnnecessaryVariable")
class SimpleBuySyncFactoryTest {

    private val remoteState: CustodialWalletManager = mock()
    private val availabilityChecker: SimpleBuyAvailability = mock()
    private val localState: SimpleBuyInflateAdapter = mock()

    private val subject = SimpleBuySyncFactory(
        custodialWallet = remoteState,
        localStateAdapter = localState,
        availabilityChecker = availabilityChecker
    )

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    private fun whenSimpleBuyIsEnabled() =
        whenever(availabilityChecker.isAvailable()).thenReturn(Single.just(true))

    private fun whenSimpleBuyIsDisabled() =
        whenever(availabilityChecker.isAvailable()).thenReturn(Single.just(false))

    @Test
    fun `There are no buys in progress anywhere`() {
        whenSimpleBuyIsEnabled()

        whenever(localState.fetch()).thenReturn(null)
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(Single.just(emptyList()))

        subject.performSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        validateFinalState(null)
    }

    @Test
    fun `there is a local buy, in a pre-confirmed state, and no other buys in progress`() {
        whenSimpleBuyIsEnabled()

        val localInput = SimpleBuyState(
            amount = FiatValue.fromMinor("EUR", 1000),
            fiatCurrency = "EUR",
            selectedCryptoCurrency = CryptoCurrency.BTC,
            orderState = OrderState.INITIALISED,
            expirationDate = Date(),
            kycVerificationState = null,
            currentScreen = FlowScreen.KYC
        )

        whenever(localState.fetch()).thenReturn(localInput)
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(Single.just(emptyList()))

        val expectedResult = localInput

        subject.performSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        validateFinalState(expectedResult)
    }

    @Test
    @Ignore("Temp not clearing state to facilitate happy path testing")
    fun `user is not eligible, clear any local state`() {
        whenSimpleBuyIsDisabled()

        subject.performSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        // Local state is cleared
        verify(localState, atLeastOnce()).clear()

        // Local and remote state is not queried
        verifyNoMoreInteractions(localState)
        verifyNoMoreInteractions(remoteState)
    }

    // TODO: Remove this, when we harden availability and have a new clearing state strategy
    @Test
    fun `Don't clear local state is SB is not available - temp fix, remove this`() {
        whenSimpleBuyIsDisabled()

        subject.performSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        // Local state is NOT cleared
        verify(localState, never()).clear()

        // Local and remote state is not queried
        verifyNoMoreInteractions(localState)
        verifyNoMoreInteractions(remoteState)
    }

    @Test
    fun `there is a remote buy, in an awaiting funds state, and no other buys in progress`() {
        whenSimpleBuyIsEnabled()

        val remoteInput = BuyOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.AWAITING_FUNDS,
            paymentMethodId = PaymentMethod.BANK_PAYMENT_ID,
            expires = Date(),
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        whenever(localState.fetch()).thenReturn(null)
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(
            Single.just(
                listOf(remoteInput)
            )
        )

        val expectedResult = remoteInput.toSimpleBuyState() // Minor hack - should prob HC this

        subject.performSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        validateFinalState(expectedResult)
    }

    @Test
    fun `there is a remote buy, in a pending state, and no other buys in progress`() {
        whenSimpleBuyIsEnabled()

        val remoteInput = BuyOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.PENDING_EXECUTION,
            expires = Date(),
            paymentMethodId = PaymentMethod.BANK_PAYMENT_ID,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        whenever(localState.fetch()).thenReturn(null)
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(
            Single.just(
                listOf(remoteInput)
            )
        )

        val expectedResult = remoteInput.toSimpleBuyState()

        subject.performSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        validateFinalState(expectedResult)
    }

    @Test
    fun `there are several remote buys, all in awaiting funds state, no local buy in progress`() {
        // Which shouldn't ever happen, but it does.

        whenSimpleBuyIsEnabled()

        val remoteInput1 = BuyOrder(
            id = ORDER_ID_2,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            paymentMethodId = PaymentMethod.BANK_PAYMENT_ID,
            state = OrderState.AWAITING_FUNDS,
            expires = MIDDLE_ORDER_DATE,
            fee = FiatValue.zero("EUR"),
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        val remoteInput2 = BuyOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.AWAITING_FUNDS,
            expires = LAST_ORDER_DATE,
            paymentMethodId = PaymentMethod.BANK_PAYMENT_ID,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        val remoteInput3 = BuyOrder(
            id = ORDER_ID_3,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.AWAITING_FUNDS,
            paymentMethodId = PaymentMethod.BANK_PAYMENT_ID,
            expires = EARLY_ORDER_DATE,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        whenever(localState.fetch()).thenReturn(null)
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(
            Single.just(
                listOf(
                    remoteInput1,
                    remoteInput2,
                    remoteInput3
                )
            )
        )

        // If and when we encounter this situation, we will take the one that was submitted last
        val expectedResult = remoteInput2.toSimpleBuyState() // Minor hack - should prob HC this

        subject.performSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        validateFinalState(expectedResult)
    }

    @Test
    fun `there are several remote buys, all in various completed states, no local buy in progress`() {
        // Which shouldn't ever happen, but it does.
        whenSimpleBuyIsEnabled()

        val remoteInput1 = BuyOrder(
            id = ORDER_ID_1,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.CANCELED,
            paymentMethodId = PaymentMethod.BANK_PAYMENT_ID,
            expires = MIDDLE_ORDER_DATE,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        val remoteInput2 = BuyOrder(
            id = ORDER_ID_2,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.PENDING_EXECUTION,
            paymentMethodId = PaymentMethod.BANK_PAYMENT_ID,
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        val remoteInput3 = BuyOrder(
            id = ORDER_ID_3,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.FINISHED,
            paymentMethodId = PaymentMethod.BANK_PAYMENT_ID,
            expires = EARLY_ORDER_DATE,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        val remoteInput4 = BuyOrder(
            id = ORDER_ID_4,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            paymentMethodId = "123-123",
            state = OrderState.FAILED,
            expires = EARLY_ORDER_DATE,
            fee = FiatValue.zero("EUR"),
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        whenever(localState.fetch()).thenReturn(null)
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(
            Single.just(
                listOf(
                    remoteInput1,
                    remoteInput2,
                    remoteInput3,
                    remoteInput4
                )
            )
        )

        val expectedResult = remoteInput2.toSimpleBuyState()

        subject.performSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        validateFinalState(expectedResult)
    }

    @Test
    fun `there are several remote buys, some in awaiting funds some in pending state, no local buy in progress`() {

        whenSimpleBuyIsEnabled()

        val remoteInput1 = BuyOrder(
            id = ORDER_ID_2,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.PENDING_EXECUTION,
            paymentMethodId = PaymentMethod.BANK_PAYMENT_ID,
            expires = MIDDLE_ORDER_DATE,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        val remoteInput2 = BuyOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.PENDING_EXECUTION,
            paymentMethodId = PaymentMethod.BANK_PAYMENT_ID,
            expires = EARLY_ORDER_DATE,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        val remoteInput3 = BuyOrder(
            id = ORDER_ID_2,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.AWAITING_FUNDS,
            paymentMethodId = PaymentMethod.BANK_PAYMENT_ID,
            expires = EARLY_ORDER_DATE,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        val remoteInput4 = BuyOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.PENDING_CONFIRMATION,
            paymentMethodId = PaymentMethod.BANK_PAYMENT_ID,
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        whenever(localState.fetch()).thenReturn(null)
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(
            Single.just(
                listOf(
                    remoteInput1,
                    remoteInput2,
                    remoteInput3,
                    remoteInput4
                )
            )
        )

        // If and when we encounter this situation, we will take the one that was submitted last
        val expectedResult = remoteInput4.toSimpleBuyState() // Minor hack - should prob HC this

        subject.performSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        validateFinalState(expectedResult)
    }

    @Test
    fun `remote overrides local`() {
        whenSimpleBuyIsEnabled()
        // We have a local confirmed buy, but it has been completed on another device
        // We should have no local state

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoCurrency = CryptoCurrency.BTC,
            orderState = OrderState.AWAITING_FUNDS,
            expirationDate = LAST_ORDER_DATE,
            kycVerificationState = null,
            currentScreen = FlowScreen.KYC
        )

        val remoteInput = BuyOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.FINISHED,
            paymentMethodId = "123-123",
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        whenever(localState.fetch()).thenReturn(localInput)
        whenever(remoteState.getBuyOrder(EXPECTED_ORDER_ID)).thenReturn(Single.just(remoteInput))
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(
            Single.just(
                listOf(
                    remoteInput
                )
            )
        )

        val expectedResult = null

        subject.performSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        validateFinalState(expectedResult)
    }

    @Test
    fun `remote pending overrides local`() {
        whenSimpleBuyIsEnabled()
        // We have a local confirmed buy, but it has been completed on another device
        // We should have no local state

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoCurrency = CryptoCurrency.BTC,
            orderState = OrderState.AWAITING_FUNDS,
            expirationDate = LAST_ORDER_DATE,
            kycVerificationState = null,
            currentScreen = FlowScreen.KYC
        )

        val remoteInput = BuyOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.PENDING_EXECUTION,
            paymentMethodId = "123-123",
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        whenever(localState.fetch()).thenReturn(localInput)
        whenever(remoteState.getBuyOrder(EXPECTED_ORDER_ID)).thenReturn(Single.just(remoteInput))
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(
            Single.just(
                listOf(
                    remoteInput
                )
            )
        )

        val expectedResult = remoteInput.toSimpleBuyState()

        subject.performSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        validateFinalState(expectedResult)
    }

    @Test
    fun `remote awaiting funds overrides local pending confirmation`() {
        whenSimpleBuyIsEnabled()
        // We have a local confirmed buy, but it has been completed on another device
        // We should have no local state

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoCurrency = CryptoCurrency.BTC,
            orderState = OrderState.PENDING_CONFIRMATION,
            expirationDate = LAST_ORDER_DATE,
            kycVerificationState = null,
            currentScreen = FlowScreen.KYC
        )

        val remoteInput = BuyOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.AWAITING_FUNDS,
            paymentMethodId = PaymentMethod.BANK_PAYMENT_ID,
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        whenever(localState.fetch()).thenReturn(localInput)
        whenever(remoteState.getBuyOrder(EXPECTED_ORDER_ID)).thenReturn(Single.just(remoteInput))
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(
            Single.just(
                listOf(
                    remoteInput
                )
            )
        )

        val expectedResult = remoteInput.toSimpleBuyState()

        subject.performSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        validateFinalState(expectedResult)
    }

    @Test
    fun `remote awaiting funds overrides local initialised`() {
        whenSimpleBuyIsEnabled()
        // We have an unconfirmed local buy, but another has been set up on another
        // device and is awaiting funds
        // We should use the remote

        val localInput = SimpleBuyState(
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoCurrency = CryptoCurrency.BTC,
            orderState = OrderState.INITIALISED,
            expirationDate = LAST_ORDER_DATE,
            kycVerificationState = null,
            currentScreen = FlowScreen.KYC
        )

        val remoteInput = BuyOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.AWAITING_FUNDS,
            paymentMethodId = PaymentMethod.BANK_PAYMENT_ID,
            expires = MIDDLE_ORDER_DATE,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        whenever(localState.fetch()).thenReturn(localInput)
        whenever(remoteState.getAllOutstandingBuyOrders()).thenReturn(
            Single.just(
                listOf(
                    remoteInput
                )
            )
        )

        val expectedResult = remoteInput.toSimpleBuyState()

        subject.performSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        verify(remoteState, never()).getBuyOrder(EXPECTED_ORDER_ID)
        validateFinalState(expectedResult)
    }

    // Test lightweight sync
    @Test
    fun `lightweight, no local state`() {
        whenSimpleBuyIsEnabled()

        whenever(localState.fetch()).thenReturn(null)

        val expectedResult = null

        subject.lightweightSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        validateFinalStateLightweight(expectedResult)
    }

    @Test
    fun `lightweight, local state initialised`() {
        whenSimpleBuyIsEnabled()

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoCurrency = CryptoCurrency.BTC,
            orderState = OrderState.INITIALISED,
            expirationDate = LAST_ORDER_DATE,
            kycVerificationState = null,
            currentScreen = FlowScreen.KYC
        )

        whenever(localState.fetch()).thenReturn(localInput)

        val expectedResult = localInput

        subject.lightweightSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        validateFinalStateLightweight(expectedResult)
    }

    @Test
    fun `lightweight, local state awaiting funds, remote awaiting funds`() {
        whenSimpleBuyIsEnabled()

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoCurrency = CryptoCurrency.BTC,
            orderState = OrderState.AWAITING_FUNDS,
            expirationDate = LAST_ORDER_DATE,
            selectedPaymentMethod = SelectedPaymentMethod(PaymentMethod.BANK_PAYMENT_ID,
                paymentMethodType = PaymentMethodType.BANK_ACCOUNT),
            currentScreen = FlowScreen.BANK_DETAILS
        )

        val remoteInput = BuyOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.AWAITING_FUNDS,
            paymentMethodId = PaymentMethod.BANK_PAYMENT_ID,
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        whenever(localState.fetch()).thenReturn(localInput)
        whenever(remoteState.getBuyOrder(EXPECTED_ORDER_ID)).thenReturn(Single.just(remoteInput))

        val expectedResult = remoteInput.toSimpleBuyState()

        subject.lightweightSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        validateFinalState(expectedResult)
    }

    @Test
    fun `lightweight, local state awaiting funds, remote pending`() {
        whenSimpleBuyIsEnabled()

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoCurrency = CryptoCurrency.BTC,
            orderState = OrderState.AWAITING_FUNDS,
            expirationDate = LAST_ORDER_DATE,
            kycVerificationState = KycState.VERIFIED_AND_ELIGIBLE,
            currentScreen = FlowScreen.BANK_DETAILS
        )

        val remoteInput = BuyOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.PENDING_EXECUTION,
            paymentMethodId = "123-123",
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        whenever(localState.fetch()).thenReturn(localInput)
        whenever(remoteState.getBuyOrder(EXPECTED_ORDER_ID)).thenReturn(Single.just(remoteInput))

        val expectedResult = remoteInput.toSimpleBuyState()

        subject.lightweightSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        validateFinalState(expectedResult)
    }

    @Test
    fun `lightweight, local state awaiting funds, remote finished`() {
        whenSimpleBuyIsEnabled()

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoCurrency = CryptoCurrency.BTC,
            orderState = OrderState.AWAITING_FUNDS,
            expirationDate = LAST_ORDER_DATE,
            kycVerificationState = KycState.VERIFIED_AND_ELIGIBLE,
            currentScreen = FlowScreen.BANK_DETAILS
        )

        val remoteInput = BuyOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.FINISHED,
            paymentMethodId = "123-123",
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        whenever(localState.fetch()).thenReturn(localInput)
        whenever(remoteState.getBuyOrder(EXPECTED_ORDER_ID)).thenReturn(Single.just(remoteInput))

        val expectedResult = null

        subject.lightweightSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        validateFinalStateLightweight(expectedResult)
    }

    @Test
    fun `lightweight, local state awaiting funds, remote canceled`() {
        whenSimpleBuyIsEnabled()

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoCurrency = CryptoCurrency.BTC,
            orderState = OrderState.AWAITING_FUNDS,
            expirationDate = LAST_ORDER_DATE,
            kycVerificationState = KycState.VERIFIED_AND_ELIGIBLE,
            currentScreen = FlowScreen.BANK_DETAILS
        )

        val remoteInput = BuyOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.CANCELED,
            paymentMethodId = "123-123",
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        whenever(localState.fetch()).thenReturn(localInput)
        whenever(remoteState.getBuyOrder(EXPECTED_ORDER_ID)).thenReturn(Single.just(remoteInput))

        val expectedResult = null

        subject.lightweightSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        validateFinalStateLightweight(expectedResult)
    }

    @Test
    fun `lightweight, local state awaiting funds, remote failed`() {
        whenSimpleBuyIsEnabled()

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            amount = FiatValue.fromMinor("EUR", 10000),
            fiatCurrency = "EUR",
            selectedCryptoCurrency = CryptoCurrency.BTC,
            orderState = OrderState.AWAITING_FUNDS,
            expirationDate = LAST_ORDER_DATE,
            kycVerificationState = KycState.VERIFIED_AND_ELIGIBLE,
            currentScreen = FlowScreen.BANK_DETAILS
        )

        val remoteInput = BuyOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.FAILED,
            paymentMethodId = "123-123",
            expires = LAST_ORDER_DATE,
            paymentMethodType = PaymentMethodType.BANK_ACCOUNT
        )

        whenever(localState.fetch()).thenReturn(localInput)
        whenever(remoteState.getBuyOrder(EXPECTED_ORDER_ID)).thenReturn(Single.just(remoteInput))

        val expectedResult = null

        subject.lightweightSync()
            .test()
            .assertComplete()
            .awaitTerminalEvent()

        validateFinalStateLightweight(expectedResult)
    }

    private fun validateFinalState(expected: SimpleBuyState?) {
        if (expected != null) {
            argumentCaptor<SimpleBuyState>().apply {
                verify(localState).update(capture())
                assertEquals(expected, firstValue)
            }
        } else {
            verify(localState, atLeastOnce()).clear()
        }

        verify(localState, atLeastOnce()).fetch()
        verifyNoMoreInteractions(localState)
    }

    private fun validateFinalStateLightweight(expected: SimpleBuyState?) {
        // Lightweight sync only clears state, if remote is complete, so never updates
        if (expected == null) {
            verify(localState, atLeastOnce()).clear()
        }

        verify(localState, atLeastOnce()).fetch()
        verifyNoMoreInteractions(localState)
    }

    companion object {
        private const val EXPECTED_ORDER_ID = "12345-12345-1234-1234567890"
        private const val ORDER_ID_1 = "11111-11111-1111-1111111111"
        private const val ORDER_ID_2 = "22222-22222-2222-2222222222"
        private const val ORDER_ID_3 = "33333-33333-3333-3333333333"
        private const val ORDER_ID_4 = "44444-44444-4444-4444444444"

        @SuppressLint("SimpleDateFormat")
        private val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

        private val EARLY_ORDER_DATE = format.parse("2020-02-21T10:36:40.245+0000")
        private val MIDDLE_ORDER_DATE = format.parse("2020-02-21T10:46:40.245+0000")
        private val LAST_ORDER_DATE = format.parse("2020-02-21T10:56:40.245+0000")
    }
}
