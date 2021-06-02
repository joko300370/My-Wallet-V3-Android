package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.text.InputFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.databinding.ItemSendConfirmNoteBinding
import piuk.blockchain.android.ui.activity.detail.adapter.INPUT_FIELD_FLAGS
import piuk.blockchain.android.ui.activity.detail.adapter.MAX_NOTE_LENGTH
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel

class ConfirmNoteItemDelegate<in T>(
    private val model: TransactionModel
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        return items[position] is TxConfirmationValue.Description
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        NoteItemViewHolder(
            ItemSendConfirmNoteBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as NoteItemViewHolder).bind(
        items[position] as TxConfirmationValue.Description,
        model
    )
}

private class NoteItemViewHolder(private val binding: ItemSendConfirmNoteBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: TxConfirmationValue.Description,
        model: TransactionModel
    ) {

        binding.confirmDetailsNoteInput.apply {
            inputType = INPUT_FIELD_FLAGS
            filters = arrayOf(InputFilter.LengthFilter(MAX_NOTE_LENGTH))

            setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE && v.text.isNotEmpty()) {
                    model.process(
                        TransactionIntent.ModifyTxOption(TxConfirmationValue.Description(text = v.text.toString()))
                    )
                    clearFocus()
                }
                false
            }

            setText(item.text, TextView.BufferType.EDITABLE)
        }
    }
}
