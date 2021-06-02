package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import org.koin.core.KoinComponent
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewTransactionProgressBinding
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible

class TransactionProgressView(context: Context, attrs: AttributeSet) :
    ConstraintLayout(context, attrs), KoinComponent {

    private val binding: ViewTransactionProgressBinding =
        ViewTransactionProgressBinding.inflate(LayoutInflater.from(context), this, true)

    fun setAssetIcon(@DrawableRes assetIcon: Int) {
        binding.txIcon.setImageResource(assetIcon)
    }

    fun onCtaClick(fn: () -> Unit) {
        binding.txOkBtn.setOnClickListener {
            fn()
        }
    }

    fun configureSecondaryButton(text: String, fn: () -> Unit) {
        with(binding.secondaryBtn) {
            visible()
            this.text = text
            setOnClickListener { fn() }
        }
    }

    fun showTxInProgress(title: String, subtitle: String) {
        with(binding) {
            progress.visible()
            txStateIndicator.gone()
            txOkBtn.gone()
        }
        setText(title, subtitle)
    }

    fun showTxPending(title: String, subtitle: String) {
        with(binding) {
            progress.gone()
            txStateIndicator.visible()
            txOkBtn.visible()
            txStateIndicator.setImageResource(R.drawable.ic_pending_clock)
        }
        setText(title, subtitle)
    }

    fun showTxSuccess(
        title: String,
        subtitle: String
    ) {
        with(binding) {
            txStateIndicator.setImageResource(R.drawable.ic_check_circle)
            txStateIndicator.visible()
        }
        showEndStateUi()
        setText(title, subtitle)
    }

    fun showPendingTx(
        title: String,
        subtitle: String,
        locksNote: SpannableStringBuilder
    ) {
        with(binding) {
            txStateIndicator.setImageResource(R.drawable.ic_check_circle)
            txStateIndicator.visible()
            showEndStateUi()
            txTitle.text = title
            txSubtitle.text = subtitle
            txNoteLocks.run {
                setText(locksNote, TextView.BufferType.SPANNABLE)
                movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    fun showTxError(title: String, subtitle: String) {
        with(binding) {
            txIcon.setImageResource(R.drawable.ic_alert)
            txStateIndicator.gone()
        }
        showEndStateUi()
        setText(title, subtitle)
    }

    fun showFiatTxSuccess(title: String, subtitle: String, currency: String) {
        setFiatAssetIcon(currency)
        with(binding.txStateIndicator) {
            setImageResource(R.drawable.ic_tx_deposit_w_green_bkgd)
            visible()
        }
        showEndStateUi()
        setText(title, subtitle)
    }

    fun showFiatTxPending(title: String, subtitle: String, currency: String) {
        setFiatAssetIcon(currency)
        showTxInProgress(title, subtitle)
    }

    fun showFiatTxError(title: String, subtitle: String, currency: String) {
        setFiatAssetIcon(currency)
        with(binding) {
            txIcon.setImageResource(R.drawable.ic_alert)
            txStateIndicator.gone()
        }
        showEndStateUi()
        setText(title, subtitle)
    }

    private fun setFiatAssetIcon(currency: String) =
        setAssetIcon(
            when (currency) {
                "EUR" -> R.drawable.ic_funds_euro_masked
                "GBP" -> R.drawable.ic_funds_euro_masked
                else -> R.drawable.ic_funds_usd_masked
            }
        )

    private fun showEndStateUi() {
        with(binding) {
            progress.gone()
            txOkBtn.visible()
        }
    }

    private fun setText(title: String, subtitle: String) {
        with(binding) {
            txTitle.text = title
            txSubtitle.text = subtitle
        }
    }
}