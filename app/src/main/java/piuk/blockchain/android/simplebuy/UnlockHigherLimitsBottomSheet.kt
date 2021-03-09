package piuk.blockchain.android.simplebuy

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.service.TierService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.UnlockHigherLimitsLayoutBinding
import piuk.blockchain.android.sdd.SDDAnalytics
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.customviews.ButtonOptions
import piuk.blockchain.android.ui.customviews.VerifyIdentityIconedBenefitItem

class UnlockHigherLimitsBottomSheet : SlidingModalBottomDialog<UnlockHigherLimitsLayoutBinding>() {

    private val tiers: TierService by scopedInject()

    private val compositeDisposable = CompositeDisposable()

    interface Host : SlidingModalBottomDialog.Host {
        fun unlockHigherLimits()
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a UnlockHigherLimitsBottomSheet.Host"
        )
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): UnlockHigherLimitsLayoutBinding =
        UnlockHigherLimitsLayoutBinding.inflate(inflater, container, false)

    override fun initControls(binding: UnlockHigherLimitsLayoutBinding) {
        compositeDisposable += tiers.tiers().map {
            it.tierForLevel(KycTierLevel.GOLD).limits?.dailyFiat?.let { dailyLimit ->
                dailyLimit.toStringWithSymbol()
            } ?: getString(R.string.empty)
        }.onErrorReturn { getString(R.string.empty) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { limit ->
                binding.unlockMoreView.initWithBenefits(
                    benefits = listOf(
                        VerifyIdentityIconedBenefitItem(
                            title = getString(R.string.cash_accounts),
                            subtitle = getString(R.string.cash_accounts_description),
                            icon = R.drawable.ic_cash
                        ),
                        VerifyIdentityIconedBenefitItem(
                            title = getString(R.string.link_a_bank),
                            subtitle = getString(R.string.link_a_bank_description),
                            icon = R.drawable.ic_bank_details
                        ),
                        VerifyIdentityIconedBenefitItem(
                            title = getString(R.string.earn_interest),
                            subtitle = getString(R.string.earn_interest_description),
                            icon = R.drawable.ic_interest
                        )
                    ),
                    title = getString(R.string.unlock_gold_level_trading),
                    description = if (limit.isNotEmpty()) getString(
                        R.string.verify_your_identity_limits, limit
                    ) else getString(R.string.empty),
                    icon = R.drawable.ic_gold_square,
                    primaryButton = ButtonOptions(
                        true,
                        getString(R.string.apply_and_unlock)
                    ) {
                        dismiss()
                        host.unlockHigherLimits()
                    },
                    secondaryButton = ButtonOptions(false)
                )
            }, onError = {})

        analytics.logEvent(SDDAnalytics.UPGRADE_TO_GOLD_SEEN)
    }
}