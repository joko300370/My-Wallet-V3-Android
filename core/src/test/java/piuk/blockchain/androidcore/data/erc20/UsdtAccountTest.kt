package piuk.blockchain.androidcore.data.erc20

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.ethereum.data.Erc20AddressResponse
import info.blockchain.wallet.ethereum.data.Erc20TransferResponse
import io.reactivex.Observable
import junit.framework.Assert.assertEquals
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.erc20.datastores.Erc20DataStore
import piuk.blockchain.androidcore.data.ethereum.EthDataManager

class UsdtAccountTest {

    private lateinit var usdtAccount: UsdtAccount
    private val ethDataManager: EthDataManager = mock()
    private val erc20DataStore: Erc20DataStore = mock()
    private val environmentSettings: EnvironmentConfig = mock()

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    private val erc20AddressResponse = Erc20AddressResponse().apply {
        accountHash = "0x4058a004dd718babab47e14dd0d744742e5b9903"
        tokenHash = "0x8e870d67f660d95d5be530380d0ec0bd388289e1"
        balance = 2838277460000000000.toBigInteger()
        transfers = listOf(Erc20TransferResponse(), Erc20TransferResponse())
    }

    @Before
    fun setUp() {
        usdtAccount = UsdtAccount(
            ethDataManager,
            erc20DataStore,
            environmentSettings = environmentSettings
        )
    }

    @Test
    fun clearErc20AccountDetails() {
        // Actv
        usdtAccount.clear()
        // Assert
        verify(erc20DataStore).clearData()
        verifyNoMoreInteractions(erc20DataStore)
    }

    @Test
    fun fetchErc20Address() {
        // Arrange
        whenever(environmentSettings.environment).thenReturn(Environment.PRODUCTION)
        whenever(ethDataManager.getErc20Address(CryptoCurrency.USDT)).thenReturn(Observable.just(
            erc20AddressResponse))
        // Act
        val testObserver = usdtAccount.fetchErc20Address().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue {
            it.accountHash == erc20AddressResponse.accountHash &&
                    it.totalBalance.amount == erc20AddressResponse.balance &&
                    it.totalBalance.currency == CryptoCurrency.USDT
        }
        verify(erc20DataStore).erc20DataModel = any(Erc20DataModel::class)
        verify(ethDataManager).getErc20Address(CryptoCurrency.USDT)
    }

    @Test
    fun fetchErc20AddressTestNet() {
        // Arrange
        whenever(environmentSettings.environment).thenReturn(Environment.TESTNET)
        // Act
        val testObserver = usdtAccount.fetchErc20Address().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()

        verify(erc20DataStore).erc20DataModel = null
        verifyZeroInteractions(ethDataManager)
    }

    @Test
    fun `no transactions should be returned from empty model`() {
        // Act
        val testObserver = usdtAccount.getTransactions().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertNoValues()
    }

    @Test
    fun `transactions from not null model should return the correct transactions`() {
        // Arrange
        whenever(erc20DataStore.erc20DataModel).thenReturn(Erc20DataModel(erc20AddressResponse, CryptoCurrency.USDT))
        // Act
        val testObserver = usdtAccount.getTransactions().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue {
            it[0] == Erc20Transfer(erc20AddressResponse.transfers[0]) &&
                    it[1] == Erc20Transfer(erc20AddressResponse.transfers[1]) &&
                    it.size == 2
        }
    }

    @Test
    fun `account has should be the correct one`() {
        // Arrange
        whenever(erc20DataStore.erc20DataModel).thenReturn(Erc20DataModel(erc20AddressResponse, CryptoCurrency.USDT))
        // Act
        val testObserver = usdtAccount.getAccountHash().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue {
            it == erc20AddressResponse.accountHash
        }
    }

    @Test
    fun `raw transaction fields should be correct`() {
        val nonce = 10.toBigInteger()
        val to = "0xD1220A0cf47c7B9Be7A2E63A89F429762e7b9aDb"
        val contractAddress = "0x8E870D67F660D95d5be530380D0eC0bd388289E1"
        val gasPrice = 1.toBigInteger()
        val gasLimit = 5.toBigInteger()
        val amount = 7.toBigInteger()

        val rawTransaction =
            usdtAccount.createTransaction(nonce, to, contractAddress, gasPrice, gasLimit, amount)

        assertEquals(nonce, rawTransaction!!.nonce)
        assertEquals(gasPrice, rawTransaction.gasPrice)
        assertEquals(gasLimit, rawTransaction.gasLimit)
        assertEquals("0x8E870D67F660D95d5be530380D0eC0bd388289E1", rawTransaction.to)
        assertEquals(0.toBigInteger(), rawTransaction.value)
        assertEquals(
            "a9059cbb000000000000000000000000d1220a0cf47c7b9be7a2e63a89f429762e7b" +
                    "9adb0000000000000000000000000000000000000000000000000000000000000007",
            rawTransaction.data)
    }
}
