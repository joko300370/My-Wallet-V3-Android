package piuk.blockchain.android.simplebuy

import android.annotation.SuppressLint
import com.blockchain.android.testutils.rxInit
import com.blockchain.swap.nabu.datamanagers.BuyOrder
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.OrderState
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
        whenever(remoteState.getOutstandingBuyOrders()).thenReturn(Single.just(emptyList()))

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
            enteredAmount = "1000",
            currency = "EUR",
            selectedCryptoCurrency = CryptoCurrency.BTC,
            orderState = OrderState.INITIALISED,
            expirationDate = Date(),
            kycVerificationState = null,
            currentScreen = FlowScreen.KYC
        )

        whenever(localState.fetch()).thenReturn(localInput)
        whenever(remoteState.getOutstandingBuyOrders()).thenReturn(Single.just(emptyList()))

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
            expires = Date()
        )

        whenever(localState.fetch()).thenReturn(null)
        whenever(remoteState.getOutstandingBuyOrders()).thenReturn(
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
    fun `there are several remote buys, all in awaiting funds state, no local buy in progress`() {
        // Which shouldn't ever happen, but it does.

        whenSimpleBuyIsEnabled()

        val remoteInput1 = BuyOrder(
            id = ORDER_ID_2,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.AWAITING_FUNDS,
            expires = MIDDLE_ORDER_DATE
        )

        val remoteInput2 = BuyOrder(
            id = EXPECTED_ORDER_ID,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.AWAITING_FUNDS,
            expires = LAST_ORDER_DATE
        )

        val remoteInput3 = BuyOrder(
            id = ORDER_ID_2,
            pair = "EUR-BTC",
            fiat = FiatValue.fromMinor("EUR", 10000),
            crypto = CryptoValue.ZeroBtc,
            state = OrderState.AWAITING_FUNDS,
            expires = EARLY_ORDER_DATE
        )

        whenever(localState.fetch()).thenReturn(null)
        whenever(remoteState.getOutstandingBuyOrders()).thenReturn(
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
    fun `remote overrides local`() {
        whenSimpleBuyIsEnabled()
        // We have a local confirmed buy, but it has been completed on another device
        // We should have no local state

        val localInput = SimpleBuyState(
            id = EXPECTED_ORDER_ID,
            enteredAmount = "10000",
            currency = "EUR",
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
            expires = LAST_ORDER_DATE
        )

        whenever(localState.fetch()).thenReturn(localInput)
        whenever(remoteState.getBuyOrder(EXPECTED_ORDER_ID)).thenReturn(Single.just(remoteInput))
        whenever(remoteState.getOutstandingBuyOrders()).thenReturn(
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
    fun `remote awaiting funds overrides local initialised`() {
        whenSimpleBuyIsEnabled()
        // We have an unconfirmed local buy, but another has been set up on another
        // device and is awaiting funds
        // We should use the remote

        val localInput = SimpleBuyState(
            enteredAmount = "10000",
            currency = "EUR",
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
            expires = MIDDLE_ORDER_DATE
        )

        whenever(localState.fetch()).thenReturn(localInput)
        whenever(remoteState.getOutstandingBuyOrders()).thenReturn(
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

    companion object {
        private const val EXPECTED_ORDER_ID = "12345-12345-1234-1234567890"
        private const val ORDER_ID_2 = "22222-22222-2222-2222222222"

        @SuppressLint("SimpleDateFormat")
        private val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

        private val EARLY_ORDER_DATE = format.parse("2020-02-21T10:36:40.245+0000")
        private val MIDDLE_ORDER_DATE = format.parse("2020-02-21T10:46:40.245+0000")
        private val LAST_ORDER_DATE = format.parse("2020-02-21T10:56:40.245+0000")
    }
}
