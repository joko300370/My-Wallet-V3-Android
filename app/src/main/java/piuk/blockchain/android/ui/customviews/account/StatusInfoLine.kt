package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import piuk.blockchain.android.databinding.ViewStatusLineInfoBinding
import piuk.blockchain.android.util.visibleIf

class StatusInfoLine @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(ctx, attr, defStyle) {

    private val binding = ViewStatusLineInfoBinding.inflate(LayoutInflater.from(ctx), this, true)

    var status: String = ""
        set(value) {
            field = value
            refreshText()
        }

    var textColour: Int = -1
        set(value) {
            field = value
            refreshTextColour()
        }

    var background: Int = -1
        set(value) {
            field = value
            refreshBkgdColour()
        }

    private fun refreshTextColour() {
        binding.message.setTextColor(ContextCompat.getColor(context, textColour))
    }

    private fun refreshBkgdColour() {
        binding.itemAccountParent.background =
            ContextCompat.getDrawable(context, background)
    }

    var isIconVisible: Boolean = false
        set(value) {
            field = value
            refreshIconVisibility()
        }

    private fun refreshIconVisibility() {
        binding.icon.visibleIf { isIconVisible }
    }

    private fun refreshText() {
        binding.message.text = status
    }
}
