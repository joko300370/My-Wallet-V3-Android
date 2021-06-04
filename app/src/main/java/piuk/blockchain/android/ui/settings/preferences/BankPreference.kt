package piuk.blockchain.android.ui.settings.preferences

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.blockchain.nabu.datamanagers.Bank
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.preference_bank_layout.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.loadInterMedium
import piuk.blockchain.android.util.visible
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedDrawable

class BankPreference(
    fiatCurrency: String,
    private val bank: Bank? = null,
    context: Context
) : Preference(context, null, R.attr.preferenceStyle, 0) {
    private val typeface: Typeface = context.loadInterMedium()

    init {
        widgetLayoutResource = R.layout.preference_bank_layout

        this.title = title // Forces setting fonts when Title is set via XML

        title = bank?.name ?: context.getString(R.string.add_bank_title, fiatCurrency)
        summary = bank?.currency ?: ""
        icon = context.getResolvedDrawable(R.drawable.ic_bank_transfer)
    }

    override fun setTitle(titleResId: Int) {
        title = context.getString(titleResId)
    }

    override fun setTitle(title: CharSequence?) {
        super.setTitle(title?.applyFont(typeface))
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val titleView = holder.findViewById(android.R.id.title) as? TextView
        val imageView = holder.findViewById(android.R.id.icon) as? ImageView
        imageView?.run {
            val size = resources.getDimension(R.dimen.asset_icon_size).toInt()
            layoutParams?.height = size
            layoutParams?.width = size
            requestLayout()
        }
        val addBank = holder.itemView.add_bank
        val accountInfo = holder.itemView.account_info
        val endDigits = holder.itemView.end_digits

        bank?.let { bank ->
            addBank.gone()
            accountInfo.visible()
            endDigits.visible()
            accountInfo.text = bank.toHumanReadableAccount()
            endDigits.text = bank.account
            if (bank.iconUrl.isNotEmpty()) {
                imageView?.let {
                    Glide.with(context).load(bank.iconUrl).into(it)
                }
            }
        } ?: kotlin.run {
            accountInfo.gone()
            endDigits.gone()
            addBank.visible()
        }
        titleView?.ellipsize = TextUtils.TruncateAt.END
        titleView?.isSingleLine = true
        holder.isDividerAllowedAbove = true
    }
}
