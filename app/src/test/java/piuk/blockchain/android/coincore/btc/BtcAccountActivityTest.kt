package piuk.blockchain.android.coincore.btc

import com.blockchain.android.testutils.rxInit
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.SwapDirection
import com.blockchain.swap.nabu.datamanagers.SwapOrderState
import com.blockchain.swap.nabu.datamanagers.repositories.swap.SwapTransactionItem
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Single
import org.bitcoinj.core.NetworkParameters
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.SwapActivitySummaryItem
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import java.math.BigInteger

class BtcAccountActivityTest {

    private val payloadDataManager: PayloadDataManager = mock()
    private val sendDataManager: SendDataManager = mock()
    private val feeDataManager: FeeDataManager = mock()
    private val exchangeRates: ExchangeRateDataManager = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val networkParameters: NetworkParameters = mock()
    private val walletPrefs: WalletStatus = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()

    private val subject =
        BtcCryptoWalletAccount(
            label = "TestBtcAccount",
            address = "",
            payloadManager = payloadDataManager,
            hdAccountIndex = -1,
            sendDataManager = sendDataManager,
            feeDataManager = feeDataManager,
            isDefault = true,
            exchangeRates = exchangeRates,
            networkParameters = networkParameters,
            internalAccount = mock(),
            isHDAccount = true,
            walletPreferences = walletPrefs,
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
    fun fetchTransactionsOnAccount() {

        val summary = TransactionSummary().apply {
            confirmations = 3
            transactionType = TransactionSummary.TransactionType.RECEIVED
            fee = BigInteger.ONE
            total = BigInteger.TEN
            hash = "hash"
            inputsMap = HashMap()
            outputsMap = HashMap()
            time = 1000000L
        }

        val transactionSummaries = listOf(summary)

        val swapSummary = SwapTransactionItem(
            "123",
            1L,
            SwapDirection.ON_CHAIN,
            "sendingAddress",
            "receivingAddress",
            SwapOrderState.FINISHED,
            CryptoValue.ZeroBtc,
            CryptoValue.ZeroEth,
            CryptoValue.ZeroEth,
            CryptoCurrency.BTC,
            CryptoCurrency.ETHER,
            FiatValue.zero("USD"),
            "USD"
        )

        val summaryList = listOf(swapSummary)

        whenever(payloadDataManager.getAccountTransactions(any(), any(), any()))
            .thenReturn(Single.just(transactionSummaries))
        whenever(custodialWalletManager.getSwapActivityForAsset(any(), any()))
            .thenReturn(Single.just(summaryList))

        subject.activity
            .test()
            .assertValueCount(1)
            .assertComplete()
            .assertNoErrors()
            .assertValueAt(0) {
                val btcItem = it[0]
                val swapItem = it[1]

                it.size == 2 &&
                    btcItem is BtcActivitySummaryItem &&
                    btcItem.txId == summary.hash &&
                    btcItem.confirmations == summary.confirmations &&
                    btcItem.transactionType == summary.transactionType &&

                    swapItem is SwapActivitySummaryItem &&
                    swapItem.txId == swapSummary.txId &&
                    swapItem.direction == swapSummary.direction &&
                    swapItem.sendingAsset == swapSummary.sendingAsset &&
                    swapItem.receivingAsset == swapSummary.receivingAsset &&
                    swapItem.sendingAddress == swapSummary.sendingAddress &&
                    swapItem.receivingAddress == swapSummary.receivingAddress &&
                    swapItem.state == swapSummary.state &&
                    swapItem.fiatValue == swapSummary.fiatValue &&
                    swapItem.fiatCurrency == swapSummary.fiatCurrency
            }

        verify(payloadDataManager).getAccountTransactions(any(), any(), any())
    }
}
