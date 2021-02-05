package piuk.blockchain.android.ui.backup.verify

import android.os.Bundle
import androidx.annotation.StringRes
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.ui.base.View

interface BackupVerifyView : View {

    fun getPageBundle(): Bundle?

    fun showProgressDialog()

    fun hideProgressDialog()

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun showCompletedFragment()

    fun showStartingFragment()

    fun showWordHints(hints: List<Int>)
}