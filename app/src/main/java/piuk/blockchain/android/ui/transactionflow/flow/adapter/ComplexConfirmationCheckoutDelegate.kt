package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.TxConfirmation
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.databinding.ItemCheckoutComplexInfoBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.flow.ConfirmationPropertyKey
import piuk.blockchain.android.ui.transactionflow.flow.TxConfirmReadOnlyMapperNewCheckout

class ComplexConfirmationCheckoutDelegate(private val mapper: TxConfirmReadOnlyMapperNewCheckout) :
    AdapterDelegate<TxConfirmationValue> {
    override fun isForViewType(items: List<TxConfirmationValue>, position: Int): Boolean {
        return items[position].confirmation == TxConfirmation.COMPLEX_READ_ONLY
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ComplexConfirmationCheckoutItemItemViewHolder(
            ItemCheckoutComplexInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            mapper
        )

    override fun onBindViewHolder(
        items: List<TxConfirmationValue>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ComplexConfirmationCheckoutItemItemViewHolder).bind(
        items[position]
    )
}

private class ComplexConfirmationCheckoutItemItemViewHolder(
    val binding: ItemCheckoutComplexInfoBinding,
    private val mapper: TxConfirmReadOnlyMapperNewCheckout
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: TxConfirmationValue) {
        with(binding) {
            mapper.map(item)?.let {
                complexItemLabel.text = it[ConfirmationPropertyKey.LABEL] as String
                complexItemTitle.text = it[ConfirmationPropertyKey.TITLE] as String
                complexItemSubtitle.text = it[ConfirmationPropertyKey.SUBTITLE] as String

                it[ConfirmationPropertyKey.IS_IMPORTANT]?.let { isImportant ->
                    if (isImportant as Boolean) {
                        complexItemLabel.setTextAppearance(R.style.Text_Semibold_16)
                        complexItemTitle.setTextAppearance(R.style.Text_Semibold_16)
                    } else {
                        complexItemLabel.setTextAppearance(R.style.Text_Standard_14)
                        complexItemTitle.setTextAppearance(R.style.Text_Standard_14)
                    }
                }
            }
        }
    }
}