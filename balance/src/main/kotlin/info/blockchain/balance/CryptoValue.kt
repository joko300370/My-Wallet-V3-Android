package info.blockchain.balance

import info.blockchain.utils.tryParseBigDecimal
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Locale

data class CryptoValue(
    val currency: CryptoCurrency,

    /**
     * Amount in the smallest unit of the currency, Satoshi/Wei for example.
     */
    val amount: BigInteger
) : Money {

    override val maxDecimalPlaces: Int = currency.dp

    override val userDecimalPlaces: Int = currency.userDp

    override val currencyCode = currency.networkTicker
    override val symbol = currency.displayTicker

    override fun toStringWithSymbol() = formatWithUnit(Locale.getDefault())

    override fun toStringWithoutSymbol() = format(Locale.getDefault())

    override fun toNetworkString(): String = format(Locale.US).removeComma()

    /**
     * Amount in the major value of the currency, Bitcoin/Ether for example.
     */
    override fun toBigDecimal(): BigDecimal = amount.toBigDecimal().movePointLeft(currency.dp)

    override val isPositive: Boolean get() = amount.signum() == 1

    override val isZero: Boolean get() = amount.signum() == 0

    companion object {
        val ZeroBtc = bitcoinFromSatoshis(0L)
        val ZeroBch = bitcoinCashFromSatoshis(0L)
        val ZeroEth = CryptoValue(CryptoCurrency.ETHER, BigInteger.ZERO)
        val ZeroStx = CryptoValue(CryptoCurrency.STX, BigInteger.ZERO)
        val ZeroXlm = CryptoValue(CryptoCurrency.XLM, BigInteger.ZERO)
        val ZeroPax = CryptoValue(CryptoCurrency.PAX, BigInteger.ZERO)
        val ZeroAlg = CryptoValue(CryptoCurrency.ALGO, BigInteger.ZERO)

        fun zero(cryptoCurrency: CryptoCurrency) = when (cryptoCurrency) {
            CryptoCurrency.BTC -> ZeroBtc
            CryptoCurrency.BCH -> ZeroBch
            CryptoCurrency.ETHER -> ZeroEth
            CryptoCurrency.XLM -> ZeroXlm
            CryptoCurrency.PAX -> ZeroPax
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALGO -> ZeroAlg
        }

        fun bitcoinFromSatoshis(satoshi: Long) = CryptoValue(CryptoCurrency.BTC, satoshi.toBigInteger())
        fun bitcoinFromSatoshis(satoshi: BigInteger) = CryptoValue(CryptoCurrency.BTC, satoshi)

        fun bitcoinFromMajor(bitcoin: Int) = bitcoinFromMajor(bitcoin.toBigDecimal())
        fun bitcoinFromMajor(bitcoin: BigDecimal) = fromMajor(CryptoCurrency.BTC, bitcoin)

        fun bitcoinCashFromSatoshis(satoshi: Long) = CryptoValue(CryptoCurrency.BCH, satoshi.toBigInteger())
        fun bitcoinCashFromSatoshis(satoshi: BigInteger) = CryptoValue(CryptoCurrency.BCH, satoshi)

        fun bitcoinCashFromMajor(bitcoinCash: Int) = bitcoinCashFromMajor(bitcoinCash.toBigDecimal())
        fun bitcoinCashFromMajor(bitcoinCash: BigDecimal) = fromMajor(CryptoCurrency.BCH, bitcoinCash)

        fun etherFromWei(wei: BigInteger) = CryptoValue(CryptoCurrency.ETHER, wei)

        fun etherFromMajor(ether: Long) = etherFromMajor(ether.toBigDecimal())
        fun etherFromMajor(ether: BigDecimal) = fromMajor(CryptoCurrency.ETHER, ether)

        fun lumensFromMajor(lumens: BigDecimal) = fromMajor(CryptoCurrency.XLM, lumens)
        fun lumensFromStroop(stroop: BigInteger) = CryptoValue(CryptoCurrency.XLM, stroop)

        fun usdPaxFromMajor(usdPax: BigDecimal) = fromMajor(CryptoCurrency.PAX, usdPax)
        fun usdPaxFromMinor(value: BigInteger) = CryptoValue(CryptoCurrency.PAX, value)

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

        fun min(a: CryptoValue, b: CryptoValue) = if (a <= b) a else b

        fun max(a: CryptoValue, b: CryptoValue) = if (a >= b) a else b
    }

    /**
     * Amount in the major value of the currency, Bitcoin/Ether for example.
     */
    fun toMajorUnitDouble() = toBigDecimal().toDouble()

    override fun toZero(): CryptoValue = zero(currency)

    operator fun plus(other: CryptoValue): CryptoValue {
        ensureComparable("add", currency, other.currency)
        return CryptoValue(currency, amount + other.amount)
    }

    operator fun minus(other: CryptoValue): CryptoValue {
        ensureComparable("subtract", currency, other.currency)
        return CryptoValue(currency, amount - other.amount)
    }
}

operator fun CryptoValue.compareTo(other: CryptoValue): Int {
    ensureComparable("compare", currency, other.currency)
    return amount.compareTo(other.amount)
}

private fun ensureComparable(operation: String, a: CryptoCurrency, b: CryptoCurrency) {
    if (a != b) throw ValueTypeMismatchException(operation, a.networkTicker, b.networkTicker)
}

fun CryptoCurrency.withMajorValue(majorValue: BigDecimal) = CryptoValue.fromMajor(this, majorValue)

fun CryptoCurrency.withMajorValueOrZero(majorValue: String, locale: Locale = Locale.getDefault()) =
    CryptoValue.fromMajor(this, majorValue.tryParseBigDecimal(locale) ?: BigDecimal.ZERO)
