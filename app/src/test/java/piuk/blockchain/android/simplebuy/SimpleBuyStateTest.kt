package piuk.blockchain.android.simplebuy

import com.blockchain.swap.nabu.datamanagers.BuyLimits
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPair
import info.blockchain.balance.CryptoCurrency
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimpleBuyStateTest {

    @Test
    fun `amount is valid when entered amount is number and between limits`() {
        val state = SimpleBuyState(supportedPairsAndLimits = listOf(
            SimpleBuyPair("BTC-USD", BuyLimits(0, 10000))
        ), enteredAmount = "99.32", currency = "USD", selectedCryptoCurrency = CryptoCurrency.BTC)
        assertTrue(state.isAmountValid)
    }

    @Test
    fun `amount is not valid when entered amount is not between limits`() {
        val state = SimpleBuyState(supportedPairsAndLimits = listOf(
            SimpleBuyPair("BTC-USD", BuyLimits(0, 1000))
        ), enteredAmount = "101", currency = "USD", selectedCryptoCurrency = CryptoCurrency.BTC)
        assertFalse(state.isAmountValid)
    }

    @Test
    fun `amount is not valid when entered amount is not numeric`() {
        val state = SimpleBuyState(supportedPairsAndLimits = listOf(
            SimpleBuyPair("BTC-USD", BuyLimits(0, 1000))
        ), enteredAmount = "1021f1", selectedCryptoCurrency = CryptoCurrency.BTC)
        assertFalse(state.isAmountValid)
    }

    @Test
    fun `amount is not valid when entered amount is empty`() {
        val state = SimpleBuyState(supportedPairsAndLimits = listOf(
            SimpleBuyPair("BTC-USD", BuyLimits(0, 1000))
        ), enteredAmount = "", currency = "USD", selectedCryptoCurrency = CryptoCurrency.BTC)
        assertFalse(state.isAmountValid)
    }
}