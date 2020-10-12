package piuk.blockchain.android.ui.backup.transfer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.item_address.view.*
import kotlinx.android.synthetic.main.spinner_item.view.*

import piuk.blockchain.android.R
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.account.formatDisplayBalance
import piuk.blockchain.androidcoreui.utils.extensions.gone

class AddressAdapter(
    context: Context,
    textViewResourceId: Int,
    accountList: List<ItemAccount>,
    private val showText: Boolean
) : ArrayAdapter<ItemAccount>(context, textViewResourceId, accountList) {

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = LayoutInflater.from(context).inflate(R.layout.spinner_item, parent)

        if (showText) {
            view.text.text = getItem(position)?.label ?: ""
        }
        return view
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_address, parent, false)

        getItem(position)?.let {
            if (it.tag.isEmpty()) {
                view.tvTag.gone()
            } else {
                view.tvTag.text = it.tag
            }
            view.tvLabel.text = it.label
            view.tvBalance.text = it.formatDisplayBalance()
        }

        return view
    }
}
