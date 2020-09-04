package piuk.blockchain.android.ui.interest

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_interest_dashboard_asset_info.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.drawableResFilled
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import timber.log.Timber

class InterestDashboardAssetItem<in T>(
    private val disposable: CompositeDisposable,
    private val custodialWalletManager: CustodialWalletManager,
    private val itemClicked: (CryptoCurrency, Boolean) -> Unit
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position] as InterestDashboardItem
        return item is InterestAssetInfoItem
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        InterestAssetItemViewHolder(
            parent.inflate(R.layout.item_interest_dashboard_asset_info)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as InterestAssetItemViewHolder).bind(
        items[position] as InterestAssetInfoItem,
        disposable,
        custodialWalletManager,
        itemClicked
    )
}

private class InterestAssetItemViewHolder(val parent: View) :
    RecyclerView.ViewHolder(parent),
    LayoutContainer {

    override val containerView: View?
        get() = itemView

    fun bind(
        item: InterestAssetInfoItem,
        disposables: CompositeDisposable,
        custodialWalletManager: CustodialWalletManager,
        itemClicked: (CryptoCurrency, Boolean) -> Unit
    ) {
        itemView.item_interest_asset_icon.setImageResource(item.cryptoCurrency.drawableResFilled())
        itemView.item_interest_asset_title.text =
            parent.context.getString(item.cryptoCurrency.assetName())

        itemView.item_interest_acc_balance_title.text =
            parent.context.getString(R.string.interest_dashboard_item_balance_title,
                item.cryptoCurrency.displayTicker)

        itemView.item_interest_cta.isEnabled = item.isKyc

        val interestDetails = InterestDetails()
        disposables += custodialWalletManager.getInterestAccountDetails(item.cryptoCurrency)
            .flatMap { details ->
                interestDetails.totalInterest = details.totalInterest
                interestDetails.balance = details.balance

                custodialWalletManager.getInterestAccountRates(item.cryptoCurrency).map { interestRate ->
                    interestDetails.interestRate = interestRate

                    interestDetails
                }
            }.observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    itemView.item_interest_acc_earned_label.text =
                        "${it.totalInterest?.toStringWithSymbol()}"

                    it.balance?.let { balance ->
                        itemView.item_interest_acc_balance_label.text = balance.toStringWithSymbol()

                        if (balance.isPositive) {
                            itemView.item_interest_cta.text =
                                parent.context.getString(R.string.interest_dashboard_item_action_view)
                        } else {
                            itemView.item_interest_cta.text =
                                parent.context.getString(R.string.interest_dashboard_item_action_earn)
                        }

                        itemView.item_interest_cta.setOnClickListener {
                            itemClicked(item.cryptoCurrency, balance.isPositive)
                        }
                    }

                    it.interestRate?.let { rate ->
                        val rateIntro = parent.context.getString(R.string.interest_dashboard_item_rate_1)
                        val rateInfo = "$rate%"
                        val rateOutro = parent.context.getString(R.string.interest_dashboard_item_rate_2,
                            item.cryptoCurrency.displayTicker)

                        val sb = SpannableStringBuilder()
                        sb.append(rateIntro)
                        sb.append(rateInfo)
                        sb.setSpan(StyleSpan(Typeface.BOLD), rateIntro.length,
                            rateIntro.length + rateInfo.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        sb.append(rateOutro)

                        itemView.item_interest_info_text.setText(sb, TextView.BufferType.SPANNABLE)
                    }
                },
                onError = {
                    Timber.e("Error loading interest dashboard item: $it")
                }
            )
    }

    private data class InterestDetails(
        var balance: CryptoValue? = null,
        var totalInterest: CryptoValue? = null,
        var interestRate: Double? = null
    )
}