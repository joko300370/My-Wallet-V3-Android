package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import com.google.android.material.textfield.TextInputEditText

class KeyPreImeEditText : TextInputEditText {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    var keyImeChangeListener: KeyImeChange? = null

    interface KeyImeChange {
        fun onKeyIme(keyCode: Int, event: KeyEvent?)
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        keyImeChangeListener?.onKeyIme(keyCode, event)
        return false
    }
}