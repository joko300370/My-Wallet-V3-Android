package piuk.blockchain.android.ui.transfer.send.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_send_account.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoSingleAccount

class AccountsAdapter(
    private val onAccountSelected: (CryptoSingleAccount) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var itemsList = mutableListOf<CryptoSingleAccount>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
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
        (holder as AccountViewHolder).bind(itemsList[position], onAccountSelected)
    }
}

private class AccountViewHolder(val parent: View) :
    RecyclerView.ViewHolder(parent),
    LayoutContainer {
    override val containerView: View?
        get() = itemView

    fun bind(item: CryptoSingleAccount, onAccountSelected: (CryptoSingleAccount) -> Unit) {
        itemView.setOnClickListener { onAccountSelected(item) }
        itemView.details.account = item
    }
}
