package piuk.blockchain.android.ui.accounts

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_account_select_fiat.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class FiatAccountDelegate<in T>(
    private val onAccountClicked: (FiatAccount) -> Unit
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is FiatAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FiatAccountViewHolder(
            parent.inflate(R.layout.item_account_select_fiat)
        )

    override fun onBindViewHolder(items: List<T>, position: Int, holder: RecyclerView.ViewHolder) {
        (holder as FiatAccountViewHolder).bind(items[position] as FiatAccount, onAccountClicked)
    }
}

private class FiatAccountViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    internal fun bind(
        account: FiatAccount,
        onAccountClicked: (FiatAccount) -> Unit
    ) {
        with(itemView) {
            item.account = account
            setOnClickListener { onAccountClicked(account) }
        }
    }
}
