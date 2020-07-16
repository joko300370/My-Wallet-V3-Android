package piuk.blockchain.android.ui.settings.preferences

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.blockchain.swap.nabu.datamanagers.LinkedBank
import kotlinx.android.synthetic.main.preference_bank_layout.view.*
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.helperfunctions.CustomFont
import piuk.blockchain.androidcoreui.utils.helperfunctions.loadFont

class BankPreference(
    fiatCurrency: String,
    private val bank: LinkedBank? = null,
    context: Context
) : Preference(context, null, R.attr.preferenceStyle, 0) {
    private var typeface: Typeface? = null

    init {
        widgetLayoutResource = R.layout.preference_bank_layout

        loadFont(context, CustomFont.MONTSERRAT_REGULAR) {
            typeface = it
            this.title = title // Forces setting fonts when Title is set via XML
        }
        title = bank?.title ?: context.getString(R.string.add_bank_title, fiatCurrency)
        summary = bank?.currency ?: ""
        icon = getContext().getDrawable(R.drawable.ic_bank_transfer)
    }

    override fun setTitle(titleResId: Int) {
        title = context.getString(titleResId)
    }

    override fun setTitle(title: CharSequence?) {
        typeface?.let {
            super.setTitle(title?.applyFont(typeface))
        } ?: super.setTitle(title)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val titleView = holder.findViewById(android.R.id.title) as? TextView
        val addBank = holder.itemView.add_bank
        val endDigits = holder.itemView.account_end_digits
        bank?.let {
            addBank.gone()
            endDigits.visible()
            endDigits.text = it.accountDotted
        } ?: kotlin.run {
            endDigits.gone()
            addBank.visible()
        }
        titleView?.ellipsize = TextUtils.TruncateAt.END
        titleView?.setSingleLine(true)
        holder.isDividerAllowedAbove = true
    }
}
