package piuk.blockchain.android.ui.customviews

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import info.blockchain.wallet.util.PasswordUtil
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewPasswordStrengthBinding
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedDrawable
import kotlin.math.roundToInt

class PasswordStrengthView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private val binding: ViewPasswordStrengthBinding =
        ViewPasswordStrengthBinding.inflate(LayoutInflater.from(context), this, true)

    private val strengthVerdicts = intArrayOf(
        R.string.strength_weak,
        R.string.strength_medium,
        R.string.strength_normal,
        R.string.strength_strong
    )

    private val strengthProgressDrawables = intArrayOf(
        R.drawable.progress_red,
        R.drawable.progress_orange,
        R.drawable.progress_blue,
        R.drawable.progress_green
    )

    private val strengthColors = intArrayOf(
        R.color.product_red_medium,
        R.color.product_orange_medium,
        R.color.primary_blue_medium,
        R.color.product_green_medium
    )

    init {
        binding.passStrengthBar.max = 100
    }

    // TODO make other methods private and only expose the update method - unify how we define password strengths
    fun updatePasswordStrength(password: String) {
        val passwordStrength = PasswordUtil.getStrength(password).roundToInt()
        setStrengthProgress(passwordStrength)

        when (passwordStrength) {
            in 0..25 -> updateLevelUI(0)
            in 26..50 -> updateLevelUI(1)
            in 51..75 -> updateLevelUI(2)
            in 76..100 -> updateLevelUI(3)
        }
    }

    fun setStrengthProgress(score: Int) {
        with(binding.passStrengthBar) {
            ObjectAnimator.ofInt(
                this,
                "progress",
                this.progress,
                score * 10
            ).apply {
                duration = 300
                interpolator = DecelerateInterpolator()
                start()
            }
        }
    }

    fun updateLevelUI(level: Int) {
        with(binding) {
            passStrengthVerdict.setText(strengthVerdicts[level])
            passStrengthBar.progressDrawable = context.getResolvedDrawable(strengthProgressDrawables[level])
            passStrengthVerdict.setText(strengthVerdicts[level])
            passStrengthVerdict.setTextColor(context.getResolvedColor(strengthColors[level]))
        }
    }
}