package piuk.blockchain.android.ui.dashboard.assetdetails

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.databinding.DialogDashboardAssetLabelItemBinding
import piuk.blockchain.android.databinding.ViewAccountCryptoOverviewBinding
import piuk.blockchain.android.ui.customviews.account.CellDecorator
import piuk.blockchain.android.ui.customviews.account.addViewToBottomWithConstraints
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import kotlin.properties.Delegates

data class AssetDetailItem(
    val assetFilter: AssetFilter,
    val account: BlockchainAccount,
    val balance: Money,
    val fiatBalance: Money,
    val actions: Set<AssetAction>,
    val interestRate: Double
)

class AssetDetailViewHolder(private val binding: ViewAccountCryptoOverviewBinding, private val labels: DefaultLabels) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: AssetDetailItem,
        onAccountSelected: (BlockchainAccount, AssetFilter) -> Unit,
        disposable: CompositeDisposable,
        block: AssetDetailsDecorator
    ) {
        with(binding) {
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

class LabelViewHolder(private val binding: DialogDashboardAssetLabelItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(token: CryptoAsset) {
        binding.assetLabelDescription.text = when (token.asset) {
            CryptoCurrency.ALGO -> context.getString(R.string.algorand_asset_label)
            CryptoCurrency.DOT -> context.getString(R.string.polkadot_asset_label)
            else -> ""
        }
    }
}

// TODO convert this to adapter delegate and break out 4 types of viewholders
internal class AssetDetailAdapter(
    private val onAccountSelected: (BlockchainAccount, AssetFilter) -> Unit,
    private val showBanner: Boolean,
    private val token: CryptoAsset,
    private val assetResources: AssetResources,
    private val labels: DefaultLabels,
    private val assetDetailsDecorator: AssetDetailsDecorator
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val compositeDisposable = CompositeDisposable()

    var itemList: List<AssetDetailItem> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            notifyDataSetChanged()
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        compositeDisposable.clear()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_CRYPTO) {
            AssetDetailViewHolder(
                ViewAccountCryptoOverviewBinding.inflate(LayoutInflater.from(parent.context), parent, false), labels
            )
        } else {
            LabelViewHolder(
                DialogDashboardAssetLabelItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

    override fun getItemCount(): Int = if (showBanner) itemList.size + 1 else itemList.size

    override fun getItemViewType(position: Int): Int =
        if (showBanner) {
            if (position >= itemList.size) {
                TYPE_LABEL
            } else {
                TYPE_CRYPTO
            }
        } else {
            TYPE_CRYPTO
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AssetDetailViewHolder) {
            holder.bind(
                itemList[position],
                onAccountSelected,
                compositeDisposable,
                assetDetailsDecorator
            )
        } else {
            (holder as LabelViewHolder).bind(token)
        }
    }

    companion object {
        private const val TYPE_CRYPTO = 0
        private const val TYPE_LABEL = 1
    }
}

typealias AssetDetailsDecorator = (AssetDetailItem) -> CellDecorator