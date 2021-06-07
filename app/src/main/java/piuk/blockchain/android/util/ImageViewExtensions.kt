package piuk.blockchain.android.util

import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import piuk.blockchain.android.R

fun ImageView.setImageDrawable(@DrawableRes res: Int) {
    setImageDrawable(AppCompatResources.getDrawable(context, res))
}

fun ImageView.setAssetIconColours(@ColorRes tintColor: Int, @ColorRes filterColor: Int) {
    setBackgroundResource(R.drawable.bkgd_tx_circle)
    ViewCompat.setBackgroundTintList(this,
        ColorStateList.valueOf(ContextCompat.getColor(context, tintColor)))
    setColorFilter(ContextCompat.getColor(context, filterColor))
}

fun ImageView.setTransactionHasFailed() =
    this.apply {
        setImageResource(R.drawable.ic_close)
        setAssetIconColours(
            tintColor = R.color.red_200,
            filterColor = R.color.red_600
        )
    }

fun ImageView.setTransactionIsConfirming() =
    this.apply {
        setImageResource(R.drawable.ic_tx_confirming)
        background = null
        setColorFilter(Color.TRANSPARENT)
    }