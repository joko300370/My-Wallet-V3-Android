package piuk.blockchain.android.ui.activity.detail

import com.blockchain.android.testutils.rxInit
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CustodialActivitySummaryItem
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import java.util.Date

class ActivityDetailsModelTest {

    private lateinit var model: ActivityDetailsModel
    private var state = ActivityDetailState()
    private val interactor: ActivityDetailsInteractor = mock()

    private data class NonCustodialTestClass(
        override val exchangeRates: ExchangeRateDataManager = mock(),
        override val cryptoCurrency: CryptoCurrency = mock(),
        override val txId: String = "123",
        override val timeStampMs: Long = 1L,
        override val cryptoValue: CryptoValue = mock(),
        override val direction: TransactionSummary.Direction = TransactionSummary.Direction.SENT,
        override val fee: Observable<CryptoValue> = mock(),
        override val inputsMap: Map<String, CryptoValue> = mock(),
        override val outputsMap: Map<String, CryptoValue> = mock(),
        override val description: String? = "desc",
        override val account: CryptoAccount = mock()
    ) : NonCustodialActivitySummaryItem()

    private val custodialItem = CustodialActivitySummaryItem(
        exchangeRates = mock(),
        cryptoCurrency = mock(),
        txId = "123",
        timeStampMs = 1L,
        cryptoValue = mock(),
        fundedFiat = mock(),
        status = OrderState.FINISHED,
        fee = mock(),
        account = mock(),
        paymentMethodId = "123"
    )

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setup() {
        model = ActivityDetailsModel(state, Schedulers.io(), interactor)
    }

    @Test
    fun `starting the model with non custodial item loads non custodial details`() {
        val item = NonCustodialTestClass()
        val crypto = CryptoCurrency.BCH
        val txId = "123455"
        whenever(interactor.getNonCustodialActivityDetails(crypto, txId)).thenReturn(item)

        model.process(LoadActivityDetailsIntent(crypto, txId, false))

        verify(interactor).getNonCustodialActivityDetails(crypto, txId)
    }

    @Test
    fun `starting the model with custodial item loads custodial details`() {
        val crypto = CryptoCurrency.BCH
        val txId = "123455"
        whenever(interactor.getCustodialActivityDetails(crypto, txId)).thenReturn(custodialItem)
        whenever(interactor.loadCustodialItems(custodialItem)).thenReturn(
            Single.just(emptyList())
        )

        model.process(LoadActivityDetailsIntent(crypto, txId, true))

        verify(interactor).getCustodialActivityDetails(crypto, txId)
    }

    @Test
    fun `processing non custodial item loads header details correctly`() {
        val item = NonCustodialTestClass()

        val testObserver = model.state.test()
        model.process(LoadNonCustodialHeaderDataIntent(item))

        testObserver.assertValueAt(0, state)
        testObserver.assertValueAt(1, state.copy(
            direction = item.direction,
            amount = item.cryptoValue,
            isPending = item.isPending,
            isFeeTransaction = item.isFeeTransaction,
            confirmations = item.confirmations,
            totalConfirmations = item.cryptoCurrency.requiredConfirmations
        ))
    }

    @Test
    fun `processing custodial item loads header details correctly`() {
        val testObserver = model.state.test()
        model.process(LoadCustodialHeaderDataIntent(custodialItem))

        testObserver.assertValueAt(0, state)
        testObserver.assertValueAt(1, state.copy(
            direction = TransactionSummary.Direction.BUY,
            amount = custodialItem.cryptoValue,
            isPending = false,
            isFeeTransaction = false,
            confirmations = 0,
            totalConfirmations = 0
        ))
    }

    @Test
    fun `processing creation date returns correct values`() {
        val item = NonCustodialTestClass()
        val returnDate = Date()
        whenever(interactor.loadCreationDate(item)).thenReturn(returnDate)
        whenever(interactor.loadConfirmedSentItems(item)).thenReturn(Single.just(listOf()))

        val testObserver = model.state.test()
        model.process(LoadNonCustodialCreationDateIntent(item))

        verify(interactor).loadCreationDate(item)

        val list = state.listOfItems.toMutableSet()
        list.add(Created(returnDate))
        testObserver.assertValueAt(0, state)
        testObserver.assertValueAt(1, state.copy(listOfItems = list))
    }

    @Test
    fun `failing to load non custodial details updates state correctly`() {
        val crypto = CryptoCurrency.BCH
        val txId = "123455"
        whenever(interactor.getNonCustodialActivityDetails(crypto, txId)).thenReturn(null)

        val testObserver = model.state.test()
        model.process(LoadActivityDetailsIntent(crypto, txId, false))

        testObserver.assertValueAt(0, state)
        testObserver.assertValueAt(1, state.copy(isError = true))
    }

    @Test
    fun `failing to load custodial details updates state correctly`() {
        val crypto = CryptoCurrency.BCH
        val txId = "123455"
        whenever(interactor.getCustodialActivityDetails(crypto, txId)).thenReturn(null)

        val testObserver = model.state.test()
        model.process(LoadActivityDetailsIntent(crypto, txId, true))

        testObserver.assertValueAt(0, state)
        testObserver.assertValueAt(1, state.copy(isError = true))
    }

    @Test
    fun `activity items load correctly`() {
        val list = listOf(Fee(mock()), Amount(mock()), To(""), From(""))

        val currentList = state.listOfItems.toMutableSet()
        currentList.addAll(list.toSet())

        val testObserver = model.state.test()
        model.process(ListItemsLoadedIntent(list))

        testObserver.assertValueAt(0, state)
        testObserver.assertValueAt(1, state.copy(
            listOfItems = currentList
        ))
    }
}