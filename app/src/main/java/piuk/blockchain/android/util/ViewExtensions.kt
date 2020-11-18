package piuk.blockchain.android.util

import android.graphics.Rect
import android.view.View
import android.widget.CheckBox
import kotlinx.android.synthetic.main.activity_scan.*

fun CheckBox.setThrottledCheckedChange(interval: Long = 500L, action: (Boolean) -> Unit) {
    var lastClickTime = 0L

    this.setOnCheckedChangeListener { view, isChecked ->
        if (System.currentTimeMillis() - lastClickTime > interval) {
            view.isChecked = isChecked
            action.invoke(isChecked)
            lastClickTime = System.currentTimeMillis()
        } else {
            view.isChecked = !isChecked
        }
    }
}

// In window/screen co-ordinates
val View.windowRect: Rect
    get() {
        val loc = IntArray(2)
        getLocationInWindow(loc)
        return Rect(loc[0], loc[1], loc[0] + width, loc[1] + height)
    }
