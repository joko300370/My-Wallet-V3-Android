package piuk.blockchain.android.cards.views

import android.content.Context
import android.util.AttributeSet
import com.braintreepayments.cardform.view.CvvEditText
import piuk.blockchain.android.R

class CardCvvEditText : CvvEditText {
    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle) {
    }

    override fun getErrorMessage(): String {
        return resources.getString(R.string.invalid_cvv)
    }
}