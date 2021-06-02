package piuk.blockchain.android.simplebuy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.CurrencySelectionItemBinding
import kotlin.properties.Delegates

class CurrenciesAdapter(
    private val showSectionDivider: Boolean = false,
    private val onChecked: (CurrencyItem) -> Unit
) : RecyclerView.Adapter<CurrenciesAdapter.CurrenciesViewHolder>() {

    var items: List<CurrencyItem> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            notifyDataSetChanged()
        }
    }

    var canSelect: Boolean by Delegates.observable(true) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            notifyDataSetChanged()
        }
    }

    class CurrenciesViewHolder(binding: CurrencySelectionItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val title: TextView = binding.name
        val symbol: TextView = binding.symbol
        val checkIcon: ImageView = binding.icCheck
        val rootView: ViewGroup = binding.rootView
        val cellDivider: View = binding.cellDivider
        val sectionDivider: View = binding.sectionSeparator
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrenciesViewHolder =
        CurrenciesViewHolder(
            CurrencySelectionItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CurrenciesViewHolder, position: Int) {

        with(holder) {
            val item = items[position]

            checkIcon.setImageResource(
                if (item.isChecked)
                    R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_unchecked
            )
            rootView.setOnClickListener {
                if (canSelect) {
                    items.forEachIndexed { index, item ->
                        item.isChecked = position == index
                    }
                    notifyDataSetChanged()
                    onChecked(item)
                }
            }
            title.text = item.name
            symbol.text = item.symbol

            when {
                position == items.filter { it.isAvailable }.size - 1 && showSectionDivider -> {
                    cellDivider.visibility = View.GONE
                    sectionDivider.visibility = View.VISIBLE
                }
                position != items.size -> {
                    cellDivider.visibility = View.VISIBLE
                    sectionDivider.visibility = View.GONE
                }
                else -> {
                    cellDivider.visibility = View.GONE
                    sectionDivider.visibility = View.GONE
                }
            }
        }
    }
}