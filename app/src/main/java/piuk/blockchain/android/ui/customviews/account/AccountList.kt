package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.fiat.FiatCustodialAccount
import piuk.blockchain.android.coincore.fiat.LinkedBankAccount
import piuk.blockchain.android.coincore.impl.AllWalletsAccount
import piuk.blockchain.android.databinding.ItemAccountSelectBankBinding
import piuk.blockchain.android.databinding.ItemAccountSelectCryptoBinding
import piuk.blockchain.android.databinding.ItemAccountSelectFiatBinding
import piuk.blockchain.android.databinding.ItemAccountSelectGroupBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.customviews.IntroHeaderView
import piuk.blockchain.android.util.context

typealias StatusDecorator = (BlockchainAccount) -> CellDecorator

internal data class SelectableAccountItem(
    val account: BlockchainAccount,
    var isSelected: Boolean
)

class AccountList @JvmOverloads constructor(
    ctx: Context,
    val attr: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(ctx, attr, defStyle) {

    private val disposables = CompositeDisposable()
    private val uiScheduler = AndroidSchedulers.mainThread()
    private var lastSelectedAccount: BlockchainAccount? = null

    init {
        setBackgroundColor(Color.WHITE)
        setFadingEdgeLength(resources.getDimensionPixelSize(R.dimen.size_small))
        isVerticalFadingEdgeEnabled = true
        layoutManager = LinearLayoutManager(
            context,
            VERTICAL,
            false
        )
        addItemDecoration(
            BlockchainListDividerDecor(context)
        )
    }

    fun initialise(
        source: Single<List<BlockchainAccount>>,
        status: StatusDecorator = {
            DefaultCellDecorator()
        },
        introView: IntroHeaderView? = null,
        shouldShowSelectionStatus: Boolean = false
    ) {
        removeAllHeaderDecorations()

        introView?.let {
            addItemDecoration(
                HeaderDecoration.with(context)
                    .parallax(0.5f)
                    .setView(it)
                    .build()
            )
        }

        if (adapter == null) {
            adapter = AccountsDelegateAdapter(
                statusDecorator = status,
                onAccountClicked = { onAccountSelected(it) },
                showSelectionStatus = shouldShowSelectionStatus
            )
        }
        loadItems(source)
    }

    fun loadItems(source: Single<List<BlockchainAccount>>) {
        disposables += source
            .observeOn(uiScheduler)
            .doOnSubscribe {
                onListLoading()
            }
            .subscribeBy(
                onSuccess = {
                    (adapter as? AccountsDelegateAdapter)?.items = it.map { account ->
                        SelectableAccountItem(account, false)
                    }
                    onListLoaded(it.isEmpty())

                    lastSelectedAccount?.let {
                        updatedSelectedAccount(it)
                        lastSelectedAccount = null
                    }
                },
                onError = {
                    onLoadError(it)
                }
            )
    }

    fun updatedSelectedAccount(selectedAccount: BlockchainAccount) {
        (adapter as AccountsDelegateAdapter).items.let {
            if (it.isNotEmpty()) {
                (adapter as AccountsDelegateAdapter).items = it.map { sAccount ->
                    SelectableAccountItem(
                        sAccount.account,
                        selectedAccount == sAccount.account
                    )
                }
            } else {
                // if list is empty, we're in a race condition between loading and selecting, so store value and check
                // it once items loaded
                lastSelectedAccount = selectedAccount
            }
        }
    }

    fun clearSelectedAccount() {
        (adapter as AccountsDelegateAdapter).items =
            (adapter as AccountsDelegateAdapter).items.map {
                SelectableAccountItem(it.account, false)
            }
    }

    var onLoadError: (Throwable) -> Unit = {}
    var onAccountSelected: (BlockchainAccount) -> Unit = {}
    var onListLoaded: (isEmpty: Boolean) -> Unit = {}
    var onListLoading: () -> Unit = {}

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposables.clear()
    }
}

private class AccountsDelegateAdapter(
    statusDecorator: StatusDecorator,
    onAccountClicked: (BlockchainAccount) -> Unit,
    showSelectionStatus: Boolean
) : DelegationAdapter<SelectableAccountItem>(AdapterDelegatesManager(), emptyList()) {

    override var items: List<SelectableAccountItem> = emptyList()
        set(value) {
            val diffResult =
                DiffUtil.calculateDiff(AccountsDiffUtil(this.items, value))
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

    init {
        with(delegatesManager) {
            addAdapterDelegate(
                CryptoAccountDelegate(
                    statusDecorator,
                    onAccountClicked,
                    showSelectionStatus
                )
            )
            addAdapterDelegate(
                FiatAccountDelegate(
                    statusDecorator,
                    onAccountClicked,
                    showSelectionStatus
                )
            )
            addAdapterDelegate(
                AllWalletsAccountDelegate(
                    statusDecorator,
                    onAccountClicked,
                    compositeDisposable
                )
            )
            addAdapterDelegate(
                BankAccountDelegate(
                    onAccountClicked,
                    showSelectionStatus
                )
            )
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        (holder as? CryptoSingleAccountViewHolder)?.dispose()
    }
}

private class CryptoAccountDelegate(
    private val statusDecorator: StatusDecorator,
    private val onAccountClicked: (CryptoAccount) -> Unit,
    private val showSelectionStatus: Boolean
) : AdapterDelegate<SelectableAccountItem> {

    override fun isForViewType(items: List<SelectableAccountItem>, position: Int): Boolean =
        items[position].account is CryptoAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CryptoSingleAccountViewHolder(
            showSelectionStatus,
            ItemAccountSelectCryptoBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(
        items: List<SelectableAccountItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as CryptoSingleAccountViewHolder).bind(
        items[position],
        statusDecorator,
        onAccountClicked
    )
}

private class CryptoSingleAccountViewHolder(
    private val showSelectionStatus: Boolean,
    private val binding: ItemAccountSelectCryptoBinding
) : RecyclerView.ViewHolder(binding.root), DisposableViewHolder {

    fun bind(
        selectableAccountItem: SelectableAccountItem,
        statusDecorator: StatusDecorator,
        onAccountClicked: (CryptoAccount) -> Unit
    ) {
        with(binding) {
            if (showSelectionStatus) {
                if (selectableAccountItem.isSelected) {
                    cryptoAccountParent.background = ContextCompat.getDrawable(context, R.drawable.item_selected_bkgd)
                } else {
                    cryptoAccountParent.background = null
                }
            }
            cryptoAccount.updateAccount(
                selectableAccountItem.account as CryptoAccount, onAccountClicked,
                statusDecorator(selectableAccountItem.account)
            )
        }
    }

    override fun dispose() {
        binding.cryptoAccount.dispose()
    }
}

private class FiatAccountDelegate(
    private val statusDecorator: StatusDecorator,
    private val onAccountClicked: (FiatAccount) -> Unit,
    private val showSelectionStatus: Boolean
) : AdapterDelegate<SelectableAccountItem> {
    override fun isForViewType(items: List<SelectableAccountItem>, position: Int): Boolean =
        items[position].account is FiatCustodialAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FiatAccountViewHolder(
            showSelectionStatus,
            ItemAccountSelectFiatBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(items: List<SelectableAccountItem>, position: Int, holder: RecyclerView.ViewHolder) =
        (holder as FiatAccountViewHolder).bind(
            items[position],
            statusDecorator,
            onAccountClicked
        )
}

private class FiatAccountViewHolder(
    private val showSelectionStatus: Boolean,
    private val binding: ItemAccountSelectFiatBinding
) : RecyclerView.ViewHolder(binding.root), DisposableViewHolder {

    fun bind(
        selectableAccountItem: SelectableAccountItem,
        statusDecorator: StatusDecorator,
        onAccountClicked: (FiatAccount) -> Unit
    ) {
        with(binding) {
            if (showSelectionStatus) {
                if (selectableAccountItem.isSelected) {
                    fiatContainer.background = ContextCompat.getDrawable(context, R.drawable.item_selected_bkgd)
                } else {
                    fiatContainer.background = null
                }
            }
            fiatContainer.alpha = 1f
            fiatAccount.updateAccount(
                selectableAccountItem.account as FiatAccount,
                statusDecorator(selectableAccountItem.account),
                onAccountClicked
            )
        }
    }

    override fun dispose() {
        binding.fiatAccount.dispose()
    }
}

private class BankAccountDelegate(
    private val onAccountClicked: (LinkedBankAccount) -> Unit,
    private val showSelectionStatus: Boolean
) : AdapterDelegate<SelectableAccountItem> {

    override fun isForViewType(items: List<SelectableAccountItem>, position: Int): Boolean =
        items[position].account is LinkedBankAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        BankAccountViewHolder(
            showSelectionStatus,
            ItemAccountSelectBankBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(
        items: List<SelectableAccountItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as BankAccountViewHolder).bind(
        items[position],
        onAccountClicked
    )
}

private class BankAccountViewHolder(
    private val showSelectionStatus: Boolean,
    private val binding: ItemAccountSelectBankBinding
) : RecyclerView.ViewHolder(binding.root), DisposableViewHolder {

    fun bind(
        selectableAccountItem: SelectableAccountItem,
        onAccountClicked: (LinkedBankAccount) -> Unit
    ) {
        with(binding) {
            if (showSelectionStatus) {
                if (selectableAccountItem.isSelected) {
                    bankContainer.background = ContextCompat.getDrawable(context, R.drawable.item_selected_bkgd)
                } else {
                    bankContainer.background = null
                }
            }
            bankContainer.alpha = 1f
            bankAccount.updateAccount(
                account = selectableAccountItem.account as LinkedBankAccount,
                onAccountClicked = onAccountClicked
            )
        }
    }

    override fun dispose() {
        // nothing to dispose
    }
}

private class AllWalletsAccountDelegate(
    private val statusDecorator: StatusDecorator,
    private val onAccountClicked: (BlockchainAccount) -> Unit,
    private val compositeDisposable: CompositeDisposable
) : AdapterDelegate<SelectableAccountItem> {

    override fun isForViewType(items: List<SelectableAccountItem>, position: Int): Boolean =
        items[position].account is AllWalletsAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AllWalletsAccountViewHolder(
            compositeDisposable,
            ItemAccountSelectGroupBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(
        items: List<SelectableAccountItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AllWalletsAccountViewHolder).bind(
        items[position],
        statusDecorator,
        onAccountClicked
    )
}

private class AllWalletsAccountViewHolder(
    private val compositeDisposable: CompositeDisposable,
    private val binding: ItemAccountSelectGroupBinding
) : RecyclerView.ViewHolder(binding.root), DisposableViewHolder {

    fun bind(
        selectableAccountItem: SelectableAccountItem,
        statusDecorator: StatusDecorator,
        onAccountClicked: (BlockchainAccount) -> Unit
    ) {
        with(binding) {

            accountGroup.updateAccount(selectableAccountItem.account as AllWalletsAccount)
            accountGroup.alpha = 1f

            compositeDisposable += statusDecorator(selectableAccountItem.account).isEnabled()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { root.setOnClickListener { } }
                .subscribeBy(
                    onSuccess = { isEnabled ->
                        if (isEnabled) {
                            root.setOnClickListener { onAccountClicked(selectableAccountItem.account) }
                            accountGroup.alpha = 1f
                        } else {
                            accountGroup.alpha = .6f
                            root.setOnClickListener { }
                        }
                    }
                )
        }
    }

    override fun dispose() {
        binding.accountGroup.dispose()
    }
}

interface DisposableViewHolder {
    fun dispose()
}