package piuk.blockchain.android.ui.send

import android.support.annotation.ColorRes
import android.support.annotation.Nullable
import android.support.annotation.StringRes
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.ui.send.external.ViewX
import java.util.Locale

interface SendView : ViewX {

    val locale: Locale

    // Update field
    fun updateSendingAddress(label: String)

    fun updateCryptoAmount(amountString: String?)

    fun updateFiatAmount(amountString: String?)

    fun updateWarning(message: String)

    fun updateMaxAvailable(maxAmount: String)

    fun updateMaxAvailableColor(@ColorRes color: Int)

    fun updateReceivingAddress(address: String)

    fun updateFeeAmount(fee: String)

    // Set property
    fun setCryptoMaxLength(length: Int)

    fun setFeePrioritySelection(index: Int)

    fun clearWarning()

    // Hide / Show
    fun showMaxAvailable()

    fun hideMaxAvailable()

    fun showFeePriority()

    fun hideFeePriority()

    // Enable / Disable
    fun disableCryptoTextChangeListener()

    fun enableCryptoTextChangeListener()

    fun disableFiatTextChangeListener()

    fun enableFiatTextChangeListener()

    fun enableFeeDropdown()

    fun disableFeeDropdown()

    fun setSendButtonEnabled(enabled: Boolean)

    fun disableInput()

    fun enableInput()

    // Fetch value
    fun getCustomFeeValue(): Long

    fun getClipboardContents(): String?

    fun getReceivingAddress(): String?

    fun getFeePriority(): Int

    // Prompts
    fun showSnackbar(@StringRes message: Int, duration: Int)

    fun showSnackbar(message: String, @Nullable extraInfo: String?, duration: Int)

    fun showEthContractSnackbar()

    fun showBIP38PassphrasePrompt(scanData: String)

    fun showWatchOnlyWarning(address: String)

    fun showProgressDialog(@StringRes title: Int)

    fun showSpendFromWatchOnlyWarning(address: String)

    fun showSecondPasswordDialog()

    fun showPaymentDetails(
        confirmationDetails: PaymentConfirmationDetails,
        note: String?,
        allowFeeChange: Boolean
    )

    fun showLargeTransactionWarning()

    fun showTransactionSuccess(
        hash: String,
        transactionValue: Long,
        cryptoCurrency: CryptoCurrency
    )

    fun dismissProgressDialog()

    fun dismissConfirmationDialog()

    fun finishPage()

    fun hideCurrencyHeader()
}
