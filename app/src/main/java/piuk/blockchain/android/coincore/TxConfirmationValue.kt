package piuk.blockchain.android.coincore

import com.blockchain.annotations.CommonCode
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import piuk.blockchain.android.ui.transactionflow.flow.FeeInfo

sealed class TxConfirmationValue(open val confirmation: TxConfirmation) {

    data class ExchangePriceConfirmation(val money: Money, val asset: CryptoCurrency) :
        TxConfirmationValue(TxConfirmation.READ_ONLY)

    data class NewExchangePriceConfirmation(val money: Money, val asset: CryptoCurrency) :
        TxConfirmationValue(TxConfirmation.EXPANDABLE_SIMPLE_READ_ONLY)

    data class From(val from: String) : TxConfirmationValue(TxConfirmation.READ_ONLY)

    data class NewFrom(val sourceAccount: BlockchainAccount, val sourceAsset: CryptoCurrency) :
        TxConfirmationValue(TxConfirmation.SIMPLE_READ_ONLY)

    data class NewSale(val amount: Money, val exchange: Money) :
        TxConfirmationValue(TxConfirmation.COMPLEX_READ_ONLY)

    data class To(val to: String) : TxConfirmationValue(TxConfirmation.READ_ONLY)

    data class NewTo(
        val txTarget: TransactionTarget,
        val assetAction: AssetAction,
        val sourceAccount: BlockchainAccount? = null
    ) : TxConfirmationValue(TxConfirmation.SIMPLE_READ_ONLY)

    data class Total(val total: Money, val exchange: Money? = null) : TxConfirmationValue(TxConfirmation.READ_ONLY)

    data class NewTotal(val totalWithFee: Money, val exchange: Money) :
        TxConfirmationValue(TxConfirmation.COMPLEX_READ_ONLY)

    data class FiatTxFee(val fee: Money) : TxConfirmationValue(TxConfirmation.READ_ONLY)

    object EstimatedDepositCompletion : TxConfirmationValue(TxConfirmation.READ_ONLY)

    object EstimatedWithdrawalCompletion : TxConfirmationValue(TxConfirmation.READ_ONLY)

    @CommonCode("This structure is repeated in non-confirmation FEeSelection. They should be merged")
    data class FeeSelection(
        val feeDetails: FeeState? = null,
        val exchange: Money? = null,
        val selectedLevel: FeeLevel,
        val customFeeAmount: Long = -1L,
        val availableLevels: Set<FeeLevel> = emptySet(),
        val feeInfo: FeeLevelRates? = null,
        val asset: CryptoCurrency
    ) : TxConfirmationValue(TxConfirmation.FEE_SELECTION)

    data class BitPayCountdown(
        val timeRemainingSecs: Long
    ) : TxConfirmationValue(TxConfirmation.INVOICE_COUNTDOWN)

    data class ErrorNotice(val status: ValidationState, val money: Money? = null) :
        TxConfirmationValue(TxConfirmation.ERROR_NOTICE)

    data class Description(val text: String = "") : TxConfirmationValue(TxConfirmation.DESCRIPTION)

    data class Memo(val text: String?, val isRequired: Boolean, val id: Long?, val editable: Boolean = true) :
        TxConfirmationValue(TxConfirmation.MEMO)

    data class NetworkFee(
        val txFee: TxFee
    ) : TxConfirmationValue(TxConfirmation.NETWORK_FEE)

    data class NewNetworkFee(
        val feeAmount: Money,
        val exchange: Money,
        val asset: CryptoCurrency
    ) : TxConfirmationValue(TxConfirmation.EXPANDABLE_COMPLEX_READ_ONLY)

    data class CompoundNetworkFee(
        val sendingFeeInfo: FeeInfo? = null,
        val receivingFeeInfo: FeeInfo? = null,
        val feeLevel: FeeLevel? = null,
        val ignoreErc20LinkedNote: Boolean = false
    ) : TxConfirmationValue(TxConfirmation.COMPOUND_EXPANDABLE_READ_ONLY)

    data class NewSwapExchange(
        val unitCryptoCurrency: Money,
        val price: Money
    ) : TxConfirmationValue(TxConfirmation.EXPANDABLE_COMPLEX_READ_ONLY)

    data class TxBooleanConfirmation<T>(
        override val confirmation: TxConfirmation,
        val data: T? = null,
        val value: Boolean = false
    ) : TxConfirmationValue(confirmation)

    data class SwapSourceValue(val swappingAssetValue: CryptoValue) : TxConfirmationValue(TxConfirmation.READ_ONLY)

    data class SwapDestinationValue(val receivingAssetValue: CryptoValue) :
        TxConfirmationValue(TxConfirmation.READ_ONLY)
}