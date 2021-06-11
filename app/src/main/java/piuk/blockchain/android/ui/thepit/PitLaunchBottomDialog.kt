package piuk.blockchain.android.ui.thepit

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.ui.extensions.throttledClicks
import com.blockchain.ui.urllinks.URL_THE_PIT_LAUNCH_SUPPORT
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.PitLaunchBottomDialogBinding
import piuk.blockchain.android.ui.customviews.ErrorBottomDialog
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.launchUrlInBrowser
import piuk.blockchain.android.util.visible

class PitLaunchBottomDialog : ErrorBottomDialog<PitLaunchBottomDialogBinding>() {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): PitLaunchBottomDialogBinding =
        PitLaunchBottomDialogBinding.inflate(inflater, container, false)

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

        private fun newInstance(content: Content): PitLaunchBottomDialog {
            return PitLaunchBottomDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CONTENT, content)
                }
            }
        }

        fun launch(activity: FragmentActivity) {
            newInstance(
                Content(
                    title = activity.getString(R.string.the_exchange_title),
                    description = "",
                    ctaButtonText = R.string.launch_the_exchange,
                    dismissText = R.string.the_exchange_contact_support,
                    icon = R.drawable.ic_the_exchange_colour
                )
            ).apply {
                onCtaClick = { activity.launchUrlInBrowser(BuildConfig.PIT_LAUNCHING_URL) }
                onDismissClick = { activity.launchUrlInBrowser(URL_THE_PIT_LAUNCH_SUPPORT) }
                show(activity.supportFragmentManager, "BottomDialog")
            }
        }
    }
}
