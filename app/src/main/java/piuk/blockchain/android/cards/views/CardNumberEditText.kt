package piuk.blockchain.android.cards.views

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import androidx.core.widget.TextViewCompat
import com.blockchain.preferences.SimpleBuyPrefs
import com.braintreepayments.cardform.view.CardEditText
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.icon
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class CardNumberEditText : CardEditText, KoinComponent {
    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle) {
    }

    private val simpleBuyPrefs: SimpleBuyPrefs by inject()

    private val supportedCardTypes: String by unsafeLazy {
        simpleBuyPrefs.getSupportedCardTypes() ?: "VISA"
    }

    override fun getErrorMessage(): String {
        return if (cardNumberIsInvalid())
            resources.getString(R.string.invalid_card_number)
        else
            resources.getString(R.string.card_not_supported)
    }

    private fun cardNumberIsInvalid(): Boolean = super.isValid().not()

    override fun afterTextChanged(editable: Editable) {
        super.afterTextChanged(editable)
        if (supportedCardTypes.contains(cardType.name)) {
            updateIcon(cardType.icon())
        } else updateIcon(0)
    }

    private fun updateIcon(frontResource: Int) {
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(this,
            0,
            0,
            frontResource,
            0)
    }

    override fun isValid(): Boolean {
        return super.isValid() && supportedCardTypes.contains(cardType.name)
    }
}