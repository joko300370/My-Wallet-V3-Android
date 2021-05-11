package piuk.blockchain.android.simplebuy

import com.blockchain.nabu.datamanagers.TransferLimits
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimpleBuyStateTest {

    @Test
    fun `amount is valid when entered amount is number and between limits`() {
        val state = SimpleBuyState(
            transferLimits = TransferLimits(
                minLimit = FiatValue.zero("USD"),
                maxOrder = FiatValue.fromMinor("USD", 1000),
                maxLimit = FiatValue.fromMinor("USD", 20000)
            ),
            amount = FiatValue.fromMajor("USD", 99.32.toBigDecimal()),
            fiatCurrency = "USD",
            selectedCryptoCurrency = CryptoCurrency.BTC
        )
        assertTrue(state.isAmountValid)
    }

    @Test
    fun `amount is not valid when entered amount is not between limits`() {
        val state = SimpleBuyState(
            transferLimits = TransferLimits(
                minLimit = FiatValue.zero("USD"),
                maxOrder = FiatValue.fromMinor("USD", 10000),
                maxLimit = FiatValue.fromMinor("USD", 20000)
            ),
            amount = FiatValue.fromMinor("USD", 30000),
            fiatCurrency = "USD",
            selectedCryptoCurrency = CryptoCurrency.BTC
        )
        assertFalse(state.isAmountValid)
    }

    @Test
    fun `amount is not valid when entered amount is null`() {
        val state = SimpleBuyState(
            amount = null, fiatCurrency = "USD", selectedCryptoCurrency = CryptoCurrency.BTC,
            transferLimits = TransferLimits(
                minLimit = FiatValue.zero("USD"),
                maxOrder = FiatValue.fromMinor("USD", 10000),
                maxLimit = FiatValue.fromMinor("USD", 10000)
            )
        )
        assertFalse(state.isAmountValid)
    }
}