package piuk.blockchain.android.ui.transfer.send.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_send_account.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAccount
import timber.log.Timber

class AccountsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var itemsList = mutableListOf<CryptoAccount>()
        set(value) {
            Timber.e("----- adding to list $value")
            field = value
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Timber.e("----- creating viewholder")
        return AccountViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_send_account,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int =
        itemsList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        Timber.e("----- binding viewholder")
        (holder as AccountViewHolder).bind(itemsList[position])
    }
}

private class AccountViewHolder(val parent: View) : RecyclerView.ViewHolder(parent),
    LayoutContainer {
    override val containerView: View?
        get() = itemView

    fun bind(item: CryptoAccount) {
        Timber.e("----- binding item $item")
        itemView.item_account_title.text = item.label
        item.balance.subscribe({ itemView.item_account_details.text = it.toStringWithSymbol() }, {})

    }
}