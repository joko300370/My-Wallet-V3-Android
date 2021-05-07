package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.coincore.TxConfirmation
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.databinding.ItemCheckoutSimpleInfoBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.flow.ConfirmationPropertyKey
import piuk.blockchain.android.ui.transactionflow.flow.TxConfirmReadOnlyMapperNewCheckout

class SimpleConfirmationCheckoutDelegate(private val mapper: TxConfirmReadOnlyMapperNewCheckout) :
    AdapterDelegate<TxConfirmationValue> {
    override fun isForViewType(items: List<TxConfirmationValue>, position: Int): Boolean {
        return items[position].confirmation == TxConfirmation.SIMPLE_READ_ONLY
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        SimpleConfirmationCheckoutItemViewHolder(
            ItemCheckoutSimpleInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            mapper
        )

    override fun onBindViewHolder(
        items: List<TxConfirmationValue>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as SimpleConfirmationCheckoutItemViewHolder).bind(
        items[position]
    )
}

private class SimpleConfirmationCheckoutItemViewHolder(
    val binding: ItemCheckoutSimpleInfoBinding,
    val mapper: TxConfirmReadOnlyMapperNewCheckout
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: TxConfirmationValue) {
        mapper.map(item)?.let {
            with(binding) {
                simpleItemLabel.text = it[ConfirmationPropertyKey.LABEL] as String
                simpleItemTitle.text = it[ConfirmationPropertyKey.TITLE] as String
            }
        }
    }
}
