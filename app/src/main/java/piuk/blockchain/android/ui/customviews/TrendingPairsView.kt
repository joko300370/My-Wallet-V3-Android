package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.koin.scopedInject
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_trending_pair_row.view.*
import kotlinx.android.synthetic.main.view_trending_pairs.view.*
import kotlinx.android.synthetic.main.view_trending_pairs.view.trending_title
import org.koin.core.KoinComponent
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.ui.dashboard.assetdetails.selectFirstAccount
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.drawableResFilled
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible

class TrendingPairsView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    private var viewType: TrendingType = TrendingType.OTHER

    init {
        inflate(context, R.layout.view_trending_pairs, this)

        setupView(context, attrs)
    }

    fun initialise(pairs: List<TrendingPair>, onSwapPairClicked: (TrendingPair) -> Unit) {
        setupPairs(pairs, onSwapPairClicked)
    }

    private fun setupView(context: Context, attrs: AttributeSet) {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.TrendingPairsView, 0, 0)

        viewType =
            TrendingType.fromInt(attributes.getInt(R.styleable.TrendingPairsView_trending_type, 1))

        attributes.recycle()
    }

    private fun setupPairs(pairs: List<TrendingPair>, onSwapPairClicked: (TrendingPair) -> Unit) {
        if (pairs.isEmpty()) {
            trending_empty.visible()
            trending_list.gone()
        } else {
            trending_empty.gone()
            trending_list.apply {
                addItemDecoration(
                    DividerItemDecoration(
                        context,
                        DividerItemDecoration.VERTICAL
                    )
                )
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                adapter = TrendingPairsAdapter(
                    type = viewType,
                    itemClicked = {
                        onSwapPairClicked(it)
                    },
                    items = pairs
                )
                visible()
            }
        }
    }

    enum class TrendingType {
        SWAP,
        OTHER;

        companion object {
            fun fromInt(value: Int) = values()[value]
        }
    }
}

interface TrendingPairsProvider {
    fun getTrendingPairs(): Single<List<TrendingPair>>
}

class SwapTrendingPairsProvider : TrendingPairsProvider, KoinComponent {
    private val coincore: Coincore by scopedInject()

    override fun getTrendingPairs(): Single<List<TrendingPair>> =
        Singles.zip(
            coincore[CryptoCurrency.BTC].accountGroup(AssetFilter.Custodial).toSingle(),
            coincore[CryptoCurrency.ETHER].accountGroup(AssetFilter.Custodial).toSingle(),
            coincore[CryptoCurrency.PAX].accountGroup(AssetFilter.Custodial).toSingle(),
            coincore[CryptoCurrency.BCH].accountGroup(AssetFilter.Custodial).toSingle(),
            coincore[CryptoCurrency.XLM].accountGroup(AssetFilter.Custodial).toSingle()
        ) { btcGroup, ethGroup, paxGroup, bchGroup, xlmGroup ->
            val btcCustodialAccount = btcGroup.selectFirstAccount()
            val ethCustodialAccount = ethGroup.selectFirstAccount()
            val paxCustodialAccount = paxGroup.selectFirstAccount()
            val bchCustodialAccount = bchGroup.selectFirstAccount()
            val xlmCustodialAccount = xlmGroup.selectFirstAccount()

            listOf(
                TrendingPair(btcCustodialAccount, ethCustodialAccount, btcCustodialAccount.isFunded),
                TrendingPair(btcCustodialAccount, paxCustodialAccount, btcCustodialAccount.isFunded),
                TrendingPair(btcCustodialAccount, xlmCustodialAccount, btcCustodialAccount.isFunded),
                TrendingPair(btcCustodialAccount, bchCustodialAccount, btcCustodialAccount.isFunded),
                TrendingPair(ethCustodialAccount, paxCustodialAccount, ethCustodialAccount.isFunded)
            )
        }.onErrorReturn {
            emptyList()
        }
}

private class TrendingPairsAdapter(
    val type: TrendingPairsView.TrendingType,
    val itemClicked: (TrendingPair) -> Unit,
    private val items: List<TrendingPair>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        TrendingPairViewHolder(parent.inflate(R.layout.item_trending_pair_row, false), itemClicked)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as TrendingPairViewHolder).bind(type, items[position])
    }

    override fun getItemCount(): Int = items.size

    private class TrendingPairViewHolder(parent: View, val itemClicked: (TrendingPair) -> Unit) :
        RecyclerView.ViewHolder(parent), LayoutContainer {

        override val containerView: View?
            get() = itemView

        fun bind(type: TrendingPairsView.TrendingType, item: TrendingPair) {
            itemView.apply {
                trending_icon_in.setImageResource(item.sourceAccount.asset.drawableResFilled())
                trending_icon_out.setImageResource(item.destinationAccount.asset.drawableResFilled())
                if (item.enabled) {
                    trending_root.setOnClickListener {
                        itemClicked(item)
                    }
                    trending_root.alpha = 1f
                } else {
                    trending_root.setOnClickListener(null)
                    trending_root.alpha = 0.6f
                }

                when (type) {
                    TrendingPairsView.TrendingType.SWAP -> {
                        trending_title.text = context.getString(R.string.trending_swap,
                            context.getString(item.sourceAccount.asset.assetName()))
                        trending_subtitle.text = context.getString(R.string.trending_receive,
                            context.getString(item.destinationAccount.asset.assetName()))
                        trending_icon_type.setImageDrawable(
                            ContextCompat.getDrawable(context, R.drawable.ic_swap_light_blue))
                    }
                    else -> {
                        // do nothing
                    }
                }
            }
        }
    }
}

data class TrendingPair(
    val sourceAccount: CryptoAccount,
    val destinationAccount: CryptoAccount,
    val enabled: Boolean
)