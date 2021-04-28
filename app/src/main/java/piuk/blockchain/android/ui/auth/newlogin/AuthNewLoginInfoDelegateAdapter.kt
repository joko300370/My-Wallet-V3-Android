package piuk.blockchain.android.ui.auth.newlogin

import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter

class AuthNewLoginInfoDelegateAdapter :
    DelegationAdapter<AuthNewLoginDetailsType>(AdapterDelegatesManager(), emptyList()) {

    init {
        with(delegatesManager) {
            addAdapterDelegate(NewLoginAuthInfoItemDelegate())
        }
    }
}