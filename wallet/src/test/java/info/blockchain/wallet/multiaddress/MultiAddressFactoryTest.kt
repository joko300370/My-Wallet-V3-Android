package info.blockchain.wallet.multiaddress

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.api.BitcoinApi
import info.blockchain.wallet.MockedResponseTest
import info.blockchain.wallet.multiaddress.TransactionSummary.TransactionType
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.util.parseMultiAddressResponse
import org.amshove.kluent.itReturns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Call
import retrofit2.Response

class MultiAddressFactoryTest : MockedResponseTest() {

    private val bitcoinApi: BitcoinApi = mock()
    private val subject = MultiAddressFactory(bitcoinApi)

    private val dormantAddress = "1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW"
    private val dormantXpub =
        "xpub6CFgfYG9chNp7rzZ7ByXyAJruku5JSVhtGmGqR9tmeLRwu3jtioyBZpXC" +
                "6GAnpMQPBQg5rviqTwMN4EwgMCZNVT3N22sSnM1yEfBQzjHXJt"

    @Test
    fun getMultiAddress_legacyAddressOnly() {
        val resource = loadResourceContent("multiaddress/multi_address_1jH7K.txt")
        val body = parseMultiAddressResponse(resource)

        val mockResponse = mockApiResponse(body)
        whenever(bitcoinApi.getMultiAddress(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(mockResponse)

        val xpub = XPub(address = dormantAddress, derivation = XPub.Format.LEGACY)
        val xpubs = XPubs(xpub)

        val summary = subject.getAccountTransactions(
            all = listOf(xpubs),
            activeImported = null,
            onlyShow = listOf(dormantAddress),
            limit = 100,
            offset = 0,
            startingBlockHeight = 0
        )
        assertEquals(2, summary.size.toLong())
        assertEquals(1, summary[0].getInputsMap().size.toLong())
        assertEquals(1, summary[0].getOutputsMap().size.toLong())
        assertEquals(20000, summary[0].total.toLong())
        assertEquals(TransactionType.SENT, summary[0].transactionType)
        assertEquals(10000, summary[0].fee.toLong())
        assertEquals(1436437493, summary[0].time)
        assertEquals(
            "04734caac4e2ae7feba9b74fb8d2c145db9ea9651487371c4d741428f8f5a24b", summary[0].hash
        )
    }

    @Test
    fun getMultiAddress_xpubOnly() {
        val resource = loadResourceContent("multiaddress/multi_address_xpub6CFg.txt")
        val body = parseMultiAddressResponse(resource)

        val mockResponse = mockApiResponse(body)
        whenever(bitcoinApi.getMultiAddress(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(mockResponse)

        val xpub = XPub(address = dormantAddress, derivation = XPub.Format.LEGACY)
        val xpubs = XPubs(xpub)

        val summary = subject.getAccountTransactions(
            listOf(xpubs),
            null,
            listOf(dormantXpub),
            100,
            0,
            0
        )

        assertEquals(34, summary.size.toLong())
        assertEquals(1, summary[0].getInputsMap().size.toLong())
        assertEquals(1, summary[0].getOutputsMap().size.toLong())
        assertEquals(20000, summary[0].total.toLong())
        assertEquals(TransactionType.SENT, summary[0].transactionType)
        assertEquals(10000, summary[0].fee.toLong())
        assertEquals(1452868237, summary[0].time)
        assertEquals(
            "34c22edb3466708b974a7549d5b3cb51e05d4444f74d2a1b41484f8711dffd04", summary[0].hash
        )

        assertEquals(5, subject.getNextChangeAddressIndex(dormantXpub).toLong())
        assertEquals(
            10, subject.getNextReceiveAddressIndex(dormantXpub, ArrayList()).toLong()
        )

        assertTrue(subject.isOwnHDAddress("1CAAzobQ2UrE4QUR3HJrkZs8UFA8wi5wwQ"))
        assertTrue(subject.isOwnHDAddress("1KTKN43STRsmRSNtChuDUzQtcQGMXyBMN1"))
        assertTrue(subject.isOwnHDAddress("1CkFCfj7YQ8hjH1ReW398rax9NXCJcceE9"))
        assertFalse(subject.isOwnHDAddress("1PPNN4psDFyAgdjQcKBJ8GSgE4ES4GHP9c"))
    }

    @Test
    fun getMultiAddress_xpubAndLegacyAddress() {
        val resource = loadResourceContent("multiaddress/multi_address_all.txt")
        val body = parseMultiAddressResponse(resource)

        val mockResponse = mockApiResponse(body)
        whenever(bitcoinApi.getMultiAddress(any(), any(), any(), anyOrNull(), any(), any(), any()))
            .thenReturn(mockResponse)

        val xpubs1 = XPubs(XPub(address = dormantAddress, derivation = XPub.Format.LEGACY))
        val xpubs2 = XPubs(XPub(address = dormantXpub, derivation = XPub.Format.LEGACY))

        /* List<TransactionSummary> summary =*/
        val summary = subject.getAccountTransactions(
            all = listOf(xpubs1, xpubs2),
            activeImported = null,
            onlyShow = null,
            limit = 100,
            offset = 0,
            startingBlockHeight = 0
        )

        assertEquals(36, summary.size.toLong())
        assertEquals(1, summary[0].getInputsMap().size.toLong())
        assertEquals(1, summary[0].getOutputsMap().size.toLong())
        assertEquals(20000, summary[0].total.toLong())
        assertEquals(TransactionType.SENT, summary[0].transactionType)
        assertEquals(10000, summary[0].fee.toLong())
        assertEquals(1452868237, summary[0].time)
        assertEquals(
            "34c22edb3466708b974a7549d5b3cb51e05d4444f74d2a1b41484f8711dffd04", summary[0].hash
        )

        assertTrue(subject.isOwnHDAddress("1CAAzobQ2UrE4QUR3HJrkZs8UFA8wi5wwQ"))
        assertTrue(subject.isOwnHDAddress("1KTKN43STRsmRSNtChuDUzQtcQGMXyBMN1"))
        assertTrue(subject.isOwnHDAddress("1CkFCfj7YQ8hjH1ReW398rax9NXCJcceE9"))
        assertFalse(subject.isOwnHDAddress("1PPNN4psDFyAgdjQcKBJ8GSgE4ES4GHP9c"))

        assertEquals(5, subject.getNextChangeAddressIndex(dormantXpub).toLong())
        assertEquals(10, subject.getNextReceiveAddressIndex(dormantXpub, ArrayList()).toLong())
    }

    @Test
    fun getMultiAddress_MoreCases() {

        val xpub1 =
            "xpub6Bx1J3neE11W2XpvKRFQVwWpZFsDfnRkLJ2V4JjPWNRD" +
                    "XbRvZrwnytbSbBng2F1fRejxkMWAi6fYJuAJrGg6TP8Key4jvs9YqpVo5LJ8jSk"
        val xpub2 =
            "xpub6Bx1J3neE11W3XsMUTWVBKECFJee9Tj" +
                    "JDSZJ53LKhr7AaAPJpNtz4KZTCe8nctTdu6kLYB4uZncjsy7EBi18mKb4HLg3WLfhPFW2KFGjScE"
        val address = "1DtkXqBjvXWsboMpc72U1kfRrK8JTntBLQ"

        val resource = loadResourceContent("multiaddress/multi_address_1Dtk.txt")
        val body = parseMultiAddressResponse(resource)

        val mockResponse = mockApiResponse(body)
        whenever(bitcoinApi.getMultiAddress(any(), any(), any(), anyOrNull(), any(), any(), any()))
            .thenReturn(mockResponse)

        val summary = subject.getAccountTransactions(
            listOf(
                XPubs(XPub(address = xpub1, derivation = XPub.Format.LEGACY)),
                XPubs(XPub(address = xpub2, derivation = XPub.Format.LEGACY)),
                XPubs(XPub(address = address, derivation = XPub.Format.LEGACY))
            ),
            null,
            null,
            100, 0, 0
        )

        assertEquals(7, summary.size.toLong())

        var txSummary = summary[0]
        assertEquals(1, txSummary.getInputsMap().size.toLong())
        assertEquals(1, txSummary.getOutputsMap().size.toLong())
        assertEquals(166486, txSummary.total.toLong())
        assertEquals(TransactionType.SENT, txSummary.transactionType)
        assertEquals(27120, txSummary.fee.toLong())
        assertEquals(1492614742, txSummary.time)
        assertEquals("de2db2e9b430f949f8c94ef4cd9093a020ef10c614b6802320920f7d84a8afab", txSummary.hash)

        txSummary = summary[1]
        assertEquals(1, txSummary.getInputsMap().size.toLong())
        assertEquals(1, txSummary.getOutputsMap().size.toLong())
        assertEquals(446212, txSummary.total.toLong())
        assertEquals(TransactionType.SENT, txSummary.transactionType)
        assertEquals(27120, txSummary.fee.toLong())
        assertEquals(1492614706, txSummary.time)
        assertEquals("8a5327e09c1789f9ef9467298bfb8e46748effd79ff981226df14e5a468378b6", txSummary.hash)

        txSummary = summary[2]
        assertEquals(1, txSummary.getInputsMap().size.toLong())
        assertEquals(1, txSummary.getOutputsMap().size.toLong())
        assertEquals(166486, txSummary.total.toLong())
        assertEquals(TransactionType.TRANSFERRED, txSummary.transactionType)
        assertEquals(27120, txSummary.fee.toLong())
        assertEquals(1492614681, txSummary.time)
        assertEquals("165b251a736e0e5d1e9aa287687b8d6fd5eb91c72b1138dd6047e34f8ed17217", txSummary.hash)

        txSummary = summary[3]
        assertEquals(1, txSummary.getInputsMap().size.toLong())
        assertEquals(1, txSummary.getOutputsMap().size.toLong())
        assertEquals(83243, txSummary.total.toLong())
        assertEquals(TransactionType.TRANSFERRED, txSummary.transactionType)
        assertEquals(27120, txSummary.fee.toLong())
        assertEquals(1492614642, txSummary.time)
        assertEquals("0b2804884f0ae1d151a7260d2009168078259ef6428c861b001ce6a028a19977", txSummary.hash)

        txSummary = summary[4]
        assertEquals(1, txSummary.getInputsMap().size.toLong())
        assertEquals(1, txSummary.getOutputsMap().size.toLong())
        assertEquals(750181, txSummary.total.toLong())
        assertEquals(TransactionType.RECEIVED, txSummary.transactionType)
        assertEquals(0, txSummary.fee.toLong())
        assertEquals(1492614623, txSummary.time)
        assertEquals("9fccf050f52ed23ee4fe20a89b03780a944d795ad897b38ff44a7369d6c7e665", txSummary.hash)

        txSummary = summary[5]
        assertEquals(1, txSummary.getInputsMap().size.toLong())
        assertEquals(2, txSummary.getOutputsMap().size.toLong())
        assertEquals(909366, txSummary.total.toLong())
        assertEquals(TransactionType.SENT, txSummary.transactionType)
        assertEquals(133680, txSummary.fee.toLong())
        assertEquals(1492497642, txSummary.time)
        assertEquals("8765362f7fd1895bb35942197c9f74a6e25c85d0043f38858021442b20bfa112", txSummary.hash)

        txSummary = summary[6]
        assertEquals(1, txSummary.getInputsMap().size.toLong())
        assertEquals(1, txSummary.getOutputsMap().size.toLong())
        assertEquals(909366, txSummary.total.toLong())
        assertEquals(TransactionType.RECEIVED, txSummary.transactionType)
        assertEquals(0, txSummary.fee.toLong())
        assertEquals(1486028570, txSummary.time)
        assertEquals("50115fce313d537b4a97ea24bb42d08b48f21d921b5710b765f07fc4fd23b101", txSummary.hash)
    }

    @Test
    fun getMultiAddress_MoreCases2() {
        val xpub1 =
            "xpub6CdH6yzYXhTtR7UHJHtoTeWm3nbuyg9msj3rJvFnfMew9C" +
                    "Bff6Rp62zdTrC57Spz4TpeRPL8m9xLiVaddpjEx4Dzidtk44rd4N2xu9XTrSV"
        val xpub2 =
            "xpub6CdH6yzYXhTtTGPPL4Djjp1HqFmAPx4uyqoG6Ffz9nPysv8vR8t8PEJ" +
                    "3RGaSRwMm7kRZ3MAcKgB6u4g1znFo82j4q2hdShmDyw3zuMxhDSL"
        val address = "189iKJLruPtUorasDuxmc6fMRVxz6zxpPS"

        val resource = loadResourceContent("multiaddress/wallet_v3_6_m1.txt")
        val body = parseMultiAddressResponse(resource)

        val mockResponse = mockApiResponse(body)
        whenever(bitcoinApi.getMultiAddress(any(), any(), any(), anyOrNull(), any(), any(), any()))
            .thenReturn(mockResponse)

        val transactionSummaries = subject.getAccountTransactions(
            listOf(
                XPubs(XPub(address = xpub1, derivation = XPub.Format.LEGACY)),
                XPubs(XPub(address = xpub2, derivation = XPub.Format.LEGACY)),
                XPubs(XPub(address = address, derivation = XPub.Format.LEGACY))
            ),
            null,
            null,
            100,
            0,
            0
        )

        assertEquals(8, transactionSummaries.size.toLong())

        var summary = transactionSummaries[0]
        assertEquals(68563, summary.total.toLong())
        assertEquals(TransactionType.TRANSFERRED, summary.transactionType)
        assertEquals(1, summary.getInputsMap().size.toLong())
        assertTrue(
            summary.getInputsMap().keys.contains("125QEfWq3eKzAQQHeqcMcDMeZGm13hVRvU")
        ) // My Bitcoin Account
        assertEquals(2, summary.getOutputsMap().size.toLong())
        assertTrue(
            summary.getOutputsMap().keys.contains("1Nm1yxXCTodAkQ9RAEquVdSneJGeubqeTw")
        ) // Savings account
        assertTrue(summary.getOutputsMap().keys.contains("189iKJLruPtUorasDuxmc6fMRVxz6zxpPS"))

        summary = transactionSummaries[1]
        assertEquals(138068, summary.total.toLong())
        assertEquals(TransactionType.SENT, summary.transactionType)
        assertEquals(1, summary.getInputsMap().size.toLong())
        assertTrue(
            summary.getInputsMap().keys.contains("1CQpuTQrJQLW6PEar17zsd9EV14cZknqWJ")
        ) // My Bitcoin Wallet
        assertEquals(2, summary.getOutputsMap().size.toLong())
        assertTrue(summary.getOutputsMap().keys.contains("1LQwNvEMnYjNCNxeUJzDfD8mcSqhm2ouPp"))
        assertTrue(summary.getOutputsMap().keys.contains("1AdTcerDBY735kDhQWit5Scroae6piQ2yw"))

        summary = transactionSummaries[2]
        assertEquals(800100, summary.total.toLong())
        assertEquals(TransactionType.RECEIVED, summary.transactionType)
        assertEquals(1, summary.getInputsMap().size.toLong())
        assertTrue(summary.getInputsMap().keys.contains("19CMnkUgBnTBNiTWXwoZr6Gb3aeXKHvuGG"))
        assertEquals(1, summary.getOutputsMap().size.toLong())
        assertTrue(
            summary.getOutputsMap().keys.contains("1CQpuTQrJQLW6PEar17zsd9EV14cZknqWJ")
        ) // My Bitcoin Wallet

        summary = transactionSummaries[3]
        assertEquals(35194, summary.total.toLong())
        assertEquals(TransactionType.SENT, summary.transactionType)
        assertEquals(1, summary.getInputsMap().size.toLong())
        assertTrue(
            summary.getInputsMap().keys.contains("15HjFY96ZANBkN5kvPRgrXH93jnntqs32n")
        ) // My Bitcoin Wallet
        assertEquals(1, summary.getOutputsMap().size.toLong())
        assertTrue(
            summary.getOutputsMap().keys.contains("1PQ9ZYhv9PwbWQQN74XRqUCjC32JrkyzB9")
        )

        summary = transactionSummaries[4]
        assertEquals(98326, summary.total.toLong())
        assertEquals(TransactionType.TRANSFERRED, summary.transactionType)
        assertEquals(1, summary.getInputsMap().size.toLong())
        assertTrue(
            summary.getInputsMap().keys.contains("1Peysd3qYDe35yNp6KB1ZkbVYHr42JT9zZ")
        ) // My Bitcoin Wallet
        assertEquals(1, summary.getOutputsMap().size.toLong())
        assertTrue(summary.getOutputsMap().keys.contains("189iKJLruPtUorasDuxmc6fMRVxz6zxpPS"))

        summary = transactionSummaries[5]
        assertEquals(160640, summary.total.toLong())
        assertEquals(TransactionType.RECEIVED, summary.transactionType)
        assertEquals(1, summary.getInputsMap().size.toLong())
        assertTrue(summary.getInputsMap().keys.contains("1BZe6YLaf2HiwJdnBbLyKWAqNia7foVe1w"))
        assertEquals(1, summary.getOutputsMap().size.toLong())
        assertTrue(
            summary.getOutputsMap().keys.contains("1Peysd3qYDe35yNp6KB1ZkbVYHr42JT9zZ")
        ) // My Bitcoin Wallet

        summary = transactionSummaries[6]
        assertEquals(9833, summary.total.toLong())
        assertEquals(TransactionType.TRANSFERRED, summary.transactionType)
        assertEquals(1, summary.getInputsMap().size.toLong())
        assertTrue(
            summary.getInputsMap().keys.contains("17ijgwpGsVQRzMjsdAfdmeP53kpw9yvXur")
        ) // My Bitcoin Wallet
        assertEquals(1, summary.getOutputsMap().size.toLong())
        assertTrue(
            summary.getOutputsMap().keys.contains("1AtunWT3F6WvQc3aaPuPbNGeBpVF3ZPM5r")
        ) // Savings account

        summary = transactionSummaries[7]
        assertEquals(40160, summary.total.toLong())
        assertEquals(TransactionType.RECEIVED, summary.transactionType)
        assertEquals(1, summary.getInputsMap().size.toLong())
        assertTrue(summary.getInputsMap().keys.contains("1Baa1cjB1CyBVSjw8SkFZ2YBuiwKnKLXhe"))
        assertEquals(1, summary.getOutputsMap().size.toLong())
        assertTrue(
            summary.getOutputsMap().keys.contains("17ijgwpGsVQRzMjsdAfdmeP53kpw9yvXur")
        ) // My Bitcoin Wallet
    }

    private fun <T> mockApiResponse(responseBody: T): Call<T> {
        val response: Response<T> = mock {
            on { body() } itReturns responseBody
            on { isSuccessful } itReturns true
        }

        return mock {
            on { execute() } itReturns response
        }
    }
}