package piuk.blockchain.android.ui.interest

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemInterestDashboardVerificationBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.customviews.ButtonOptions
import piuk.blockchain.android.ui.customviews.VerifyIdentityNumericBenefitItem
import piuk.blockchain.android.util.context

class InterestDashboardVerificationItem<in T>(
    private val verificationClicked: () -> Unit
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position] as InterestDashboardItem
        return item is InterestIdentityVerificationItem
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        InterestAssetVerificationViewHolder(
            ItemInterestDashboardVerificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as InterestAssetVerificationViewHolder).bind(
        verificationClicked
    )
}

private class InterestAssetVerificationViewHolder(
    private val binding: ItemInterestDashboardVerificationBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        verificationClicked: () -> Unit
    ) {
        binding.itemVerification.initWithBenefits(
            benefits = listOf(
                VerifyIdentityNumericBenefitItem(
                    context.getString(R.string.interest_dashboard_verify_point_one_title),
                    context.getString(R.string.interest_dashboard_verify_point_one_label)),
                VerifyIdentityNumericBenefitItem(
                    context.getString(R.string.interest_dashboard_verify_point_two_title),
                    context.getString(R.string.interest_dashboard_verify_point_two_label)),
                VerifyIdentityNumericBenefitItem(
                    context.getString(R.string.interest_dashboard_verify_point_three_title),
                    context.getString(R.string.interest_dashboard_verify_point_three_label))
            ),
            title = context.getString(R.string.interest_dashboard_verify_title),
            description = context.getString(R.string.interest_dashboard_verify_label),
            icon = R.drawable.ic_interest_blue_circle,
            primaryButton = ButtonOptions(true, cta = verificationClicked),
            secondaryButton = ButtonOptions(false),
            showSheetIndicator = false
        )
    }
}