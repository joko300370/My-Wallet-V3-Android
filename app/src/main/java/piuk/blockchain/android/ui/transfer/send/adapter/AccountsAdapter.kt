package piuk.blockchain.android.ui.transfer.send.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_send_account.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.SendState
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.goneIf

//class AccountsAdapter(
//    private val itemsList: MutableList<CryptoAccount> = mutableListOf(),
//    private val onAccountSelected: (CryptoAccount) -> Unit
//) : RecyclerView.Adapter<AccountViewHolder>() {
//
//    private val disposables = CompositeDisposable()
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
//        return AccountViewHolder(
//            LayoutInflater.from(parent.context).inflate(
//                R.layout.item_send_account,
//                parent,
//                false
//            )
//        )
//    }
//
//    override fun getItemCount(): Int =
//        itemsList.size
//
//    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
//        holder.bind(itemsList[position], disposables, onAccountSelected)
//    }
//}
//
//class AccountViewHolder(val parent: View) :
//    RecyclerView.ViewHolder(parent),
//    LayoutContainer {
//    override val containerView: View?
//        get() = itemView
//
//    fun bind(
//        item: CryptoAccount,
//        disposables: CompositeDisposable,
//        onItemClicked: (CryptoAccount) -> Unit
//    ) {
//        itemView.setOnClickListener { onItemClicked(item) }
//        itemView.details.account = item
//
//        resetMessage()
//
//        disposables += item.sendState
//            .subscribeBy(
//                onSuccess = {
//                    configureMessage(it)
//                }
//            )
//    }
//
//    private fun resetMessage() {
//        itemView.message.gone()
//    }
//
//    private fun configureMessage(sendState: SendState) {
//        val msg = when (sendState) {
//            SendState.NO_FUNDS -> "No funds"
//            SendState.NOT_SUPPORTED -> "Send not supported on this account"
//            SendState.NOT_ENOUGH_GAS -> "ETH balance low, unable to pay network fee"
//            SendState.SEND_IN_FLIGHT -> "Unavailable due to pending transaction."
//            SendState.CAN_SEND -> null
//        }
//
//        itemView.msg_body.text = msg
//        itemView.message.goneIf(msg == null)
//    }
//}
