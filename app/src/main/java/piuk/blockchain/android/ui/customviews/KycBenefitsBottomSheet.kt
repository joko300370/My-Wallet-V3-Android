package piuk.blockchain.android.ui.customviews

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.KycBenefitsBottomSheetBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class KycBenefitsBottomSheet : SlidingModalBottomDialog<KycBenefitsBottomSheetBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun verificationCtaClicked()
    }

    private val benefitsDetails: BenefitsDetails by lazy {
        arguments?.getParcelable(BENEFITS_DETAILS) ?: BenefitsDetails()
    }

    override val host: Host by lazy {
        super.host as? Host ?: throw IllegalStateException(
            "Host fragment is not a KycBenefitsBottomSheet.Host"
        )
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): KycBenefitsBottomSheetBinding =
        KycBenefitsBottomSheetBinding.inflate(inflater, container, false)

    override fun initControls(binding: KycBenefitsBottomSheetBinding) {
        with(binding) {
            benefitsView.initWithBenefits(
                benefits = benefitsDetails.listOfBenefits,
                title = benefitsDetails.title,
                description = benefitsDetails.description,
                icon = benefitsDetails.icon,
                primaryButton = ButtonOptions(true) {
                    dismiss()
                    host.verificationCtaClicked()
                },
                secondaryButton = ButtonOptions(true) {
                    dismiss()
                }
            )
        }
    }

    companion object {
        private const val BENEFITS_DETAILS = "BENEFITS_DETAILS"

        fun newInstance(
            details: BenefitsDetails
        ): KycBenefitsBottomSheet = KycBenefitsBottomSheet().apply {
            arguments = Bundle().apply {
                putParcelable(BENEFITS_DETAILS, details)
            }
        }
    }

    @Parcelize
    data class BenefitsDetails(
        val title: String = "",
        val description: String = "",
        val listOfBenefits: List<VerifyIdentityNumericBenefitItem> = emptyList(),
        val icon: Int = R.drawable.ic_verification_badge
    ) : Parcelable
}