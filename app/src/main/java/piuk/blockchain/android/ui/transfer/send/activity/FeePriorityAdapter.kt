package piuk.blockchain.android.ui.transfer.send.activity

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil

import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemFeePriorityDropdownBinding

class FeePriorityAdapter internal constructor(
    context: Context,
    private val feeOptions: List<DisplayFeeOptions>
) : ArrayAdapter<DisplayFeeOptions>(context, 0, feeOptions) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, parent, false)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, parent, true)
    }

    private fun getCustomView(position: Int, parent: ViewGroup, isDropdownView: Boolean): View {
        if (isDropdownView) {
            val binding = DataBindingUtil.inflate<ItemFeePriorityDropdownBinding>(
                LayoutInflater.from(context),
                R.layout.item_fee_priority_dropdown,
                parent,
                false
            )

            val option = feeOptions[position]
            binding.title.text = option.title
            binding.description.text = option.description
            return binding.root
        } else {
            return View(context)
        }
    }
}
