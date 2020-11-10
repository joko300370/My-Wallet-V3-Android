package piuk.blockchain.android.coincore.xlm

import com.blockchain.android.testutils.rxInit
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.sunriver.HorizonKeyPair
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.XlmFeesFetcher
import com.blockchain.sunriver.models.XlmTransaction
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.TransferDirection
import com.blockchain.swap.nabu.datamanagers.SwapOrderState
import com.blockchain.swap.nabu.datamanagers.repositories.swap.SwapTransactionItem
import com.blockchain.testutils.stroops
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.coincore.SwapActivitySummaryItem
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import java.math.BigInteger

class XlmAccountActivityTest {

    private val payloadManager: PayloadDataManager = mock()
    private val exchangeRates: ExchangeRateDataManager = mock()
    private val currencyPrefs: CurrencyPrefs = mock()

    private val xlmDataManager: XlmDataManager = mock()
    private val xlmFeesFetcher: XlmFeesFetcher = mock()
    private val walletOptionsDataManager: WalletOptionsDataManager = mock()
    private val walletPreferences: WalletStatus = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()

    private val subject =
        XlmCryptoWalletAccount(
            payloadManager = payloadManager,
            label = "Test Xlm Account",
            address = "Test XLM Address",
            xlmManager = xlmDataManager,
            exchangeRates = exchangeRates,
            xlmFeesFetcher = xlmFeesFetcher,
            walletOptionsDataManager = walletOptionsDataManager,
            walletPreferences = walletPreferences,
            custodialWalletManager = custodialWalletManager
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

        val swapSummary = SwapTransactionItem(
            "123",
            1L,
            TransferDirection.ON_CHAIN,
            "sendingAddress",
            "receivingAddress",
            SwapOrderState.FINISHED,
            CryptoValue.ZeroXlm,
            CryptoValue.ZeroBtc,
            CryptoValue.ZeroBtc,
            CryptoCurrency.XLM,
            CryptoCurrency.BTC,
            FiatValue.zero("USD"),
            "USD"
        )

        val summaryList = listOf(swapSummary)
        whenever(custodialWalletManager.getSwapActivityForAsset(CryptoCurrency.XLM, subject.nonCustodialSwapDirections))
            .thenReturn(Single.just(summaryList))

        // Act
        subject.activity.test()
            .assertValueCount(1)
            .assertComplete()
            .assertNoErrors()
            .assertValueAt(0) {
                val xlmItem = it[0]
                val swapItem = it[1]
                it.size == 2 &&
                    swapItem is SwapActivitySummaryItem &&
                    swapItem.txId == swapSummary.txId &&
                    swapItem.direction == swapSummary.direction &&
                    swapItem.sendingAsset == swapSummary.sendingAsset &&
                    swapItem.receivingAsset == swapSummary.receivingAsset &&
                    swapItem.sendingAddress == swapSummary.sendingAddress &&
                    swapItem.receivingAddress == swapSummary.receivingAddress &&
                    swapItem.state == swapSummary.state &&
                    swapItem.fiatValue == swapSummary.fiatValue &&
                    swapItem.fiatCurrency == swapSummary.fiatCurrency &&
                    xlmItem is NonCustodialActivitySummaryItem &&
                    CryptoCurrency.XLM == xlmItem.cryptoCurrency &&
                    xlmTransaction.hash == xlmItem.txId &&
                    TransactionSummary.TransactionType.RECEIVED == xlmItem.transactionType &&
                    1 == xlmItem.confirmations &&
                    !xlmItem.isFeeTransaction &&
                    output == xlmItem.value.toBigInteger() &&
                    mapOf(HORIZON_ACCOUNT_ID_2 to CryptoValue.fromMinor(CryptoCurrency.XLM,
                        BigInteger.ZERO)) == xlmItem.inputsMap &&
                    mapOf(
                        HORIZON_ACCOUNT_ID_1 to CryptoValue.fromMinor(CryptoCurrency.XLM, output)) == xlmItem.outputsMap
            }

        verify(xlmDataManager).getTransactionList()
        verify(custodialWalletManager).getSwapActivityForAsset(CryptoCurrency.XLM, subject.nonCustodialSwapDirections)
    }

    companion object {
        private const val HORIZON_ACCOUNT_ID_1 =
            "GAIH3ULLFQ4DGSECF2AR555KZ4KNDGEKN4AFI4SU2M7B43MGK3QJZNSR"
        private const val HORIZON_ACCOUNT_ID_2 =
            "GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4"
    }
}