package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.blockchain.ui.urllinks.URL_BLOCKCHAIN_SUPPORT_PORTAL
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewEmptyStateBinding
import piuk.blockchain.android.util.calloutToExternalSupportLinkDlg
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedDrawable

class EmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewEmptyStateBinding = ViewEmptyStateBinding.inflate(LayoutInflater.from(context), this, true)

    fun setDetails(
        @StringRes title: Int = R.string.common_empty_title,
        @StringRes description: Int = R.string.common_empty_details,
        @DrawableRes icon: Int = R.drawable.ic_wallet_intro_image,
        @StringRes ctaText: Int = R.string.common_empty_cta,
        contactSupportEnabled: Boolean = false,
        action: () -> Unit
    ) {
        with(binding) {
            viewEmptyTitle.text = context.getString(title)
            viewEmptyDesc.text = context.getString(description)
            viewEmptyIcon.setImageDrawable(context.getResolvedDrawable(icon))
            viewEmptyCta.text = context.getString(ctaText)
            viewEmptyCta.setOnClickListener {
                action()
            }
            viewEmptySupportCta.visibleIf {
                contactSupportEnabled
            }
            viewEmptySupportCta.setOnClickListener {
                calloutToExternalSupportLinkDlg(context, URL_BLOCKCHAIN_SUPPORT_PORTAL)
            }
        }
    }
}