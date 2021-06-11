package piuk.blockchain.android.cards

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.databinding.PickerItemBinding
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible

class PickerItemsAdapter(private val block: (PickerItem) -> Unit) :
    RecyclerView.Adapter<PickerItemsAdapter.ViewHolder>() {

    var items: List<PickerItem> = emptyList()
        set(items) {
            field = items
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder = ViewHolder(PickerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(private val binding: PickerItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pickerItem: PickerItem) {
            with(binding) {
                itemTitle.text = pickerItem.label
                pickerItem.icon?.let {
                    itemIcon.text = it
                    itemIcon.visible()
                } ?: itemIcon.gone()

                rootView.setOnClickListener {
                    block(pickerItem)
                }
            }
        }
    }
}