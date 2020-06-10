package piuk.blockchain.android.ui.transfer.send.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_send_account.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoSingleAccount

class AccountsAdapter(
    private val disposable: CompositeDisposable,
    private val onAccountSelected: (CryptoSingleAccount) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var itemsList = mutableListOf<CryptoSingleAccount>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return AccountViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_send_account,
                parent,
                false
            ),
            disposable
        )
    }

    override fun getItemCount(): Int =
        itemsList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as AccountViewHolder).bind(itemsList[position], onAccountSelected)
    }
}

private class AccountViewHolder(val parent: View, val disposable: CompositeDisposable) :
    RecyclerView.ViewHolder(parent),
    LayoutContainer {
    override val containerView: View?
        get() = itemView

    fun bind(item: CryptoSingleAccount, onAccountSelected: (CryptoSingleAccount) -> Unit) {
        itemView.item_account_parent.setOnClickListener { onAccountSelected(item) }
        itemView.item_account_title.text = item.label
        disposable += item.balance.observeOn(AndroidSchedulers.mainThread()).subscribe({
            itemView.item_account_details.text = it.toStringWithSymbol()
        }, {})
    }
}