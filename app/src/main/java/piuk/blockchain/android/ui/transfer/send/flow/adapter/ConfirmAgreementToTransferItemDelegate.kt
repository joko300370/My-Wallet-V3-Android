package piuk.blockchain.android.ui.transfer.send.flow.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_send_confirm_agreement_transfer.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendModel
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class ConfirmAgreementToTransferItemDelegate<in T>(
    private val model: SendModel
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position] as ConfirmItemType
        return item is ConfirmAgreementTextItem
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AgreementTextItemViewHolder(
            parent.inflate(R.layout.item_send_confirm_agreement_transfer)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AgreementTextItemViewHolder).bind(
        items[position] as ConfirmAgreementTextItem,
        model
    )
}

private class AgreementTextItemViewHolder(val parent: View) :
    RecyclerView.ViewHolder(parent),
    LayoutContainer {

    override val containerView: View?
        get() = itemView

    fun bind(
        item: ConfirmAgreementTextItem,
        model: SendModel
    ) {
        itemView.confirm_details_checkbox.setText(item.agreementText, TextView.BufferType.SPANNABLE)

        val option = item.state.pendingTx?.getOption<TxOptionValue.TxBooleanOption>(
            TxOption.AGREEMENT_INTEREST_TRANSFER)

        itemView.confirm_details_checkbox.isChecked = option?.value ?: false

        itemView.confirm_details_checkbox.isEnabled = true
        itemView.confirm_details_checkbox.setOnCheckedChangeListener { view, isChecked ->
            view.isEnabled = false
            option?.let {
                model.process(SendIntent.ModifyTxOption(it.copy(value = isChecked)))
            }
        }
    }
}