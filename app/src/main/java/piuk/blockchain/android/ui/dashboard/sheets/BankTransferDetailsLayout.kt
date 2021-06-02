package piuk.blockchain.android.ui.dashboard.sheets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.BankTransferDetailsLayoutBinding

class BankTransferDetailsLayout(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    private val binding = BankTransferDetailsLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.BankTransferDetailsLayout)
        with(binding) {
            icon.setImageDrawable(attributes.getDrawable(R.styleable.BankTransferDetailsLayout_image))
            title.text = attributes.getString(R.styleable.BankTransferDetailsLayout_title)
            subtitle.text = attributes.getString(R.styleable.BankTransferDetailsLayout_subtitle)
        }
        attributes.recycle()
    }

    fun updateSubtitle(text: String) {
        binding.subtitle.text = text
    }
}