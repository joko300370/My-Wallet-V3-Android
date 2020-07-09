package info.blockchain.balance

import info.blockchain.balance.Money.Companion.max
import info.blockchain.balance.Money.Companion.min
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should throw the Exception`
import org.amshove.kluent.`with message`
import org.junit.Test
import java.math.BigInteger

class CryptoValueMinAndMaxTest {

    @Test
    fun `max of two`() {
        val a = CryptoValue.bitcoinCashFromSatoshis(1)
        val b = CryptoValue.bitcoinCashFromSatoshis(2)
        max(a, b) `should be` b
    }

    @Test
    fun `max of two reversed`() {
        val a = CryptoValue.bitcoinCashFromSatoshis(1)
        val b = CryptoValue.bitcoinCashFromSatoshis(2)
        max(b, a) `should be` b
    }

    @Test
    fun `max of two the same`() {
        val a = CryptoValue.bitcoinCashFromSatoshis(1)
        val b = CryptoValue.bitcoinCashFromSatoshis(1)
        max(a, b) `should be` a
    }

    @Test
    fun `min of two`() {
        val a = CryptoValue.bitcoinCashFromSatoshis(1)
        val b = CryptoValue.bitcoinCashFromSatoshis(2)
        min(a, b) `should be` a
    }

    @Test
    fun `min of two reversed`() {
        val a = CryptoValue.bitcoinCashFromSatoshis(1)
        val b = CryptoValue.bitcoinCashFromSatoshis(2)
        min(b, a) `should be` a
    }

    @Test
    fun `min of two the same`() {
        val a = CryptoValue.bitcoinCashFromSatoshis(1)
        val b = CryptoValue.bitcoinCashFromSatoshis(1)
        min(a, b) `should be` a
    }

    @Test
    fun `max of two with different currencies`() {
        val a = CryptoValue.bitcoinFromSatoshis(1)
        val b = CryptoValue.bitcoinCashFromSatoshis(2);
        {
            max(a, b)
        } `should throw the Exception` Exception::class `with message` "Can't compare BTC and BCH"
    }

    @Test
    fun `min of two with different currencies`() {
        val a = CryptoValue(CryptoCurrency.ETHER, BigInteger.ONE)
        val b = CryptoValue.bitcoinFromSatoshis(2);
        {
            min(a, b)
        } `should throw the Exception` Exception::class `with message` "Can't compare ETH and BTC"
    }
}
