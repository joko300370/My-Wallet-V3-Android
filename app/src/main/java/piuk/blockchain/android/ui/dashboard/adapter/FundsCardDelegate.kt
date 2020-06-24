package piuk.blockchain.android.ui.dashboard.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.FundsBalanceState
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class FundsCardDelegate<in T> : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is FundsBalanceState

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FundsCardViewHolder(parent.inflate(R.layout.item_dashboard_funds))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as FundsCardViewHolder).bind(items[position] as FundsBalanceState)
}

private class FundsCardViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    fun bind(funds: FundsBalanceState) {
        if(funds.fiatBalances.isEmpty()) {
            itemView.gone()
        }
    }
}
