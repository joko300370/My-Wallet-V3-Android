package info.blockchain.wallet.payload

import com.blockchain.testutils.satoshi
import com.blockchain.testutils.satoshiCash
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should throw the Exception`
import org.amshove.kluent.`with message`
import org.junit.Test
import java.math.BigInteger

class CryptoBalanceMapTest {

    @Test
    fun `empty values`() {
        calculateCryptoBalanceMap(
            CryptoCurrency.BTC,
            { emptyMap<String, Long>() }.toBalanceQuery(),
            emptyList(),
            emptyList()
        ).apply {
            totalSpendable `should equal` CryptoValue.zero(CryptoCurrency.BTC)
            totalSpendableImported `should equal` CryptoValue.zero(CryptoCurrency.BTC)
        }
    }

    @Test
    fun `XPub appears in total balance - alternative currency`() {
        calculateCryptoBalanceMap(
            CryptoCurrency.ETHER,
            { mapOf("A" to 123L) }.toBalanceQuery(),
            xpubs = listOf(
                XPubs(
                    XPub(address = "A", derivation = XPub.Format.LEGACY)
                )
            ),
            imported = emptyList()
        ).apply {
            totalSpendable `should equal` CryptoValue(CryptoCurrency.ETHER, 123L.toBigInteger())
            totalSpendableImported `should equal` CryptoValue.zero(CryptoCurrency.ETHER)
        }
    }

    @Test
    fun `XPub appears in total balance`() {
        calculateCryptoBalanceMap(
            CryptoCurrency.BTC,
            { mapOf("A" to 123L) }.toBalanceQuery(),
            xpubs = listOf(
                XPubs(
                    XPub(address = "A", derivation = XPub.Format.LEGACY)
                )
            ),
            imported = emptyList()
        ).apply {
            totalSpendable `should equal` 123.satoshi()
            totalSpendableImported `should equal` CryptoValue.zero(CryptoCurrency.BTC)
        }
    }

    @Test
    fun `two XPubs appear summed in total balance`() {
        calculateCryptoBalanceMap(
            CryptoCurrency.BTC,
            { mapOf("A" to 123L, "B" to 456L) }.toBalanceQuery(),
            xpubs = listOf(
                XPubs(XPub(address = "A", derivation = XPub.Format.LEGACY)),
                XPubs(XPub(address = "B", derivation = XPub.Format.LEGACY))
            ),
            imported = emptyList()
        ).apply {
            totalSpendable `should equal` 579.satoshi()
            totalSpendableImported `should equal` CryptoValue.zero(CryptoCurrency.BTC)
        }
    }

    @Test
    fun `spendable imported appears in spendable total and total`() {
        calculateCryptoBalanceMap(
            CryptoCurrency.BTC,
            { mapOf("A" to 123L) }.toBalanceQuery(),
            xpubs = emptyList(),
            imported = listOf("A")
        ).apply {
            totalSpendable `should equal` 123L.satoshi()
            totalSpendableImported `should equal` 123L.satoshi()
        }
    }

    @Test
    fun `all addresses are queried`() {
        val getBalances: BalanceQuery = mock {
            on { getBalancesFor(any(), any()) } `it returns` emptyMap()
        }

        val xpubs = listOf(
            XPubs(XPub(address = "A", derivation = XPub.Format.LEGACY)),
            XPubs(XPub(address = "B", derivation = XPub.Format.LEGACY)),
            XPubs(XPub(address = "G", derivation = XPub.Format.SEGWIT)),
            XPubs(XPub(address = "H", derivation = XPub.Format.SEGWIT))
        )

        val imported = listOf("C", "D")

        calculateCryptoBalanceMap(
            CryptoCurrency.BTC,
            getBalances,
            xpubs = xpubs,
            imported = imported
        ).apply {
            totalSpendable `should equal` CryptoValue.zero(CryptoCurrency.BTC)
            totalSpendableImported `should equal` CryptoValue.zero(CryptoCurrency.BTC)
        }

        verify(getBalances).getBalancesFor(xpubs, imported)
    }

    @Test
    fun `can look up individual balances`() {
        val xpubs = listOf(
            XPubs(XPub(address = "A", derivation = XPub.Format.LEGACY)),
            XPubs(XPub(address = "D", derivation = XPub.Format.SEGWIT))
        )

        calculateCryptoBalanceMap(
            CryptoCurrency.BCH,
            { mapOf(
                "A" to 100L,
                "B" to 200L,
                "C" to 300L,
                "D" to 400L,
                "Not listed" to 500L
            ) }.toBalanceQuery(),
            xpubs = xpubs,
            imported = listOf("B")
        ).apply {
            get("A") `should equal` 100L.satoshiCash()
            get("B") `should equal` 200L.satoshiCash()
            get("C") `should equal` 300L.satoshiCash()
            get("D") `should equal` 400L.satoshiCash()
            get("Not listed") `should equal` 500L.satoshiCash()
            get("Missing") `should equal` 0L.satoshiCash()
        }
    }

    @Test
    fun `can adjust an xpub balance`() {
        val xpubs = listOf(
            XPubs(XPub(address = "A", derivation = XPub.Format.LEGACY)),
            XPubs(XPub(address = "D", derivation = XPub.Format.SEGWIT))
        )

        calculateCryptoBalanceMap(
            CryptoCurrency.BTC,
            { mapOf("A" to 100L, "B" to 200L) }.toBalanceQuery(),
            xpubs = xpubs,
            imported = listOf("B")
        ).apply {
            totalSpendable `should equal` 300L.satoshi()
            totalSpendableImported `should equal` 200L.satoshi()
        }.run {
            subtractAmountFromAddress("A", 30L.satoshi())
        }.apply {
            totalSpendable `should equal` 270L.satoshi()
            totalSpendableImported `should equal` 200L.satoshi()
            get("A") `should equal` 70L.satoshi()
            get("B") `should equal` 200L.satoshi()
        }
    }

    @Test
    fun `can adjust a imported balance`() {
        val xpubs = listOf(
            XPubs(XPub(address = "A", derivation = XPub.Format.LEGACY)),
            XPubs(XPub(address = "D", derivation = XPub.Format.SEGWIT))
        )

        calculateCryptoBalanceMap(
            CryptoCurrency.BCH,
            { mapOf("A" to 100L, "B" to 200L) }.toBalanceQuery(),
            xpubs = xpubs,
            imported = listOf("B")
        ).apply {
            totalSpendable `should equal` 300.satoshiCash()
            totalSpendableImported `should equal` 200.satoshiCash()
        }.run {
            subtractAmountFromAddress("B", 50.satoshi())
        }.apply {
            totalSpendable `should equal` 250.satoshiCash()
            totalSpendableImported `should equal` 150.satoshiCash()
            get("A") `should equal` 100.satoshiCash()
            get("B") `should equal` 150.satoshiCash()
        }
    }

    @Test
    fun `can adjust a watch only balance`() {
        val xpubs = listOf(
            XPubs(XPub(address = "A", derivation = XPub.Format.LEGACY)),
            XPubs(XPub(address = "E", derivation = XPub.Format.SEGWIT))
        )

        calculateCryptoBalanceMap(
            CryptoCurrency.BCH,
            { mapOf("A" to 100L, "B" to 200L) }.toBalanceQuery(),
            xpubs = xpubs,
            imported = listOf("B")
        ).apply {
            totalSpendable `should equal` 300.satoshiCash()
            totalSpendableImported `should equal` 200.satoshiCash()
        }.apply {
            totalSpendable `should equal` 300.satoshiCash()
            totalSpendableImported `should equal` 200.satoshiCash()
            get("A") `should equal` 100.satoshiCash()
            get("B") `should equal` 200.satoshiCash()
        }
    }

    @Test
    fun `can't adjust a missing balance`() {

        val xpubs = listOf(
            XPubs(XPub(address = "A", derivation = XPub.Format.LEGACY)),
            XPubs(XPub(address = "D", derivation = XPub.Format.SEGWIT))
        )

        calculateCryptoBalanceMap(
            CryptoCurrency.BCH,
            { mapOf("A" to 100L, "B" to 200L) }.toBalanceQuery(),
            xpubs = xpubs,
            imported = listOf("B")
        ).apply {
            {
                subtractAmountFromAddress("Missing", 500L.satoshi())
            } `should throw the Exception` Exception::class `with message`
                    "No info for this address. updateAllBalances should be called first."
        }
    }
}

private fun (() -> Map<String, Long>).toBalanceQuery() =
    object : BalanceQuery {
        override fun getBalancesFor(
            xpubs: List<XPubs>,
            legacyImported: List<String>
        ): Map<String, BigInteger> {
            return this@toBalanceQuery().toBigIntegerMap()
        }
    }

private fun <K> Map<K, Long>.toBigIntegerMap() =
    map { (k, v) -> k to v.toBigInteger() }.toMap()
