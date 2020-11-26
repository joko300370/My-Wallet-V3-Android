package piuk.blockchain.android.ui.swap.homebrew.exchange.history

import com.blockchain.android.testutils.rxInit
import com.blockchain.swap.common.trade.MorphTradeDataHistoryList
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.swapold.exchange.history.ExchangeUiState
import piuk.blockchain.android.ui.swapold.exchange.history.TradeHistoryPresenter
import piuk.blockchain.android.ui.swapold.exchange.history.TradeHistoryView
import piuk.blockchain.android.util.DateUtil
import java.util.Locale

class TradeHistoryPresenterTest {

    private lateinit var subject: TradeHistoryPresenter
    private val dataManager: MorphTradeDataHistoryList = mock()
    private val dateUtil: DateUtil = mock()
    private val view: TradeHistoryView = mock()

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = TradeHistoryPresenter(dataManager, dateUtil)
        subject.initView(view)

        whenever(dateUtil.formatted(any())).thenReturn("DATE")

        Locale.setDefault(Locale.UK)
    }

    @Test
    fun `getTradeHistory fails to load trades`() {
        // Arrange
        whenever(dataManager.getTrades()).thenReturn(Single.error { Throwable() })
        // Act
        subject.getTradeHistory()
        // Assert
        verify(view).renderUi(ExchangeUiState.Loading)
        verify(view).renderUi(ExchangeUiState.Error)
    }

    @Test
    fun `getTradeHistory loads empty list`() {
        // Arrange
        whenever(dataManager.getTrades()).thenReturn(Single.just(emptyList()))
        // Act
        subject.getTradeHistory()
        // Assert
        verify(view).renderUi(ExchangeUiState.Loading)
        verify(view).renderUi(ExchangeUiState.Empty)
    }
}