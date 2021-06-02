package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.coincore.TxConfirmation
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.databinding.ItemSendConfirmDetailsBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.flow.TxConfirmReadOnlyMapper

class ConfirmInfoItemDelegate<in T>(private val mapper: TxConfirmReadOnlyMapper) :
    AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        return (items[position] as? TxConfirmationValue)?.confirmation == TxConfirmation.READ_ONLY
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        InfoItemViewHolder(
            ItemSendConfirmDetailsBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            mapper
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as InfoItemViewHolder).bind(
        items[position] as TxConfirmationValue
    )
}

class InfoItemViewHolder(
    private val binding: ItemSendConfirmDetailsBinding,
    private val mapper: TxConfirmReadOnlyMapper
) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: TxConfirmationValue) {
        mapper.map(item).let { (title, value) ->
            with(binding) {
                confirmationItemLabel.text = title
                confirmationItemValue.text = value
            }
        }
    }
}
