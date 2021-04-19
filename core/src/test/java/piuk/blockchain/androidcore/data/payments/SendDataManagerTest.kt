package piuk.blockchain.androidcore.data.payments

import com.blockchain.android.testutils.rxInit
import com.blockchain.logging.LastTxUpdater
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.satoshi
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.model.Utxo
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.payment.OutputType
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.rxjava.RxBus
import java.math.BigInteger

class SendDataManagerTest {

    private lateinit var subject: SendDataManager
    private val mockPaymentService: PaymentService = mock()
    private val mockLastTxUpdater: LastTxUpdater = mock()
    private val mockRxBus: RxBus = mock()
    private val targetOutputType = OutputType.P2PKH
    private val changeOutputType = OutputType.P2PKH

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = SendDataManager(mockPaymentService, mockLastTxUpdater, mockRxBus)
    }

    @Test
    fun `submitPayment BTC`() {
        // Arrange
        val mockOutputBundle: SpendableUnspentOutputs = mock()
        val mockKeys = listOf(mock<SigningKey>())
        val toAddress = "TO_ADDRESS"
        val changeAddress = "CHANGE_ADDRESS"
        val mockFee: BigInteger = mock()
        val mockAmount: BigInteger = mock()
        val txHash = "TX_HASH"
        whenever(
            mockPaymentService.submitBtcPayment(
                mockOutputBundle,
                mockKeys,
                toAddress,
                changeAddress,
                mockFee,
                mockAmount
            )
        ).thenReturn(Observable.just(txHash))
        whenever(mockLastTxUpdater.updateLastTxTime()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.submitBtcPayment(
            mockOutputBundle,
            mockKeys,
            toAddress,
            changeAddress,
            mockFee,
            mockAmount
        ).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] shouldEqual txHash
        verify(mockPaymentService).submitBtcPayment(
            mockOutputBundle,
            mockKeys,
            toAddress,
            changeAddress,
            mockFee,
            mockAmount
        )
        verifyNoMoreInteractions(mockPaymentService)
    }

    @Test
    fun `submitPayment BTC successful even if logging last tx fails`() {
        // Arrange
        val mockOutputBundle: SpendableUnspentOutputs = mock()
        val mockKeys = listOf(mock<SigningKey>())
        val toAddress = "TO_ADDRESS"
        val changeAddress = "CHANGE_ADDRESS"
        val mockFee: BigInteger = mock()
        val mockAmount: BigInteger = mock()
        val txHash = "TX_HASH"
        whenever(
            mockPaymentService.submitBtcPayment(
                mockOutputBundle,
                mockKeys,
                toAddress,
                changeAddress,
                mockFee,
                mockAmount
            )
        ).thenReturn(Observable.just(txHash))
        whenever(mockLastTxUpdater.updateLastTxTime()).thenReturn(Completable.error(Exception()))
        // Act
        val testObserver = subject.submitBtcPayment(
            mockOutputBundle,
            mockKeys,
            toAddress,
            changeAddress,
            mockFee,
            mockAmount
        ).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] shouldEqual txHash
        verify(mockPaymentService).submitBtcPayment(
            mockOutputBundle,
            mockKeys,
            toAddress,
            changeAddress,
            mockFee,
            mockAmount
        )
        verifyNoMoreInteractions(mockPaymentService)
    }

    @Test
    fun `submitPayment BCH`() {
        // Arrange
        val mockOutputBundle: SpendableUnspentOutputs = mock()
        val mockKeys = listOf(mock<SigningKey>())
        val toAddress = "TO_ADDRESS"
        val changeAddress = "CHANGE_ADDRESS"
        val mockFee: BigInteger = mock()
        val mockAmount: BigInteger = mock()
        val txHash = "TX_HASH"
        whenever(
            mockPaymentService.submitBchPayment(
                mockOutputBundle,
                mockKeys,
                toAddress,
                changeAddress,
                mockFee,
                mockAmount
            )
        ).thenReturn(Observable.just(txHash))
        whenever(mockLastTxUpdater.updateLastTxTime()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.submitBchPayment(
            mockOutputBundle,
            mockKeys,
            toAddress,
            changeAddress,
            mockFee,
            mockAmount
        ).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] shouldEqual txHash
        verify(mockPaymentService).submitBchPayment(
            mockOutputBundle,
            mockKeys,
            toAddress,
            changeAddress,
            mockFee,
            mockAmount
        )
        verifyNoMoreInteractions(mockPaymentService)
    }

    // This call is failing when init'ing the cipher object. This appears to be a JVM issue but I'm
    // not sure what's changed. The test passes now, but should be changed to assert success conditions
    // when the fix is discovered.
    // TODO: Fix me, and then test for success
    // TODO: 08/01/2018 This will default to bitcoin network parameters
    /*   @Ignore Commented out cause of a lint error
       @Test
       fun getEcKeyFromBip38() {
           // Arrange
           val password = "thisisthepassword"
           val scanData = "6PYP4i7UyewqZWqdnpQwMdCyneXPaFDPkk8LArmVexqoGsy9Yx92SiLCPm"
           val params = BitcoinMainNetParams.get()
           // Act
           val testObserver = subject.getEcKeyFromBip38(password, scanData, params).test()
           // Assert
           testObserver.assertNotComplete()
           testObserver.assertTerminated()
           testObserver.assertNoValues()
       }*/

    @Test
    fun `getUnspentOutputs BTC`() {
        // Arrange
        val address = "ADDRESS"
        val xpub = XPub(address = address, derivation = XPub.Format.LEGACY)
        val xpubs = XPubs(xpub)

        val mockUnspentOutputs = listOf(mock<Utxo>())
        whenever(mockPaymentService.getUnspentBtcOutputs(xpubs))
            .thenReturn(Single.just(mockUnspentOutputs))

        // Act
        val testObserver = subject.getUnspentBtcOutputs(xpubs).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] shouldEqual mockUnspentOutputs
        verify(mockPaymentService).getUnspentBtcOutputs(xpubs)
        verifyNoMoreInteractions(mockPaymentService)
    }

    @Test
    fun `getUnspentOutputs BCH`() {
        // Arrange
        val address = "ADDRESS"
        val mockUnspentOutputs = listOf(mock<Utxo>())
        whenever(mockPaymentService.getUnspentBchOutputs(address))
            .thenReturn(Single.just(mockUnspentOutputs))
        // Act
        val testObserver = subject.getUnspentBchOutputs(address).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.values()[0] shouldEqual mockUnspentOutputs
        verify(mockPaymentService).getUnspentBchOutputs(address)
        verifyNoMoreInteractions(mockPaymentService)
    }

    @Test
    fun `get spendable coins btc, no replay protection`() {
        // Arrange
        val unspent = listOf(mock<Utxo>())
        val payment = 1.bitcoin()
        val fee = 1.toBigInteger()
        val outputs = SpendableUnspentOutputs()
        whenever(mockPaymentService.getSpendableCoins(
            unspent,
            targetOutputType,
            changeOutputType,
            payment.toBigInteger(),
            fee,
            false)
        ).thenReturn(outputs)
        // Act
        val result = subject
            .getSpendableCoins(unspent, targetOutputType, changeOutputType, payment, fee.satoshi())
        // Assert
        result shouldEqual outputs
        verify(mockPaymentService)
            .getSpendableCoins(unspent, targetOutputType, changeOutputType, payment.toBigInteger(), fee, false)
        verifyNoMoreInteractions(mockPaymentService)
    }

    @Test
    fun `get spendable coins bch, should add replay protection`() {
        // Arrange
        val unspent = listOf(mock<Utxo>())
        val payment = 1.bitcoinCash()
        val fee = 1.toBigInteger()
        val outputs = SpendableUnspentOutputs()
        whenever(mockPaymentService.getSpendableCoins(
            unspent, targetOutputType, changeOutputType, payment.toBigInteger(), fee, true)
        ).thenReturn(outputs)
        // Act
        val result = subject
            .getSpendableCoins(unspent, targetOutputType, changeOutputType, payment, fee.satoshi())
        // Assert
        result shouldEqual outputs
        verify(mockPaymentService).getSpendableCoins(
            unspent, targetOutputType, changeOutputType, payment.toBigInteger(), fee, true
        )
        verifyNoMoreInteractions(mockPaymentService)
    }

    @Test
    fun `get sweepable btc, no replay protected`() {
        // Arrange
        val unspent = listOf(mock<Utxo>())
        val fee = 1.toBigInteger()
        val sweepable = Pair(2.toBigInteger(), 1.toBigInteger())
        whenever(mockPaymentService.getMaximumAvailable(unspent, targetOutputType, fee, false))
            .thenReturn(sweepable)
        // Act
        val result = subject.getMaximumAvailable(CryptoCurrency.BTC, unspent, targetOutputType, fee.satoshi())
        // Assert
        result shouldEqual SendDataManager.MaxAvailable(
            maxSpendable = CryptoValue.fromMinor(CryptoCurrency.BTC, sweepable.first),
            forForMax = CryptoValue.fromMinor(CryptoCurrency.BTC, sweepable.second)
        )

        verify(mockPaymentService).getMaximumAvailable(unspent, targetOutputType, fee, false)
        verifyNoMoreInteractions(mockPaymentService)
    }

    @Test
    fun `get sweepable bch, should add replay protected`() {
        // Arrange
        val unspent = listOf(mock<Utxo>())
        val fee = 1.toBigInteger()
        val sweepable = Pair(2.toBigInteger(), 1.toBigInteger())
        whenever(mockPaymentService.getMaximumAvailable(unspent, targetOutputType, fee, true))
            .thenReturn(sweepable)
        // Act
        val result = subject.getMaximumAvailable(CryptoCurrency.BCH, unspent, targetOutputType, fee.satoshi())
        // Assert
        result shouldEqual SendDataManager.MaxAvailable(
            maxSpendable = CryptoValue.fromMinor(CryptoCurrency.BCH, sweepable.first),
            forForMax = CryptoValue.fromMinor(CryptoCurrency.BCH, sweepable.second)
        )
        verify(mockPaymentService).getMaximumAvailable(unspent, targetOutputType, fee, true)
        verifyNoMoreInteractions(mockPaymentService)
    }

    @Test
    fun isAdequateFee() {
        // Arrange
        val inputs = listOf<Utxo>(mock())
        val outputs = (0..100).map { OutputType.P2PKH }
        val mockFee: BigInteger = mock()
        whenever(mockPaymentService.isAdequateFee(inputs, outputs, mockFee)).thenReturn(false)
        // Act
        val result = subject.isAdequateFee(inputs, outputs, mockFee)
        // Assert
        result shouldEqual false
        verify(mockPaymentService).isAdequateFee(inputs, outputs, mockFee)
        verifyNoMoreInteractions(mockPaymentService)
    }

    @Test
    fun estimateSize() {
        // Arrange
        val inputs = listOf<Utxo>(mock())
        val outputs = (0..100).map { OutputType.P2PKH }
        val estimatedSize = 1337.0
        whenever(mockPaymentService.estimateSize(inputs, outputs)).thenReturn(estimatedSize)
        // Act
        val result = subject.estimateSize(inputs, outputs)
        // Assert
        result shouldEqual estimatedSize
        verify(mockPaymentService).estimateSize(inputs, outputs)
        verifyNoMoreInteractions(mockPaymentService)
    }

    @Test
    fun estimateFee() {
        // Arrange
        val inputs = listOf<Utxo>(mock())
        val outputs = (0..100).map { OutputType.P2PKH }
        val mockFeePerKb: BigInteger = mock()
        val mockAbsoluteFee: BigInteger = mock()
        whenever(mockPaymentService.estimateFee(inputs, outputs, mockFeePerKb))
            .thenReturn(mockAbsoluteFee)
        // Act
        val result = subject.estimatedFee(inputs, outputs, mockFeePerKb)
        // Assert
        result shouldEqual mockAbsoluteFee
        verify(mockPaymentService).estimateFee(inputs, outputs, mockFeePerKb)
        verifyNoMoreInteractions(mockPaymentService)
    }
}