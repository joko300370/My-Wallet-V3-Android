package piuk.blockchain.android.ui.dashboard.assetdetails

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.koin.payloadScope
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.service.TierService
import info.blockchain.balance.CryptoCurrency
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.item_asset_action.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.InterestAccount
import piuk.blockchain.android.databinding.DialogAssetActionsSheetBinding
import piuk.blockchain.android.ui.base.mvi.MviBottomSheet
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.account.CellDecorator
import piuk.blockchain.android.ui.customviews.account.DefaultCellDecorator
import piuk.blockchain.android.ui.customviews.account.PendingBalanceAccountDecorator
import piuk.blockchain.android.ui.customviews.account.StatusDecorator
import piuk.blockchain.android.ui.customviews.account.addViewToBottomWithConstraints
import piuk.blockchain.android.ui.customviews.account.removePossibleBottomView
import piuk.blockchain.android.util.inflate
import piuk.blockchain.android.util.setAssetIconColours
import timber.log.Timber

class AssetActionsSheet :
    MviBottomSheet<AssetDetailsModel, AssetDetailsIntent, AssetDetailsState, DialogAssetActionsSheetBinding>() {
    private val disposables = CompositeDisposable()

    private val kycTierService: TierService by scopedInject()

    override val model: AssetDetailsModel by scopedInject()

    private val itemAdapter: AssetActionAdapter by lazy {
        AssetActionAdapter(
            disposable = disposables,
            statusDecorator = ::statusDecorator
        )
    }

    override fun render(newState: AssetDetailsState) {
        if (this.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {

            newState.selectedAccount?.let {
                showAssetBalances(newState)
                itemAdapter.itemList = mapActions(it, newState.actions)
            }

            if (newState.errorState != AssetDetailsError.NONE) {
                showError(newState.errorState)
            }
        }
    }

    override fun initControls(binding: DialogAssetActionsSheetBinding) {
        binding.assetActionsList.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            addItemDecoration(BlockchainListDividerDecor(requireContext()))
            adapter = itemAdapter
        }

        binding.assetActionsBack.setOnClickListener {
            model.process(ClearActionStates)
            model.process(ReturnToPreviousStep)
            dispose()
        }
    }

    override fun dismiss() {
        super.dismiss()
        model.process(ClearSheetDataIntent)
        dispose()
    }

    private fun statusDecorator(account: BlockchainAccount): CellDecorator =
        if (account is CryptoAccount) {
            AssetActionsDecorator(account)
        } else {
            DefaultCellDecorator()
        }

    private fun showError(error: AssetDetailsError) =
        when (error) {
            AssetDetailsError.TX_IN_FLIGHT -> ToastCustom.makeText(
                requireContext(),
                getString(R.string.dashboard_asset_actions_tx_in_progress), Toast.LENGTH_SHORT,
                ToastCustom.TYPE_ERROR
            )
            else -> {
                // do nothing
            }
        }

    private fun showAssetBalances(state: AssetDetailsState) {
        binding.assetActionsAccountDetails.updateAccount(
            state.selectedAccount.selectFirstAccount(),
            {},
            PendingBalanceAccountDecorator(state.selectedAccount.selectFirstAccount())
        )
    }

    private fun mapActions(
        account: BlockchainAccount,
        actions: AvailableActions
    ): List<AssetActionItem> {
        val firstAccount = account.selectFirstAccount()
        return actions.filter { it!= AssetAction.InterestDeposit || firstAccount is InterestAccount }.map {
            mapAction(it, firstAccount.asset, firstAccount)
        }
    }

    private fun mapAction(
        action: AssetAction,
        asset: CryptoCurrency,
        account: BlockchainAccount
    ): AssetActionItem =
        when (action) {
            // using the secondary ctor ensures the action is always enabled if it is present
            AssetAction.ViewActivity ->
                AssetActionItem(
                    getString(R.string.activities_title),
                    R.drawable.ic_tx_activity_clock,
                    getString(R.string.fiat_funds_detail_activity_details), asset,
                    action
                ) {
                    processAction(AssetAction.ViewActivity)
                }
            AssetAction.Send ->
                AssetActionItem(
                    account, getString(R.string.common_send), R.drawable.ic_tx_sent,
                    getString(
                        R.string.dashboard_asset_actions_send_dsc,
                        asset.displayTicker
                    ), asset, action
                ) {
                    processAction(AssetAction.Send)
                }
            AssetAction.Receive ->
                AssetActionItem(
                    getString(R.string.common_receive), R.drawable.ic_tx_receive,
                    getString(
                        R.string.dashboard_asset_actions_receive_dsc,
                        asset.displayTicker
                    ), asset, action
                ) {
                    processAction(AssetAction.Receive)
                }
            AssetAction.Swap -> AssetActionItem(
                account, getString(R.string.common_swap),
                R.drawable.ic_tx_swap,
                getString(R.string.dashboard_asset_actions_swap_dsc, asset.displayTicker),
                asset, action
            ) {
                processAction(AssetAction.Swap)
            }
            AssetAction.Summary -> AssetActionItem(
                getString(R.string.dashboard_asset_actions_summary_title),
                R.drawable.ic_tx_interest,
                getString(R.string.dashboard_asset_actions_summary_dsc, asset.displayTicker),
                asset, action
            ) {
                goToSummary()
            }

            AssetAction.InterestDeposit -> AssetActionItem(
                getString(R.string.common_transfer),
                R.drawable.ic_tx_deposit_arrow,
                getString(R.string.dashboard_asset_actions_deposit_dsc, asset.displayTicker),
                asset, action
            ) {
                goToInterestDeposit()
            }
            AssetAction.Sell -> AssetActionItem(
                getString(R.string.common_sell),
                R.drawable.ic_tx_sell,
                getString(R.string.convert_your_crypto_to_cash),
                asset, action
            ) {
                processAction(AssetAction.Sell)
            }
            AssetAction.Withdraw -> throw IllegalStateException("Cannot Withdraw a non-fiat currency")
            AssetAction.FiatDeposit -> throw IllegalStateException("Cannot Deposit a non-fiat currency to Fiat")
        }

    private fun goToInterestDeposit() {
        model.process(HandleActionIntent(AssetAction.InterestDeposit))
    }

    private fun goToSummary() {
        checkForKycStatus {
            processAction(AssetAction.Summary)
        }
    }

    private fun checkForKycStatus(action: () -> Unit) {
        disposables += kycTierService.tiers().subscribeBy(
            onSuccess = { tiers ->
                if (tiers.isApprovedFor(KycTierLevel.GOLD)) {
                    action()
                } else {
                    model.process(ShowInterestDashboard)
                    dismiss()
                }
            },
            onError = {
                Timber.e("Error getting tiers in asset actions sheet $it")
            }
        )
    }

    private fun processAction(action: AssetAction) {
        model.process(HandleActionIntent(action))
        dispose()
    }

    companion object {
        fun newInstance(): AssetActionsSheet = AssetActionsSheet()
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogAssetActionsSheetBinding =
        DialogAssetActionsSheetBinding.inflate(inflater, container, false)
}

private class AssetActionAdapter(
    val disposable: CompositeDisposable,
    val statusDecorator: StatusDecorator
) : RecyclerView.Adapter<AssetActionAdapter.ActionItemViewHolder>() {
    var itemList: List<AssetActionItem> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val compositeDisposable = CompositeDisposable()

    private val assetResources: AssetResources = payloadScope.get()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionItemViewHolder =
        ActionItemViewHolder(compositeDisposable, parent.inflate(R.layout.item_asset_action))

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: ActionItemViewHolder, position: Int) =
        holder.bind(itemList[position], statusDecorator, assetResources)

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        compositeDisposable.clear()
    }

    private class ActionItemViewHolder(private val compositeDisposable: CompositeDisposable, private val view: View) :
        RecyclerView.ViewHolder(view) {
        fun bind(
            item: AssetActionItem,
            statusDecorator: StatusDecorator,
            assetResources: AssetResources
        ) {
            addDecorator(item, statusDecorator)

            view.apply {
                item_action_icon.setImageResource(item.icon)
                item_action_icon.setAssetIconColours(
                    tintColor = assetResources.assetTint(item.asset),
                    filterColor = assetResources.assetFilter(item.asset)
                )
                item_action_title.text = item.title
                item_action_label.text = item.description
            }
        }

        private fun addDecorator(
            item: AssetActionItem,
            statusDecorator: StatusDecorator
        ) {
            item.account?.let { account ->
                compositeDisposable += statusDecorator(account).isEnabled()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onSuccess = { enabled ->
                            if (enabled) {
                                view.item_action_holder.alpha = 1f
                                view.item_action_holder.setOnClickListener {
                                    item.actionCta()
                                }
                            } else {
                                view.item_action_holder.alpha = .6f
                                view.item_action_holder.setOnClickListener {}
                            }
                        },
                        onError = {
                            Timber.e("Error getting decorator info $it")
                        }
                    )

                view.item_action_holder.removePossibleBottomView()
                compositeDisposable += statusDecorator(account).view(view.context)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        view.item_action_holder.addViewToBottomWithConstraints(
                            it,
                            bottomOfView = view.item_action_label,
                            startOfView = view.item_action_icon,
                            endOfView = null
                        )
                    }
            } ?: view.item_action_holder.setOnClickListener {
                item.actionCta()
            }
        }
    }
}

private data class AssetActionItem(
    val account: BlockchainAccount?,
    val title: String,
    val icon: Int,
    val description: String,
    val asset: CryptoCurrency,
    val action: AssetAction,
    val actionCta: () -> Unit
) {
    constructor(
        title: String,
        icon: Int,
        description: String,
        asset: CryptoCurrency,
        action: AssetAction,
        actionCta: () -> Unit
    ) : this(
        null,
        title,
        icon,
        description,
        asset,
        action,
        actionCta
    )
}