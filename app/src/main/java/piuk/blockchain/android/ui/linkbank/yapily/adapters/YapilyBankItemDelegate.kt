package piuk.blockchain.android.ui.linkbank.yapily.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.models.data.YapilyInstitution
import com.bumptech.glide.Glide
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemBankDetailsBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate

class BankInfoItemDelegate(
    private val onBankItemClicked: (YapilyInstitution) -> Unit
) : AdapterDelegate<YapilyInstitution> {
    override fun isForViewType(items: List<YapilyInstitution>, position: Int): Boolean =
        items.isNotEmpty()

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        YapilyBankViewHolder(
            ItemBankDetailsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(
        items: List<YapilyInstitution>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as YapilyBankViewHolder).bind(items[position], onBankItemClicked)
}

class YapilyBankViewHolder(
    val binding: ItemBankDetailsBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: YapilyInstitution, onBankItemClicked: (YapilyInstitution) -> Unit) {
        with(binding) {
            bankDetailsRoot.setOnClickListener { onBankItemClicked.invoke(item) }
            bankName.text = item.name

            Glide.with(bankIcon)
                .load(item.iconLink.toString())
                .centerCrop()
                .placeholder(R.drawable.ic_bank_transfer)
                .into(bankIcon)
        }
    }
}