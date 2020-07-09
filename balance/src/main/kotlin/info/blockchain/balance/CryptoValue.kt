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

    override fun toFiat(exchangeRates: ExchangeRates, fiatCurrency: String) =
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

        fun bitcoinFromSatoshis(satoshi: Long) =
            CryptoValue(CryptoCurrency.BTC, satoshi.toBigInteger())

        fun bitcoinFromMajor(bitcoin: Int) = bitcoinFromMajor(bitcoin.toBigDecimal())
        fun bitcoinFromMajor(bitcoin: BigDecimal) = fromMajor(CryptoCurrency.BTC, bitcoin)

        fun bitcoinCashFromSatoshis(satoshi: Long) =
            CryptoValue(CryptoCurrency.BCH, satoshi.toBigInteger())

        fun bitcoinCashFromMajor(bitcoinCash: Int) =
            bitcoinCashFromMajor(bitcoinCash.toBigDecimal())

        fun bitcoinCashFromMajor(bitcoinCash: BigDecimal) =
            fromMajor(CryptoCurrency.BCH, bitcoinCash)

        fun etherFromMajor(ether: Long) = etherFromMajor(ether.toBigDecimal())
        fun etherFromMajor(ether: BigDecimal) = fromMajor(CryptoCurrency.ETHER, ether)

        fun lumensFromMajor(lumens: BigDecimal) = fromMajor(CryptoCurrency.XLM, lumens)
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
