package piuk.blockchain.android.ui.dashboard.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import info.blockchain.balance.FiatValue
import kotlinx.android.synthetic.main.item_dashboard_funds.view.*
import kotlinx.android.synthetic.main.item_dashboard_funds_bordered.view.*
import kotlinx.android.synthetic.main.item_dashboard_funds_parent.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.FundsBalanceState
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.toFiatWithCurrency
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf

class FundsCardDelegate<in T>(
    private val selectedFiat: String,
    private val onFundsItemClicked: (FiatValue) -> Unit,
    private val exchangeRateDataManager: ExchangeRateDataManager
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is FundsBalanceState

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FundsCardViewHolder(parent.inflate(R.layout.item_dashboard_funds_parent),
            onFundsItemClicked, selectedFiat, exchangeRateDataManager)

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as FundsCardViewHolder).bind(items[position] as FundsBalanceState)
}

private class FundsCardViewHolder(
    itemView: View,
    private val onFundsItemClicked: (FiatValue) -> Unit,
    private val selectedFiat: String,
    private val exchangeRateDataManager: ExchangeRateDataManager
) : RecyclerView.ViewHolder(itemView) {
    private val multipleFundsAdapter: MultipleFundsAdapter by lazy {
        MultipleFundsAdapter(onFundsItemClicked, selectedFiat, exchangeRateDataManager)
    }

    fun bind(funds: FundsBalanceState) {
        if (funds.fiatBalances.size == 1) {
            val fiatValue = funds.fiatBalances[0]
            val ticker = fiatValue.currencyCode
            itemView.apply {
                funds_balance_other_fiat.visibleIf { selectedFiat != ticker }
                funds_balance_other_fiat.text = fiatValue.toStringWithSymbol()
                funds_list.gone()
                funds_single_item.setOnClickListener {
                    onFundsItemClicked(fiatValue)
                }
                funds_title.setStringFromTicker(context, ticker)
                funds_fiat_ticker.text = ticker
                funds_balance.text = if (selectedFiat == ticker) {
                    fiatValue.toStringWithSymbol()
                } else {
                    fiatValue.toFiatWithCurrency(exchangeRateDataManager, selectedFiat)
                        .toStringWithSymbol()
                }
                funds_icon.setIcon(ticker)
            }
        } else {
            itemView.funds_single_item.gone()
            itemView.funds_list.apply {
                layoutManager =
                    LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
                adapter = multipleFundsAdapter
            }
            multipleFundsAdapter.items = funds.fiatBalances
        }
    }
}

private class MultipleFundsAdapter(
    private val onFundsItemClicked: (FiatValue) -> Unit,
    private val selectedFiat: String,
    private val exchangeRateDataManager: ExchangeRateDataManager
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var items = listOf<FiatValue>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        SingleFundsViewHolder(parent.inflate(R.layout.item_dashboard_funds_bordered),
            onFundsItemClicked, selectedFiat, exchangeRateDataManager)

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) = (holder as SingleFundsViewHolder).bind(items[position])

    private class SingleFundsViewHolder(
        itemView: View,
        private val onFundsItemClicked: (FiatValue) -> Unit,
        private val selectedFiat: String,
        private val exchangeRateDataManager: ExchangeRateDataManager
    ) : RecyclerView.ViewHolder(itemView) {

        fun bind(value: FiatValue) {
            val ticker = value.currencyCode
            itemView.apply {
                bordered_funds_balance_other_fiat.visibleIf { selectedFiat != ticker }
                bordered_funds_balance_other_fiat.text = value.toStringWithSymbol()

                bordered_funds_parent.setOnClickListener {
                    onFundsItemClicked(value)
                }
                bordered_funds_title.setStringFromTicker(context, ticker)
                bordered_funds_fiat_ticker.text = ticker
                bordered_funds_balance.text = if (selectedFiat == ticker) {
                    value.toStringWithSymbol()
                } else {
                    value.toFiatWithCurrency(exchangeRateDataManager, selectedFiat)
                        .toStringWithSymbol()
                }
                bordered_funds_icon.setIcon(ticker)
            }
        }
    }
}

private fun TextView.setStringFromTicker(context: Context, ticker: String) {
    text = context.getString(
        when (ticker) {
            "EUR" -> R.string.euros
            "GBP" -> R.string.pounds
            else -> R.string.empty
        }
    )
}
