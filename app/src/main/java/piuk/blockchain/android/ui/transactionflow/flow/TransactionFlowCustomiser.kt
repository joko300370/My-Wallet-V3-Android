package piuk.blockchain.android.ui.transactionflow.flow

import android.content.res.Resources
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.isCustodial
import piuk.blockchain.android.ui.transactionflow.engine.TransactionErrorState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.util.assetName

interface TransactionFlowCustomiser {
    // UI Element text, icons etc may be customised here:
    fun selectSourceAddressTitle(state: TransactionState): String
    fun selectTargetAddressTitle(state: TransactionState): String
    fun selectTargetAddressInputHint(state: TransactionState): String
    fun selectTargetNoAddressMessageText(state: TransactionState): String?
    fun selectTargetShowManualEnterAddress(state: TransactionState): Boolean
    fun enterAmountTitle(state: TransactionState): String
    fun enterAmountActionIcon(state: TransactionState): Int
    fun enterAmountMaxButton(state: TransactionState): String
    fun confirmTitle(state: TransactionState): String
    fun confirmCtaText(state: TransactionState): String
    fun confirmListItemTitle(assetAction: AssetAction): String
    fun transactionProgressTitle(state: TransactionState): String
    fun transactionProgressMessage(state: TransactionState): String
    fun transactionCompleteTitle(state: TransactionState): String
    fun transactionCompleteMessage(state: TransactionState): String

    // Format those flash error messages:
    fun errorFlashMessage(state: TransactionState): String?
}

class TransactionFlowCustomiserImpl(
    private val resources: Resources
) : TransactionFlowCustomiser {
    override fun enterAmountActionIcon(state: TransactionState): Int {
        return when (state.action) {
            AssetAction.NewSend -> R.drawable.ic_tx_sent
            AssetAction.Deposit -> R.drawable.ic_tx_deposit_arrow
            // AssetAction.Swap -> resources.getString(R.string.common_swap)
            AssetAction.Sell -> R.drawable.ic_tx_sell
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun selectSourceAddressTitle(state: TransactionState): String = "Select Source Address"

    override fun selectTargetAddressInputHint(state: TransactionState): String =
        when (state.action) {
            AssetAction.NewSend -> resources.getString(
                R.string.send_enter_asset_address_hint,
                resources.getString(state.asset.assetName()))
            AssetAction.Sell -> ""
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }

    override fun selectTargetNoAddressMessageText(state: TransactionState): String? =
        when (state.action) {
            AssetAction.NewSend -> resources.getString(
                R.string.send_internal_transfer_message,
                resources.getString(state.asset.assetName()),
                state.asset.displayTicker
            )
            else -> null
        }

    override fun selectTargetAddressTitle(state: TransactionState): String =
        when (state.action) {
            AssetAction.NewSend -> resources.getString(R.string.common_send)
            AssetAction.Sell -> resources.getString(R.string.common_sell)
            AssetAction.Deposit -> resources.getString(R.string.common_transfer)
            // AssetAction.Swap -> resources.getString(R.string.common_swap)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }

    override fun selectTargetShowManualEnterAddress(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.NewSend -> !state.sendingAccount.isCustodial()
            else -> false
        }

    override fun enterAmountTitle(state: TransactionState): String {
        return when (state.action) {
            AssetAction.NewSend -> resources.getString(
                R.string.send_enter_amount_title, state.sendingAccount.asset.displayTicker
            )
            // AssetAction.Swap -> "Swap..."
            AssetAction.Deposit -> resources.getString(R.string.tx_title_deposit,
                state.sendingAccount.asset.displayTicker)
            AssetAction.Sell -> resources.getString(R.string.tx_title_sell,
                state.sendingAccount.asset.displayTicker)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun enterAmountMaxButton(state: TransactionState): String =
        when (state.action) {
            AssetAction.NewSend -> resources.getString(R.string.send_enter_amount_max)
            AssetAction.Deposit -> resources.getString(R.string.send_enter_amount_deposit_max)
            // AssetAction.Swap -> "Swap..."
            AssetAction.Sell -> resources.getString(R.string.sell_enter_amount_max)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }

    override fun confirmTitle(state: TransactionState): String {
        val amount = state.pendingTx?.amount?.toStringWithSymbol() ?: ""

        return when (state.action) {
            AssetAction.NewSend -> resources.getString(
                R.string.send_confirmation_title, amount
            )
            // AssetAction.Swap -> "Swap ${state.sendingAccount.asset.displayTicker}"
            AssetAction.Deposit -> resources.getString(R.string.common_confirm)
            AssetAction.Sell -> resources.getString(R.string.checkout)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun confirmCtaText(state: TransactionState): String {
        val amount = state.pendingTx?.amount?.toStringWithSymbol() ?: ""

        return when (state.action) {
            AssetAction.NewSend -> resources.getString(
                R.string.send_confirmation_cta_button, amount
            )
            // AssetAction.Swap -> "Execute Trade"
            AssetAction.Sell -> resources.getString(
                R.string.sell_confirmation_cta_button, amount
            )
            AssetAction.Deposit -> resources.getString(
                R.string.send_confirmation_deposit_cta_button)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun confirmListItemTitle(assetAction: AssetAction): String {
        return when (assetAction) {
            AssetAction.NewSend -> resources.getString(R.string.common_send)
            AssetAction.Deposit -> resources.getString(R.string.common_transfer)
            AssetAction.Sell -> resources.getString(R.string.common_sell)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun transactionProgressTitle(state: TransactionState): String {
        val amount = state.pendingTx?.amount?.toStringWithSymbol() ?: ""

        return when (state.action) {
            AssetAction.NewSend -> resources.getString(
                R.string.send_progress_sending_title, amount
            )
            // AssetAction.Swap -> "Execute Trade"
            AssetAction.Deposit -> resources.getString(R.string.send_confirmation_progress_title,
                amount)
            AssetAction.Sell -> resources.getString(R.string.sell_confirmation_progress_title,
                amount)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun transactionProgressMessage(state: TransactionState): String {
        return when (state.action) {
            AssetAction.NewSend -> resources.getString(R.string.send_progress_sending_subtitle)
            AssetAction.Deposit -> resources.getString(R.string.send_confirmation_progress_message,
                state.sendingAccount.asset.displayTicker)
            AssetAction.Sell -> resources.getString(R.string.sell_confirmation_progress_message)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun transactionCompleteTitle(state: TransactionState): String {
        val amount = state.pendingTx?.amount?.toStringWithSymbol() ?: ""

        return when (state.action) {
            AssetAction.NewSend -> resources.getString(
                R.string.send_progress_complete_title, amount
            )
            AssetAction.Sell ->
                resources.getString(
                    R.string.sell_progress_complete_title, state.pendingTx?.amount?.toStringWithSymbol()
                )
            AssetAction.Deposit -> resources.getString(R.string.send_confirmation_success_title,
                amount)
            // AssetAction.Swap -> "Execute Trade"
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun transactionCompleteMessage(state: TransactionState): String {
        return when (state.action) {
            AssetAction.NewSend -> resources.getString(
                R.string.send_progress_complete_subtitle, state.sendingAccount.asset.displayTicker
            )
            AssetAction.Deposit -> resources.getString(R.string.send_confirmation_success_message,
                state.sendingAccount.asset.displayTicker)
            AssetAction.Sell -> resources.getString(R.string.sell_confirmation_success_message,
                (state.selectedTarget as? FiatAccount)?.fiatCurrency)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun errorFlashMessage(state: TransactionState): String? =
        when (state.errorState) {
            TransactionErrorState.NONE -> null
            TransactionErrorState.INSUFFICIENT_FUNDS -> resources.getString(
                R.string.send_enter_amount_error_insufficient_funds,
                state.sendingAccount.asset.displayTicker
            )
            TransactionErrorState.INVALID_AMOUNT -> resources.getString(
                R.string.send_enter_amount_error_invalid_amount,
                state.sendingAccount.asset.displayTicker
            )
            TransactionErrorState.INVALID_ADDRESS -> resources.getString(
                R.string.send_error_not_valid_asset_address,
                state.sendingAccount.asset.displayTicker
            )
            TransactionErrorState.ADDRESS_IS_CONTRACT -> resources.getString(
                R.string.send_error_address_is_eth_contract)
            TransactionErrorState.INVALID_PASSWORD -> resources.getString(
                R.string.send_enter_invalid_password)
            TransactionErrorState.NOT_ENOUGH_GAS -> resources.getString(
                R.string.send_enter_insufficient_gas)
            TransactionErrorState.UNEXPECTED_ERROR -> resources.getString(
                R.string.send_enter_unexpected_error)
            TransactionErrorState.BELOW_MIN_LIMIT -> when (state.action) {
                AssetAction.Deposit -> resources.getString(R.string.send_enter_amount_min_deposit,
                    state.pendingTx?.minLimit?.toStringWithSymbol())
                AssetAction.Sell -> resources.getString(R.string.sell_enter_amount_min_error,
                    state.pendingTx?.minLimit?.toStringWithSymbol())
                AssetAction.NewSend -> resources.getString(R.string.send_enter_amount_min_send,
                    state.pendingTx?.minLimit?.toStringWithSymbol())
                else -> throw IllegalArgumentException(
                    "Action not supported by Send Flow ${state.action}")
            }
            TransactionErrorState.ABOVE_MAX_LIMIT -> resources.getString(R.string.sell_enter_amount_max_error,
                state.pendingTx?.maxLimit?.toStringWithSymbol())
            TransactionErrorState.TRANSACTION_IN_FLIGHT -> resources.getString(R.string.send_error_tx_in_flight)
            TransactionErrorState.TX_OPTION_INVALID -> resources.getString(R.string.send_error_tx_option_invalid)
        }
}