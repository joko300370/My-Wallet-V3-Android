package piuk.blockchain.android.util

import android.widget.CheckBox

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