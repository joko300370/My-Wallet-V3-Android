package info.blockchain.balance

import java.math.BigDecimal
import java.math.BigInteger

internal fun Number.gbp() = FiatValue.fromMajor("GBP", numberToBigDecimal())

internal fun Number.usd() = FiatValue.fromMajor("USD", numberToBigDecimal())

internal fun Number.jpy() = FiatValue.fromMajor("JPY", numberToBigDecimal())

internal fun Number.eur() = FiatValue.fromMajor("EUR", numberToBigDecimal())

internal fun Number.cad() = FiatValue.fromMajor("CAD", numberToBigDecimal())

private fun Number.numberToBigDecimal(): BigDecimal =
    when (this) {
        is Double -> toBigDecimal()
        is Int -> toBigDecimal()
        is Long -> toBigDecimal()
        is BigDecimal -> this
        else -> throw NotImplementedError(this.javaClass.name)
    }

private fun Number.numberToBigInteger(): BigInteger =
    when (this) {
        is BigInteger -> this
        is Int -> toBigInteger()
        is Long -> toBigInteger()
        else -> throw NotImplementedError(this.javaClass.name)
    }

internal fun Number.bitcoin() = CryptoValue.fromMajor(CryptoCurrency.BTC, numberToBigDecimal())
internal fun Number.satoshi() = CryptoValue.fromMinor(CryptoCurrency.BTC, numberToBigInteger())
internal fun Number.ether() = CryptoValue.fromMajor(CryptoCurrency.ETHER, numberToBigDecimal())
internal fun Number.wei() = CryptoValue.fromMinor(CryptoCurrency.ETHER, numberToBigDecimal())
internal fun Number.bitcoinCash() = CryptoValue.fromMajor(CryptoCurrency.BCH, numberToBigDecimal())
internal fun Number.satoshiCash() = CryptoValue.fromMinor(CryptoCurrency.BCH, numberToBigDecimal())
internal fun Number.lumens() = CryptoValue.fromMajor(CryptoCurrency.XLM, numberToBigDecimal())
internal fun Number.stroops() = CryptoValue.fromMinor(CryptoCurrency.XLM, numberToBigInteger())
internal fun Number.usdPax() = CryptoValue.fromMajor(CryptoCurrency.PAX, numberToBigDecimal())
internal fun Number.usdt() = CryptoValue.fromMajor(CryptoCurrency.USDT, numberToBigDecimal())
