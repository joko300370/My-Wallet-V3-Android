package piuk.blockchain.android.ui.addresses.adapter

import com.blockchain.featureflags.InternalFeatureFlagApi
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.android.util.autoNotify
import kotlin.properties.Delegates

sealed class AccountListItem {

    data class Account(
        val account: CryptoNonCustodialAccount
    ) : AccountListItem()

    data class InternalHeader(val enableCreate: Boolean) : AccountListItem()
    data class ImportedHeader(val enableImport: Boolean) : AccountListItem()
}

class AccountAdapter(
    listener: Listener,
    features: InternalFeatureFlagApi
) : DelegationAdapter<AccountListItem>(AdapterDelegatesManager(), emptyList()) {

    interface Listener {
        fun onCreateNewClicked()
        fun onImportAddressClicked()
        fun onAccountClicked(account: CryptoAccount)
    }

    init {
        delegatesManager.apply {
            addAdapterDelegate(InternalAccountsHeaderDelegate(listener, features))
            addAdapterDelegate(ImportedAccountsHeaderDelegate(listener))
            addAdapterDelegate(AccountDelegate(listener))
        }
    }

    /**
     * Observes the items list and automatically notifies the adapter of changes to the data based
     * on the comparison we make here, which is a simple equality check.
     */
    override var items: List<AccountListItem> by Delegates.observable(emptyList()) { _, oldList, newList ->
        autoNotify(oldList, newList) { _, _ -> false }
//        autoNotify(oldList, newList) { o, n -> o == n }
    }

    /**
     * Required so that [setHasStableIds] = true doesn't break the RecyclerView and show duplicated
     * layouts.
     */
    override fun getItemId(position: Int): Long = items[position].hashCode().toLong()
}
