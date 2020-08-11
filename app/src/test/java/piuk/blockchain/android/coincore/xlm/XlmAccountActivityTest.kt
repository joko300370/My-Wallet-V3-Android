package piuk.blockchain.android.coincore.xlm

import com.blockchain.android.testutils.rxInit
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.sunriver.HorizonKeyPair
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.models.XlmTransaction
import com.blockchain.testutils.stroops
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class XlmAccountActivityTest {

    private val payloadManager: PayloadDataManager = mock()
    private val currencyState: CurrencyState = mock()
    private val exchangeRates: ExchangeRateDataManager = mock()
    private val currencyPrefs: CurrencyPrefs = mock()

    private val xlmDataManager: XlmDataManager = mock()

    private val subject =
        XlmCryptoWalletAccount(
            payloadManager = payloadManager,
            label = "Test Xlm Account",
            address = "Test XLM Address",
            xlmManager = xlmDataManager,
            exchangeRates = exchangeRates
        )

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setup() {
        whenever(currencyPrefs.selectedFiatCurrency).thenReturn("USD")
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.XLM)
    }

    @Test
    fun getXlmTransactionList() {
        // Arrange
        val output = BigInteger.valueOf(1000000L)
        val xlmTransaction = XlmTransaction(
            "2018-10-11T12:54:15Z",
            output.stroops(),
            100.stroops(),
            "hash",
            HorizonKeyPair.Public(HORIZON_ACCOUNT_ID_1),
            HorizonKeyPair.Public(HORIZON_ACCOUNT_ID_2)
        )

        whenever(xlmDataManager.getTransactionList())
            .thenReturn(Single.just(listOf(xlmTransaction)))

        // Act
        val test = subject.activity.test()

        verify(xlmDataManager).getTransactionList()

        val result = test.values()[0]
        assertEquals(1, result.size.toLong())

        val activityItem = result[0] as NonCustodialActivitySummaryItem
        assertEquals(CryptoCurrency.XLM, activityItem.cryptoCurrency)
        assertEquals("hash", activityItem.txId)
        assertEquals(TransactionSummary.Direction.RECEIVED, activityItem.direction)
        assertEquals(1, activityItem.confirmations.toLong())
        assertFalse(activityItem.isFeeTransaction)
        assertEquals(output, activityItem.value.toBigInteger())
        assertEquals(
            mapOf(HORIZON_ACCOUNT_ID_2 to CryptoValue.fromMinor(CryptoCurrency.XLM, BigInteger.ZERO)),
            activityItem.inputsMap
        )
        assertEquals(
            mapOf(HORIZON_ACCOUNT_ID_1 to CryptoValue.fromMinor(CryptoCurrency.XLM, output)),
            activityItem.outputsMap
        )
    }

    companion object {
        private const val HORIZON_ACCOUNT_ID_1 =
            "GAIH3ULLFQ4DGSECF2AR555KZ4KNDGEKN4AFI4SU2M7B43MGK3QJZNSR"
        private const val HORIZON_ACCOUNT_ID_2 =
            "GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4"
    }
}