package piuk.blockchain.android.ui.swapold.exchange.history

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.ui.swapold.exchange.detail.HomebrewTradeDetailActivity
import piuk.blockchain.android.ui.swapold.exchange.history.adapter.TradeHistoryAdapter
import piuk.blockchain.android.ui.swapold.exchange.model.Trade
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.SwapAnalyticsEvents
import org.koin.android.ext.android.get
import kotlinx.android.synthetic.main.activity_homebrew_trade_history.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible

class TradeHistoryActivity :
    BaseMvpActivity<TradeHistoryView, TradeHistoryPresenter>(),
    TradeHistoryView {

    private val tradeHistoryAdapter = TradeHistoryAdapter(this::tradeClicked)
    private val analytics: Analytics by inject()
    private val tradeHistoryPresenter: TradeHistoryPresenter by scopedInject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homebrew_trade_history)
        get<Analytics>().logEvent(AnalyticsEvents.ExchangeHistory)

        setupToolbar(R.id.toolbar_constraint, R.string.swap)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@TradeHistoryActivity)
            adapter = tradeHistoryAdapter
        }

        swipe_layout.setOnRefreshListener { onViewReady() }

        presenter.onViewReady()
    }

    override fun onResume() {
        super.onResume()
        presenter.onViewResumed()
    }

    override fun renderUi(uiState: ExchangeUiState) {
        when (uiState) {
            is ExchangeUiState.Data -> renderData(uiState)
            ExchangeUiState.Error -> renderError()
            ExchangeUiState.Empty -> renderError()
            ExchangeUiState.Loading -> swipe_layout.isRefreshing = true
        }
    }

    private fun renderData(uiState: ExchangeUiState.Data) {
        tradeHistoryAdapter.items = uiState.trades
        recyclerView.visible()
        swipe_layout.isRefreshing = false
    }

    private fun renderError() {
        swipe_layout.isRefreshing = false
        emptyState.visible()
        recyclerView.gone()
    }

    private fun tradeClicked(trade: Trade) {
        HomebrewTradeDetailActivity.start(this, trade)
        analytics.logEvent(SwapAnalyticsEvents.SwapHistoryOrderClick)
    }

    override fun onSupportNavigateUp(): Boolean = consume { finish() }

    override fun createPresenter(): TradeHistoryPresenter = tradeHistoryPresenter

    override fun getView(): TradeHistoryView = this

    companion object {
        fun start(context: Context) {
            Intent(context, TradeHistoryActivity::class.java)
                .run { context.startActivity(this) }
        }
    }
}
