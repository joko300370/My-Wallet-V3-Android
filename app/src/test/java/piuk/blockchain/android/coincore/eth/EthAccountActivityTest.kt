package piuk.blockchain.android.coincore.eth

import com.blockchain.android.testutils.rxInit
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.ethereum.data.EthLatestBlockNumber
import info.blockchain.wallet.ethereum.data.EthTransaction
import io.reactivex.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.spy
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class EthAccountActivityTest {

    private val payloadManager: PayloadDataManager = mock()
    private val ethDataManager: EthDataManager = mock()
    private val feeDataManager: FeeDataManager = mock()
    private val exchangeRates: ExchangeRateDataManager = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val walletPrefs: WalletStatus = mock()

    private val subject =
        spy(EthCryptoWalletAccount(
            payloadManager = payloadManager,
            label = "TestEthAccount",
            address = "Test Address",
            ethDataManager = ethDataManager,
            fees = feeDataManager,
            exchangeRates = exchangeRates,
            walletPreferences = walletPrefs
        ))

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
    fun fetchTransactionsEthereum() {
        // Arrange
        val latestBlock = EthLatestBlockNumber()
        val transaction: EthTransaction = mock()

        val to = "12345"
        whenever(transaction.hash).thenReturn("hash")
        whenever(transaction.to).thenReturn(to)

        val ethModel: CombinedEthModel = mock()

        whenever(ethDataManager.getLatestBlockNumber())
            .thenReturn(Single.just(latestBlock))

        whenever(ethDataManager.getEthTransactions())
            .thenReturn(Single.just(listOf(transaction)))

        whenever(ethDataManager.getEthResponseModel())
            .thenReturn(ethModel)

        doReturn(true).`when`(subject).isErc20FeeTransaction(to)

        subject.activity
            .test()
            .assertComplete()
            .assertNoErrors()

        verify(ethDataManager).getLatestBlockNumber()
        verify(ethDataManager).getEthTransactions()
    }

    @Test
    fun getEthTransactionsListWithOneErc20FeeTransactionInTheList() {
        // Arrange
        val ethTransaction = EthTransaction(
            to = "0x8E870D67F660D95d5be530380D0eC0bd388289E1"
        )

        whenever(ethDataManager.getLatestBlockNumber())
            .thenReturn(Single.just(EthLatestBlockNumber()))

        whenever(ethDataManager.getEthResponseModel())
            .thenReturn(mock())
        whenever(ethDataManager.getEthTransactions())
            .thenReturn(Single.just(listOf(ethTransaction)))

        doReturn(true).`when`(subject).isErc20FeeTransaction(ethTransaction.to)

        subject.activity
            .test()
            .assertValueCount(1)
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.size == 1
            }
            .assertValue {
                (it[0] as NonCustodialActivitySummaryItem).isFeeTransaction
            }

        verify(ethDataManager).getEthTransactions()
    }

    @Test
    fun getEthTransactionsListWithNoErc20FeeTransactionInTheList() {
        // Arrange
        val ethTransaction = EthTransaction(
            to = "0x8E870D67F660D95d5be530380D0eC0bd388289E1"
        )

        whenever(ethDataManager.getLatestBlockNumber())
            .thenReturn(Single.just(EthLatestBlockNumber()))
        doReturn(false).`when`(subject).isErc20FeeTransaction(ethTransaction.to)

        whenever(ethDataManager.getEthResponseModel())
            .thenReturn(mock())
        whenever(ethDataManager.getEthTransactions())
            .thenReturn(Single.just(listOf(ethTransaction)))

        subject.activity
            .test()
            .assertValueCount(1)
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.size == 1
            }
            .assertValue {
                !(it[0] as NonCustodialActivitySummaryItem).isFeeTransaction
            }

        verify(ethDataManager).getEthTransactions()
    }
}
