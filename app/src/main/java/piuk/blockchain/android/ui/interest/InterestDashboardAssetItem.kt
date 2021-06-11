package piuk.blockchain.android.ui.interest

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.models.responses.interest.DisabledReason
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.databinding.ItemInterestDashboardAssetInfoBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf
import timber.log.Timber

class InterestDashboardAssetItem<in T>(
    private val assetResources: AssetResources,
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
            ItemInterestDashboardAssetInfoBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as InterestAssetItemViewHolder).bind(
        assetResources,
        items[position] as InterestAssetInfoItem,
        disposable,
        custodialWalletManager,
        itemClicked
    )
}

private class InterestAssetItemViewHolder(private val binding: ItemInterestDashboardAssetInfoBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(
        assetResources: AssetResources,
        item: InterestAssetInfoItem,
        disposables: CompositeDisposable,
        custodialWalletManager: CustodialWalletManager,
        itemClicked: (CryptoCurrency, Boolean) -> Unit
    ) {
        with(binding) {
        itemInterestAssetIcon.setImageResource(assetResources.drawableResFilled(item.cryptoCurrency))
        itemInterestAssetTitle.text = context.getString(assetResources.assetNameRes(item.cryptoCurrency))

        itemInterestAccBalanceTitle.text =
            context.getString(R.string.interest_dashboard_item_balance_title,
                item.cryptoCurrency.displayTicker)

        disposables += Singles.zip(
            custodialWalletManager.getInterestAccountDetails(item.cryptoCurrency),
            custodialWalletManager.getInterestAccountRates(item.cryptoCurrency),
            custodialWalletManager.getInterestEligibilityForAsset(item.cryptoCurrency)
        ) { details, rate, eligibility ->
            InterestDetails(
                totalInterest = details.totalInterest,
                balance = details.balance,
                interestRate = rate,
                available = eligibility.eligible,
                disabledReason = eligibility.ineligibilityReason
            )
        }.observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { details ->
                    showInterestDetails(details, item, itemClicked)
                },
                onError = {
                    Timber.e("Error loading interest dashboard item: $it")
                    showDisabledState()
                }
            )
        }
    }

    private fun showDisabledState() {
        with(binding) {
            itemInterestCta.isEnabled = false
            itemInterestCta.text = context.getString(R.string.interest_dashboard_item_action_earn)
            itemInterestExplainer.visible()
            itemInterestExplainer.text = context.getString(R.string.interest_item_issue_other)
        }
    }

    private fun showInterestDetails(
        details: InterestDetails,
        item: InterestAssetInfoItem,
        itemClicked: (CryptoCurrency, Boolean) -> Unit
    ) {
        with(binding) {
            itemInterestAccEarnedLabel.text = details.totalInterest.toStringWithSymbol()

            itemInterestAccBalanceLabel.text = details.balance.toStringWithSymbol()

            setDisabledExplanation(details)

            setCta(item, details, itemClicked)

            setInterestInfo(details, item)
        }
    }

    private fun ItemInterestDashboardAssetInfoBinding.setCta(
        item: InterestAssetInfoItem,
        details: InterestDetails,
        itemClicked: (CryptoCurrency, Boolean) -> Unit
    ) {
        itemInterestCta.isEnabled = item.isKycGold && details.available
        itemInterestCta.text = if (details.balance.isPositive) {
            context.getString(R.string.interest_dashboard_item_action_view)
        } else {
            context.getString(R.string.interest_dashboard_item_action_earn)
        }

        itemInterestCta.setOnClickListener {
            itemClicked(item.cryptoCurrency, details.balance.isPositive)
        }
    }

    private fun ItemInterestDashboardAssetInfoBinding.setDisabledExplanation(details: InterestDetails) {
        itemInterestExplainer.text = context.getString(
            when (details.disabledReason) {
                DisabledReason.REGION -> R.string.interest_item_issue_region
                DisabledReason.KYC_TIER -> R.string.interest_item_issue_kyc
                DisabledReason.NONE -> R.string.empty
                else -> R.string.interest_item_issue_other
            }
        )

        itemInterestExplainer.visibleIf { details.disabledReason != DisabledReason.NONE }
    }

    private fun ItemInterestDashboardAssetInfoBinding.setInterestInfo(
        details: InterestDetails,
        item: InterestAssetInfoItem
    ) {
        val rateIntro = context.getString(R.string.interest_dashboard_item_rate_1)
        val rateInfo = "${details.interestRate}%"
        val rateOutro = context.getString(R.string.interest_dashboard_item_rate_2, item.cryptoCurrency.displayTicker)

        val sb = SpannableStringBuilder()
            .append(rateIntro)
            .append(rateInfo)
            .append(rateOutro)
        sb.setSpan(StyleSpan(Typeface.BOLD), rateIntro.length,
            rateIntro.length + rateInfo.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        itemInterestInfoText.setText(sb, TextView.BufferType.SPANNABLE)
    }

    private data class InterestDetails(
        val balance: CryptoValue,
        val totalInterest: CryptoValue,
        val interestRate: Double,
        val available: Boolean,
        val disabledReason: DisabledReason
    )
}