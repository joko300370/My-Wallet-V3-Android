@file:JvmName("ViewExtensions")

package piuk.blockchain.androidcoreui.utils.extensions

import android.annotation.SuppressLint
import androidx.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import timber.log.Timber

/**
 * Sets the visibility of a [View] to [View.VISIBLE]
 */
fun View?.visible() {
    if (this != null) visibility = View.VISIBLE
}

fun View?.isVisible(): Boolean =
    this?.let { visibility == View.VISIBLE } ?: false

/**
 * Sets the visibility of a [View] to [View.INVISIBLE]
 */
fun View?.invisible() {
    if (this != null) visibility = View.INVISIBLE
}

/**
 * Sets the visibility of a [View] to [View.GONE]
 */
fun View?.gone() {
    if (this != null) visibility = View.GONE
}

/**
 * Sets the visibility of a [View] to [View.VISIBLE] depending on a value
 */
fun View?.visibleIf(func: () -> Boolean) {
    if (this != null) {
        visibility = if (func()) View.VISIBLE else View.GONE
    }
}

/**
 * Sets the visibility of a [View] to [View.GONE] depending on a predicate
 *
 * @param func If true, the visibility of the [View] will be set to [View.GONE], else [View.VISIBLE]
 */
fun View?.goneIf(func: () -> Boolean) {
    if (this != null) {
        visibility = if (func()) View.GONE else View.VISIBLE
    }
}

/**
 * Sets the visibility of a [View] to [View.GONE] depending on a value
 *
 * @param value If true, the visibility of the [View] will be set to [View.GONE], else [View.VISIBLE]
 */
fun View?.goneIf(value: Boolean) {
    if (this != null) {
        visibility = if (value) View.GONE else View.VISIBLE
    }
}

/**
 * Sets the visibility of a [View] to [View.INVISIBLE] depending on a predicate
 *
 * @param func If true, the visibility of the [View] will be set to [View.INVISIBLE], else [View.VISIBLE]
 */
fun View?.invisibleIf(func: () -> Boolean) {
    if (this != null) {
        visibility = if (func()) View.INVISIBLE else View.VISIBLE
    }
}

/**
 * Sets the visibility of a [View] to [View.INVISIBLE] depending on a value
 *
 * @param value If true, the visibility of the [View] will be set to [View.INVISIBLE], else [View.VISIBLE]
 */
fun View?.invisibleIf(value: Boolean) {
    if (this != null) {
        visibility = if (value) View.INVISIBLE else View.VISIBLE
    }
}

/**
 * Allows a [ViewGroup] to inflate itself without all of the unneeded ceremony of getting a
 * [LayoutInflater] and always passing the [ViewGroup] + false. True can optionally be passed if
 * needed.
 *
 * @param layoutId The layout ID as an [Int]
 * @return The inflated [View]
 */
fun ViewGroup.inflate(@LayoutRes layoutId: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutId, this, attachToRoot)
}

/**
 * Returns the current [String] entered into an [EditText]. Non-null, ie can return an empty String.
 */
fun EditText?.getTextString(): String {
    return this?.text?.toString() ?: ""
}

/**
 * This disables the soft keyboard as an input for a given [EditText]. The method
 * [EditText.setShowSoftInputOnFocus] is officially only available on >API21, but is actually hidden
 * from >API16. Here, we attempt to set that field to false, and catch any exception that might be
 * thrown if the Android implementation doesn't include it for some reason.
 */
@SuppressLint("NewApi")
fun EditText.disableSoftKeyboard() {
    try {
        showSoftInputOnFocus = false
    } catch (e: Exception) {
        Timber.e(e)
    }
}

/**
 * Returns a physics-based [SpringAnimation] for a given [View].
 *
 * @param property The [DynamicAnimation.ViewProperty] you wish to animate, such as rotation,
 * X or Y position etc.
 * @param finalPosition The end position for the [View] after animation complete
 * @param stiffness The stiffness of the animation, see [SpringForce]
 * @param dampingRatio The damping ratio of the animation, see [SpringForce]
 */
fun View.createSpringAnimation(
    property: DynamicAnimation.ViewProperty,
    finalPosition: Float,
    stiffness: Float,
    dampingRatio: Float
) = SpringAnimation(this, property).apply {
    spring = SpringForce(finalPosition).apply {
        this.stiffness = stiffness
        this.dampingRatio = dampingRatio
    }
}

/**
 * Debounced onClickListener
 *
 * Filter out fast double taps
 */
private class DebouncingOnClickListener(private val onClickListener: (View?) -> Unit) : View.OnClickListener {
    private var lastClick = 0L
    override fun onClick(v: View?) {
        val now = System.currentTimeMillis()
        if (now > lastClick + DEBOUNCE_TIMEOUT) {
            lastClick = now
            onClickListener(v)
        }
    }

    companion object {
        private const val DEBOUNCE_TIMEOUT = 500L
    }
}

fun View.setOnClickListenerDebounced(onClickListener: (View?) -> Unit) =
    this.setOnClickListener(DebouncingOnClickListener(onClickListener = onClickListener))

fun View.afterMeasured(f: (View) -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (measuredWidth > 0 && measuredHeight > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    f(this@afterMeasured)
                }
            }
        })
}