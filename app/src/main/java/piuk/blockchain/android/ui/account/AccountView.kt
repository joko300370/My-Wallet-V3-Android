package piuk.blockchain.android.ui.account

import androidx.annotation.StringRes
import info.blockchain.wallet.payload.data.LegacyAddress
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import java.util.Locale

interface AccountView : View {

    val locale: Locale

    fun showProgressDialog(@StringRes message: Int)

    fun dismissProgressDialog()

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun showWatchOnlyWarningDialog(address: String)

    fun showRenameImportedAddressDialog(address: LegacyAddress)

    fun startScanForResult()

    fun showBip38PasswordDialog(data: String)

    fun updateAccountList(displayAccounts: List<AccountItem>)

    fun hideCurrencyHeader()

    fun showTransferFunds(sendingAccount: CryptoAccount, defaultAccount: SingleAccount)
}
