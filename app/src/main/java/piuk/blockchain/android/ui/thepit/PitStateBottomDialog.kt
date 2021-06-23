package piuk.blockchain.android.ui.thepit

import android.os.Bundle
import android.os.Parcelable
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.ui.extensions.throttledClicks
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.databinding.DialogSheetWalletMercuryLinkingBinding
import piuk.blockchain.android.ui.customviews.ErrorBottomDialog
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.goneIf
import piuk.blockchain.android.util.visible

class PitStateBottomDialog : ErrorBottomDialog<DialogSheetWalletMercuryLinkingBinding>() {
    @Parcelize
    data class StateContent(val content: Content, val isLoading: Boolean) : Parcelable

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isLoading = arguments?.getBoolean(ARG_LOADING, false) ?: false
        binding.stateLoading.goneIf(!isLoading)
        binding.dialogIcon.goneIf(isLoading)
    }

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

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetWalletMercuryLinkingBinding =
        DialogSheetWalletMercuryLinkingBinding.inflate(inflater, container, false)

    companion object {
        private const val ARG_CONTENT = "arg_content"
        private const val ARG_LOADING = "arg_loading"
        fun newInstance(stateContent: StateContent): PitStateBottomDialog {
            return PitStateBottomDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CONTENT, stateContent.content)
                    putBoolean(ARG_LOADING, stateContent.isLoading)
                }
            }
        }
    }
}