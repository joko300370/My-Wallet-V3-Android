package piuk.blockchain.android.ui.accounts

import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter

class AccountsDelegateAdapter(
    onAccountClicked: (BlockchainAccount) -> Unit
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            addAdapterDelegate(
                CryptoAccountDelegate(
                    onAccountClicked
                )
            )
            addAdapterDelegate(
                FiatAccountDelegate(
                    onAccountClicked = onAccountClicked
                )
            )
            addAdapterDelegate(
                AllWalletsAccountDelegate(
                    onAccountClicked
                )
            )
        }
    }
}