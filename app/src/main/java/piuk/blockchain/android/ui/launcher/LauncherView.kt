package piuk.blockchain.android.ui.launcher

import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.ui.base.View

interface LauncherView : View {

    fun getPageIntent(): Intent

    fun onNoGuid()

    fun onRequestPin()

    fun onCorruptPayload()

    fun onStartMainActivity(uri: Uri?, launchBuySellIntro: Boolean = false)

    fun launchEmailVerification()

    fun onReEnterPassword()

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun showMetadataNodeFailure()

    fun showSecondPasswordDialog()

    fun updateProgressVisibility(show: Boolean)
}
