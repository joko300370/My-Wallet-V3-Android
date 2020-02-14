package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import kotlinx.android.synthetic.main.fragment_simple_buy_kyc_pending.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf

class SimpleBuyPendingKycFragment : MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState>(), SimpleBuyScreen {

    override val model: SimpleBuyModel by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_simple_buy_kyc_pending)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.process(SimpleBuyIntent.FetchKycState)
        model.process(SimpleBuyIntent.FlowCurrentScreen(FlowScreen.KYC_VERIFICATION))
        analytics.logEvent(SimpleBuyAnalytics.KYC_VERYFING)
        continue_to_wallet.setOnClickListener {
            navigator().exitSimpleBuyFlow()
        }
    }

    override fun render(newState: SimpleBuyState) {
        kyc_progress.visibleIf { newState.kycVerificationState == KycState.PENDING }
        kyc_failed_icon.visibleIf {
            newState.kycVerificationState == KycState.FAILED ||
                    newState.kycVerificationState == KycState.UNDECIDED ||
                    newState.kycVerificationState == KycState.VERIFIED_BUT_NOT_ELIGIBLE
        }

        verif_text.text = when (newState.kycVerificationState) {
            KycState.PENDING -> resources.getString(R.string.kyc_verifying_info)
            KycState.FAILED -> resources.getString(R.string.kyc_manual_review_required)
            KycState.UNDECIDED -> resources.getString(R.string.kyc_pending_review)
            KycState.VERIFIED_BUT_NOT_ELIGIBLE -> resources.getString(R.string.kyc_veriff_but_not_eligible_review)
            else -> ""
        }

        verif_time.text = when (newState.kycVerificationState) {
            KycState.PENDING -> resources.getString(R.string.kyc_verifying_time_info)
            KycState.FAILED,
            KycState.UNDECIDED -> resources.getString(R.string.kyc_verifying_manual_review_required_info)
            KycState.VERIFIED_BUT_NOT_ELIGIBLE -> resources.getString(R.string.kyc_veriff_but_not_eligible_review_info)
            else -> ""
        }

        continue_to_wallet.visibleIf {
            newState.kycVerificationState == KycState.FAILED ||
                    newState.kycVerificationState == KycState.UNDECIDED ||
                    newState.kycVerificationState == KycState.VERIFIED_BUT_NOT_ELIGIBLE
        }

        if (newState.kycVerificationState == KycState.VERIFIED_AND_ELIGIBLE) {
            navigator().goToCheckOutScreen()
        }

        kyc_failed_icon.setImageResource(
            when (newState.kycVerificationState) {
                KycState.FAILED -> R.drawable.ic_kyc_failed_warning
                KycState.VERIFIED_BUT_NOT_ELIGIBLE -> R.drawable.ic_kyc_approved
                else -> R.drawable.ic_kyc_pending
            }
        )

        when (newState.kycVerificationState) {
            KycState.VERIFIED_BUT_NOT_ELIGIBLE -> analytics.logEvent(SimpleBuyAnalytics.KYC_NOT_ELIGIBLE)
            KycState.VERIFIED_AND_ELIGIBLE -> analytics.logEvent(SimpleBuyAnalytics.KYC_VERYFING)
            KycState.PENDING -> analytics.logEvent(SimpleBuyAnalytics.KYC_PENDING)
            KycState.IN_REVIEW -> analytics.logEvent(SimpleBuyAnalytics.KYC_MANUAL)
            else -> {
            }
        }
    }

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true
}