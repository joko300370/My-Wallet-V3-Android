package piuk.blockchain.android.coincore.eth

import com.blockchain.android.testutils.rxInit
import com.blockchain.preferences.CurrencyPrefs
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.ethereum.Erc20TokenData
import info.blockchain.wallet.ethereum.data.EthLatestBlockNumber
import info.blockchain.wallet.ethereum.data.EthTransaction
import io.reactivex.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class EthAccountActivityTest {

    private val payloadManager: PayloadDataManager = mock()
    private val ethDataManager: EthDataManager = mock()
    private val feeDataManager: FeeDataManager = mock()
    private val currencyState: CurrencyState = mock()
    private val exchangeRates: ExchangeRateDataManager = mock()
    private val currencyPrefs: CurrencyPrefs = mock()

    private val subject =
        EthCryptoWalletAccount(
            payloadManager = payloadManager,
            label = "TestEthAccount",
            address = "Test Address",
            ethDataManager = ethDataManager,
            fees = feeDataManager,
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
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)
    }

    @Test
    fun fetchTransactionsEthereum() {
        // Arrange
        val latestBlock = EthLatestBlockNumber()
        val transaction: EthTransaction = mock()

        whenever(transaction.hash).thenReturn("hash")

        val ethModel: CombinedEthModel = mock()

        whenever(ethDataManager.getLatestBlockNumber())
            .thenReturn(Single.just(latestBlock))

        whenever(ethDataManager.getEthTransactions())
            .thenReturn(Single.just(listOf(transaction)))

        whenever(ethDataManager.getEthResponseModel())
            .thenReturn(ethModel)

        whenever(ethDataManager.getErc20TokenData(CryptoCurrency.PAX))
            .thenReturn(Erc20TokenData.createPaxTokenData(""))

        subject.activity
            .test()
            .assertComplete()
            .assertNoErrors()

        verify(ethDataManager).getLatestBlockNumber()
        verify(ethDataManager).getEthTransactions()
        verify(ethDataManager).getErc20TokenData(CryptoCurrency.PAX)
    }

    @Test
    fun getEthTransactionsListWithOneErc20FeeTransactionInTheList() {
        // Arrange
        val ethTransaction = EthTransaction(
            to = "0x8E870D67F660D95d5be530380D0eC0bd388289E1"
        )

        whenever(ethDataManager.getLatestBlockNumber())
            .thenReturn(Single.just(EthLatestBlockNumber()))
        whenever(ethDataManager.getErc20TokenData(CryptoCurrency.PAX))
            .thenReturn(Erc20TokenData.createPaxTokenData(""))
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
                (it[0] as NonCustodialActivitySummaryItem).isFeeTransaction
            }

        verify(ethDataManager).getErc20TokenData(CryptoCurrency.PAX)
        verify(ethDataManager).getEthTransactions()
    }

    @Test
    fun getEthTransactionsListWithNoErc20FeeTransactionInTheList() {
        // Arrange
        val ethTransaction = EthTransaction(
            to = "0x8E870D234660D95d5be530380D0eC0bd388289E1"
        )

        whenever(ethDataManager.getLatestBlockNumber())
            .thenReturn(Single.just(EthLatestBlockNumber()))
        whenever(ethDataManager.getErc20TokenData(CryptoCurrency.PAX))
            .thenReturn(Erc20TokenData.createPaxTokenData(""))
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

        verify(ethDataManager).getErc20TokenData(CryptoCurrency.PAX)
        verify(ethDataManager).getEthTransactions()
    }
}
