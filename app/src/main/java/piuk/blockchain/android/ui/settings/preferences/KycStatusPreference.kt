package piuk.blockchain.android.ui.settings.preferences

import android.content.Context
import androidx.core.content.ContextCompat
import android.util.AttributeSet
import android.widget.TextView
import com.blockchain.swap.nabu.models.nabu.Kyc2TierState
import piuk.blockchain.android.R

class KycStatusPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.preferenceStyle,
    defStyleRes: Int = 0
) : BaseStatusPreference<Kyc2TierState>(context, attrs, defStyleAttr, defStyleRes) {

    override val defaultValue = Kyc2TierState.Hidden

    override fun doUpdateValue(value: Kyc2TierState, view: TextView) {
        view.apply {
            text = when (value) {
                Kyc2TierState.Hidden -> ""
                Kyc2TierState.Locked -> context.getString(R.string.kyc_settings_tier_status_locked)
                Kyc2TierState.Tier1InReview,
                Kyc2TierState.Tier1Pending -> context.getString(R.string.kyc_settings_silver_level_in_review)
                Kyc2TierState.Tier1Approved -> context.getString(R.string.kyc_settings_silver_level_approved)
                Kyc2TierState.Tier1Failed -> context.getString(R.string.kyc_settings_tier_status_failed)
                Kyc2TierState.Tier2InReview,
                Kyc2TierState.Tier2InPending -> context.getString(R.string.kyc_settings_gold_level_in_review)
                Kyc2TierState.Tier2Approved -> context.getString(R.string.kyc_settings_gold_level_approved)
                Kyc2TierState.Tier2Failed -> context.getString(R.string.kyc_settings_tier_status_failed)
            }

            val background = when (value) {
                Kyc2TierState.Hidden -> 0
                Kyc2TierState.Locked -> 0
                Kyc2TierState.Tier1InReview, Kyc2TierState.Tier1Pending -> R.drawable.pref_status_bkgrd_orange
                Kyc2TierState.Tier1Approved -> R.drawable.pref_status_bkgrd_complete
                Kyc2TierState.Tier1Failed -> R.drawable.pref_status_bkgrd_failed
                Kyc2TierState.Tier2InReview, Kyc2TierState.Tier2InPending -> R.drawable.pref_status_bkgrd_orange
                Kyc2TierState.Tier2Approved -> R.drawable.pref_status_bkgrd_complete
                Kyc2TierState.Tier2Failed -> R.drawable.pref_status_bkgrd_failed
            }
            val foreground = when (value) {
                Kyc2TierState.Locked -> R.color.kyc_progress_text_blue
                else -> R.color.kyc_progress_text_white
            }
            setBackgroundResource(background)
            setTextColor(ContextCompat.getColor(context, foreground))
        }
    }
}
