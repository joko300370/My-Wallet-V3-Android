package piuk.blockchain.android.ui.transfer.widgets

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
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_send_account.view.*
import kotlinx.android.synthetic.main.view_account_list.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.SingleAccountList

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

    fun initialise(source: Single<SingleAccountList>) {

        with(list) {
            val itemList = mutableListOf<BlockchainAccount>()

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

            adapter = Adapter(itemList, onAccountSelected)

            disposables += source
                .observeOn(uiScheduler)
                .subscribeBy(
                    onSuccess = {
                        itemList.clear()
                        itemList.addAll(it)
                        if (itemList.isEmpty()) {
                            onEmptyList()
                        }
                        list.adapter?.notifyDataSetChanged()
                    },
                    onError = {
                        onLoadError(it)
                    }
                )
        }
    }

    var onLoadError: (Throwable) -> Unit = {}
    var onAccountSelected: (BlockchainAccount) -> Unit = {}
    var onEmptyList: () -> Unit = {}
}

private class Adapter(
    private val itemsList: MutableList<BlockchainAccount> = mutableListOf(),
    private val onAccountSelected: (BlockchainAccount) -> Unit
) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_send_account,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int =
        itemsList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(itemsList[position], onAccountSelected)
    }
}

private class ViewHolder(val parent: View) :
    RecyclerView.ViewHolder(parent),
    LayoutContainer {

    override val containerView: View?
        get() = itemView

    fun bind(
        item: BlockchainAccount,
        onItemClicked: (BlockchainAccount) -> Unit
    ) {
        itemView.setOnClickListener { onItemClicked(item) }
        itemView.details.account = item
    }
}
