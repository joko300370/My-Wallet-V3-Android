package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatTextView
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedDrawable

class StatusPill(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {

    private var viewType: StatusType = StatusType.UPSELL

    init {
        setupView(context, attrs)
        updateView(viewType)
    }

    fun update(text: String, type: StatusType) {
        this.text = text
        updateView(type)
    }

    private fun updateView(type: StatusType) =
        when (type) {
            StatusType.UPSELL -> setupUpsell()
            StatusType.WARNING -> setupWarning()
            StatusType.INFO -> setupInfo()
            StatusType.ERROR -> setupError()
            StatusType.LABEL -> setupLabel()
        }

    private fun setupLabel() {
        setupStyle(R.style.Text_Semibold_12)
        background = context.getResolvedDrawable(R.drawable.bkgd_grey_100_rounded)
        setTextColor(context.getResolvedColor(R.color.grey_600))
    }

    private fun setupError() {
        setupStyle(R.style.Text_Error)
        background = context.getResolvedDrawable(R.drawable.rounded_error_text_background)
    }

    private fun setupInfo() {
        setupStyle(R.style.Text_Info)
        background = context.getResolvedDrawable(R.drawable.rounded_info_text_bkgd)
    }

    private fun setupWarning() {
        setupStyle(R.style.Text_Semibold_12)
        background = context.getResolvedDrawable(R.drawable.bkgd_orange_100_rounded)
        setTextColor(context.getResolvedColor(R.color.orange_600))
    }

    private fun setupUpsell() {
        setupStyle(R.style.Text_Semibold_12)
        background = context.getResolvedDrawable(R.drawable.bkgd_green_100_rounded)
        setTextColor(context.getResolvedColor(R.color.green_600))
    }

    private fun setupStyle(@StyleRes style: Int) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            setTextAppearance(context, style)
        } else {
            setTextAppearance(style)
        }
    }

    private fun setupView(context: Context, attrs: AttributeSet) {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.StatusPill, 0, 0)

        viewType = attributes.getEnum(R.styleable.StatusPill_status_type, StatusType.UPSELL)

        attributes.recycle()
    }

    enum class StatusType {
        UPSELL,
        WARNING,
        INFO,
        ERROR,
        LABEL
    }

    private inline fun <reified T : Enum<T>> TypedArray.getEnum(index: Int, default: T) =
        getInt(index, 0).let { if (it >= 0) enumValues<T>()[it] else default }
}