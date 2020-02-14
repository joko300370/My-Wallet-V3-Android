package piuk.blockchain.android.ui.auth

import android.content.Intent
import androidx.annotation.StringRes
import android.widget.ImageView
import java.util.Locale
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.android.util.DialogButtonCallback
import piuk.blockchain.androidcoreui.utils.ViewUtils

interface PinEntryView : View {

    val pageIntent: Intent?

    val pinBoxList: List<ImageView>

    val locale: Locale

    fun showProgressDialog(@StringRes messageId: Int, suffix: String?)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun dismissProgressDialog()

    fun showMaxAttemptsDialog()

    fun showValidationDialog()

    fun showCommonPinWarning(callback: DialogButtonCallback)

    fun showWalletVersionNotSupportedDialog(walletVersion: String?)

    fun goToUpgradeWalletActivity()

    fun restartPageAndClearTop()

    fun setTitleString(@StringRes title: Int)

    fun setTitleVisibility(@ViewUtils.Visibility visibility: Int)

    fun clearPinBoxes()

    fun goToPasswordRequiredActivity()

    fun finishWithResultOk(pin: String)

    fun showFingerprintDialog(pincode: String)

    fun showKeyboard()

    fun showAccountLockedDialog()

    fun showMobileNotice(mobileNoticeDialog: MobileNoticeDialog)

    fun appNeedsUpgrade(isForced: Boolean)

    fun showTestnetWarning()

    fun restartAppWithVerifiedPin()
}
