package piuk.blockchain.android.ui.interest

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import io.reactivex.disposables.CompositeDisposable
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter

class InterestDashboardAdapter(
    assetResources: AssetResources,
    disposables: CompositeDisposable,
    custodialWalletManager: CustodialWalletManager,
    verificationClicked: () -> Unit,
    itemClicked: (CryptoCurrency, Boolean) -> Unit
) :
    DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            addAdapterDelegate(
                InterestDashboardAssetItem(
                    assetResources, disposables, custodialWalletManager, itemClicked
                ))
            addAdapterDelegate(InterestDashboardVerificationItem(verificationClicked))
        }
    }
}

sealed class InterestDashboardItem

object InterestIdentityVerificationItem : InterestDashboardItem()
class InterestAssetInfoItem(
    val isKycGold: Boolean,
    val cryptoCurrency: CryptoCurrency
) : InterestDashboardItem()