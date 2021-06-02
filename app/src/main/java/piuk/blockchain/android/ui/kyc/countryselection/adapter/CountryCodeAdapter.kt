package piuk.blockchain.android.ui.kyc.countryselection.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.databinding.ItemCountryInfoBinding
import piuk.blockchain.android.ui.kyc.countryselection.util.CountryDisplayModel
import piuk.blockchain.android.util.autoNotify
import kotlin.properties.Delegates

class CountryCodeAdapter(
    private val countrySelector: (CountryDisplayModel) -> Unit
) : RecyclerView.Adapter<CountryCodeAdapter.CountryCodeViewHolder>() {

    var items: List<CountryDisplayModel> by Delegates.observable(
        emptyList()
    ) { _, oldList, newList ->
        autoNotify(oldList, newList) { o, n -> o == n }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryCodeViewHolder =
        CountryCodeViewHolder(ItemCountryInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CountryCodeViewHolder, position: Int) {
        holder.bind(items[position], countrySelector)
    }

    class CountryCodeViewHolder(
        private val binding: ItemCountryInfoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val name = binding.textViewCountryName
        private val shortName = binding.textViewCountryShortName

        fun bind(
            country: CountryDisplayModel,
            countrySelector: (CountryDisplayModel) -> Unit
        ) {
            name.text = country.name
            shortName.text = country.shortName
            itemView.setOnClickListener { countrySelector(country) }
        }
    }
}