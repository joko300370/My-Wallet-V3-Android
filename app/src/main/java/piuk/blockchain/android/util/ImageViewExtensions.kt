package piuk.blockchain.android.util

import android.content.res.ColorStateList
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