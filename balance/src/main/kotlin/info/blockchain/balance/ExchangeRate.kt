package info.blockchain.balance

import java.math.BigDecimal
import java.math.RoundingMode

sealed class ExchangeRate(var rate: BigDecimal) {

    protected val rateInverse: BigDecimal get() = BigDecimal.valueOf(1.0 / rate.toDouble())

    abstract fun convert(value: Money): Money
    abstract fun price(): Money
    abstract fun inverse(): ExchangeRate

    class CryptoToCrypto(
        val from: CryptoCurrency,
        val to: CryptoCurrency,
        rate: BigDecimal
    ) : ExchangeRate(rate) {
        fun applyRate(cryptoValue: CryptoValue): CryptoValue {
            validateCurrency(from, cryptoValue.currency)
            return CryptoValue.fromMajor(
                to,
                rate.multiply(cryptoValue.toBigDecimal())
            )
        }

        override fun convert(value: Money): Money =
            applyRate(value as CryptoValue)

        override fun price(): Money =
            CryptoValue.fromMajor(to, rate)

        override fun inverse() =
            CryptoToCrypto(to, from, rateInverse)
    }

    class CryptoToFiat(
        val from: CryptoCurrency,
        val to: String,
        rate: BigDecimal
    ) : ExchangeRate(rate) {
        fun applyRate(cryptoValue: CryptoValue): FiatValue {
            validateCurrency(from, cryptoValue.currency)
            return FiatValue.fromMajor(
                currencyCode = to,
                major = rate.multiply(cryptoValue.toBigDecimal())
            )
        }

        override fun convert(value: Money): Money =
            applyRate(value as CryptoValue)

        override fun price(): Money =
            FiatValue.fromMajor(to, rate)

        override fun inverse() =
            FiatToCrypto(to, from, rateInverse)
    }

    class FiatToCrypto(
        val from: String,
        val to: CryptoCurrency,
        rate: BigDecimal
    ) : ExchangeRate(rate) {
        fun applyRate(fiatValue: FiatValue): CryptoValue {
            validateCurrency(from, fiatValue.currencyCode)
            return CryptoValue.fromMajor(
                to,
                rate.multiply(fiatValue.toBigDecimal())
            )
        }

        override fun convert(value: Money): Money =
            applyRate(value as FiatValue)

        override fun price(): Money =
            CryptoValue.fromMajor(to, rate)

        override fun inverse() =
            CryptoToFiat(to, from, rateInverse)
    }

    class FiatToFiat(
        val from: String,
        val to: String,
        rate: BigDecimal
    ) : ExchangeRate(rate) {
        fun applyRate(fiatValue: FiatValue): FiatValue {
            validateCurrency(from, fiatValue.currencyCode)
            return FiatValue.fromMajor(
                to,
                rate.multiply(fiatValue.toBigDecimal())
            )
        }

        override fun convert(value: Money): Money =
            applyRate(value as FiatValue)

        override fun price(): Money =
            FiatValue.fromMajor(to, rate)

        override fun inverse() =
            FiatToFiat(to, from, rateInverse)
    }

    companion object {
        private fun validateCurrency(expected: CryptoCurrency, got: CryptoCurrency) {
            if (expected != got)
                throw IllegalArgumentException(
                    "Currency Mismatch. Expect $expected, got $got"
                )
        }

        private fun validateCurrency(expected: String, got: String) {
            if (expected != got)
                throw IllegalArgumentException(
                    "Currency Mismatch. Expect $expected, got $got"
                )
        }
    }
}

operator fun CryptoValue?.times(rate: ExchangeRate.CryptoToCrypto?) =
    this?.let { rate?.applyRate(it) }

operator fun CryptoValue?.div(rate: ExchangeRate.CryptoToCrypto?) =
    this?.let { rate?.inverse()?.applyRate(it) }

operator fun FiatValue?.times(rate: ExchangeRate.FiatToCrypto?) =
    this?.let { rate?.applyRate(it) }

operator fun CryptoValue?.times(exchangeRate: ExchangeRate.CryptoToFiat?) =
    this?.let { exchangeRate?.applyRate(it) }

operator fun CryptoValue?.div(exchangeRate: ExchangeRate.FiatToCrypto?) =
    this?.let { exchangeRate?.inverse()?.applyRate(it) }

operator fun FiatValue?.div(exchangeRate: ExchangeRate.CryptoToFiat?) =
    this?.let { exchangeRate?.inverse()?.applyRate(it) }

fun ExchangeRate?.percentageDelta(previous: ExchangeRate?): Double =
    if (this != null && previous != null && previous.rate != BigDecimal.ZERO) {
        val current = rate
        val prev = previous.rate

        (current - prev)
            .divide(prev, 4, RoundingMode.HALF_EVEN)
            .movePointRight(2)
            .toDouble()
    } else {
        Double.NaN
    }