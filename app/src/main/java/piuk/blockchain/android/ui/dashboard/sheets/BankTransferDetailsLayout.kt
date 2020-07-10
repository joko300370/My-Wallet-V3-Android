package piuk.blockchain.android.ui.dashboard.sheets

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.bank_transfer_details_layout.view.*
import piuk.blockchain.android.R

class BankTransferDetailsLayout(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    init {
        inflate(context, R.layout.bank_transfer_details_layout, this)
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.BankTransferDetailsLayout)
        icon.setImageDrawable(attributes.getDrawable(R.styleable.BankTransferDetailsLayout_image))
        title.text = attributes.getString(R.styleable.BankTransferDetailsLayout_title)
        subtitle.text = attributes.getString(R.styleable.BankTransferDetailsLayout_subtitle)
        attributes.recycle()
    }

    fun updateSubtitle(text: String) {
        subtitle.text = text
    }
}