package piuk.blockchain.android.ui.tour

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import kotlinx.android.synthetic.main.tour_guide_overlay.view.*
import android.widget.FrameLayout
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.DashboardPrefs
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import piuk.blockchain.android.R
import java.lang.IllegalStateException
import kotlin.math.max

typealias TriggerClick = () -> Unit

data class IntroTourStep(
    val name: String,
    val lookupTriggerView: () -> View?,
    val triggerClick: TriggerClick? = null,
    val analyticsEvent: IntroTourAnalyticsEvent? = null,
    @DrawableRes val msgIcon: Int,
    @StringRes val msgTitle: Int,
    @StringRes val msgBody: Int,
    @StringRes val msgButton: Int
)

interface IntroTourHost {
    fun showTourDialog(dlg: BottomSheetDialogFragment)
    fun onTourFinished()
}

class TourGuide @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), KoinComponent {

    private val prefs: DashboardPrefs by inject()
    private val analytics: Analytics by inject()

    private var host: IntroTourHost? = null
    private val steps: MutableList<IntroTourStep> = mutableListOf()
    private var currentStepIndex = -1

    private val currentStep: IntroTourStep
        get() = steps[currentStepIndex]

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.tour_guide_overlay, this, true)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        touch_hook.setOnClickListener { onClick(true) }
        tour_overlay.setOnClickListener { onClick(false) }
    }

    private fun onClick(pulseClicked: Boolean) {
        if (pulseClicked) {
            val item = currentStep

            pulse.stop()
            item.triggerClick?.invoke() ?: item.lookupTriggerView.invoke()?.performClick()

            val dlg = TourBottomDialog.newInstance(
                TourBottomDialog.Content(
                    iconId = item.msgIcon,
                    titleText = item.msgTitle,
                    bodyText = item.msgBody,
                    btnText = item.msgButton,
                    onBtnClick = {
                        nextStep()
                    }
                )
            )
            host?.showTourDialog(dlg)

            item.analyticsEvent?.let {
                analytics.logEvent(it)
            }
        } else {
            onTourCanceled()
        }
    }

    // Creates the pulse view to be drawn directly over the specified view on the
    // TourGuide frame. The pulse will be square, having the same height as the target view
    // and centered on the center of the target view.
    // In some cases, specifically on menu items, this may not be where we want it to appear
    // in which case offsetX and offsetY can be specified to more the center point of the pulse.
    private fun overlayView(v: View, offsetX: Int = 0, offsetY: Int = 0) {
        val out = IntArray(2)

        getLocationOnScreen(out)

        val (x0, y0) = out // Parent offsets

        v.getLocationOnScreen(out)

        val (xV, yV) = out // overlay location

        // Size of the pulse:
        val scale = 1.75f
        val heightPulse = v.height * scale
        val widthPulse = heightPulse

        // Location of the pulse
        val x = x0 + xV + v.width / 2 - widthPulse / 2
        val y = y0 + yV + v.height / 2 - heightPulse / 2

        val pulseParams = LayoutParams(widthPulse.toInt(), heightPulse.toInt())

        pulseParams.leftMargin = x.toInt() + offsetX
        pulseParams.topMargin = y.toInt() + offsetY

        pulse.stop()
        pulse.layoutParams = pulseParams

        // Do we need to set a larger bounds area? For ie menus etc
        // If the target view extends beyond the pulse in x or y directions, then expand the touchable view to cover
        // the target view.

        val dW: Int = (max(v.width - widthPulse, 0f) / 2).toInt()
        val dH: Int = (max(v.height - heightPulse, 0f) / 2).toInt()

        val touchWidth = widthPulse.toInt() + dW * 2

        val touchParams = LayoutParams(touchWidth, heightPulse.toInt() + dH * 2)
        touchParams.leftMargin = x.toInt() - dW
        touchParams.topMargin = y.toInt() - dH
        touch_hook.layoutParams = touchParams

        pulse.start()
    }

    private fun onTourCanceled() {
        stop()
        host?.onTourFinished()
    }

    fun start(host: IntroTourHost, tourSteps: List<IntroTourStep>) {
        this.host = host
        this.steps.clear()
        this.steps.addAll(tourSteps)

        currentStepIndex = getCurrentStep()

        visibility = View.VISIBLE
        nextStep()
    }

    private fun getCurrentStep(): Int {
        if (prefs.isTourComplete) {
            throw IllegalStateException("Tour has already completed")
        }

        val stage = prefs.tourStage
        if (stage.isEmpty()) return -1

        // If we can't find the correct stage, return -1 and restart the tour
        return max(steps.indexOfFirst { it.name == stage } - 1, -1)
    }

    fun stop() {
        pulse.stop()
        visibility = View.GONE
    }

    private fun nextStep() {
        ++currentStepIndex

        if (currentStepIndex >= steps.size) {
            stop()
            prefs.setTourComplete()
            host?.onTourFinished()
        } else {
            prefs.setTourStage(currentStep.name)
            val v = currentStep.lookupTriggerView.invoke()
            if (v != null) {
                overlayView(v)
            } else {
                // We will be called back with a view, so switch off the pulse
                pulse.stop()
            }
        }
    }

    fun setDeferredTriggerView(v: View, offsetX: Int = 0, offsetY: Int = 0) {
        if (isActive) {
            overlayView(v, offsetX, offsetY)
        }
    }

    val isActive: Boolean
        get() = visibility == View.VISIBLE
}
