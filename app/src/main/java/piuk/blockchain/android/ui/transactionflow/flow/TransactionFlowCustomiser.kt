package piuk.blockchain.android.ui.transactionflow.flow

import android.content.res.Resources
import info.blockchain.balance.CryptoValue
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.NullAddress
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.isCustodial
import piuk.blockchain.android.ui.customviews.CurrencyType
import piuk.blockchain.android.ui.customviews.account.StatusDecorator
import piuk.blockchain.android.ui.swap.SwapAccountSelectSheetFeeDecorator
import piuk.blockchain.android.ui.transactionflow.engine.TransactionErrorState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.util.assetName
import piuk.blockchain.android.util.maskedAsset

interface TransactionFlowCustomiser {
    // UI Element text, icons etc may be customised here:
    fun selectSourceAddressTitle(state: TransactionState): String
    fun selectTargetAddressTitle(state: TransactionState): String
    fun selectTargetAddressInputHint(state: TransactionState): String
    fun selectTargetNoAddressMessageText(state: TransactionState): String?
    fun selectTargetShowManualEnterAddress(state: TransactionState): Boolean
    fun selectTargetShouldShowSubtitle(state: TransactionState): Boolean
    fun selectTargetSubtitle(state: TransactionState): String
    fun selectTargetSourceLabel(state: TransactionState): String
    fun selectTargetDestinationLabel(state: TransactionState): String
    fun enterAmountTitle(state: TransactionState): String
    fun enterAmountActionIcon(state: TransactionState): Int
    fun enterAmountActionIconCustomisation(state: TransactionState): Boolean
    fun enterAmountMaxButton(state: TransactionState): String
    fun enterAmountSourceLabel(state: TransactionState): String
    fun enterAmountTargetLabel(state: TransactionState): String
    fun confirmTitle(state: TransactionState): String
    fun confirmCtaText(state: TransactionState): String
    fun confirmListItemTitle(assetAction: AssetAction): String
    fun confirmDisclaimerBlurb(assetAction: AssetAction): String
    fun confirmDisclaimerVisibility(assetAction: AssetAction): Boolean
    fun transactionProgressTitle(state: TransactionState): String
    fun transactionProgressMessage(state: TransactionState): String
    fun transactionCompleteTitle(state: TransactionState): String
    fun transactionCompleteMessage(state: TransactionState): String
    fun selectTargetAccountTitle(state: TransactionState): String
    fun selectSourceAccountTitle(state: TransactionState): String
    fun selectSourceAccountSubtitle(state: TransactionState): String
    fun selectTargetAccountDescription(state: TransactionState): String
    fun enterTargetAddressSheetState(state: TransactionState): TargetAddressSheetState
    fun transactionProgressIcon(state: TransactionState): Int
    fun amountHeaderConfirmationVisible(state: TransactionState): Boolean
    fun defInputType(state: TransactionState): CurrencyType

    // Format those flash error messages:
    fun issueFlashMessage(state: TransactionState): String?
    fun selectIssueType(state: TransactionState): IssueType
    fun showTargetIcon(state: TransactionState): Boolean
    fun sourceAccountSelectionStatusDecorator(state: TransactionState): StatusDecorator
}

class TransactionFlowCustomiserImpl(
    private val resources: Resources
) : TransactionFlowCustomiser {
    override fun enterAmountActionIcon(state: TransactionState): Int {
        return when (state.action) {
            AssetAction.Send -> R.drawable.ic_tx_sent
            AssetAction.Deposit -> R.drawable.ic_tx_deposit_arrow
            AssetAction.Swap -> R.drawable.ic_swap_light_blue
            AssetAction.Sell -> R.drawable.ic_tx_sell
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun enterAmountActionIconCustomisation(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.Swap -> false
            else -> true
        }

    override fun selectSourceAddressTitle(state: TransactionState): String = "Select Source Address"

    override fun selectTargetAddressInputHint(state: TransactionState): String =
        when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.send_enter_asset_address_hint,
                resources.getString(state.asset.assetName()))
            AssetAction.Sell -> ""
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }

    override fun selectTargetNoAddressMessageText(state: TransactionState): String? =
        when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.send_internal_transfer_message,
                resources.getString(state.asset.assetName()),
                state.asset.displayTicker
            )
            else -> null
        }

    override fun selectTargetAddressTitle(state: TransactionState): String =
        when (state.action) {
            AssetAction.Send -> resources.getString(R.string.common_send)
            AssetAction.Sell -> resources.getString(R.string.common_sell)
            AssetAction.Deposit -> resources.getString(R.string.common_transfer)
            AssetAction.Swap -> resources.getString(R.string.swap_select_target_title)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }

    override fun selectTargetShouldShowSubtitle(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.Swap -> true
            else -> false
        }

    override fun selectTargetSubtitle(state: TransactionState): String =
        resources.getString(when (state.action) {
            AssetAction.Swap -> R.string.swap_select_target_subtitle
            else -> R.string.empty
        })

    override fun selectTargetSourceLabel(state: TransactionState): String =
        when (state.action) {
            AssetAction.Swap -> resources.getString(R.string.common_swap)
            else -> resources.getString(R.string.common_from)
        }

    override fun selectTargetDestinationLabel(state: TransactionState): String =
        when (state.action) {
            AssetAction.Swap -> resources.getString(R.string.common_receive)
            else -> resources.getString(R.string.common_to)
        }

    override fun selectTargetShowManualEnterAddress(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.Send -> !state.sendingAccount.isCustodial()
            else -> false
        }

    override fun enterAmountTitle(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.send_enter_amount_title, state.sendingAccount.asset.displayTicker
            )
            AssetAction.Swap -> resources.getString(R.string.swap_enter_amount_title,
                state.sendingAccount.asset.displayTicker
            )
            AssetAction.Deposit -> resources.getString(R.string.tx_title_deposit,
                state.sendingAccount.asset.displayTicker)
            AssetAction.Sell -> resources.getString(R.string.tx_title_sell,
                state.sendingAccount.asset.displayTicker)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun enterAmountMaxButton(state: TransactionState): String =
        when (state.action) {
            AssetAction.Send -> resources.getString(R.string.send_enter_amount_max)
            AssetAction.Deposit -> resources.getString(R.string.send_enter_amount_deposit_max)
            AssetAction.Swap -> resources.getString(R.string.swap_enter_amount_max)
            AssetAction.Sell -> resources.getString(R.string.sell_enter_amount_max)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }

    override fun enterAmountSourceLabel(state: TransactionState): String =
        when (state.action) {
            AssetAction.Swap -> resources.getString(R.string.swap_enter_amount_source,
                state.amount.toStringWithSymbol())
            else -> resources.getString(R.string.send_enter_amount_from, state.sendingAccount.label)
        }

    override fun enterAmountTargetLabel(state: TransactionState): String =
        when (state.action) {
            AssetAction.Swap -> {
                val amount = state.targetRate?.convert(state.amount) ?: CryptoValue.zero(
                    (state.selectedTarget as CryptoAccount).asset
                )
                resources.getString(
                    R.string.swap_enter_amount_target,
                    amount.toStringWithSymbol()
                )
            }
            else -> resources.getString(R.string.send_enter_amount_to, state.selectedTarget.label)
        }

    override fun confirmTitle(state: TransactionState): String {
        val amount = state.pendingTx?.amount?.toStringWithSymbol() ?: ""

        return when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.send_confirmation_title, amount
            )
            AssetAction.Swap -> resources.getString(R.string.common_confirm)
            AssetAction.Deposit -> resources.getString(R.string.common_confirm)
            AssetAction.Sell -> resources.getString(R.string.checkout)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun confirmCtaText(state: TransactionState): String {
        val amount = state.pendingTx?.amount?.toStringWithSymbol() ?: ""

        return when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.send_confirmation_cta_button, amount
            )
            AssetAction.Swap -> resources.getString(
                R.string.swap_confirmation_cta_button,
                state.sendingAccount.asset.displayTicker,
                (state.selectedTarget as CryptoAccount).asset.displayTicker
            )
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
            AssetAction.Send -> resources.getString(R.string.common_send)
            AssetAction.Deposit -> resources.getString(R.string.common_transfer)
            AssetAction.Sell -> resources.getString(R.string.common_sell)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun confirmDisclaimerBlurb(assetAction: AssetAction): String =
        when (assetAction) {
            AssetAction.Swap -> resources.getString(R.string.swap_confirmation_disclaimer)
            else -> throw IllegalStateException("Disclaimer not set for asset action $assetAction")
        }

    override fun confirmDisclaimerVisibility(assetAction: AssetAction): Boolean =
        when (assetAction) {
            AssetAction.Swap -> true
            else -> false
        }

    override fun transactionProgressTitle(state: TransactionState): String {
        val amount = state.pendingTx?.amount?.toStringWithSymbol() ?: ""

        return when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.send_progress_sending_title, amount
            )
            AssetAction.Swap -> {
                val receivingAmount = state.targetRate?.convert(state.amount) ?: CryptoValue.zero(
                    (state.selectedTarget as CryptoAccount).asset
                )
                resources.getString(R.string.swap_progress_title,
                    state.amount.toStringWithSymbol(), receivingAmount.toStringWithSymbol()
                )
            }
            AssetAction.Deposit -> resources.getString(R.string.send_confirmation_progress_title,
                amount)
            AssetAction.Sell -> resources.getString(R.string.sell_confirmation_progress_title,
                amount)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun transactionProgressMessage(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Send -> resources.getString(R.string.send_progress_sending_subtitle)
            AssetAction.Deposit -> resources.getString(R.string.send_confirmation_progress_message,
                state.sendingAccount.asset.displayTicker)
            AssetAction.Sell -> resources.getString(R.string.sell_confirmation_progress_message)
            AssetAction.Swap -> resources.getString(R.string.swap_confirmation_progress_message)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun transactionCompleteTitle(state: TransactionState): String {
        val amount = state.pendingTx?.amount?.toStringWithSymbol() ?: ""

        return when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.send_progress_complete_title, amount
            )
            AssetAction.Swap -> resources.getString(R.string.swap_progress_complete_title)
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
            AssetAction.Send -> resources.getString(
                R.string.send_progress_complete_subtitle, state.sendingAccount.asset.displayTicker
            )
            AssetAction.Deposit -> resources.getString(R.string.send_confirmation_success_message,
                state.sendingAccount.asset.displayTicker)
            AssetAction.Sell -> resources.getString(R.string.sell_confirmation_success_message,
                (state.selectedTarget as? FiatAccount)?.fiatCurrency)
            AssetAction.Swap -> resources.getString(R.string.swap_confirmation_success_message,
                (state.selectedTarget as CryptoAccount).asset.displayTicker)
            else -> throw IllegalArgumentException("Action not supported by Send Flow")
        }
    }

    override fun selectTargetAccountTitle(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Swap -> resources.getString(R.string.receive)
            AssetAction.Send -> resources.getString(R.string.send)
            AssetAction.Sell -> resources.getString(R.string.sell)
            else -> ""
        }
    }

    override fun selectSourceAccountTitle(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Swap -> resources.getString(R.string.swap)
            else -> ""
        }
    }

    override fun selectSourceAccountSubtitle(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Swap -> resources.getString(R.string.swap_account_select_subtitle)
            else -> ""
        }
    }

    override fun selectTargetAccountDescription(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Swap -> resources.getString(R.string.select_target_account_for_swap)
            else -> ""
        }
    }

    override fun enterTargetAddressSheetState(state: TransactionState): TargetAddressSheetState {
        return if (state.selectedTarget == NullAddress) {
            if (state.targetCount > MAX_ACCOUNTS_FOR_SHEET) {
                TargetAddressSheetState.SelectAccountWhenOverMaxLimitSurpassed
            } else {
                TargetAddressSheetState.SelectAccountWhenWithinMaxLimit(state.availableTargets.take(
                    MAX_ACCOUNTS_FOR_SHEET).map { it as BlockchainAccount })
            }
        } else {
            TargetAddressSheetState.TargetAccountSelected(state.selectedTarget)
        }
    }

    override fun issueFlashMessage(state: TransactionState): String? =
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
                AssetAction.Send -> resources.getString(R.string.send_enter_amount_min_send,
                    state.pendingTx?.minLimit?.toStringWithSymbol())
                AssetAction.Swap -> resources.getString(R.string.swap_enter_amount_min_swap,
                    state.pendingTx?.minLimit?.toStringWithSymbol())
                else -> throw IllegalArgumentException(
                    "Action not supported by Send Flow ${state.action}")
            }
            TransactionErrorState.ABOVE_MAX_LIMIT -> resources.getString(R.string.sell_enter_amount_max_error,
                state.pendingTx?.maxLimit?.toStringWithSymbol())
            TransactionErrorState.OVER_SILVER_TIER_LIMIT -> resources.getString(R.string.swap_enter_amount_silver_limit)
            TransactionErrorState.OVER_GOLD_TIER_LIMIT -> resources.getString(R.string.swap_enter_amount_over_limit,
                state.pendingTx?.maxLimit?.toStringWithSymbol())
            TransactionErrorState.TRANSACTION_IN_FLIGHT -> resources.getString(R.string.send_error_tx_in_flight)
            TransactionErrorState.TX_OPTION_INVALID -> resources.getString(R.string.send_error_tx_option_invalid)
            TransactionErrorState.UNKNOWN_ERROR -> resources.getString(R.string.send_error_tx_option_invalid)
        }

    override fun selectIssueType(state: TransactionState): IssueType =
        when (state.errorState) {
            TransactionErrorState.OVER_SILVER_TIER_LIMIT -> IssueType.INFO
            else -> IssueType.ERROR
        }

    override fun showTargetIcon(state: TransactionState): Boolean =
        state.action == AssetAction.Swap

    override fun transactionProgressIcon(state: TransactionState): Int =
        when (state.action) {
            AssetAction.Swap -> R.drawable.swap_masked_asset
            else -> state.sendingAccount.asset.maskedAsset()
        }

    override fun amountHeaderConfirmationVisible(state: TransactionState): Boolean =
        state.action != AssetAction.Swap

    override fun defInputType(state: TransactionState): CurrencyType =
        if (state.action == AssetAction.Swap) CurrencyType.Fiat else CurrencyType.Crypto

    override fun sourceAccountSelectionStatusDecorator(state: TransactionState): StatusDecorator =
        when (state.action) {
            AssetAction.Swap -> {
                {
                    SwapAccountSelectSheetFeeDecorator(it)
                }
            }
            else -> throw IllegalStateException("Action is not supported")
        }

    companion object {
        const val MAX_ACCOUNTS_FOR_SHEET = 3
    }
}

enum class IssueType {
    ERROR,
    INFO
}

sealed class TargetAddressSheetState(val accounts: List<TransactionTarget>) {
    object SelectAccountWhenOverMaxLimitSurpassed : TargetAddressSheetState(emptyList())
    class TargetAccountSelected(account: TransactionTarget) : TargetAddressSheetState(listOf(account))
    class SelectAccountWhenWithinMaxLimit(accounts: List<BlockchainAccount>) :
        TargetAddressSheetState(accounts.map { it as TransactionTarget })
}