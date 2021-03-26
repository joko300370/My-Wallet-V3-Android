package piuk.blockchain.android.ui.shortcuts.receive

import android.content.Intent
import android.graphics.Bitmap
import androidx.annotation.StringRes
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.ui.base.View

internal interface ReceiveQrView : View {

    val pageIntent: Intent

    fun finishActivity()

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun setAddressLabel(label: String)

    fun setAddressInfo(addressInfo: String)

    fun setImageBitmap(bitmap: Bitmap)

    fun showClipboardWarning(receiveAddressString: String)
}
