package piuk.blockchain.androidcore.data.fees

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.FeeApi
import io.reactivex.Observable
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.rxjava.RxBus

class FeeDataManagerTest {

    private lateinit var subject: FeeDataManager
    private val rxBus = RxBus()
    private val feeApi: FeeApi = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = FeeDataManager(feeApi, rxBus)
    }

    @Test
    fun `Use default BCH fee on API Error`() {
        whenever(feeApi.bchFeeOptions)
            .thenReturn(Observable.error(Throwable()))
        subject.bchFeeOptions
            .test()
            .values()
            .first()
            .apply {
                priorityFee `should equal to` 4
                regularFee `should equal to` 4
            }
    }

    @Test
    fun `Use default ETH fee on API Error`() {
        whenever(feeApi.ethFeeOptions)
            .thenReturn(Observable.error(Throwable()))

        subject.ethFeeOptions
            .test()
            .values()
            .first()
            .apply {
                priorityFee `should equal to` 23
                regularFee `should equal to` 23
                gasLimit `should equal to` 21000
                limits!!.min `should equal to` 23
                limits!!.max `should equal to` 23
            }
    }

    @Test
    fun `Use default BTC fee on API Error`() {
        whenever(feeApi.btcFeeOptions)
            .thenReturn(Observable.error(Throwable()))

        subject.btcFeeOptions
            .test()
            .values()
            .first()
            .apply {
                priorityFee `should equal to` 11
                regularFee `should equal to` 5
                limits!!.min `should equal to` 2
                limits!!.max `should equal to` 16
            }
    }

    @Test
    fun `Use default XLM fee on API Error`() {
        whenever(feeApi.xlmFeeOptions)
            .thenReturn(Observable.error(Throwable()))

        subject.xlmFeeOptions
            .test()
            .values()
            .first()
            .apply {
                priorityFee `should equal to` 100
                regularFee `should equal to` 100
            }
    }

    @Test
    fun `Use default ERC20 fee on API Error`() {
        whenever(feeApi.getErc20FeeOptions(""))
            .thenReturn(Observable.error(Throwable()))
        subject.getErc20FeeOptions("")
            .test()
            .values()
            .first()
            .apply {
                priorityFee `should equal to` 23
                regularFee `should equal to` 23
                gasLimit `should equal to` 21000
                limits!!.min `should equal to` 23
                limits!!.max `should equal to` 23
            }
    }
}