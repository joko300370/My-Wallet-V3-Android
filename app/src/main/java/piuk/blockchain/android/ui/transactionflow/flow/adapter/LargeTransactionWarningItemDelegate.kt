package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.coincore.TxConfirmation
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.databinding.ItemSendLargeTxConfirmItemBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel

class LargeTransactionWarningItemDelegate<in T>(
    private val model: TransactionModel
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean =
        (items[position] as? TxConfirmationValue)?.confirmation == TxConfirmation.LARGE_TRANSACTION_WARNING

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        LargeTransactionViewHolder(
            ItemSendLargeTxConfirmItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as LargeTransactionViewHolder).bind(
        items[position] as TxConfirmationValue.TxBooleanConfirmation<Unit>,
        model
    )
}

private class LargeTransactionViewHolder(private val binding: ItemSendLargeTxConfirmItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: TxConfirmationValue.TxBooleanConfirmation<Unit>,
        model: TransactionModel
    ) {
        with(binding.confirmCheckbox) {
            isChecked = item.value
            setOnCheckedChangeListener { view, isChecked ->
                model.process(TransactionIntent.ModifyTxOption(item.copy(value = isChecked)))
                view.isEnabled = false
            }
        }
    }
}