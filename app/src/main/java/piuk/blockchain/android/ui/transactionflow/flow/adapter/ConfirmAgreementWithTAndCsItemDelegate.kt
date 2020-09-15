package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.app.Activity
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.ui.urllinks.INTEREST_PRIVACY_POLICY
import com.blockchain.ui.urllinks.INTEREST_TERMS_OF_SERVICE
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_send_confirm_agreement_tcs.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.TxOption
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class ConfirmAgreementWithTAndCsItemDelegate<in T>(
    private val model: TransactionModel,
    private val stringUtils: StringUtils,
    private val activityContext: Activity
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean =
        (items[position] as? TxOptionValue)?.option == TxOption.AGREEMENT_INTEREST_T_AND_C

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AgreementItemViewHolder(
            parent.inflate(R.layout.item_send_confirm_agreement_tcs)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AgreementItemViewHolder).bind(
        items[position] as TxOptionValue.TxBooleanOption<Unit>,
        model,
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
        item: TxOptionValue.TxBooleanOption<Unit>,
        model: TransactionModel,
        stringUtils: StringUtils,
        activityContext: Activity
    ) {

        val linksMap = mapOf<String, Uri>(
            "interest_tos" to Uri.parse(INTEREST_TERMS_OF_SERVICE),
            "interest_pp" to Uri.parse(INTEREST_PRIVACY_POLICY)
        )

        itemView.confirm_details_checkbox_text.text = stringUtils.getStringWithMappedLinks(
            R.string.send_confirmation_interest_tos_pp,
            linksMap,
            activityContext
        )

        itemView.confirm_details_checkbox_text.movementMethod = LinkMovementMethod.getInstance()

        itemView.confirm_details_checkbox.isChecked = item.value

        itemView.confirm_details_checkbox.setOnCheckedChangeListener { view, isChecked ->
            model.process(TransactionIntent.ModifyTxOption(item.copy(value = isChecked)))
            view.isEnabled = false
        }
    }
}