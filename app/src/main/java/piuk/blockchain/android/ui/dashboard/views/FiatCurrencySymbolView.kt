package piuk.blockchain.android.ui.dashboard.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import piuk.blockchain.android.R

class FiatCurrencySymbolView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val dpPadding = 8
    private val pxPadding = (dpPadding * resources.displayMetrics.density + 0.5f).toInt()

    init {
        background = ContextCompat.getDrawable(context, R.drawable.rounded_view_green_500)
        setPadding(pxPadding, pxPadding, pxPadding, pxPadding)
    }

    fun setIcon(fiat: String) =
        setImageDrawable(
            ContextCompat.getDrawable(context,
                when (fiat) {
                    "EUR" -> R.drawable.ic_vector_euro
                    "GBP" -> R.drawable.ic_vector_pound
                    else -> R.drawable.ic_vector_dollar // show dollar if currency isn't selected
                }
            ))
}