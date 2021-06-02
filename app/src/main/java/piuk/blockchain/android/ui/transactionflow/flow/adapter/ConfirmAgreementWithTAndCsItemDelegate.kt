package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.app.Activity
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.ui.urllinks.INTEREST_PRIVACY_POLICY
import com.blockchain.ui.urllinks.INTEREST_TERMS_OF_SERVICE
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.TxConfirmation
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.databinding.ItemSendConfirmAgreementTcsBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.setThrottledCheckedChange

class ConfirmAgreementWithTAndCsItemDelegate<in T>(
    private val model: TransactionModel,
    private val stringUtils: StringUtils,
    private val activityContext: Activity
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean =
        (items[position] as? TxConfirmationValue)?.confirmation == TxConfirmation.AGREEMENT_INTEREST_T_AND_C

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AgreementItemViewHolder(
            ItemSendConfirmAgreementTcsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AgreementItemViewHolder).bind(
        items[position] as TxConfirmationValue.TxBooleanConfirmation<Unit>,
        model,
        stringUtils,
        activityContext
    )
}

private class AgreementItemViewHolder(private val binding: ItemSendConfirmAgreementTcsBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(
        item: TxConfirmationValue.TxBooleanConfirmation<Unit>,
        model: TransactionModel,
        stringUtils: StringUtils,
        activityContext: Activity
    ) {

        val linksMap = mapOf<String, Uri>(
            "interest_tos" to Uri.parse(INTEREST_TERMS_OF_SERVICE),
            "interest_pp" to Uri.parse(INTEREST_PRIVACY_POLICY)
        )

        binding.apply {
            confirmDetailsCheckboxText.text = stringUtils.getStringWithMappedAnnotations(
                R.string.send_confirmation_interest_tos_pp,
                linksMap,
                activityContext
            )

            confirmDetailsCheckboxText.movementMethod = LinkMovementMethod.getInstance()

            confirmDetailsCheckbox.isChecked = item.value

            confirmDetailsCheckbox.setThrottledCheckedChange { isChecked ->
                model.process(TransactionIntent.ModifyTxOption(item.copy(value = isChecked)))
            }
        }
    }
}