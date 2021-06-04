package piuk.blockchain.androidcore.data.payments

import com.blockchain.android.testutils.rxInit
import com.blockchain.testutils.`should be assignable from`
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.dust.DustService
import info.blockchain.wallet.api.dust.data.DustInput
import info.blockchain.wallet.exceptions.TransactionHashApiException
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payment.InsufficientMoneyException
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.Single
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.itReturns
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Transaction
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import info.blockchain.api.ApiException
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.payload.model.Utxo
import info.blockchain.wallet.payment.OutputType
import org.junit.Assert.assertEquals
import retrofit2.Call
import retrofit2.Response
import java.math.BigInteger

class PaymentServiceTest {

    private lateinit var subject: PaymentService
    private val payment: Payment = mock()

    private val dustService: DustService = mock()
    private val targetOutputType = OutputType.P2PKH
    private val changeOutputType = OutputType.P2PKH

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = PaymentService(payment, dustService)
    }

    @Test
    fun submitPaymentSuccess() {
        // Arrange
        val txHash = "TX_HASH"
        val mockOutputBundle: SpendableUnspentOutputs = mock()
        val mockOutputs = listOf<Utxo>(mock())
        whenever(mockOutputBundle.spendableOutputs).thenReturn(mockOutputs)
        val mockKeys = listOf<SigningKey>(mock())
        val toAddress = "TO_ADDRESS"
        val changeAddress = "CHANGE_ADDRESS"
        val mockFee: BigInteger = mock()
        val mockAmount: BigInteger = mock()
        val mockTx: Transaction = mock {
            on { hashAsString } itReturns txHash
        }

        whenever(
            payment.makeBtcSimpleTransaction(
                eq(mockOutputs),
                any(),
                eq(mockFee),
                eq(changeAddress)
            )
        ).thenReturn(mockTx)

        val response = Response.success(mock<ResponseBody>())
        val mockCall = mock<Call<ResponseBody>> {
            on { execute() } itReturns response
        }
        whenever(payment.publishBtcSimpleTransaction(mockTx)).thenReturn(mockCall)

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
        assertEquals(txHash, testObserver.values()[0])
        verify(payment).makeBtcSimpleTransaction(
            eq(mockOutputs),
            any(),
            eq(mockFee),
            eq(changeAddress)
        )
        verify(payment).signBtcTransaction(
            mockTx,
            mockKeys
        )
        verify(payment).publishBtcSimpleTransaction(mockTx)
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun submitPaymentFailure() {
        // Arrange
        val txHash = "TX_HASH"
        val mockOutputs = listOf<Utxo>(mock())
        val mockOutputBundle: SpendableUnspentOutputs = mock {
            on { spendableOutputs } itReturns mockOutputs
        }
        val mockKeys = listOf<SigningKey>(mock())
        val toAddress = "TO_ADDRESS"
        val changeAddress = "CHANGE_ADDRESS"
        val mockFee: BigInteger = mock()
        val mockAmount: BigInteger = mock()
        val mockTx: Transaction = mock {
            on { hashAsString } itReturns txHash
        }
        whenever(
            payment.makeBtcSimpleTransaction(
                eq(mockOutputs),
                any(),
                eq(mockFee),
                eq(changeAddress)
            )
        ).thenReturn(mockTx)

        val mockCall = mock<Call<ResponseBody>>()
        val response = Response.error<ResponseBody>(
            500,
            ResponseBody.create(("application/json").toMediaTypeOrNull(), "{}")
        )
        whenever(mockCall.execute()).thenReturn(response)
        whenever(payment.publishBtcSimpleTransaction(mockTx)).thenReturn(mockCall)
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
        testObserver.assertNotComplete()
        testObserver.assertTerminated()
        testObserver.assertNoValues()
        testObserver.assertError {
            it `should be instance of` TransactionHashApiException::class
            if (it is TransactionHashApiException) {
                it.message `should equal` "500: {}"
                it.hashString `should equal` "TX_HASH"
            }
            true
        }
        verify(payment).makeBtcSimpleTransaction(
            eq(mockOutputs),
            any(),
            eq(mockFee),
            eq(changeAddress)
        )
        verify(payment).signBtcTransaction(
            mockTx,
            mockKeys
        )
        verify(payment).publishBtcSimpleTransaction(mockTx)
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun submitPaymentException() {
        // Arrange
        val txHash = "TX_HASH"
        val mockOutputs = listOf<Utxo>(mock())
        val mockOutputBundle: SpendableUnspentOutputs = mock {
            on { spendableOutputs } itReturns mockOutputs
        }
        val mockKeys = listOf<SigningKey>(mock())
        val toAddress = "TO_ADDRESS"
        val changeAddress = "CHANGE_ADDRESS"
        val mockFee: BigInteger = mock()
        val mockAmount: BigInteger = mock()

        whenever(
            payment.makeBtcSimpleTransaction(
                eq(mockOutputs),
                any(),
                eq(mockFee),
                eq(changeAddress)
            )
        ).doAnswer { throw InsufficientMoneyException(BigInteger("1")) }
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
        testObserver.assertNotComplete()
        testObserver.assertTerminated()
        testObserver.assertNoValues()
        testObserver.assertError(InsufficientMoneyException::class.java)
        verify(payment).makeBtcSimpleTransaction(
            eq(mockOutputs),
            any(),
            eq(mockFee),
            eq(changeAddress)
        )
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun submitBchPaymentSuccess() {
        // Arrange
        val txHash = "TX_HASH"
        val mockOutputs = listOf<Utxo>(mock())
        val mockOutputBundle: SpendableUnspentOutputs = mock {
            on { spendableOutputs } itReturns mockOutputs
        }

        val mockKeys: List<SigningKey> = listOf(mock())
        val toAddress = "TO_ADDRESS"
        val changeAddress = "CHANGE_ADDRESS"
        val mockFee: BigInteger = mock()
        val mockAmount: BigInteger = mock()

        val mockDust: DustInput = mock {
            on { lockSecret } itReturns "SECRET"
        }

        val mockHash: Sha256Hash = mock {
            on { toString() } itReturns txHash
        }

        val mockTx: Transaction = mock {
            on { txId } itReturns mockHash
        }

        whenever(
            payment.makeBchNonReplayableTransaction(
                eq(mockOutputs),
                any(),
                eq(mockFee),
                eq(changeAddress),
                eq(mockDust)
            )
        ).thenReturn(mockTx)

        whenever(dustService.getDust(CryptoCurrency.BCH)).thenReturn(Single.just(mockDust))

        val response: Response<ResponseBody> = Response.success<ResponseBody>(mock())
        val mockCall: Call<ResponseBody> = mock {
            on { execute() }.thenReturn(response)
        }

        whenever(payment.publishBchTransaction(mockTx, "SECRET"))
            .thenReturn(mockCall)

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
        assertEquals(txHash, testObserver.values()[0])
        verify(payment).makeBchNonReplayableTransaction(
            eq(mockOutputs),
            any(),
            eq(mockFee),
            eq(changeAddress),
            eq(mockDust)
        )
        verify(payment).signBchTransaction(
            mockTx,
            mockKeys
        )
        verify(payment).publishBchTransaction(mockTx, "SECRET")
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun submitBchPaymentFailure() {
        // Arrange
        val txHash = "TX_HASH"
        val mockOutputBundle: SpendableUnspentOutputs = mock()
        val mockOutputs: List<Utxo> = listOf(mock())
        whenever(mockOutputBundle.spendableOutputs).thenReturn(mockOutputs)
        val mockKeys: List<SigningKey> = listOf(mock())
        val toAddress = "TO_ADDRESS"
        val changeAddress = "CHANGE_ADDRESS"
        val mockFee: BigInteger = mock()
        val mockAmount: BigInteger = mock()

        val mockHash: Sha256Hash = mock {
            on { toString() } itReturns txHash
        }

        val mockTx: Transaction = mock {
            on { txId } itReturns mockHash
        }

        val mockDust: DustInput = mock {
            on { lockSecret } itReturns "SECRET"
        }

        whenever(
            payment.makeBchNonReplayableTransaction(
                eq(mockOutputs),
                any(),
                eq(mockFee),
                eq(changeAddress),
                eq(mockDust)
            )
        ).thenReturn(mockTx)
        whenever(dustService.getDust(CryptoCurrency.BCH)).thenReturn(Single.just(mockDust))
        val mockCall: Call<ResponseBody> = mock()
        val response = Response.error<ResponseBody>(
            500,
            ResponseBody.create(("application/json").toMediaTypeOrNull(), "{}")
        )
        whenever(mockCall.execute()).thenReturn(response)
        whenever(payment.publishBchTransaction(mockTx, "SECRET"))
            .thenReturn(mockCall)
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
        testObserver.assertNotComplete()
        testObserver.assertTerminated()
        testObserver.assertNoValues()
        testObserver.assertError {
            it `should be instance of` TransactionHashApiException::class
            if (it is TransactionHashApiException) {
                it.message `should equal` "500: {}"
                it.hashString `should equal` "TX_HASH"
            }
            true
        }
        verify(payment).makeBchNonReplayableTransaction(
            eq(mockOutputs),
            any(),
            eq(mockFee),
            eq(changeAddress),
            eq(mockDust)
        )
        verify(payment).signBchTransaction(
            mockTx,
            mockKeys
        )
        verify(payment).publishBchTransaction(mockTx, "SECRET")
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun submitBchPaymentException() {
        // Arrange
        val txHash = "TX_HASH"
        val mockOutputBundle: SpendableUnspentOutputs = mock()
        val mockOutputs: List<Utxo> = listOf(mock())
        whenever(mockOutputBundle.spendableOutputs).thenReturn(mockOutputs)
        val mockKeys: List<SigningKey> = listOf(mock())
        val toAddress = "TO_ADDRESS"
        val changeAddress = "CHANGE_ADDRESS"
        val mockFee: BigInteger = mock()
        val mockAmount: BigInteger = mock()
        val mockTx: Transaction = mock()
        whenever(mockTx.hashAsString).thenReturn(txHash)
        val mockDust: DustInput = mock()
        whenever(dustService.getDust(CryptoCurrency.BCH)).thenReturn(Single.just(mockDust))
        whenever(
            payment.makeBchNonReplayableTransaction(
                eq(mockOutputs),
                any(),
                eq(mockFee),
                eq(changeAddress),
                eq(mockDust)
            )
        ).doAnswer { throw InsufficientMoneyException(BigInteger("1")) }

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
        testObserver.assertNotComplete()
        testObserver.assertTerminated()
        testObserver.assertNoValues()
        testObserver.assertError(InsufficientMoneyException::class.java)
        verify(payment).makeBchNonReplayableTransaction(
            eq(mockOutputs),
            any(),
            eq(mockFee),
            eq(changeAddress),
            eq(mockDust)
        )
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun getUnspentOutputsSuccess() {
        // Arrange
        val address = "ADDRESS"
        val xpub = XPub(address = address, derivation = XPub.Format.LEGACY)
        val xpubs = XPubs(xpub)

        val mockOutputs = listOf<Utxo>(mock())
        whenever(payment.getUnspentBtcCoins(xpubs)).thenReturn(Single.just(mockOutputs))

        // Act
        subject.getUnspentBtcOutputs(xpubs)
            .test()
            .assertComplete()
            .assertNoErrors()
            .assertValueAt(0, mockOutputs)

        verify(payment).getUnspentBtcCoins(xpubs)
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun getUnspentBchOutputsSuccess() {
        // Arrange
        val address = "ADDRESS"
        val mockOutputs = listOf<Utxo>(mock())
        whenever(payment.getUnspentBchCoins(listOf(address))).thenReturn(Single.just(mockOutputs))

        // Act
        subject.getUnspentBchOutputs(address)
            .test()
            .assertComplete()
            .assertNoErrors()
            .assertValueAt(0, mockOutputs)

        verify(payment).getUnspentBchCoins(listOf(address))
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun getSpendableCoins() {
        // Arrange
        val mockUnspent = listOf<Utxo>(mock())
        val mockPayment: BigInteger = mock()
        val mockFee: BigInteger = mock()
        val mockOutputs: SpendableUnspentOutputs = mock()
        whenever(payment.getSpendableCoins(
            mockUnspent, targetOutputType, changeOutputType, mockPayment, mockFee, false))
            .thenReturn(mockOutputs)
        // Act
        val result = subject.getSpendableCoins(
            mockUnspent, targetOutputType, changeOutputType, mockPayment, mockFee, false)
        // Assert
        assertEquals(mockOutputs, result)
        verify(payment).getSpendableCoins(
            mockUnspent, targetOutputType, changeOutputType, mockPayment, mockFee, false)
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun getMaximumAvailable() {
        // Arrange
        val mockUnspent = listOf<Utxo>(mock())
        val mockFee: BigInteger = mock()
        val mockSweepableCoins = mock<Pair<BigInteger, BigInteger>>()
        whenever(payment.getMaximumAvailable(mockUnspent, targetOutputType, mockFee, false))
            .thenReturn(mockSweepableCoins)

        // Act
        val result = subject.getMaximumAvailable(mockUnspent, targetOutputType, mockFee, false)

        // Assert
        assertEquals(mockSweepableCoins, result)
        verify(payment).getMaximumAvailable(mockUnspent, targetOutputType, mockFee, false)
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun isAdequateFee() {
        // Arrange
        val inputs = listOf<Utxo>(mock())
        val outputs = (0..100).map { OutputType.P2PKH }
        val mockFee: BigInteger = mock()
        whenever(payment.isAdequateFee(inputs, outputs, mockFee)).thenReturn(false)
        // Act
        val result = subject.isAdequateFee(inputs, outputs, mockFee)
        // Assert
        assertEquals(false, result)
        verify(payment).isAdequateFee(inputs, outputs, mockFee)
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun estimateSize() {
        // Arrange
        val inputs = listOf<Utxo>(mock())
        val outputs = (0..100).map { OutputType.P2PKH }
        val estimatedSize = 1337.0
        whenever(payment.estimatedSize(inputs, outputs)).thenReturn(estimatedSize)
        // Act
        val result = subject.estimateSize(inputs, outputs)
        // Assert
        assertEquals(estimatedSize, result, 0.0)
        verify(payment).estimatedSize(inputs, outputs)
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun estimateFee() {
        // Arrange
        val inputs = listOf<Utxo>(mock())
        val outputs = (0..100).map { OutputType.P2PKH }
        val mockFeePerKb: BigInteger = mock()
        val mockAbsoluteFee: BigInteger = mock()
        whenever(payment.estimatedFee(inputs, outputs, mockFeePerKb)).thenReturn(mockAbsoluteFee)
        // Act
        val result = subject.estimateFee(inputs, outputs, mockFeePerKb)
        // Assert
        assertEquals(mockAbsoluteFee, result)
        verify(payment).estimatedFee(inputs, outputs, mockFeePerKb)
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun `TransactionHashApiException is also an ApiException`() {
        ApiException::class `should be assignable from` TransactionHashApiException::class
    }
}
