package piuk.blockchain.android.cards.views

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import com.braintreepayments.cardform.view.ExpirationDateEditText
import piuk.blockchain.android.R

class CardExpirationDateEditText : ExpirationDateEditText {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle)

    /*
    We need to do that because ExpirationDateEditText accepts expiration date only in mm / yy or mm / yyyy
    After using autocomplete of Google pay we are getting back mm/yy that its not acceptable and its modified as
    mm / /yy as framework add " / " after the 2 first characters.
    */

    override fun afterTextChanged(editable: Editable?) {
        if (editable?.contains("/") == true && editable.contains(" / ").not()) {
            setText(editable.toString().replace("/", ""))
        } else {
            super.afterTextChanged(editable)
        }
    }

    override fun getErrorMessage(): String {
        return resources.getString(R.string.invalid_date)
    }
}