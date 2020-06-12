package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.text.Editable
import android.text.Selection
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import piuk.blockchain.androidcoreui.utils.helperfunctions.AfterTextChangedWatcher
import kotlin.properties.Delegates

class PrefixedOrSuffixedEditText : AppCompatEditText {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle)

    init {
        addTextChangedListener(object : AfterTextChangedWatcher() {
            override fun afterTextChanged(s: Editable?) {
                prefix?.let {
                    if (!s.toString().startsWith(it)) {
                        setText(it)
                    }
                }
                suffix?.let {
                    if (!s.toString().endsWith(it)) {
                        setText(it)
                    }
                }
            }
        })
        setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && majorValue.toDoubleOrNull() == 0.toDouble()) {
                setText(text.toString().replace(digitsOnlyRegex, ""))
            }
        }
        isEnabled = false
    }

    private val digitsOnlyRegex by lazy {
        ("[\\d.]").toRegex()
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        suffix?.let {
            if (selEnd > text.toString().length - it.length) Selection.setSelection(text, 0)
        }
        prefix?.let {
            if (selEnd < it.length) Selection.setSelection(text, text.toString().length)
        }
    }

    internal val majorValue: String
        get() = text.toString().removePrefix(prefix ?: "").removeSuffix(suffix ?: "")

    private var prefix: String? = null

    private var suffix: String? = null

    internal var configuration: Configuration by Delegates.observable(Configuration()) { _, oldValue, newValue ->
        if (newValue != oldValue) {
            if (newValue.isPrefix) {
                suffix = null
                prefix = newValue.prefixOrSuffix
                setText(prefix.plus(newValue.initialText))
                Selection.setSelection(text, text.toString().length)
                suffix = null
            } else {
                prefix = null
                suffix = newValue.prefixOrSuffix
                setText(newValue.initialText.plus(suffix))
                Selection.setSelection(text, 0)
            }
            isEnabled = true
        }
    }
}

internal data class Configuration(
    val prefixOrSuffix: String = "",
    val isPrefix: Boolean = true,
    val initialText: String = ""
)