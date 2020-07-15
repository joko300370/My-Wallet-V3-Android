package piuk.blockchain.android.ui.customviews.account

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_status_line_info.view.*
import piuk.blockchain.android.R

class StatusInfoLine @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(ctx, attr, defStyle) {

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.view_status_line_info, this, true)
    }

    var status: String = "This is a message"
        set(value) {
            field = value
            refreshUi()
        }

    private fun refreshUi() {
        message.text = status
    }
}
