package piuk.blockchain.android.ui.linkbank.yapily.adapters

import com.blockchain.nabu.models.data.YapilyInstitution
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter

class YapilyBanksDelegateAdapter(
    private val onBankItemClicked: (YapilyInstitution) -> Unit,
    private val onAddNewBankClicked: () -> Unit
) : DelegationAdapter<YapilyInstitution>(AdapterDelegatesManager(), emptyList()) {

    override var items: List<YapilyInstitution> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        with(delegatesManager) {
            addAdapterDelegate(BankInfoItemDelegate(onBankItemClicked))
            addAdapterDelegate(BankInfoEmptyDelegate(onAddNewBankClicked))
        }
    }

    // this is needed so that an empty VH displays when there are no items in the list
    override fun getItemCount(): Int = if (items.isEmpty()) 1 else items.size
}