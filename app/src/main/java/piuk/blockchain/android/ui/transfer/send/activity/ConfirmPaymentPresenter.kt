package piuk.blockchain.android.ui.transfer.send.activity

import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.base.UiState

class ConfirmPaymentPresenter : BasePresenter<ConfirmPaymentView>() {

    override fun onViewReady() {
        val paymentDetails = view.paymentDetails

        if (paymentDetails == null) {
            view.closeDialog()
            return
        }

        val contactNote = view.contactNote
        if (contactNote != null) {
            view.contactNote = contactNote
        }

        val contactNoteDescription = view.contactNoteDescription
        if (contactNoteDescription != null) {
            view.contactNoteDescription = contactNoteDescription
        }

        view.setFromLabel(paymentDetails.fromLabel)
        view.setToLabel(paymentDetails.toLabel)
        view.setAmount(
            String.format(
                AMOUNT_FORMAT,
                paymentDetails.cryptoAmount,
                paymentDetails.crypto.displayTicker,
                paymentDetails.fiatSymbol,
                paymentDetails.fiatAmount
            )
        )
        view.setFee(
            String.format(
                AMOUNT_FORMAT,
                paymentDetails.cryptoFee,
                paymentDetails.cryptoFeeUnit,
                paymentDetails.fiatSymbol,
                paymentDetails.fiatFee
            )
        )

        if (paymentDetails.showCryptoTotal) {
            view.setTotals(
                paymentDetails.cryptoTotal + " " + paymentDetails.crypto.displayTicker,
                paymentDetails.fiatTotal
            )
        } else {
            view.setFiatTotalOnly(paymentDetails.fiatTotal)
        }

        if (!paymentDetails.warningText.isEmpty()) {
            view.setWarning(paymentDetails.warningText)
            view.setWarningSubText(paymentDetails.warningSubtext)
        }

        view.setUiState(UiState.CONTENT)
    }

    companion object {
        private const val AMOUNT_FORMAT = "%1\$s %2\$s (%3\$s%4\$s)"
    }
}
