package piuk.blockchain.android.ui.customviews

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.view_intro_header.view.*
import piuk.blockchain.android.R

class IntroHeaderView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    init {
        inflate(context, R.layout.view_intro_header, this)

        setupView(context, attrs)
    }

    private fun setupView(context: Context, attrs: AttributeSet) {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.IntroHeaderView, 0, 0)

        val title = attributes.getString(R.styleable.IntroHeaderView_intro_header_title)
        val label = attributes.getString(R.styleable.IntroHeaderView_intro_header_label)
        val icon = attributes.getDrawable(R.styleable.IntroHeaderView_intro_header_icon)

        intro_header_title.text = title
        intro_header_label.text = label
        intro_header_icon.setImageDrawable(icon)

        attributes.recycle()
    }

    fun setDetails(@StringRes title: Int, @StringRes label: Int, @DrawableRes icon: Int) {
        intro_header_title.text = context.getString(title)
        intro_header_label.text = context.getString(label)
        intro_header_icon.setImageDrawable(context.getDrawable(icon))
    }
}