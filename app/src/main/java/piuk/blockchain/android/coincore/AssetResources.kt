package piuk.blockchain.android.coincore

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import info.blockchain.balance.CryptoCurrency

interface AssetResources {
    fun getDisplayName(cryptoCurrency: CryptoCurrency): String

    @ColorRes
    fun colorRes(cryptoCurrency: CryptoCurrency): Int

    @ColorInt
    fun chartLineColour(cryptoCurrency: CryptoCurrency, context: Context): Int

    @DrawableRes
    fun drawableResFilled(cryptoCurrency: CryptoCurrency): Int

    @DrawableRes
    fun coinIconWhite(cryptoCurrency: CryptoCurrency): Int

    @DrawableRes
    fun maskedAsset(cryptoCurrency: CryptoCurrency): Int

    @DrawableRes
    fun errorIcon(cryptoCurrency: CryptoCurrency): Int

    @StringRes
    fun assetNameRes(cryptoCurrency: CryptoCurrency): Int

    fun assetName(cryptoCurrency: CryptoCurrency): String

    @ColorRes
    fun assetTint(cryptoCurrency: CryptoCurrency): Int

    @ColorRes
    fun assetFilter(cryptoCurrency: CryptoCurrency): Int

    fun makeBlockExplorerUrl(cryptoCurrency: CryptoCurrency, transactionHash: String): String

    fun numOfDecimalsForChart(cryptoCurrency: CryptoCurrency): Int
}