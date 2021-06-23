package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.databinding.VerifyIdentityBenefitsLayoutBinding
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visibleIf

class VerifyIdentityBenefitsView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    private val binding: VerifyIdentityBenefitsLayoutBinding = VerifyIdentityBenefitsLayoutBinding.inflate(
        LayoutInflater.from(context), this, true)

    fun initWithBenefits(
        benefits: List<VerifyIdentityItem>,
        title: String,
        description: String,
        @DrawableRes icon: Int,
        primaryButton: ButtonOptions,
        secondaryButton: ButtonOptions,
        footerText: String = "",
        showSheetIndicator: Boolean = true
    ) {
        with(binding) {
            kycBenefitsIntroTitle.text = title
            kycBenefitsIntroDescription.text = description
            kycBenefitsDefaultSymbol.setImageResource(icon)
            kycBenefitsNegativeAction.visibleIf { secondaryButton.visible }
            kycBenefitsPositiveAction.visibleIf { primaryButton.visible }
            kycBenefitsPositiveAction.setOnClickListener {
                primaryButton.cta()
            }
            primaryButton.text?.let {
                kycBenefitsPositiveAction.text = it
            }

            secondaryButton.text?.let {
                kycBenefitsNegativeAction.text = it
            }

            kycBenefitsNegativeAction.setOnClickListener {
                secondaryButton.cta()
            }
            this.footerText.visibleIf { footerText.isNotEmpty() }
            this.footerText.text = footerText

            val adapter = BenefitsDelegateAdapter().apply {
                items = benefits
            }

            rvBenefits.layoutManager = LinearLayoutManager(context)
            rvBenefits.adapter = adapter

            if (!showSheetIndicator) {
                kycBenefitsSheetIndicator.gone()
            }
        }
    }
}

@Parcelize
data class VerifyIdentityNumericBenefitItem(override val title: String, override val subtitle: String) :
    VerifyIdentityItem,
    Parcelable

data class VerifyIdentityIconedBenefitItem(
    override val title: String,
    override val subtitle: String,
    @DrawableRes val icon: Int
) : VerifyIdentityItem

data class ButtonOptions(val visible: Boolean, val text: String? = null, val cta: () -> Unit = {})

interface VerifyIdentityItem {
    val title: String
    val subtitle: String
}
