package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.item_account_select_crypto.view.*
import kotlinx.android.synthetic.main.item_account_select_fiat.view.*
import kotlinx.android.synthetic.main.item_account_select_group.view.*
import kotlinx.android.synthetic.main.view_account_list.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.impl.AllWalletsAccount
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.inflate

typealias StatusDecorator = (BlockchainAccount) -> Single<String>

class AccountList @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(ctx, attr, defStyle) {

    private val disposables = CompositeDisposable()
    private val uiScheduler = AndroidSchedulers.mainThread()

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.view_account_list, this, true)
    }

    fun initialise(source: Single<List<BlockchainAccount>>, status: StatusDecorator? = null) {

        val itemList = mutableListOf<BlockchainAccount>()

        with(list) {
            addItemDecoration(
                DividerItemDecoration(
                    context,
                    DividerItemDecoration.VERTICAL
                )
            )

            layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.VERTICAL,
                false
            )

            val theAdapter = AccountsDelegateAdapter(
                statusDecorator = status,
                onAccountClicked = { onAccountSelected(it) }
            )
            adapter = theAdapter
            theAdapter.items = itemList
        }

        disposables += source
            .observeOn(uiScheduler)
            .subscribeBy(
                onSuccess = {
                    itemList.clear()
                    itemList.addAll(it)
                    list.adapter?.notifyDataSetChanged()

                    if (it.isEmpty()) {
                        onEmptyList()
                    }
                },
                onError = {
                    onLoadError(it)
                }
            )
    }

    var onLoadError: (Throwable) -> Unit = {}
    var onAccountSelected: (BlockchainAccount) -> Unit = {}
    var onEmptyList: () -> Unit = {}
}

private class AccountsDelegateAdapter(
    statusDecorator: StatusDecorator?,
    onAccountClicked: (BlockchainAccount) -> Unit
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    init {
        with(delegatesManager) {
            addAdapterDelegate(
                CryptoAccountDelegate(
                    statusDecorator,
                    onAccountClicked
                )
            )
            addAdapterDelegate(
                FiatAccountDelegate(
                    statusDecorator,
                    onAccountClicked
                )
            )
            addAdapterDelegate(
                AllWalletsAccountDelegate(
                    statusDecorator,
                    onAccountClicked
                )
            )
        }
    }
}

private class CryptoAccountDelegate<in T>(
    private val statusDecorator: StatusDecorator?,
    private val onAccountClicked: (CryptoAccount) -> Unit
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is CryptoAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CryptoSingleAccountViewHolder(parent.inflate(R.layout.item_account_select_crypto))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as CryptoSingleAccountViewHolder).bind(
        items[position] as CryptoAccount,
        statusDecorator,
        onAccountClicked
    )
}

private class CryptoSingleAccountViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    private val disposables = CompositeDisposable()

    internal fun bind(
        account: CryptoAccount,
        statusDecorator: StatusDecorator?,
        onAccountClicked: (CryptoAccount) -> Unit
    ) {
        with(itemView) {
            disposables.clear()

            crypto_account.updateAccount(account, disposables)

            setOnClickListener { onAccountClicked(account) }

            itemView.crypto_status.gone()

            statusDecorator?.let { decorator ->
                disposables += decorator(account).subscribeBy(
                        onSuccess = { status ->
                            itemView.crypto_status.status = status
                            itemView.crypto_status.goneIf(status.isBlank())
                        }
                    )
            }
        }
    }
}

private class FiatAccountDelegate<in T>(
    private val statusDecorator: StatusDecorator?,
    private val onAccountClicked: (FiatAccount) -> Unit
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is FiatAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FiatAccountViewHolder(
            parent.inflate(R.layout.item_account_select_fiat)
        )

    override fun onBindViewHolder(items: List<T>, position: Int, holder: RecyclerView.ViewHolder) =
        (holder as FiatAccountViewHolder).bind(
            items[position] as FiatAccount,
            statusDecorator,
            onAccountClicked
        )
}

private class FiatAccountViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    private val disposables = CompositeDisposable()

    internal fun bind(
        account: FiatAccount,
        statusDecorator: StatusDecorator?,
        onAccountClicked: (FiatAccount) -> Unit
    ) {
        with(itemView) {
            disposables.clear()

            fiat_account.updateAccount(account, disposables)
            setOnClickListener { onAccountClicked(account) }
        }
    }
}

private class AllWalletsAccountDelegate<in T>(
    private val statusDecorator: StatusDecorator?,
    private val onAccountClicked: (BlockchainAccount) -> Unit
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is AllWalletsAccount

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AllWalletsAccountViewHolder(parent.inflate(R.layout.item_account_select_group))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AllWalletsAccountViewHolder).bind(
        items[position] as AllWalletsAccount,
        statusDecorator,
        onAccountClicked
    )
}

private class AllWalletsAccountViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    private val disposables = CompositeDisposable()

    internal fun bind(
        account: AllWalletsAccount,
        statusDecorator: StatusDecorator?,
        onAccountClicked: (BlockchainAccount) -> Unit
    ) {
        with(itemView) {
            disposables.clear()

            account_group.updateAccount(account, disposables)
            setOnClickListener { onAccountClicked(account) }
        }
    }
}
