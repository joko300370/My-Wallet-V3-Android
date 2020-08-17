package piuk.blockchain.android.ui.transfer.send.flow.adapter

import android.text.InputFilter
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_send_confirm_note.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.activity.detail.adapter.INPUT_FIELD_FLAGS
import piuk.blockchain.android.ui.activity.detail.adapter.MAX_NOTE_LENGTH
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class ConfirmNoteItemDelegate<in T>(
    private val onNoteItemUpdated: (String) -> Unit
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position] as ConfirmItemType
        return item is ConfirmNoteItem
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        NoteItemViewHolder(
            parent.inflate(R.layout.item_send_confirm_note)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as NoteItemViewHolder).bind(
        items[position] as ConfirmNoteItem,
        onNoteItemUpdated
    )
}

private class NoteItemViewHolder(val parent: View) :
    RecyclerView.ViewHolder(parent),
    LayoutContainer {

    override val containerView: View?
        get() = itemView

    fun bind(
        item: ConfirmNoteItem,
        onNoteItemUpdated: (String) -> Unit
    ) {

        itemView.confirm_details_note_input.apply {
            inputType = INPUT_FIELD_FLAGS
            filters = arrayOf(InputFilter.LengthFilter(MAX_NOTE_LENGTH))

            setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE && v.text.isNotEmpty()) {

                    onNoteItemUpdated.invoke(v.text.toString())
                    clearFocus()
                }
                false
            }

            setText(item.hint, TextView.BufferType.EDITABLE)
        }
    }
}
