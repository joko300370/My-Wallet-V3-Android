package piuk.blockchain.android.ui.activity.detail.adapter

import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.databinding.ItemActivityDetailDescriptionBinding
import piuk.blockchain.android.ui.activity.detail.ActivityDetailsType
import piuk.blockchain.android.ui.activity.detail.Description
import piuk.blockchain.android.ui.adapters.AdapterDelegate

const val MAX_NOTE_LENGTH = 255

const val INPUT_FIELD_FLAGS: Int = (
    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
        InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE or
        InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE
    )

class ActivityDetailDescriptionItemDelegate<in T>(
    private val onDescriptionItemUpdated: (String) -> Unit
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position] as ActivityDetailsType
        return item is Description
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        DescriptionItemViewHolder(
            ItemActivityDetailDescriptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as DescriptionItemViewHolder).bind(
        items[position] as Description,
        onDescriptionItemUpdated
    )
}

private class DescriptionItemViewHolder(private val binding: ItemActivityDetailDescriptionBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: Description, onDescriptionUpdated: (String) -> Unit) {
        binding.itemActivityDetailDescription.apply {
            item.description?.let {
                setText(it, TextView.BufferType.EDITABLE)
                setSelection(item.description.length)
            }
            inputType =
                INPUT_FIELD_FLAGS
            filters = arrayOf(InputFilter.LengthFilter(MAX_NOTE_LENGTH))

            setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE && v.text.isNotEmpty()) {
                    onDescriptionUpdated(v.text.toString())
                    clearFocus()
                }

                false
            }
        }
    }
}