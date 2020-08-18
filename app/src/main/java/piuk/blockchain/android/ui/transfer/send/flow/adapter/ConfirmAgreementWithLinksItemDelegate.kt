package piuk.blockchain.android.ui.transfer.send.flow.adapter

import android.app.Activity
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_send_confirm_agreement.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class ConfirmAgreementWithLinksItemDelegate<in T>(
    private val onAgreementActionClicked: (Boolean) -> Unit,
    private val stringUtils: StringUtils,
    private val activityContext: Activity
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position] as ConfirmItemType
        return item is ConfirmAgreementWithLinksItem
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AgreementItemViewHolder(
            parent.inflate(R.layout.item_send_confirm_agreement)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AgreementItemViewHolder).bind(
        items[position] as ConfirmAgreementWithLinksItem,
        onAgreementActionClicked,
        stringUtils,
        activityContext
    )
}

private class AgreementItemViewHolder(val parent: View) :
    RecyclerView.ViewHolder(parent),
    LayoutContainer {

    override val containerView: View?
        get() = itemView

    fun bind(
        item: ConfirmAgreementWithLinksItem,
        onAgreementActionClicked: (Boolean) -> Unit,
        stringUtils: StringUtils,
        activityContext: Activity
    ) {

        itemView.confirm_details_checkbox.text = stringUtils.getStringWithMappedLinks(
            item.mappedString,
            item.uriMap,
            activityContext
        )

        itemView.confirm_details_checkbox.movementMethod = LinkMovementMethod.getInstance()
        itemView.confirm_details_checkbox.setOnCheckedChangeListener { _, isChecked ->
            onAgreementActionClicked.invoke(isChecked)
        }
    }
}