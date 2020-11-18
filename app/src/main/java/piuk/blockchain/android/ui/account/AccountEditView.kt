package piuk.blockchain.android.ui.account

import android.content.Intent
import android.graphics.Bitmap
import androidx.annotation.StringRes
import piuk.blockchain.androidcore.data.events.ActionEvent

import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

interface AccountEditView : View {

    val activityIntent: Intent

    fun promptAccountLabel(currentLabel: String?)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType type: String)

    fun setActivityResult(resultCode: Int)

    fun promptArchive(title: String, message: String)

    fun showXpubSharingWarning()

    fun showAddressDetails(
        heading: String?,
        note: String?,
        copy: String?,
        bitmap: Bitmap?,
        qrString: String?
    )

    fun showPaymentDetails(details: PaymentConfirmationDetails)

    fun showTransactionSuccess()

    fun showProgressDialog(@StringRes message: Int)

    fun dismissProgressDialog()

    fun sendBroadcast(event: ActionEvent)

    fun updateAppShortcuts()

    fun hideMerchantCopy()

    fun finishPage()
}
