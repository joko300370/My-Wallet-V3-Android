package piuk.blockchain.android.ui.send

import android.support.annotation.ColorRes
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import com.blockchain.transactions.Memo
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.send.external.SendConfirmationDetails
import piuk.blockchain.android.ui.send.external.BaseSendView

interface SendView : BaseSendView {

    fun setSelectedCurrency(cryptoCurrency: CryptoCurrency)

    fun updateReceivingHintAndAccountDropDowns(currency: CryptoCurrency, listSize: Int, pitAddressAvailable: Boolean)

    // Update field
    fun updateSendingAddress(label: String)

    fun updateCryptoAmount(cryptoValue: CryptoValue, silent: Boolean = false)

    fun updateFiatAmount(fiatValue: FiatValue, silent: Boolean = false)

    fun updateWarning(message: String)

    fun updateMaxAvailable(maxAmount: String)

    fun updateMaxAvailable(maxAmount: CryptoValue, min: CryptoValue)

    fun updateMaxAvailableColor(@ColorRes color: Int)

    fun updateReceivingAddress(address: String)

    fun updateFeeAmount(feeCrypto: CryptoValue, feeFiat: FiatValue)

    fun clearFeeAmount()

    fun clearAmount()

    // Set property
    fun setCryptoMaxLength(length: Int)

    fun setFeePrioritySelection(index: Int)

    fun clearWarning()

    // Hide / Show
    fun showMaxAvailable()

    fun hideMaxAvailable()

    fun showFeePriority()

    fun hideFeePriority()

    fun showMemo()

    fun hideMemo()

    fun displayMemo(usersMemo: Memo)

    fun showInfoLink()

    fun hideInfoLink()

    // Enable / Disable
    fun enableFeeDropdown()

    fun disableFeeDropdown()

    fun setSendButtonEnabled(enabled: Boolean)

    fun disableInput()

    fun showBitPayTimerAndMerchantInfo(expiry: String, merchantName: String)

    fun enableInput()

    // Fetch value
    fun getCustomFeeValue(): Long

    fun getClipboardContents(): String?

    fun getReceivingAddress(): String?

    fun getFeePriority(): Int

    // Prompts
    fun showSnackbar(@StringRes message: Int, duration: Int)

    fun showSnackbar(message: String, extraInfo: String?, duration: Int)

    fun showEthContractSnackbar()

    fun showBIP38PassphrasePrompt(scanData: String)

    fun showWatchOnlyWarning(address: String)

    fun showSpendFromWatchOnlyWarning(address: String)

    fun showSecondPasswordDialog()

    fun showPaymentDetails(
        confirmationDetails: PaymentConfirmationDetails,
        note: String?,
        noteDescription: String?,
        allowFeeChange: Boolean
    )

    fun showPaymentDetails(confirmationDetails: SendConfirmationDetails)

    fun showLargeTransactionWarning()

    fun showTransactionSuccess(cryptoCurrency: CryptoCurrency)

    fun showTransactionFailed() = showSnackbar(R.string.transaction_failed, Snackbar.LENGTH_LONG)

    fun showInsufficientGasDlg()

    fun dismissConfirmationDialog()

    fun finishPage()

    fun hideCurrencyHeader()

    fun updateRequiredLabelVisibility(isVisible: Boolean)

    fun isPitEnabled(enabled: Boolean)
}

internal fun SendConfirmationDetails.toPaymentConfirmationDetails(): PaymentConfirmationDetails {
    return PaymentConfirmationDetails().also {
        it.fromLabel = from.label
        it.toLabel = if (toLabel.isBlank()) to else toLabel

        it.cryptoUnit = amount.symbol()
        it.cryptoAmount = amount.toStringWithoutSymbol()
        it.cryptoFee = fees.toStringWithoutSymbol()
        it.cryptoTotal = total.toStringWithoutSymbol()

        it.fiatUnit = fiatAmount.currencyCode
        it.fiatSymbol = fiatAmount.symbol()
        it.fiatAmount = fiatAmount.toStringWithoutSymbol()
        it.fiatFee = fiatFees.toStringWithoutSymbol()
        it.fiatTotal = fiatTotal.toStringWithoutSymbol()
    }
}
