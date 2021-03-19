package piuk.blockchain.android.ui.customviews

import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter

class BenefitsDelegateAdapter : DelegationAdapter<VerifyIdentityItem>(AdapterDelegatesManager(), emptyList()) {
    init {
        with(delegatesManager) {
            addAdapterDelegate(
                NumericBenefitsAdapter()
            )
            addAdapterDelegate(
                IconedBenefitsAdapter()
            )
        }
    }
}