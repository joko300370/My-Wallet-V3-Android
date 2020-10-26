package info.blockchain.balance

import info.blockchain.utils.tryParseBigDecimal
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Locale

data class CryptoValue(
    val currency: CryptoCurrency,
    private val amount: BigInteger // Amount in the minor unit of the currency, Satoshi/Wei for example.
) : Money() {

    override val maxDecimalPlaces: Int = currency.dp

    override val userDecimalPlaces: Int = currency.userDp

    override val currencyCode = currency.networkTicker
    override val symbol = currency.displayTicker

    override fun toStringWithSymbol() = formatWithUnit(Locale.getDefault())

    override fun toStringWithoutSymbol() = format(Locale.getDefault())

    override fun toNetworkString(): String = format(Locale.US).removeComma()

    override fun toFiat(exchangeRates: ExchangeRates, fiatCurrency: String): FiatValue =
        FiatValue.fromMajor(
            fiatCurrency,
            exchangeRates.getLastPrice(currency, fiatCurrency).toBigDecimal() * toBigDecimal()
        )

    /**
     * Amount in the major value of the currency, Bitcoin/Ether for example.
     */
    override fun toBigDecimal(): BigDecimal = amount.toBigDecimal().movePointLeft(currency.dp)

    override fun toBigInteger(): BigInteger = amount
    override fun toFloat(): Float = toBigDecimal().toFloat()

    override val isPositive: Boolean get() = amount.signum() == 1

    override val isZero: Boolean get() = amount.signum() == 0

    companion object {
        val ZeroBtc = CryptoValue(CryptoCurrency.BTC, BigInteger.ZERO)
        val ZeroBch = CryptoValue(CryptoCurrency.BCH, BigInteger.ZERO)
        val ZeroEth = CryptoValue(CryptoCurrency.ETHER, BigInteger.ZERO)
        val ZeroStx = CryptoValue(CryptoCurrency.STX, BigInteger.ZERO)
        val ZeroXlm = CryptoValue(CryptoCurrency.XLM, BigInteger.ZERO)
        val ZeroPax = CryptoValue(CryptoCurrency.PAX, BigInteger.ZERO)
        val ZeroAlg = CryptoValue(CryptoCurrency.ALGO, BigInteger.ZERO)
        val ZeroUsdt = CryptoValue(CryptoCurrency.USDT, BigInteger.ZERO)

        fun zero(cryptoCurrency: CryptoCurrency) = when (cryptoCurrency) {
            CryptoCurrency.BTC -> ZeroBtc
            CryptoCurrency.BCH -> ZeroBch
            CryptoCurrency.ETHER -> ZeroEth
            CryptoCurrency.XLM -> ZeroXlm
            CryptoCurrency.PAX -> ZeroPax
            CryptoCurrency.STX -> ZeroStx
            CryptoCurrency.ALGO -> ZeroAlg
            CryptoCurrency.USDT -> ZeroUsdt
        }

        // These calls are currently (mostly) only used in tests, where we have a second mechanism
        // [xx.bitcoin(), xx.satoshi() etc] for generating CryptoValues.
        // So ALL these coin specific calls are deprecated and will be removed once all the tests
        // are updated to use the other, more readable, mechanism. Use the suggested
        // replacement in test code. If found in prod code, then it's CryptoValue.fromMajor/fromMinor etc
        @Deprecated("Historical method", ReplaceWith("satoshi.satoshi()"))
        fun bitcoinFromSatoshis(satoshi: Long) =
            CryptoValue(CryptoCurrency.BTC, satoshi.toBigInteger())

        @Deprecated("Historical method", ReplaceWith("satoshi.satoshiCash()"))
        fun bitcoinCashFromSatoshis(satoshi: Long) =
            CryptoValue(CryptoCurrency.BCH, satoshi.toBigInteger())

        @Deprecated("Historical method", ReplaceWith("stroop.stroops()"))
        fun lumensFromStroop(stroop: BigInteger) = CryptoValue(CryptoCurrency.XLM, stroop)

        fun fromMajor(
            currency: CryptoCurrency,
            major: BigDecimal
        ) = CryptoValue(currency, major.movePointRight(currency.dp).toBigInteger())

        fun fromMinor(
            currency: CryptoCurrency,
            minor: BigDecimal
        ) = CryptoValue(currency, minor.toBigInteger())

        fun fromMinor(
            currency: CryptoCurrency,
            minor: BigInteger
        ) = CryptoValue(currency, minor)
    }

    /**
     * Amount in the major value of the currency, Bitcoin/Ether for example.
     */
    fun toMajorUnitDouble() = toBigDecimal().toDouble()

    override fun toZero(): CryptoValue = zero(currency)

    fun abs(): CryptoValue = CryptoValue(currency, amount.abs())

    override fun add(other: Money): CryptoValue {
        require(other is CryptoValue)
        return CryptoValue(currency, amount + other.amount)
    }

    override fun subtract(other: Money): CryptoValue {
        require(other is CryptoValue)
        return CryptoValue(currency, amount - other.amount)
    }

    override fun compare(other: Money): Int {
        require(other is CryptoValue)
        return amount.compareTo(other.amount)
    }

    override fun division(other: Money): Money {
        require(other is CryptoValue)
        return CryptoValue(currency, amount / other.amount)
    }

    override fun ensureComparable(operation: String, other: Money) {
        if (other is CryptoValue) {
            if (currency != other.currency) {
                throw ValueTypeMismatchException(operation, currencyCode, other.currencyCode)
            }
        } else {
            throw ValueTypeMismatchException(operation, currencyCode, other.currencyCode)
        }
    }
}

fun CryptoCurrency.withMajorValue(majorValue: BigDecimal) = CryptoValue.fromMajor(this, majorValue)

fun CryptoCurrency.withMajorValueOrZero(majorValue: String, locale: Locale = Locale.getDefault()) =
    CryptoValue.fromMajor(this, majorValue.tryParseBigDecimal(locale) ?: BigDecimal.ZERO)
