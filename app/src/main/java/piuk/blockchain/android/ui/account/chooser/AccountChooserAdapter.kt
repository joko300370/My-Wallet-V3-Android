package piuk.blockchain.android.ui.account.chooser

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.serialization.JsonSerializableAccount
import kotlinx.android.synthetic.main.item_accounts_row.view.*
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class AccountChooserAdapter(
    private val items: List<AccountChooserItem>,
    private val clickEvent: (JsonSerializableAccount) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private fun <T : JsonSerializableAccount> clickListener(account: T?): View.OnClickListener = View.OnClickListener {
        if (account != null) {
            clickEvent(account)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        when (ViewType.values().first { it.ordinal == viewType }) {
            ViewType.VIEW_TYPE_HEADER -> {
                val header = parent.inflate(R.layout.item_accounts_row_header)
                HeaderViewHolder(header)
            }
            ViewType.VIEW_TYPE_ACCOUNT -> {
                val account = parent.inflate(R.layout.item_accounts_row)
                AccountViewHolder(account)
            }
            ViewType.VIEW_TYPE_LEGACY -> {
                val account = parent.inflate(R.layout.item_accounts_row)
                AddressViewHolder(account)
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (item) {
            is AccountChooserItem.Header -> {
                val headerViewHolder = holder as HeaderViewHolder
                headerViewHolder.header.text = item.label
                holder.itemView.setOnClickListener(null)
            }
            is AccountChooserItem.AccountSummary -> {
                (holder as AccountViewHolder).apply {
                    label.text = item.label
                    balance.text = item.displayBalance
                }
                holder.itemView.setOnClickListener(clickListener(item.accountObject))
            }
            is AccountChooserItem.LegacyAddress -> {
                (holder as AddressViewHolder).apply {
                    label.text = item.label
                    balance.text = item.displayBalance
                    address.text = item.address
                    tag.setText(R.string.watch_only)
                    address.goneIf(item.address == null)
                    tag.goneIf(item.address == null)
                }
                holder.itemView.setOnClickListener(clickListener(item.accountObject))
            }
        }
        holder.itemView.contentDescription = item.label
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) =
        when (items[position]) {
            is AccountChooserItem.Header -> ViewType.VIEW_TYPE_HEADER
            is AccountChooserItem.AccountSummary -> ViewType.VIEW_TYPE_ACCOUNT
            is AccountChooserItem.LegacyAddress -> ViewType.VIEW_TYPE_LEGACY
        }.ordinal
}

private class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    internal val header: TextView = itemView.findViewById(R.id.header_name)

    init {
        itemView.findViewById<View>(R.id.imageview_plus).visibility = View.GONE
    }
}

private class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    internal val label: TextView = itemView.findViewById(R.id.my_account_row_label)
    internal val balance: TextView = itemView.findViewById(R.id.my_account_row_amount)

    init {
        itemView.my_account_row_tag.visibility = View.GONE
        itemView.my_account_row_address.visibility = View.GONE
    }
}

private class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    internal val label: TextView = itemView.findViewById(R.id.my_account_row_label)
    internal val tag: TextView = itemView.findViewById(R.id.my_account_row_tag)
    internal val balance: TextView = itemView.findViewById(R.id.my_account_row_amount)
    internal val address: TextView = itemView.findViewById(R.id.my_account_row_address)
}

private enum class ViewType {
    VIEW_TYPE_HEADER,
    VIEW_TYPE_ACCOUNT,
    VIEW_TYPE_LEGACY
}
