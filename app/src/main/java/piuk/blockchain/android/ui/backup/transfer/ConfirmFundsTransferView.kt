package piuk.blockchain.android.ui.backup.transfer

import androidx.annotation.StringRes
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

interface ConfirmFundsTransferView : View {

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun updateFromLabel(label: String)

    fun updateTransferAmountBtc(amount: String)

    fun updateTransferAmountFiat(amount: String)

    fun updateFeeAmountBtc(amount: String)

    fun updateFeeAmountFiat(amount: String)

    fun dismissDialog()

    fun sendBroadcast(event: ActionEvent)

    fun getIfArchiveChecked(): Boolean

    fun showProgressDialog()

    fun hideProgressDialog()

    fun onUiUpdated()
}