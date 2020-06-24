package piuk.blockchain.android.ui.dashboard.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.FiatValue
import kotlinx.android.synthetic.main.item_dashboard_funds.view.*
import kotlinx.android.synthetic.main.item_dashboard_funds_bordered.view.*
import kotlinx.android.synthetic.main.item_dashboard_funds_parent.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.FundsBalanceState
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class FundsCardDelegate<in T>(
    private val prefs: CurrencyPrefs,
    private val onFundsItemClicked: (FiatValue) -> Unit
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is FundsBalanceState

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FundsCardViewHolder(parent.inflate(R.layout.item_dashboard_funds_parent),
            onFundsItemClicked, prefs)

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as FundsCardViewHolder).bind(items[position] as FundsBalanceState)
}

private class FundsCardViewHolder(
    itemView: View,
    private val onFundsItemClicked: (FiatValue) -> Unit,
    prefs: CurrencyPrefs
) : RecyclerView.ViewHolder(itemView) {
    private val multipleFundsAdapter: MultipleFundsAdapter by lazy {
        MultipleFundsAdapter(onFundsItemClicked, prefs)
    }

    fun bind(funds: FundsBalanceState) {
        if (funds.fiatBalances.size == 1) {
            val ticker = funds.fiatBalances[0].currencyCode
            itemView.apply {
                funds_list.gone()
                funds_single_item.setOnClickListener {
                    onFundsItemClicked(funds.fiatBalances[0])
                }
                funds_title.setStringFromTicker(context, ticker)
                funds_fiat_ticker.text = ticker
                funds_balance.text = funds.fiatBalances[0].toStringWithSymbol()
                funds_icon.setDrawableFromTicker(context, ticker)
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
    private val prefs: CurrencyPrefs
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var items = listOf<FiatValue>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        SingleFundsViewHolder(parent.inflate(R.layout.item_dashboard_funds_bordered),
            onFundsItemClicked)

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) = (holder as SingleFundsViewHolder).bind(items[position])

    private class SingleFundsViewHolder(
        itemView: View,
        private val onFundsItemClicked: (FiatValue) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        fun bind(value: FiatValue) {
            val ticker = value.currencyCode
            itemView.apply {
                bordered_funds_parent.setOnClickListener {
                    onFundsItemClicked(value)
                }
                bordered_funds_title.setStringFromTicker(context, ticker)
                bordered_funds_fiat_ticker.text = ticker
                bordered_funds_balance.text = value.toStringWithSymbol()
                bordered_funds_icon.setDrawableFromTicker(context, ticker)
            }
        }
    }
}

private fun TextView.setStringFromTicker(context: Context, ticker: String) {
    text = context.getString(
        when (ticker) {
            "EUR" -> R.string.common_euros
            "GBP" -> R.string.common_pounds
            else -> R.string.empty
        }
    )
}

private fun ImageView.setDrawableFromTicker(context: Context, ticker: String) {
    setImageDrawable(
        ContextCompat.getDrawable(context,
            when (ticker) {
                "EUR" -> R.drawable.ic_vector_euro
                "GBP" -> R.drawable.ic_vector_pound
                else -> android.R.drawable.menuitem_background
            }
        ))
}
