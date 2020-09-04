package piuk.blockchain.android.ui.transfer.send.activity

import com.blockchain.transactions.SendDetails
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails

data class SendConfirmationDetails(
    val sendDetails: SendDetails,
    val fees: CryptoValue,
    val fiatAmount: FiatValue,
    val fiatFees: FiatValue
) {
    val from: AccountReference = sendDetails.from
    val to: String = sendDetails.toAddress
    val toLabel: String = sendDetails.toLabel
    val amount: CryptoValue = sendDetails.value

    val total = amount + fees
    val fiatTotal = fiatAmount + fiatFees

    internal fun toPaymentConfirmationDetails(): PaymentConfirmationDetails {
        return PaymentConfirmationDetails(
            fromLabel = from.label,
            toLabel = if (toLabel.isBlank()) to else toLabel,
            crypto = amount.currency,
            cryptoAmount = amount.toStringWithoutSymbol(),
            cryptoFee = fees.toStringWithoutSymbol(),
            cryptoTotal = total.toStringWithoutSymbol(),

            fiatUnit = fiatAmount.currencyCode,
            fiatAmount = fiatAmount.toStringWithoutSymbol(),
            fiatFee = fiatFees.toStringWithoutSymbol(),
            fiatTotal = fiatTotal.toStringWithoutSymbol()
        )
    }
}
