package piuk.blockchain.android.ui.send.send2

import com.blockchain.android.testutils.rxInit
import com.blockchain.sunriver.XlmTransactionSender
import com.blockchain.testutils.lumens
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.amshove.kluent.`it returns`
import org.amshove.kluent.mock
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.send.SendView
import piuk.blockchain.androidcore.data.currency.CurrencyState
import java.util.concurrent.TimeUnit

class XlmSendPresenterStrategyTest {

    val testScheduler = TestScheduler()

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computation(testScheduler)
    }

    private fun givenXlmCurrencyState(): CurrencyState =
        mock {
            on { cryptoCurrency } `it returns` CryptoCurrency.XLM
        }

    @Test
    fun `on onCurrencySelected`() {
        val view: SendView = mock()
        XlmSendPresenterStrategy(
            givenXlmCurrencyState(),
            mock(),
            mock()
        ).apply {
            initView(view)
        }.onCurrencySelected(CryptoCurrency.XLM)
        verify(view).hideFeePriority()
        verify(view).setFeePrioritySelection(0)
        verify(view).disableFeeDropdown()
        verify(view).setCryptoMaxLength(15)
    }

    @Test
    fun `on selectDefaultOrFirstFundedSendingAccount, it updates the address`() {
        val view: SendView = mock()
        XlmSendPresenterStrategy(
            givenXlmCurrencyState(),
            mock {
                on { defaultAccount() } `it returns` Single.just(
                    AccountReference.Xlm("The Xlm account", "")
                )
            },
            mock()
        ).apply {
            initView(view)
        }.selectDefaultOrFirstFundedSendingAccount()
        verify(view).updateSendingAddress("The Xlm account")
    }

    @Test
    fun `on onContinueClicked, it takes the address from the view, latest value and executes a send`() {
        val view: SendView = mock {
            on { getReceivingAddress() } `it returns` "GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT"
        }
        val transactionSendDataManager = mock<XlmTransactionSender> {
            on { sendFunds(any(), any()) } `it returns` Completable.timer(2, TimeUnit.SECONDS)
        }
        XlmSendPresenterStrategy(
            givenXlmCurrencyState(),
            mock(),
            transactionSendDataManager
        ).apply {
            initView(view)
            onViewReady()
            onCryptoTextChange("1")
            onCryptoTextChange("10")
            onCryptoTextChange("100")
            onContinueClicked()
        }
        verify(transactionSendDataManager).sendFunds(
            value = eq(100.lumens()),
            toAccountId = eq("GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT")
        )
        verify(view).showProgressDialog(R.string.app_name)
        verify(view, never()).dismissProgressDialog()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        verify(view).dismissProgressDialog()
    }
}
