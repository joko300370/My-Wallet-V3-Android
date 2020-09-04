package piuk.blockchain.android.ui.send.strategy

import android.content.res.Resources
import com.blockchain.android.testutils.rxInit
import com.blockchain.fees.FeeType
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import com.blockchain.swap.nabu.models.nabu.NabuApiException
import com.blockchain.swap.nabu.models.nabu.NabuErrorCodes
import com.blockchain.swap.nabu.models.nabu.SendToMercuryAddressResponse
import com.blockchain.swap.nabu.models.nabu.State
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.XlmFeesFetcher
import com.blockchain.testutils.lumens
import com.blockchain.testutils.stroops
import com.blockchain.testutils.usd
import com.blockchain.transactions.Memo
import com.blockchain.transactions.SendDetails
import com.blockchain.transactions.SendFundsResult
import com.blockchain.transactions.TransactionSender
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.itReturns
import org.amshove.kluent.mock
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.R
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.ui.transfer.send.activity.SendConfirmationDetails
import piuk.blockchain.android.ui.transfer.send.activity.SendView
import piuk.blockchain.android.ui.transfer.send.activity.strategy.ResourceSendFundsResultLocalizer
import piuk.blockchain.android.ui.transfer.send.activity.strategy.XlmSendStrategy
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import java.util.concurrent.TimeUnit

class XlmSendPresenterStrategyTest {

    private val testScheduler = TestScheduler()

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computation(testScheduler)
    }

    private val nabuDataManager: NabuDataManager =
        mock {
            on {
                fetchCryptoAddressFromThePit(any(),
                    any())
            } `it returns` Single.just(SendToMercuryAddressResponse("123:2143", "", State.ACTIVE))
        }

    private val pitLinked: PitLinking =
        mock {
            on {
                isPitLinked()
            } `it returns` Single.just(true)
        }

    private val pitUnLinked: PitLinking =
        mock {
            on {
                isPitLinked()
            } `it returns` Single.just(false)
        }

    private val nabuToken: NabuToken =
        mock {
            on { fetchNabuToken() } `it returns` Single.just(NabuOfflineTokenResponse(
                "",
                ""))
        }

    private val stringUtils: StringUtils =
        mock {
            on { getFormattedString(any(), any()) } `it returns` ""
        }

    private val exchangeRates: ExchangeRateDataManager = mock {
        on { getLastPrice(any(), any()) } itReturns 0.5
    }

    private val prefs: PersistentPrefs = mock {
        on { selectedFiatCurrency } itReturns TEST_FIAT
    }

    @Test
    fun `on onCurrencySelected`() {
        val view = TestSendView()

        val dataManager = mock<XlmDataManager> {
            on { defaultAccount() } `it returns` Single.just(AccountReference.Xlm("The Xlm account", ""))
            on { getMaxSpendableAfterFees(FeeType.Regular) } `it returns` Single.just(199.5.lumens())
        }

        val feesFetcher = mock<XlmFeesFetcher> {
            on { operationFee(FeeType.Regular) } `it returns` Single.just(1.stroops())
        }

        XlmSendStrategy(
            xlmDataManager = dataManager,
            xlmFeesFetcher = feesFetcher,
            xlmTransactionSender = mock(),
            walletOptionsDataManager = mock(),
            exchangeRates = exchangeRates,
            sendFundsResultLocalizer = mock(),
            nabuToken = nabuToken,
            nabuDataManager = nabuDataManager,
            stringUtils = stringUtils,
            analytics = mock(),
            pitLinking = pitLinked,
            prefs = prefs
        ).apply {
            attachView(view)
        }.onCurrencySelected()

        verify(view.mock).hideFeePriority()
        verify(view.mock).setFeePrioritySelection(0)
        verify(view.mock).disableFeeDropdown()
        verify(view.mock).setCryptoMaxLength(15)
        verify(view.mock).showMemo()
        verify(view.mock).updateMaxAvailable(199.5.lumens(), CryptoValue.ZeroXlm)
        verify(view.mock, never()).updateCryptoAmount(any(), any())
    }

    @Test
    fun `on onSpendMaxClicked updates the CryptoAmount`() {
        val view = TestSendView()

        XlmSendStrategy(
            xlmDataManager = mock {
                on { defaultAccount() } `it returns` Single.just(AccountReference.Xlm("The Xlm account", ""))
                on { getMaxSpendableAfterFees(FeeType.Regular) } `it returns` Single.just(150.lumens())
            },
            xlmFeesFetcher = mock {
                on { operationFee(FeeType.Regular) } `it returns` Single.just(1.stroops())
            },
            xlmTransactionSender = mock(),
            exchangeRates = exchangeRates,
            sendFundsResultLocalizer = mock(),
            walletOptionsDataManager = mock(),
            nabuToken = nabuToken,
            nabuDataManager = nabuDataManager,
            pitLinking = pitLinked,
            analytics = mock(),
            stringUtils = stringUtils,
            prefs = prefs
        ).apply {
            attachView(view)
            onCurrencySelected()
        }.onSpendMaxClicked()

        verify(view.mock).updateCryptoAmount(150.lumens())
    }

    @Test
    fun `on onContinueClicked, it takes the address from the view, latest value and displays the send details`() {
        val view = TestSendView()
        val result = SendFundsResult(
            errorCode = 0,
            confirmationDetails = null,
            hash = "TX_HASH",
            sendDetails = mock()
        )
        val transactionSendDataManager = mock<TransactionSender> {
            on { dryRunSendFunds(any()) } `it returns` Single.just(result)
            on { sendFunds(any()) } `it returns` Completable.timer(2, TimeUnit.SECONDS)
                .andThen(
                    Single.just(
                        result
                    )
                )
        }
        val xlmAccountRef = AccountReference.Xlm("The Xlm account", "")
        XlmSendStrategy(
            xlmDataManager = mock {
                on { defaultAccount() } `it returns` Single.just(
                    xlmAccountRef
                )
                on { getMaxSpendableAfterFees(FeeType.Regular) } `it returns` Single.just(
                    99.lumens()
                )
            },
            xlmFeesFetcher = mock {
                on { operationFee(FeeType.Regular) } `it returns` Single.just(200.stroops())
            },
            xlmTransactionSender = transactionSendDataManager,
            walletOptionsDataManager = mock(),
            exchangeRates = exchangeRates,
            sendFundsResultLocalizer = mock(),
            stringUtils = stringUtils,
            nabuToken = nabuToken,
            pitLinking = pitLinked,
            analytics = mock(),
            nabuDataManager = nabuDataManager,
            prefs = prefs
        ).apply {
            attachView(view)
            onViewReady()
            view.assertSendButtonDisabled()
            onAddressTextChange("GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT")
            onCryptoTextChange("1")
            onCryptoTextChange("10")
            onCryptoTextChange("100")
            testScheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS)
            view.assertSendButtonEnabled()
            onContinueClicked()
        }
        verify(view.mock).showPaymentDetails(any())
        verify(view.mock).showPaymentDetails(
            SendConfirmationDetails(
                SendDetails(
                    from = xlmAccountRef,
                    toAddress = "GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT",
                    value = 100.lumens(),
                    fee = 200.stroops(),
                    memo = Memo.None
                ),
                fees = 200.stroops(),
                fiatAmount = 50.usd(),
                fiatFees = 0.00.usd()
            )
        )
        verify(transactionSendDataManager, never()).sendFunds(any())
    }

    @Test
    fun `a dry run happens during field entry and disables send`() {
        val view = TestSendView()
        val result = SendFundsResult(
            errorCode = 2,
            confirmationDetails = null,
            hash = "TX_HASH",
            sendDetails = mock()
        )
        val transactionSendDataManager = mock<TransactionSender> {
            on { dryRunSendFunds(any()) } `it returns` Single.just(result)
        }
        val xlmAccountRef = AccountReference.Xlm("The Xlm account", "")

        XlmSendStrategy(
            xlmDataManager = mock {
                on { defaultAccount() } `it returns` Single.just(
                    xlmAccountRef
                )
                on { getMaxSpendableAfterFees(FeeType.Regular) } `it returns` Single.just(
                    99.lumens()
                )
            },
            xlmFeesFetcher = mock {
                on { operationFee(FeeType.Regular) } `it returns` Single.just(99.stroops())
            },
            xlmTransactionSender = transactionSendDataManager,
            walletOptionsDataManager = mock(),
            exchangeRates = mock(),
            sendFundsResultLocalizer = mock {
                on { localize(result) } `it returns` "The warning"
            },
            stringUtils = stringUtils,
            nabuToken = nabuToken,
            pitLinking = pitLinked,
            analytics = mock(),
            nabuDataManager = nabuDataManager,
            prefs = prefs
        ).apply {
            attachView(view)
            onViewReady()
            view.assertSendButtonDisabled()
            onCryptoTextChange("1")
            onCryptoTextChange("10")
            onCryptoTextChange("100")
            onAddressTextChange("GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT")
        }
        verify(transactionSendDataManager, never()).dryRunSendFunds(any())
        testScheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS)
        verify(transactionSendDataManager).dryRunSendFunds(any())
        verify(view.mock).updateWarning("The warning")
        view.assertSendButtonDisabled()
        verify(transactionSendDataManager, never()).sendFunds(any())
    }

    @Test
    fun `when the address is empty, do not show any warning`() {
        val view = TestSendView()
        val result = SendFundsResult(
            errorCode = 2,
            confirmationDetails = null,
            hash = "TX_HASH",
            sendDetails = mock()
        )
        val transactionSendDataManager = mock<TransactionSender> {
            on { dryRunSendFunds(any()) } `it returns` Single.just(result)
        }
        val xlmAccountRef = AccountReference.Xlm("The Xlm account", "")
        XlmSendStrategy(
            xlmDataManager = mock {
                on { defaultAccount() } `it returns` Single.just(
                    xlmAccountRef
                )
                on { getMaxSpendableAfterFees(FeeType.Regular) } `it returns` Single.just(
                    99.lumens()
                )
            },
            xlmFeesFetcher = mock {
                on { operationFee(FeeType.Regular) } `it returns` Single.just(99.stroops())
            },
            xlmTransactionSender = transactionSendDataManager,
            exchangeRates = mock(),
            walletOptionsDataManager = mock(),
            sendFundsResultLocalizer = mock {
                on { localize(result) } `it returns` "The warning"
            },
            stringUtils = stringUtils,
            nabuToken = nabuToken,
            pitLinking = pitLinked,
            analytics = mock(),
            nabuDataManager = nabuDataManager,
            prefs = prefs
        ).apply {
            attachView(view)
            onViewReady()
            view.assertSendButtonDisabled()
            onCryptoTextChange("1")
            onCryptoTextChange("10")
            onCryptoTextChange("100")
            onAddressTextChange("GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT")
            onAddressTextChange("")
        }
        verify(transactionSendDataManager, never()).dryRunSendFunds(any())
        testScheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS)
        verify(transactionSendDataManager).dryRunSendFunds(any())
        verify(view.mock, never()).updateWarning("The warning")
        verify(view.mock).clearWarning()
        view.assertSendButtonDisabled()
        verify(transactionSendDataManager, never()).sendFunds(any())
    }

    @Test
    fun `multiple dry runs when spread out - debounce behaviour test`() {
        val view = TestSendView()
        val result = SendFundsResult(
            errorCode = 2,
            confirmationDetails = null,
            hash = "TX_HASH",
            sendDetails = mock()
        )
        val transactionSendDataManager = mock<TransactionSender> {
            on { dryRunSendFunds(any()) } `it returns` Single.just(result)
        }
        val xlmAccountRef = AccountReference.Xlm("The Xlm account", "")
        XlmSendStrategy(
            xlmDataManager = mock {
                on { defaultAccount() } `it returns` Single.just(
                    xlmAccountRef
                )
                on { getMaxSpendableAfterFees(FeeType.Regular) } `it returns` Single.just(
                    99.lumens()
                )
            },
            xlmFeesFetcher = mock {
                on { operationFee(FeeType.Regular) } `it returns` Single.just(99.stroops())
            },
            xlmTransactionSender = transactionSendDataManager,
            walletOptionsDataManager = mock(),
            exchangeRates = exchangeRates,
            sendFundsResultLocalizer = mock {
                on { localize(result) } `it returns` "The warning"
            },
            stringUtils = stringUtils,
            nabuToken = nabuToken,
            pitLinking = pitLinked,
            analytics = mock(),
            nabuDataManager = nabuDataManager,
            prefs = prefs
        ).apply {
            attachView(view)
            onViewReady()
            view.assertSendButtonDisabled()
            onAddressTextChange("GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT")
            onCryptoTextChange("1")
            testScheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS)
            onCryptoTextChange("10")
            testScheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS)
            onCryptoTextChange("100")
            testScheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS)
        }
        verify(transactionSendDataManager, times(3)).dryRunSendFunds(any())
        verify(view.mock, times(3)).updateWarning("The warning")
        verify(view.mock, times(3)).updateWarning("The warning")
        view.assertSendButtonDisabled()
        verify(transactionSendDataManager, never()).sendFunds(any())
    }

    @Test
    fun `a successful dry run clears the warning and enables send`() {
        val view = TestSendView()
        val result = SendFundsResult(
            errorCode = 0,
            confirmationDetails = null,
            hash = "TX_HASH",
            sendDetails = mock()
        )
        val transactionSendDataManager = mock<TransactionSender> {
            on { dryRunSendFunds(any()) } `it returns` Single.just(result)
        }
        val xlmAccountRef = AccountReference.Xlm("The Xlm account", "")
        XlmSendStrategy(
            xlmDataManager = mock {
                on { defaultAccount() } `it returns` Single.just(
                    xlmAccountRef
                )
                on { getMaxSpendableAfterFees(FeeType.Regular) } `it returns` Single.just(
                    99.lumens()
                )
            },
            xlmFeesFetcher = mock {
                on { operationFee(FeeType.Regular) } `it returns` Single.just(99.stroops())
            },
            xlmTransactionSender = transactionSendDataManager,
            walletOptionsDataManager = mock(),
            exchangeRates = mock(),
            sendFundsResultLocalizer = mock {
                on { localize(result) } `it returns` "The warning"
            },
            stringUtils = stringUtils,
            nabuToken = nabuToken,
            pitLinking = pitLinked,
            analytics = mock(),
            nabuDataManager = nabuDataManager,
            prefs = prefs
        ).apply {
            attachView(view)
            onViewReady()
            view.assertSendButtonDisabled()
            onCryptoTextChange("1")
            onAddressTextChange("GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT")
        }
        testScheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS)
        verify(view.mock).clearWarning()
        view.assertSendButtonEnabled()
        verify(transactionSendDataManager, never()).sendFunds(any())
    }

    @Test
    fun `on submitPayment, it takes the address from the view, latest value and executes a send`() {
        val sendDetails = SendDetails(
            from = AccountReference.Xlm(
                "The Xlm account",
                "GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT"
            ),
            value = 100.lumens(),
            toAddress = "GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT",
            fee = 150.stroops(),
            memo = Memo.None
        )
        val view = TestSendView()
        val result = SendFundsResult(
            errorCode = 0,
            confirmationDetails = null,
            hash = "TX_HASH",
            sendDetails = sendDetails
        )
        val transactionSendDataManager = mock<TransactionSender> {
            on { sendFunds(any()) } `it returns` Completable.timer(2, TimeUnit.SECONDS)
                .andThen(
                    Single.just(
                        result
                    )
                )
            on { dryRunSendFunds(any()) } `it returns` Single.just(result)
        }
        XlmSendStrategy(
            xlmDataManager = mock {
                on { defaultAccount() } `it returns` Single.just(
                    AccountReference.Xlm(
                        "The Xlm account",
                        "GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT"
                    )
                )
                on { getMaxSpendableAfterFees(FeeType.Regular) } `it returns` Single.just(
                    200.lumens()
                )
            },
            xlmFeesFetcher = mock {
                on { operationFee(FeeType.Regular) } `it returns` Single.just(150.stroops())
            },
            xlmTransactionSender = transactionSendDataManager,
            walletOptionsDataManager = mock(),
            exchangeRates = exchangeRates,
            sendFundsResultLocalizer = mock(),
            stringUtils = stringUtils,
            nabuToken = nabuToken,
            pitLinking = pitLinked,
            analytics = mock(),
            nabuDataManager = nabuDataManager,
            prefs = prefs
        ).apply {
            attachView(view)
            onViewReady()
            view.assertSendButtonDisabled()
            onAddressTextChange("GBAHSNSG37BOGBS4G")
            onCryptoTextChange("1")
            onCryptoTextChange("10")
            onCryptoTextChange("100")
            onAddressTextChange("GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT")
            testScheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS)
            view.assertSendButtonEnabled()
            onContinueClicked()
            verify(transactionSendDataManager, never()).sendFunds(sendDetails)
            submitPayment()
        }
        verify(transactionSendDataManager).sendFunds(
            sendDetails
        )
        verify(view.mock).showProgressDialog(R.string.app_name)
        testScheduler.advanceTimeBy(1999, TimeUnit.MILLISECONDS)
        verify(view.mock, never()).dismissProgressDialog()
        verify(view.mock, never()).dismissConfirmationDialog()
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        verify(view.mock).dismissProgressDialog()
        verify(view.mock).dismissConfirmationDialog()
        verify(view.mock).showTransactionSuccess(CryptoCurrency.XLM)
    }

    @Test
    fun `on submitPayment failure`() {
        val view = TestSendView()
        val transactionSendDataManager = mock<TransactionSender> {
            on { sendFunds(any()) } `it returns` Single.error(Exception("Failure"))
            on { dryRunSendFunds(any()) } `it returns` Single.error(Exception("Failure"))
        }
        XlmSendStrategy(
            xlmDataManager = mock {
                on { defaultAccount() } `it returns` Single.just(
                    AccountReference.Xlm(
                        "The Xlm account",
                        "GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT"
                    )
                )
                on { getMaxSpendableAfterFees(FeeType.Regular) } `it returns` Single.just(
                    200.lumens()
                )
            },
            xlmFeesFetcher = mock {
                on { operationFee(FeeType.Regular) } `it returns` Single.just(150.stroops())
            },
            xlmTransactionSender = transactionSendDataManager,
            walletOptionsDataManager = mock(),
            exchangeRates = exchangeRates,
            sendFundsResultLocalizer = mock(),
            stringUtils = stringUtils,
            nabuToken = nabuToken,
            pitLinking = pitLinked,
            analytics = mock(),
            nabuDataManager = nabuDataManager,
            prefs = prefs
        ).apply {
            attachView(view)
            onViewReady()
            onAddressTextChange("GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT")
            onCryptoTextChange("1")
            onCryptoTextChange("10")
            onCryptoTextChange("100")
            onContinueClicked()
            submitPayment()
        }
        verify(transactionSendDataManager).sendFunds(
            SendDetails(
                from = AccountReference.Xlm(
                    "The Xlm account",
                    "GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT"
                ),
                value = 100.lumens(),
                toAddress = "GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT",
                fee = 150.stroops(),
                memo = Memo.None
            )
        )
        verify(view.mock).showProgressDialog(R.string.app_name)
        verify(view.mock).dismissProgressDialog()
        verify(view.mock).dismissConfirmationDialog()
        verify(view.mock, never()).showTransactionSuccess(CryptoCurrency.XLM)
    }

    @Test
    fun `handle address scan valid address`() {
        val view = TestSendView()
        XlmSendStrategy(
            xlmDataManager = mock {
                on { defaultAccount() } `it returns` Single.just(
                    AccountReference.Xlm(
                        "The Xlm account",
                        "GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT"
                    )
                )
            },
            xlmFeesFetcher = mock {
                on { operationFee(FeeType.Regular) } `it returns` Single.just(1.stroops())
            },
            xlmTransactionSender = mock(),
            walletOptionsDataManager = mock(),
            exchangeRates = exchangeRates,
            sendFundsResultLocalizer = mock(),
            stringUtils = stringUtils,
            nabuToken = nabuToken,
            pitLinking = pitLinked,
            analytics = mock(),
            nabuDataManager = nabuDataManager,
            prefs = prefs
        ).apply {
            attachView(view)
            processURIScanAddress("GDYULVJK2T6G7HFUC76LIBKZEMXPKGINSG6566EPWJKCLXTYVWJ7XPY4")
        }
        verify(view.mock).updateCryptoAmount(0.lumens())
        verify(view.mock).updateFiatAmount(0.usd())
        verify(view.mock).updateReceivingAddress("GDYULVJK2T6G7HFUC76LIBKZEMXPKGINSG6566EPWJKCLXTYVWJ7XPY4")
    }

    @Test
    fun `handle address scan valid uri`() {
        val view = TestSendView()
        XlmSendStrategy(
            xlmDataManager = mock {
                on { defaultAccount() } `it returns` Single.just(
                    AccountReference.Xlm(
                        "The Xlm account",
                        "GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT"
                    )
                )
            },
            xlmFeesFetcher = mock {
                on { operationFee(FeeType.Regular) } `it returns` Single.just(1.stroops())
            },
            xlmTransactionSender = mock(),
            walletOptionsDataManager = mock(),
            exchangeRates = exchangeRates,
            sendFundsResultLocalizer = mock(),
            stringUtils = stringUtils,
            nabuToken = nabuToken,
            pitLinking = pitLinked,
            analytics = mock(),
            nabuDataManager = nabuDataManager,
            prefs = prefs
        ).apply {
            attachView(view)
            processURIScanAddress(
                "web+stellar:pay?destination=" +
                    "GCALNQQBXAPZ2WIRSDDBMSTAKCUH5SG6U76YBFLQLIXJTF7FE5AX7AOO&amount=" +
                    "120.1234567&memo=skdjfasf&msg=pay%20me%20with%20lumens"
            )
        }
        verify(view.mock).updateCryptoAmount(120.1234567.lumens())
        verify(view.mock).updateFiatAmount(60.06.usd())
        verify(view.mock).updateReceivingAddress("GCALNQQBXAPZ2WIRSDDBMSTAKCUH5SG6U76YBFLQLIXJTF7FE5AX7AOO")
    }

    @Test
    fun `scan address, returns confirmation details`() {
        val view = TestSendView()
        val result = SendFundsResult(
            errorCode = 0,
            confirmationDetails = null,
            hash = "TX_HASH",
            sendDetails = mock()
        )
        val transactionSendDataManager = mock<TransactionSender> {
            on { dryRunSendFunds(any()) } `it returns` Single.just(result)
            on { sendFunds(any()) } `it returns` Completable.timer(2, TimeUnit.SECONDS)
                .andThen(
                    Single.just(
                        result
                    )
                )
        }
        val xlmAccountRef = AccountReference.Xlm("The Xlm account", "")
        XlmSendStrategy(
            xlmDataManager = mock {
                on { defaultAccount() } `it returns` Single.just(
                    xlmAccountRef
                )
                on { getMaxSpendableAfterFees(FeeType.Regular) } `it returns` Single.just(
                    99.lumens()
                )
            },
            xlmFeesFetcher = mock {
                on { operationFee(FeeType.Regular) } `it returns` Single.just(200.stroops())
            },
            xlmTransactionSender = transactionSendDataManager,
            walletOptionsDataManager = mock(),
            sendFundsResultLocalizer = mock(),
            stringUtils = stringUtils,
            nabuToken = nabuToken,
            pitLinking = pitLinked,
            analytics = mock(),
            nabuDataManager = nabuDataManager,
            prefs = prefs,
            exchangeRates = exchangeRates
        ).apply {
            attachView(view)
            processURIScanAddress(
                "web+stellar:pay?destination=" +
                    "GCALNQQBXAPZ2WIRSDDBMSTAKCUH5SG6U76YBFLQLIXJTF7FE5AX7AOO&amount=" +
                    "120.1234567&memo=1234&memo_type=MEMO_ID&msg=pay%20me%20with%20lumens"
            )
            verify(view.mock).displayMemo(Memo("1234", type = "id"))
            onViewReady()
            view.assertSendButtonDisabled()
            testScheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS)
            view.assertSendButtonEnabled()
            onContinueClicked()
        }
        verify(view.mock).showPaymentDetails(any())
        verify(view.mock).showPaymentDetails(
            SendConfirmationDetails(
                SendDetails(
                    from = xlmAccountRef,
                    toAddress = "GCALNQQBXAPZ2WIRSDDBMSTAKCUH5SG6U76YBFLQLIXJTF7FE5AX7AOO",
                    value = 120.1234567.lumens(),
                    fee = 200.stroops(),
                    memo = Memo("1234", type = "id")
                ),
                fees = 200.stroops(),
                fiatAmount = 60.06.usd(),
                fiatFees = 0.00.usd()
            )
        )
        verify(transactionSendDataManager, never()).sendFunds(any())
    }

    @Test
    fun `text memo`() {
        val memo = Memo(value = "This is the memo", type = "text")
        val sendDetails = SendDetails(
            from = AccountReference.Xlm(
                "The Xlm account",
                "GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT"
            ),
            value = 100.lumens(),
            toAddress = "GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT",
            fee = 150.stroops(),
            memo = memo
        )
        val view = TestSendView()
        val result = SendFundsResult(
            errorCode = 0,
            confirmationDetails = null,
            hash = "TX_HASH",
            sendDetails = sendDetails
        )
        val transactionSendDataManager = mock<TransactionSender> {
            on { sendFunds(any()) } `it returns` Completable.timer(2, TimeUnit.SECONDS)
                .andThen(
                    Single.just(
                        result
                    )
                )
            on { dryRunSendFunds(any()) } `it returns` Single.just(result)
        }
        XlmSendStrategy(
            xlmDataManager = mock {
                on { defaultAccount() } `it returns` Single.just(
                    AccountReference.Xlm(
                        "The Xlm account",
                        "GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT"
                    )
                )
                on { getMaxSpendableAfterFees(FeeType.Regular) } `it returns` Single.just(
                    200.lumens()
                )
            },
            xlmFeesFetcher = mock {
                on { operationFee(FeeType.Regular) } `it returns` Single.just(150.stroops())
            },
            xlmTransactionSender = transactionSendDataManager,
            walletOptionsDataManager = mock(),
            exchangeRates = exchangeRates,
            sendFundsResultLocalizer = mock(),
            stringUtils = stringUtils,
            nabuToken = nabuToken,
            pitLinking = pitLinked,
            analytics = mock(),
            nabuDataManager = nabuDataManager,
            prefs = prefs
        ).apply {
            attachView(view)
            onViewReady()
            view.assertSendButtonDisabled()
            onAddressTextChange("GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT")
            onCryptoTextChange("100")
            onMemoChange(memo)
            testScheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS)
            view.assertSendButtonEnabled()
            onContinueClicked()
            verify(transactionSendDataManager, never()).sendFunds(sendDetails)
            submitPayment()
        }
        verify(transactionSendDataManager).sendFunds(
            sendDetails
        )
        verify(view.mock).showProgressDialog(R.string.app_name)
        testScheduler.advanceTimeBy(1999, TimeUnit.MILLISECONDS)
        verify(view.mock, never()).dismissProgressDialog()
        verify(view.mock, never()).dismissConfirmationDialog()
        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        verify(view.mock).dismissProgressDialog()
        verify(view.mock).dismissConfirmationDialog()
        verify(view.mock).showTransactionSuccess(CryptoCurrency.XLM)
    }

    @Test
    fun `test memo is required when address in on the exchangeAddresses list and info link is shown`() {
        val view = TestSendView()
        val walletOptionsDataManager = mock<WalletOptionsDataManager> {
            on { isXlmAddressExchange("testAddress") } `it returns` true
        }

        val dataManager = mock<XlmDataManager> {
            on { defaultAccount() } `it returns` Single.just(AccountReference.Xlm("The Xlm account", ""))
            on { getMaxSpendableAfterFees(FeeType.Regular) } `it returns` Single.just(199.5.lumens())
        }

        val feesFetcher = mock<XlmFeesFetcher> {
            on { operationFee(FeeType.Regular) } `it returns` Single.just(1.stroops())
        }

        val strategy = XlmSendStrategy(
            xlmDataManager = dataManager,
            xlmFeesFetcher = feesFetcher,
            xlmTransactionSender = mock(),
            walletOptionsDataManager = walletOptionsDataManager,
            exchangeRates = exchangeRates,
            sendFundsResultLocalizer = mock(),
            stringUtils = stringUtils,
            nabuDataManager = nabuDataManager,
            pitLinking = pitLinked,
            analytics = mock(),
            nabuToken = nabuToken,
            prefs = prefs
        ).apply {
            attachView(view)
            onViewReady()
        }

        val observer = strategy.memoRequired().test()

        strategy.onAddressTextChange("testAddress")

        observer.assertValueCount(1)
        observer.assertValue(true)
        verify(walletOptionsDataManager).isXlmAddressExchange("testAddress")
        verify(view.mock).showInfoLink()
    }

    @Test
    fun `test memo is not required when address in not on the exchangeAddresses list and info link is shown`() {
        val view = TestSendView()
        val walletOptionsDataManager = mock<WalletOptionsDataManager> {
            on { isXlmAddressExchange("testAddress") } `it returns` false
        }

        val dataManager = mock<XlmDataManager> {
            on { defaultAccount() } `it returns` Single.just(AccountReference.Xlm("The Xlm account", ""))
            on { getMaxSpendableAfterFees(FeeType.Regular) } `it returns` Single.just(199.5.lumens())
        }

        val feesFetcher = mock<XlmFeesFetcher> {
            on { operationFee(FeeType.Regular) } `it returns` Single.just(1.stroops())
        }

        val strategy = XlmSendStrategy(
            xlmDataManager = dataManager,
            xlmFeesFetcher = feesFetcher,
            xlmTransactionSender = mock(),
            walletOptionsDataManager = walletOptionsDataManager,
            exchangeRates = exchangeRates,
            sendFundsResultLocalizer = mock(),
            stringUtils = stringUtils,
            nabuDataManager = nabuDataManager,
            pitLinking = pitLinked,
            analytics = mock(),
            nabuToken = nabuToken,
            prefs = prefs
        ).apply {
            attachView(view)
            onViewReady()
        }

        val observer = strategy.memoRequired().test()

        strategy.onAddressTextChange("testAddress")

        observer.assertValueCount(1)
        observer.assertValue(false)
        verify(walletOptionsDataManager).isXlmAddressExchange("testAddress")
        verify(view.mock, times(2)).showInfoLink()
    }

    @Test
    fun `test view pit address should be available when it is returned by nabuserver`() {
        val view = TestSendView()
        val walletOptionsDataManager = mock<WalletOptionsDataManager> {
            on { isXlmAddressExchange("testAddress") } `it returns` true
        }

        val dataManager = mock<XlmDataManager> {
            on { defaultAccount() } `it returns` Single.just(AccountReference.Xlm("The Xlm account", ""))
            on { getMaxSpendableAfterFees(FeeType.Regular) } `it returns` Single.just(199.5.lumens())
        }

        val feesFetcher = mock<XlmFeesFetcher> {
            on { operationFee(FeeType.Regular) } `it returns` Single.just(1.stroops())
        }

        XlmSendStrategy(
            xlmDataManager = dataManager,
            xlmFeesFetcher = feesFetcher,
            xlmTransactionSender = mock(),
            walletOptionsDataManager = walletOptionsDataManager,
            exchangeRates = exchangeRates,
            sendFundsResultLocalizer = mock(),
            stringUtils = stringUtils,
            nabuDataManager = nabuDataManager,
            pitLinking = pitLinked,
            analytics = mock(),
            nabuToken = nabuToken,
            prefs = prefs
        ).apply {
            attachView(view)
            onViewReady()
        }
        verify(view.mock).updateReceivingHintAndAccountDropDowns(
            eq(CryptoCurrency.XLM),
            eq(1),
            eq(true),
            any()
        )
    }

    @Test
    fun `test view pit address shouldn't be available when it no address returned by nabuserver`() {
        val view = TestSendView()
        val walletOptionsDataManager = mock<WalletOptionsDataManager> {
            on { isXlmAddressExchange("testAddress") } `it returns` true
        }

        val dataManager = mock<XlmDataManager> {
            on { defaultAccount() } `it returns` Single.just(AccountReference.Xlm("The Xlm account", ""))
            on { getMaxSpendableAfterFees(FeeType.Regular) } `it returns` Single.just(199.5.lumens())
        }

        val nabuDataManager: NabuDataManager =
            mock {
                on { fetchCryptoAddressFromThePit(any(), any()) } `it returns` Single.error(Throwable())
            }

        val feesFetcher = mock<XlmFeesFetcher> {
            on { operationFee(FeeType.Regular) } `it returns` Single.just(1.stroops())
        }

        XlmSendStrategy(
            xlmDataManager = dataManager,
            xlmFeesFetcher = feesFetcher,
            xlmTransactionSender = mock(),
            walletOptionsDataManager = walletOptionsDataManager,
            exchangeRates = exchangeRates,
            sendFundsResultLocalizer = mock(),
            stringUtils = stringUtils,
            nabuDataManager = nabuDataManager,
            pitLinking = pitLinked,
            analytics = mock(),
            nabuToken = nabuToken,
            prefs = prefs
        ).apply {
            attachView(view)
            onViewReady()
        }
        verify(view.mock, never()).updateReceivingHintAndAccountDropDowns(CryptoCurrency.XLM, 1, true)
        verify(view.mock, times(2)).updateReceivingHintAndAccountDropDowns(
            eq(CryptoCurrency.XLM),
            eq(1),
            eq(false),
            any()
        )
    }

    @Test
    fun `test view pit icon should be visible when 2fa error returned by nabuserver`() {
        val view = TestSendView()
        val walletOptionsDataManager = mock<WalletOptionsDataManager> {
            on { isXlmAddressExchange("testAddress") } `it returns` true
        }

        val dataManager = mock<XlmDataManager> {
            on { defaultAccount() } `it returns` Single.just(AccountReference.Xlm("The Xlm account", ""))
            on { getMaxSpendableAfterFees(FeeType.Regular) } `it returns` Single.just(199.5.lumens())
        }

        val nabuDataManager: NabuDataManager =
            mock {
                on {
                    fetchCryptoAddressFromThePit(any(), any())
                } `it returns`
                    Single.error(NabuApiException.withErrorCode(
                        NabuErrorCodes.Bad2fa.code
                    )
                    )
            }

        val feesFetcher = mock<XlmFeesFetcher> {
            on { operationFee(FeeType.Regular) } `it returns` Single.just(1.stroops())
        }

        XlmSendStrategy(
            xlmDataManager = dataManager,
            xlmFeesFetcher = feesFetcher,
            xlmTransactionSender = mock(),
            walletOptionsDataManager = walletOptionsDataManager,
            exchangeRates = exchangeRates,
            sendFundsResultLocalizer = mock(),
            stringUtils = stringUtils,
            nabuDataManager = nabuDataManager,
            pitLinking = pitLinked,
            analytics = mock(),
            nabuToken = nabuToken,
            prefs = prefs
        ).apply {
            attachView(view)
            onViewReady()
        }

        verify(view.mock).updateReceivingHintAndAccountDropDowns(
            eq(CryptoCurrency.XLM),
            eq(1),
            eq(true),
            any()
        )
    }

    @Test
    fun `test no nabu call happens for pit address when wallet is not connected`() {
        val view = TestSendView()
        val walletOptionsDataManager = mock<WalletOptionsDataManager> {
            on { isXlmAddressExchange("testAddress") } `it returns` true
        }

        val dataManager = mock<XlmDataManager> {
            on { defaultAccount() } `it returns` Single.just(AccountReference.Xlm("The Xlm account", ""))
            on { getMaxSpendableAfterFees(FeeType.Regular) } `it returns` Single.just(199.5.lumens())
        }

        val nabuDataManager: NabuDataManager =
            mock {
                on { fetchCryptoAddressFromThePit(any(), any()) } `it returns` Single.error(Throwable())
            }

        val feesFetcher = mock<XlmFeesFetcher> {
            on { operationFee(FeeType.Regular) } `it returns` Single.just(1.stroops())
        }

        XlmSendStrategy(
            xlmDataManager = dataManager,
            xlmFeesFetcher = feesFetcher,
            xlmTransactionSender = mock(),
            walletOptionsDataManager = walletOptionsDataManager,
            exchangeRates = exchangeRates,
            sendFundsResultLocalizer = mock(),
            stringUtils = stringUtils,
            nabuDataManager = nabuDataManager,
            pitLinking = pitUnLinked,
            analytics = mock(),
            nabuToken = nabuToken,
            prefs = prefs
        ).apply {
            attachView(view)
            onViewReady()
        }
        verify(nabuDataManager, never()).fetchCryptoAddressFromThePit(any(), any())
    }

    companion object {
        private const val TEST_FIAT = "USD"
    }
}

class TestSendView(val mock: SendView = mock()) : SendView by mock {

    private var sendEnabled = true

    override fun setSendButtonEnabled(enabled: Boolean) {
        sendEnabled = enabled
        mock.setSendButtonEnabled(enabled)
    }

    fun assertSendButtonEnabled() {
        sendEnabled `should be` true
    }

    fun assertSendButtonDisabled() {
        sendEnabled `should be` false
    }
}

class ResourceSendFundsResultLocalizerTest {

    private val resources: Resources = mock {
        on { getString(R.string.transaction_submitted) } `it returns` "Success!"
        on { getString(R.string.transaction_failed) } `it returns` "Transaction failed!"
        on {
            getString(
                R.string.not_enough_funds_with_currency,
                CryptoCurrency.XLM
            )
        } `it returns` "Insufficient XLM funds!"
        on {
            getString(
                R.string.transaction_failed_min_send,
                1.5.lumens().toStringWithSymbol()
            )
        } `it returns` "Min balance required is 1.5 XLM!"
        on {
            getString(
                R.string.xlm_transaction_failed_min_balance_new_account,
                1.lumens().toStringWithSymbol()
            )
        } `it returns` "Min balance for new account is 1 XLM!"
        on { getString(R.string.invalid_address) } `it returns` "Invalid address!"
    }

    @Test
    fun `success result`() {
        ResourceSendFundsResultLocalizer(resources)
            .localize(
                SendFundsResult(
                    sendDetails = com.nhaarman.mockito_kotlin.mock(),
                    errorCode = 0,
                    confirmationDetails = null,
                    hash = "hash"
                )
            ) `should equal` "Success!"
    }

    @Test
    fun `xlm error 0 result (no hash)`() {
        ResourceSendFundsResultLocalizer(resources)
            .localize(
                SendFundsResult(
                    sendDetails = SendDetails(
                        from = AccountReference.Xlm("", ""),
                        toAddress = "",
                        value = com.nhaarman.mockito_kotlin.mock(),
                        fee = 1.stroops()
                    ),
                    errorCode = 0,
                    errorValue = 1.lumens(),
                    confirmationDetails = null,
                    hash = null
                )
            ) `should equal` "Transaction failed!"
    }

    @Test
    fun `xlm error 1 result`() {
        ResourceSendFundsResultLocalizer(resources)
            .localize(
                SendFundsResult(
                    sendDetails = SendDetails(
                        from = AccountReference.Xlm("", ""),
                        toAddress = "",
                        value = com.nhaarman.mockito_kotlin.mock(),
                        fee = 1.stroops()
                    ),
                    errorCode = 1,
                    errorValue = 1.lumens(),
                    confirmationDetails = null,
                    hash = "hash"
                )
            ) `should equal` "Transaction failed!"
    }

    @Test
    fun `xlm error 2 result`() {
        ResourceSendFundsResultLocalizer(resources)
            .localize(
                SendFundsResult(
                    sendDetails = SendDetails(
                        from = AccountReference.Xlm("", ""),
                        toAddress = "",
                        value = com.nhaarman.mockito_kotlin.mock(),
                        fee = 1.stroops()
                    ),
                    errorCode = 2,
                    errorValue = 1.5.lumens(),
                    confirmationDetails = null,
                    hash = "hash"
                )
            ) `should equal` "Min balance required is 1.5 XLM!"
    }

    @Test
    fun `xlm error 3 result`() {
        ResourceSendFundsResultLocalizer(resources)
            .localize(
                SendFundsResult(
                    sendDetails = SendDetails(
                        from = AccountReference.Xlm("", ""),
                        toAddress = "",
                        value = com.nhaarman.mockito_kotlin.mock(),
                        fee = 1.stroops()
                    ),
                    errorCode = 3,
                    errorValue = 1.lumens(),
                    confirmationDetails = null,
                    hash = "hash"
                )
            ) `should equal` "Min balance for new account is 1 XLM!"
    }

    @Test
    fun `xlm error 4 result`() {
        ResourceSendFundsResultLocalizer(resources)
            .localize(
                SendFundsResult(
                    sendDetails = SendDetails(
                        from = AccountReference.Xlm("", ""),
                        toAddress = "",
                        value = com.nhaarman.mockito_kotlin.mock(),
                        fee = 1.stroops()
                    ),
                    errorCode = 4,
                    confirmationDetails = null,
                    hash = "hash"
                )
            ) `should equal` "Insufficient XLM funds!"
    }

    @Test
    fun `xlm error 5 result`() {
        ResourceSendFundsResultLocalizer(resources)
            .localize(
                SendFundsResult(
                    sendDetails = SendDetails(
                        from = AccountReference.Xlm("", ""),
                        toAddress = "",
                        value = com.nhaarman.mockito_kotlin.mock(),
                        fee = 1.stroops()
                    ),
                    errorCode = 5,
                    confirmationDetails = null,
                    hash = "hash"
                )
            ) `should equal` "Invalid address!"
    }

    @Test
    fun `xlm error 4 result with value`() {
        ResourceSendFundsResultLocalizer(resources)
            .localize(
                SendFundsResult(
                    sendDetails = SendDetails(
                        from = AccountReference.Xlm("", ""),
                        toAddress = "",
                        value = com.nhaarman.mockito_kotlin.mock(),
                        fee = 1.stroops()
                    ),
                    errorCode = 4,
                    errorValue = 500.lumens(),
                    confirmationDetails = null,
                    hash = "hash"
                )
            ) `should equal` "Insufficient XLM funds!"
    }

    @Test
    fun `ether error 4 result`() {
        ResourceSendFundsResultLocalizer(resources)
            .localize(
                SendFundsResult(
                    sendDetails = SendDetails(
                        from = AccountReference.Ethereum("", ""),
                        toAddress = "",
                        value = com.nhaarman.mockito_kotlin.mock(),
                        fee = 1.stroops()
                    ),
                    errorCode = 4,
                    confirmationDetails = null,
                    hash = "hash"
                )
            ) `should equal` "Transaction failed!"
    }
}