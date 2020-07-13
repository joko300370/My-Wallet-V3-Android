package piuk.blockchain.android.ui.accounts

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_account_select_group.view.*
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.impl.AllWalletsAccount

class AllWalletsAccountDelegate<in T>(
    private val onAccountClicked: (BlockchainAccount) -> Unit
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is AllWalletsAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AllWalletsAccountViewHolder(parent.inflate(R.layout.item_account_select_group))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AllWalletsAccountViewHolder).bind(
        items[position] as AllWalletsAccount,
        onAccountClicked
    )
}

private class AllWalletsAccountViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    internal fun bind(
        account: AllWalletsAccount,
        onAccountClicked: (BlockchainAccount) -> Unit
    ) {
        with(itemView) {
            item.account = account
            setOnClickListener { onAccountClicked(account) }
        }
    }
}
