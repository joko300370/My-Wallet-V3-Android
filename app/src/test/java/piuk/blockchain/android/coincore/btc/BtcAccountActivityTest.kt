package piuk.blockchain.android.coincore.btc

import com.blockchain.android.testutils.rxInit
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Single
import org.bitcoinj.core.NetworkParameters
import org.junit.Before
import org.junit.Rule
import org.junit.Test
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
            walletPreferences = walletPrefs
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

        whenever(payloadDataManager.getAccountTransactions(any(), any(), any()))
            .thenReturn(Single.just(transactionSummaries))

        subject.activity
            .test()
            .assertComplete()
            .assertNoErrors()

        verify(payloadDataManager).getAccountTransactions(any(), any(), any())

        // TODO: Validate returned list
    }
}
