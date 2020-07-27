package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_transaction_progress.view.*
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.visible

class TransactionProgressView(context: Context, attrs: AttributeSet)
    : ConstraintLayout(context, attrs) {

    init {
        inflate(context, R.layout.view_transaction_progress, this)
    }

    fun setAssetIcon(@DrawableRes assetIcon: Int) {
        tx_icon.setImageResource(assetIcon)
    }

    fun onCtaClick(fn: () -> Unit) {
        tx_ok_btn.setOnClickListener {
            fn()
        }
    }

    fun showTxInProgress(title: String, subtitle: String) {
        tx_progress.visible()
        tx_state_indicator.gone()
        tx_ok_btn.gone()
        setText(title, subtitle)
    }

    fun showTxPending(title: String, subtitle: String) {
        tx_progress.gone()
        tx_state_indicator.visible()
        tx_ok_btn.visible()
        tx_state_indicator.setImageResource(R.drawable.ic_pending_clock)
        setText(title, subtitle)
    }

    fun showTxSuccess(title: String, subtitle: String) {
        tx_state_indicator.setImageResource(R.drawable.ic_check_circle)
        tx_state_indicator.visible()
        showEndStateUi()
        setText(title, subtitle)
    }

    fun showTxError(title: String, subtitle: String) {
        tx_icon.setImageResource(R.drawable.ic_alert)
        tx_state_indicator.gone()
        showEndStateUi()
        setText(title, subtitle)
    }

    private fun showEndStateUi() {
        tx_progress.gone()
        tx_ok_btn.visible()
    }

    private fun setText(title: String, subtitle: String) {
        tx_title.text = title
        tx_subtitle.text = subtitle
    }
}