package info.blockchain.balance

import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test

class ExchangeRateTest {

    @Test
    fun `crypto to crypto`() {
        ExchangeRate.CryptoToCrypto(CryptoCurrency.BTC, CryptoCurrency.BCH, 20.toBigDecimal())
            .applyRate(10.bitcoin()) `should equal` 200.bitcoinCash()
    }

    @Test
    fun `crypto to fiat`() {
        ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 20.toBigDecimal())
            .applyRate(10.bitcoin()) `should equal` 200.usd()
    }

    @Test
    fun `fiat to crypto`() {
        ExchangeRate.FiatToCrypto("USD", CryptoCurrency.BTC, 20.toBigDecimal())
            .applyRate(10.usd()) `should equal` 200.bitcoin()
    }

    @Test(expected = ValueTypeMismatchException::class)
    fun `crypto to crypto - from miss match`() {
        ExchangeRate.CryptoToCrypto(CryptoCurrency.BCH, CryptoCurrency.BCH, 20.toBigDecimal())
            .applyRate(10.bitcoin())
    }

    @Test(expected = ValueTypeMismatchException::class)
    fun `crypto to fiat - from miss match`() {
        ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 20.toBigDecimal())
            .applyRate(10.ether())
    }

    @Test(expected = ValueTypeMismatchException::class)
    fun `fiat to crypto - from miss match`() {
        ExchangeRate.FiatToCrypto("GBP", CryptoCurrency.BTC, 20.toBigDecimal())
            .applyRate(10.usd())
    }

    @Test
    fun `crypto to crypto - multiply`() {
        val rate = ExchangeRate.CryptoToCrypto(CryptoCurrency.BTC, CryptoCurrency.BCH, 20.toBigDecimal())
        val cryptoValue = 10.bitcoin()
        cryptoValue * rate `should equal` 200.bitcoinCash()
    }

    @Test
    fun `crypto to fiat - multiply`() {
        val rate: ExchangeRate.CryptoToFiat = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 20.toBigDecimal())
        val cryptoValue = 10.bitcoin()

        cryptoValue * rate `should equal` 200.usd()
    }

    @Test
    fun `fiat to crypto - multiply`() {
        val rate = ExchangeRate.FiatToCrypto("USD", CryptoCurrency.BTC, 20.toBigDecimal())

        10.usd() * rate `should equal` 200.bitcoin()
    }

    @Test
    fun `crypto to fiat - inverse`() {
        ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 20.toBigDecimal()).inverse()
            .applyRate(200.usd()) `should equal` 10.bitcoin()
    }

    @Test
    fun `crypto to fiat - divide`() {
        200.usd() / ExchangeRate.CryptoToFiat(
            CryptoCurrency.BTC,
            "USD",
            20.toBigDecimal()
        ) `should equal` 10.bitcoin()
    }

    @Test
    fun `fiat to crypto - inverse`() {
        ExchangeRate.FiatToCrypto("USD", CryptoCurrency.BTC, 20.toBigDecimal()).inverse()
            .applyRate(200.bitcoin()) `should equal` 10.usd()
    }

    @Test
    fun `fiat to crypto - divide`() {
        200.bitcoin() / ExchangeRate.FiatToCrypto(
            "USD",
            CryptoCurrency.BTC,
            20.toBigDecimal()
        ) `should equal` 10.usd()
    }

    @Test
    fun `crypto to crypto - inverse`() {
        ExchangeRate.CryptoToCrypto(CryptoCurrency.BTC, CryptoCurrency.BCH, 20.toBigDecimal()).inverse()
            .apply {
                from `should be` CryptoCurrency.BCH
                to `should be` CryptoCurrency.BTC
                rate `should equal` 0.05.toBigDecimal()
            }
    }

    @Test
    fun `crypto to crypto - divide`() {
        val rate = ExchangeRate.CryptoToCrypto(CryptoCurrency.BCH, CryptoCurrency.BTC, 20.toBigDecimal())
        val cryptoValue = 20.bitcoin()
        cryptoValue / rate `should equal` 1.bitcoinCash()
    }

    @Test
    fun `should have same currencies`() {
        val rate1 = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 10.toBigDecimal())
        val rate2 = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 12.toBigDecimal())
        rate1.hasSameCurrencies(rate2) `should be` true

        val rate3 = ExchangeRate.FiatToCrypto("USD", CryptoCurrency.BTC, 10.toBigDecimal())
        val rate4 = ExchangeRate.FiatToCrypto("USD", CryptoCurrency.BTC, 12.toBigDecimal())

        rate3.hasSameCurrencies(rate4) `should be` true
    }

    @Test
    fun `should have opposite currencies`() {
        val rate1 = ExchangeRate.FiatToCrypto("USD", CryptoCurrency.BTC, 10.toBigDecimal())
        val rate2 = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 12.toBigDecimal())
        rate1.hasOppositeCurrencies(rate2) `should be` true
    }

    @Test
    fun `should have opposite fiat currencies`() {
        val rate1 = ExchangeRate.FiatToFiat("USD", "EUR", 10.toBigDecimal())
        val rate2 = ExchangeRate.FiatToFiat("EUR", "USD", 12.toBigDecimal())
        rate1.hasOppositeCurrencies(rate2) `should be` true
    }

    @Test
    fun `should NOT have opposite or same currencies`() {
        val rate1 = ExchangeRate.FiatToCrypto("EUR", CryptoCurrency.BTC, 10.toBigDecimal())
        val rate2 = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 12.toBigDecimal())
        rate1.hasOppositeCurrencies(rate2) `should be` false
        rate1.hasSameCurrencies(rate2) `should be` false
    }
}
