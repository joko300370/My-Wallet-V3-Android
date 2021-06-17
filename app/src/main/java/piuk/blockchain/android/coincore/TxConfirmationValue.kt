package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import piuk.blockchain.android.ui.transactionflow.flow.FeeInfo

sealed class TxConfirmationValue(open val confirmation: TxConfirmation) {

    data class ExchangePriceConfirmation(val money: Money, val asset: CryptoCurrency) :
        TxConfirmationValue(TxConfirmation.EXPANDABLE_SIMPLE_READ_ONLY)

    data class From(val sourceAccount: BlockchainAccount, val sourceAsset: CryptoCurrency? = null) :
        TxConfirmationValue(TxConfirmation.SIMPLE_READ_ONLY)

    data class PaymentMethod(
        val paymentTitle: String,
        val paymentSubtitle: String,
        val accountType: String?,
        val assetAction: AssetAction
    ) : TxConfirmationValue(TxConfirmation.COMPLEX_READ_ONLY)

    data class Sale(val amount: Money, val exchange: Money) :
        TxConfirmationValue(TxConfirmation.COMPLEX_READ_ONLY)

    data class To(
        val txTarget: TransactionTarget,
        val assetAction: AssetAction,
        val sourceAccount: BlockchainAccount? = null
    ) : TxConfirmationValue(TxConfirmation.SIMPLE_READ_ONLY)

    data class Total(val totalWithFee: Money, val exchange: Money) :
        TxConfirmationValue(TxConfirmation.COMPLEX_READ_ONLY)

    data class Amount(val amount: Money, val isImportant: Boolean) :
        TxConfirmationValue(TxConfirmation.SIMPLE_READ_ONLY)

    object EstimatedCompletion : TxConfirmationValue(TxConfirmation.SIMPLE_READ_ONLY)

    data class BitPayCountdown(
        val timeRemainingSecs: Long
    ) : TxConfirmationValue(TxConfirmation.INVOICE_COUNTDOWN)

    data class ErrorNotice(val status: ValidationState, val money: Money? = null) :
        TxConfirmationValue(TxConfirmation.ERROR_NOTICE)

    data class Description(val text: String = "") : TxConfirmationValue(TxConfirmation.DESCRIPTION)

    data class Memo(val text: String?, val isRequired: Boolean, val id: Long?, val editable: Boolean = true) :
        TxConfirmationValue(TxConfirmation.MEMO)

    data class NetworkFee(
        val feeAmount: Money,
        val exchange: Money,
        val asset: CryptoCurrency
    ) : TxConfirmationValue(TxConfirmation.EXPANDABLE_COMPLEX_READ_ONLY)

    data class TransactionFee(
        val feeAmount: Money
    ) : TxConfirmationValue(TxConfirmation.SIMPLE_READ_ONLY)

    data class CompoundNetworkFee(
        val sendingFeeInfo: FeeInfo? = null,
        val receivingFeeInfo: FeeInfo? = null,
        val feeLevel: FeeLevel? = null,
        val ignoreErc20LinkedNote: Boolean = false
    ) : TxConfirmationValue(TxConfirmation.COMPOUND_EXPANDABLE_READ_ONLY)

    data class SwapExchange(
        val unitCryptoCurrency: Money,
        val price: Money
    ) : TxConfirmationValue(TxConfirmation.EXPANDABLE_COMPLEX_READ_ONLY)

    data class TxBooleanConfirmation<T>(
        override val confirmation: TxConfirmation,
        val data: T? = null,
        val value: Boolean = false
    ) : TxConfirmationValue(confirmation)
}