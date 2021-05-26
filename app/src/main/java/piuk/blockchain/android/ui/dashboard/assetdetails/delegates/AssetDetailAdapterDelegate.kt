package piuk.blockchain.android.ui.dashboard.assetdetails.delegates

import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.wallet.DefaultLabels
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsItem

class AssetDetailAdapterDelegate(
    private val onAccountSelected: (BlockchainAccount, AssetFilter) -> Unit,
    private val token: CryptoAsset,
    private val labels: DefaultLabels,
    private val onCardClicked: () -> Unit,
    private val onRecurringBuyClicked: (RecurringBuy) -> Unit,
    private val assetResources: AssetResources,
    private val assetDetailsDecorator: AssetDetailsInfoDecorator
) : DelegationAdapter<AssetDetailsItem>(AdapterDelegatesManager(), emptyList()) {
    init {
        with(delegatesManager) {
            addAdapterDelegate(AssetDetailsDelegate(onAccountSelected,
                compositeDisposable,
                assetDetailsDecorator,
                labels))
            addAdapterDelegate(RecurringBuyItemDelegate(onRecurringBuyClicked, assetResources))
            addAdapterDelegate(LabelItemDelegate(token))
            addAdapterDelegate(RecurringBuyInfoItemDelegate(onCardClicked))
        }
    }
}