package piuk.blockchain.android.ui.transfer.send.flow

import android.content.res.Resources
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.isCustodial
import piuk.blockchain.android.ui.transfer.send.SendErrorState
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.util.assetName

interface SendFlowCustomiser {
    // UI Element text, icons etc may be customised here:
    fun selectSourceAddressTitle(state: SendState): String
    fun selectTargetAddressTitle(state: SendState): String
    fun selectTargetAddressInputHint(state: SendState): String
    fun selectTargetNoAddressMessageText(state: SendState): String?
    fun selectTargetShowManualEnterAddress(state: SendState): Boolean
    fun enterAmountTitle(state: SendState): String
    fun enterAmountActionIcon(state: SendState): Int
    fun enterAmountMaxButton(state: SendState): String
    fun confirmTitle(state: SendState): String
    fun confirmCtaText(state: SendState): String
    fun confirmListItemTitle(assetAction: AssetAction): String
    fun transactionProgressTitle(state: SendState): String
    fun transactionProgressMessage(state: SendState): String
    fun transactionCompleteTitle(state: SendState): String
    fun transactionCompleteMessage(state: SendState): String

    // Format those flash error messages:
    fun errorFlashMessage(state: SendState): String?

    // Perform per-action account selection filtering
    fun targetAccountFilter(state: SendState): (SingleAccount) -> Boolean
}

class SendFlowCustomiserImpl(
    private val resources: Resources
) : SendFlowCustomiser {
    override fun enterAmountActionIcon(state: SendState): Int {
        return when (state.action) {
            AssetAction.NewSend -> R.drawable.ic_tx_sent
            AssetAction.Deposit -> R.drawable.ic_tx_deposit_arrow
            // AssetAction.Swap -> resources.getString(R.string.common_swap)
            AssetAction.Sell -> R.drawable.ic_tx_sell
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun selectSourceAddressTitle(state: SendState): String = "Select Source Address"

    override fun selectTargetAddressInputHint(state: SendState): String =
        when (state.action) {
            AssetAction.NewSend -> resources.getString(
                R.string.send_enter_asset_address_hint,
                resources.getString(state.asset.assetName()))
            AssetAction.Sell -> ""
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }

    override fun selectTargetNoAddressMessageText(state: SendState): String? =
        when (state.action) {
            AssetAction.NewSend -> resources.getString(
                R.string.send_internal_transfer_message,
                resources.getString(state.asset.assetName()),
                state.asset.displayTicker
            )
            else -> null
        }

    override fun selectTargetAddressTitle(state: SendState): String =
        when (state.action) {
            AssetAction.NewSend -> resources.getString(R.string.common_send)
            AssetAction.Sell -> resources.getString(R.string.common_sell)
            AssetAction.Deposit -> resources.getString(R.string.common_transfer)
            // AssetAction.Swap -> resources.getString(R.string.common_swap)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }

    override fun selectTargetShowManualEnterAddress(state: SendState): Boolean =
        when (state.action) {
            AssetAction.NewSend -> !state.sendingAccount.isCustodial()
            else -> false
        }

    override fun enterAmountTitle(state: SendState): String {
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

    override fun enterAmountMaxButton(state: SendState): String =
        when (state.action) {
            AssetAction.NewSend -> resources.getString(R.string.send_enter_amount_max)
            AssetAction.Deposit -> resources.getString(R.string.send_enter_amount_deposit_max)
            // AssetAction.Swap -> "Swap..."
            AssetAction.Sell -> resources.getString(R.string.sell_enter_amount_max)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }

    override fun confirmTitle(state: SendState): String {
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

    override fun confirmCtaText(state: SendState): String {
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

    override fun transactionProgressTitle(state: SendState): String {
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

    override fun transactionProgressMessage(state: SendState): String {
        return when (state.action) {
            AssetAction.NewSend -> resources.getString(R.string.send_progress_sending_subtitle)
            AssetAction.Deposit -> resources.getString(R.string.send_confirmation_progress_message,
                state.sendingAccount.asset.name)
            AssetAction.Sell -> resources.getString(R.string.sell_confirmation_progress_message)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun transactionCompleteTitle(state: SendState): String {
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

    override fun transactionCompleteMessage(state: SendState): String {
        return when (state.action) {
            AssetAction.NewSend -> resources.getString(
                R.string.send_progress_complete_subtitle, state.sendingAccount.asset.name
            )
            AssetAction.Deposit -> resources.getString(R.string.send_confirmation_success_message,
                state.sendingAccount.asset.name)
            AssetAction.Sell -> resources.getString(R.string.sell_confirmation_success_message,
                (state.sendTarget as? FiatAccount)?.fiatCurrency)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun errorFlashMessage(state: SendState): String? =
        when (state.errorState) {
            SendErrorState.NONE -> null
            SendErrorState.INSUFFICIENT_FUNDS -> resources.getString(
                R.string.send_enter_amount_error_insufficient_funds,
                state.sendingAccount.asset.displayTicker
            )
            SendErrorState.INVALID_AMOUNT -> resources.getString(
                R.string.send_enter_amount_error_invalid_amount,
                state.sendingAccount.asset.displayTicker
            )
            SendErrorState.INVALID_ADDRESS -> resources.getString(
                R.string.send_error_not_valid_asset_address,
                state.sendingAccount.asset.displayTicker
            )
            SendErrorState.ADDRESS_IS_CONTRACT -> resources.getString(
                R.string.send_error_address_is_eth_contract)
            SendErrorState.INVALID_PASSWORD -> resources.getString(
                R.string.send_enter_invalid_password)
            SendErrorState.NOT_ENOUGH_GAS -> resources.getString(
                R.string.send_enter_insufficient_gas)
            SendErrorState.UNEXPECTED_ERROR -> resources.getString(
                R.string.send_enter_unexpected_error)
            SendErrorState.BELOW_MIN_LIMIT -> when (state.action) {
                AssetAction.Deposit -> resources.getString(R.string.send_enter_amount_min_deposit,
                    state.pendingTx?.minLimit?.toStringWithSymbol())
                AssetAction.Sell -> resources.getString(R.string.sell_enter_amount_min_error,
                    state.pendingTx?.minLimit?.toStringWithSymbol())
                AssetAction.NewSend -> resources.getString(R.string.send_enter_amount_min_send,
                    state.pendingTx?.minLimit?.toStringWithSymbol())
                else -> throw IllegalArgumentException("Action not supported by Send Flow ${state.action}")
            }
            SendErrorState.ABOVE_MAX_LIMIT -> resources.getString(R.string.sell_enter_amount_max_error,
                state.pendingTx?.maxLimit?.toStringWithSymbol())
        }

    override fun targetAccountFilter(state: SendState): (SingleAccount) -> Boolean =
        when (state.action) {
            AssetAction.Sell -> {
                {
                    it is FiatAccount
                }
            }
            AssetAction.NewSend,
            AssetAction.Send -> {
                {
                    it !is FiatAccount
                }
            }
            else -> {
                { true }
            }
        }
}