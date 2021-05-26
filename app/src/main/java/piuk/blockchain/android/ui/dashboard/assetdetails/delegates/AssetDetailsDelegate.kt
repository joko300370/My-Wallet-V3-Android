package piuk.blockchain.android.ui.dashboard.assetdetails.delegates

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.databinding.ViewAccountCryptoOverviewBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.customviews.account.CellDecorator
import piuk.blockchain.android.ui.customviews.account.addViewToBottomWithConstraints
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsItem
import piuk.blockchain.android.ui.dashboard.assetdetails.selectFirstAccount
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf

class AssetDetailsDelegate(
    private val onAccountSelected: (BlockchainAccount, AssetFilter) -> Unit,
    private val disposable: CompositeDisposable,
    private val block: AssetDetailsInfoDecorator,
    private val labels: DefaultLabels
) : AdapterDelegate<AssetDetailsItem> {
    override fun isForViewType(items: List<AssetDetailsItem>, position: Int): Boolean =
        items[position] is AssetDetailsItem.CryptoDetailsInfo

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AssetWalletViewHolder(
            ViewAccountCryptoOverviewBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onAccountSelected,
            disposable,
            block,
            labels
        )

    override fun onBindViewHolder(
        items: List<AssetDetailsItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AssetWalletViewHolder).bind(
        items[position] as AssetDetailsItem.CryptoDetailsInfo,
        items.indexOfFirst { it is AssetDetailsItem.CryptoDetailsInfo } == position
    )
}

private class AssetWalletViewHolder(
    private val binding: ViewAccountCryptoOverviewBinding,
    private val onAccountSelected: (BlockchainAccount, AssetFilter) -> Unit,
    private val disposable: CompositeDisposable,
    private val block: AssetDetailsInfoDecorator,
    private val labels: DefaultLabels
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: AssetDetailsItem.CryptoDetailsInfo, isFirstItemOfCategory: Boolean) {
        with(binding) {
            walletHeaderGroup.visibleIf { isFirstItemOfCategory }

            val asset = getAsset(item.account, item.balance.currencyCode)

            assetSubtitle.text = when (item.assetFilter) {
                AssetFilter.NonCustodial,
                AssetFilter.Custodial -> labels.getAssetMasterWalletLabel(asset)
                AssetFilter.Interest -> context.resources.getString(
                    R.string.dashboard_asset_balance_interest, item.interestRate
                )
                else -> throw IllegalArgumentException("Not supported filter")
            }

            walletName.text = when (item.assetFilter) {
                AssetFilter.NonCustodial -> labels.getDefaultNonCustodialWalletLabel(asset)
                AssetFilter.Custodial -> labels.getDefaultCustodialWalletLabel(asset)
                AssetFilter.Interest -> labels.getDefaultInterestWalletLabel(asset)
                else -> throw IllegalArgumentException("Not supported filter")
            }

            root.setOnClickListener {
                onAccountSelected(item.account, item.assetFilter)
            }

            when (item.assetFilter) {
                AssetFilter.NonCustodial,
                AssetFilter.Interest,
                AssetFilter.Custodial -> {
                    assetWithAccount.visible()
                    assetWithAccount.updateIcon(
                        when (item.account) {
                            is CryptoAccount -> item.account
                            is AccountGroup -> item.account.selectFirstAccount()
                            else -> throw IllegalStateException(
                                "Unsupported account type for asset details ${item.account}"
                            )
                        }
                    )
                }
                AssetFilter.All -> assetWithAccount.gone()
            }

            walletBalanceFiat.text = item.balance.toStringWithSymbol()
            walletBalanceCrypto.text = item.fiatBalance.toStringWithSymbol()
            disposable += block(item).view(root.context)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    container.addViewToBottomWithConstraints(
                        view = it,
                        bottomOfView = assetSubtitle,
                        startOfView = assetSubtitle,
                        endOfView = walletBalanceCrypto
                    )
                }
        }
    }

    private fun getAsset(account: BlockchainAccount, currency: String): CryptoCurrency =
        when (account) {
            is CryptoAccount -> account.asset
            is AccountGroup -> account.accounts.filterIsInstance<CryptoAccount>()
                .firstOrNull()?.asset ?: throw IllegalStateException(
                "No crypto accounts found in ${this::class.java} with currency $currency "
            )
            else -> null
        } ?: throw IllegalStateException("Unsupported account type ${this::class.java}")
}

typealias AssetDetailsInfoDecorator = (AssetDetailsItem.CryptoDetailsInfo) -> CellDecorator