package piuk.blockchain.android.ui.upgrade

import androidx.annotation.StringRes
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.ui.base.View

internal interface UpgradeWalletView : View {

    fun showChangePasswordDialog()

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun onUpgradeStarted()

    fun onUpgradeCompleted()

    fun onUpgradeFailed()

    fun onBackButtonPressed()

    fun showProgressDialog(@StringRes message: Int)

    fun dismissProgressDialog()
}
