package com.blockchain.swap.common.quote

import com.blockchain.morph.CoinPair
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import info.blockchain.balance.CryptoCurrency
import org.amshove.kluent.`should be`
import org.junit.Test

class ExchangeQuoteRequestTest {

    @Test
    fun `selling pair btc eth`() {
        ExchangeQuoteRequest.Selling(
            1.234.bitcoin(),
            CryptoCurrency.ETHER
        ).pair `should be` CoinPair.BTC_TO_ETH
    }

    @Test
    fun `selling pair btc eth with fiat symbol`() {
        ExchangeQuoteRequest.Selling(
            1.234.bitcoin(),
            CryptoCurrency.ETHER,
            indicativeFiatSymbol = "USD"
        ).fiatSymbol `should be` "USD"
    }

    @Test
    fun `selling pair bch btc`() {
        ExchangeQuoteRequest.Selling(
            1.234.bitcoinCash(),
            CryptoCurrency.BTC
        ).pair `should be` CoinPair.BCH_TO_BTC
    }

    @Test
    fun `selling pair bch btc with fiat symbol`() {
        ExchangeQuoteRequest.Selling(
            1.234.bitcoinCash(),
            CryptoCurrency.BTC,
            indicativeFiatSymbol = "CAD"
        ).fiatSymbol `should be` "CAD"
    }

    @Test
    fun `buying pair btc eth`() {
        ExchangeQuoteRequest.Buying(
            CryptoCurrency.ETHER,
            1.234.bitcoin()
        ).pair `should be` CoinPair.ETH_TO_BTC
    }

    @Test
    fun `buying pair btc eth with fiat symbol`() {
        ExchangeQuoteRequest.Buying(
            CryptoCurrency.ETHER,
            1.234.bitcoin(),
            indicativeFiatSymbol = "JPY"
        ).fiatSymbol `should be` "JPY"
    }

    @Test
    fun `buying pair bch btc`() {
        ExchangeQuoteRequest.Buying(
            CryptoCurrency.BTC,
            1.234.bitcoinCash()
        ).pair `should be` CoinPair.BTC_TO_BCH
    }

    @Test
    fun `buying pair bch btc with fiat symbol`() {
        ExchangeQuoteRequest.Buying(
            CryptoCurrency.BTC,
            1.234.bitcoinCash(),
            indicativeFiatSymbol = "GBP"
        ).fiatSymbol `should be` "GBP"
    }
}