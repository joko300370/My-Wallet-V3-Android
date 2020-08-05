package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.verify_identinty_benefits_layout.view.*
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf

class VerifyIdentityBenefitsView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    init {
        inflate(context, R.layout.verify_identinty_benefits_layout, this)
    }

    fun initWithBenefits(
        benefits: List<VerifyIdentityBenefit>,
        title: String,
        description: String,
        @DrawableRes icon: Int,
        primaryButton: ButtonOptions,
        secondaryButton: ButtonOptions
    ) {
        funds_kyc_intro_title.text = title
        funds_kyc_intro_description.text = description
        funds_kyc_default_symbol.setImageResource(icon)
        funds_kyc_negative_action.visibleIf { secondaryButton.visible }
        funds_kyc_positive_action.visibleIf { primaryButton.visible }
        funds_kyc_positive_action.setOnClickListener {
            primaryButton.cta()
        }
        funds_kyc_negative_action.setOnClickListener {
            secondaryButton.cta()
        }

        val adapter = BenefitsAdapter().apply {
            items = benefits
        }

        rv_benefits.layoutManager = LinearLayoutManager(context)
        rv_benefits.adapter = adapter
    }
}

data class VerifyIdentityBenefit(val title: String, val subtitle: String)
data class ButtonOptions(val visible: Boolean, val cta: () -> Unit = {})