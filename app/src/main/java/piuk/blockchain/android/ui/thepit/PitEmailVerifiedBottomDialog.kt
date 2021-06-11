package piuk.blockchain.android.ui.thepit

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.ui.extensions.throttledClicks
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.databinding.PitEmailVerifiedBottomDialogBinding
import piuk.blockchain.android.ui.customviews.ErrorBottomDialog
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible

class PitEmailVerifiedBottomDialog : ErrorBottomDialog<PitEmailVerifiedBottomDialogBinding>() {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): PitEmailVerifiedBottomDialogBinding =
        PitEmailVerifiedBottomDialogBinding.inflate(layoutInflater, container, false)

    override fun init(content: Content) {
        with(binding) {
            dialogTitle.text = content.title
            content.icon.takeIf { it > 0 }?.let {
                dialogIcon.setImageResource(it)
                dialogIcon.visible()
            } ?: dialogIcon.gone()

            dialogBody.apply {
                with(content) {
                    text = descriptionToFormat?.let {
                        getString(descriptionToFormat.first, descriptionToFormat.second)
                    } ?: description
                    movementMethod = LinkMovementMethod.getInstance()
                }
            }

            buttonCta.apply {
                if (content.ctaButtonText != 0) {
                    setText(content.ctaButtonText)
                } else {
                    gone()
                }
            }

            buttonDismiss.apply {
                if (content.dismissText != 0) {
                    setText(content.dismissText)
                } else {
                    gone()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        clicksDisposable += binding.buttonCta.throttledClicks()
            .subscribeBy(onNext = {
                onCtaClick()
                analytics.logEvent(AnalyticsEvents.SwapErrorDialogCtaClicked)
                dismiss()
            })
        clicksDisposable += binding.buttonDismiss.throttledClicks()
            .subscribeBy(onNext = {
                analytics.logEvent(AnalyticsEvents.SwapErrorDialogDismissClicked)
                onDismissClick()
                dismiss()
            })
    }

    companion object {
        private const val ARG_CONTENT = "arg_content"
        fun newInstance(content: Content): PitEmailVerifiedBottomDialog {
            return PitEmailVerifiedBottomDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CONTENT, content)
                }
            }
        }
    }
}