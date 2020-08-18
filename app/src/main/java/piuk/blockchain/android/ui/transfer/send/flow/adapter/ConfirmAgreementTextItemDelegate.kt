package piuk.blockchain.android.ui.transfer.send.flow.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_send_confirm_agreement.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class ConfirmAgreementTextItemDelegate<in T>(
    private val onAgreementActionClicked: (Boolean) -> Unit
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position] as ConfirmItemType
        return item is ConfirmAgreementTextItem
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AgreementTextItemViewHolder(
            parent.inflate(R.layout.item_send_confirm_agreement)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AgreementTextItemViewHolder).bind(
        items[position] as ConfirmAgreementTextItem,
        onAgreementActionClicked
    )
}

private class AgreementTextItemViewHolder(val parent: View) :
    RecyclerView.ViewHolder(parent),
    LayoutContainer {

    override val containerView: View?
        get() = itemView

    fun bind(
        item: ConfirmAgreementTextItem,
        onAgreementActionClicked: (Boolean) -> Unit
    ) {
        itemView.confirm_details_checkbox.setText(item.agreementText, TextView.BufferType.SPANNABLE)

        itemView.confirm_details_checkbox.setOnCheckedChangeListener { _, isChecked ->
            onAgreementActionClicked.invoke(isChecked)
        }
    }
}