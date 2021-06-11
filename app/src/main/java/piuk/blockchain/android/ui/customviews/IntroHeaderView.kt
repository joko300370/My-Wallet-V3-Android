package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewIntroHeaderBinding
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedDrawable

class IntroHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewIntroHeaderBinding =
        ViewIntroHeaderBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        setupView(context, attrs)
    }

    private fun setupView(context: Context, attrs: AttributeSet?) {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.IntroHeaderView, 0, 0)

        attributes?.let {
            val title = it.getString(R.styleable.IntroHeaderView_intro_header_title)
            val label = it.getString(R.styleable.IntroHeaderView_intro_header_label)
            val icon = it.getDrawable(R.styleable.IntroHeaderView_intro_header_icon)
            val showSeparator = it.getBoolean(R.styleable.IntroHeaderView_intro_header_separator, true)

            with(binding) {
                introHeaderTitle.text = title
                introHeaderLabel.text = label
                introHeaderIcon.setImageDrawable(icon)
                introHeaderSeparator.visibleIf { showSeparator }
            }
            attributes.recycle()
        }
    }

    fun setDetails(
        @StringRes title: Int,
        @StringRes label: Int,
        @DrawableRes icon: Int,
        showSeparator: Boolean = true
    ) {
        with(binding) {
            introHeaderTitle.text = context.getString(title)
            introHeaderLabel.text = context.getString(label)
            introHeaderIcon.setImageDrawable(context.getResolvedDrawable(icon))
            introHeaderSeparator.visibleIf { showSeparator }
        }
    }

    fun toggleBottomSeparator(visible: Boolean) {
        binding.introHeaderSeparator.visibleIf { visible }
    }
}