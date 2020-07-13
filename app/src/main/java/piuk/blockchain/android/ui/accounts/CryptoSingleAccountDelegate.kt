package piuk.blockchain.android.ui.accounts

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import kotlinx.android.synthetic.main.item_account_select_crypto.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAccount

class CryptoAccountDelegate<in T>(
    private val onAccountClicked: (CryptoAccount) -> Unit
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is CryptoAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CryptoSingleAccountViewHolder(parent.inflate(R.layout.item_account_select_crypto))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as CryptoSingleAccountViewHolder).bind(
        items[position] as CryptoAccount,
        onAccountClicked
    )
}

private class CryptoSingleAccountViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    internal fun bind(
        account: CryptoAccount,
        onAccountClicked: (CryptoAccount) -> Unit
    ) {
        with(itemView) {
            item.account = account
            setOnClickListener { onAccountClicked(account) }
        }
    }
}
