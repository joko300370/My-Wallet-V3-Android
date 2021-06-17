package piuk.blockchain.android.ui.transactionflow.flow.customisations

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.widget.FrameLayout
import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.ui.urllinks.CHECKOUT_REFUND_POLICY
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.NonCustodialAccount
import piuk.blockchain.android.coincore.NullAddress
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.ui.customviews.CurrencyType
import piuk.blockchain.android.ui.customviews.account.AccountInfoBank
import piuk.blockchain.android.ui.customviews.account.AccountInfoCrypto
import piuk.blockchain.android.ui.customviews.account.AccountInfoFiat
import piuk.blockchain.android.ui.customviews.account.DefaultCellDecorator
import piuk.blockchain.android.ui.customviews.account.StatusDecorator
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.swap.SwapAccountSelectSheetFeeDecorator
import piuk.blockchain.android.ui.transactionflow.engine.TransactionErrorState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.TxExecutionStatus
import piuk.blockchain.android.ui.transactionflow.plugin.AccountLimitsView
import piuk.blockchain.android.ui.transactionflow.plugin.BalanceAndFeeView
import piuk.blockchain.android.ui.transactionflow.plugin.ConfirmSheetWidget
import piuk.blockchain.android.ui.transactionflow.plugin.EnterAmountWidget
import piuk.blockchain.android.ui.transactionflow.plugin.FromAndToView
import piuk.blockchain.android.ui.transactionflow.plugin.SimpleInfoHeaderView
import piuk.blockchain.android.ui.transactionflow.plugin.SmallBalanceView
import piuk.blockchain.android.ui.transactionflow.plugin.SwapInfoHeaderView
import piuk.blockchain.android.ui.transactionflow.plugin.TxFlowWidget
import piuk.blockchain.android.util.StringUtils
import java.math.BigInteger
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Locale

interface TransactionFlowCustomiser :
    EnterAmountCustomisations, SourceSelectionCustomisations, TargetSelectionCustomisations,
    TransactionConfirmationCustomisations, TransactionProgressCustomisations

class TransactionFlowCustomiserImpl(
    private val resources: Resources,
    private val assetResources: AssetResources,
    private val stringUtils: StringUtils
) : TransactionFlowCustomiser {
    override fun enterAmountActionIcon(state: TransactionState): Int {
        return when (state.action) {
            AssetAction.Send -> R.drawable.ic_tx_sent
            AssetAction.InterestDeposit -> R.drawable.ic_tx_deposit_arrow
            AssetAction.FiatDeposit -> R.drawable.ic_tx_deposit_w_green_bkgd
            AssetAction.Swap -> R.drawable.ic_swap_light_blue
            AssetAction.Sell -> R.drawable.ic_tx_sell
            AssetAction.Withdraw -> R.drawable.ic_tx_withdraw_w_green_bkgd
            AssetAction.InterestWithdraw -> R.drawable.ic_tx_withdraw
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }
    }

    override fun shouldDisableInput(errorState: TransactionErrorState): Boolean =
        errorState == TransactionErrorState.PENDING_ORDERS_LIMIT_REACHED

    override fun enterAmountActionIconCustomisation(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.Swap,
            AssetAction.FiatDeposit,
            AssetAction.Withdraw -> false
            else -> true
        }

    override fun selectSourceAddressTitle(state: TransactionState): String = "Select Source Address"

    override fun selectTargetAddressInputHint(state: TransactionState): String =
        when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.send_enter_asset_address_hint,
                assetResources.assetName(state.sendingAsset)
            )
            AssetAction.Sell -> ""
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }

    override fun selectTargetNoAddressMessageText(state: TransactionState): String? =
        when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.send_internal_transfer_message_1,
                assetResources.assetName(state.sendingAsset),
                state.sendingAsset.displayTicker
            )
            else -> null
        }

    override fun selectTargetAddressTitle(state: TransactionState): String =
        when (state.action) {
            AssetAction.Send -> resources.getString(R.string.common_send)
            AssetAction.Sell -> resources.getString(R.string.common_sell)
            AssetAction.InterestDeposit -> resources.getString(R.string.common_transfer)
            AssetAction.Swap -> resources.getString(R.string.swap_select_target_title)
            AssetAction.Withdraw -> resources.getString(R.string.common_withdraw)
            AssetAction.InterestWithdraw -> resources.getString(R.string.select_withdraw_target_title)
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }

    override fun selectTargetShouldShowSubtitle(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.Swap -> true
            else -> false
        }

    override fun selectTargetSubtitle(state: TransactionState): String =
        resources.getString(
            when (state.action) {
                AssetAction.Swap -> R.string.swap_select_target_subtitle
                else -> R.string.empty
            }
        )

    override fun shouldShowCustodialUpsell(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.Swap -> state.selectedTarget is NonCustodialAccount
            else -> false
        }

    override fun selectTargetAddressWalletsCta(state: TransactionState) =
        resources.getString(
            when (state.action) {
                AssetAction.Withdraw -> R.string.select_a_bank
                else -> R.string.select_a_wallet
            }
        )

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

    override fun selectTargetStatusDecorator(state: TransactionState): StatusDecorator =
        when (state.action) {
            AssetAction.Swap -> {
                {
                    SwapAccountSelectSheetFeeDecorator(it)
                }
            }
            else -> {
                {
                    DefaultCellDecorator()
                }
            }
        }

    override fun selectTargetShowManualEnterAddress(state: TransactionState): Boolean =
        state.action == AssetAction.Send

    override fun enterAmountTitle(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.send_enter_amount_title, state.sendingAsset.displayTicker
            )
            AssetAction.Swap -> resources.getString(R.string.common_swap)
            AssetAction.InterestDeposit -> resources.getString(
                R.string.tx_title_deposit,
                state.sendingAsset.displayTicker
            )
            AssetAction.Sell -> resources.getString(
                R.string.tx_title_sell,
                state.sendingAsset.displayTicker
            )
            AssetAction.FiatDeposit -> resources.getString(
                R.string.tx_title_fiat_deposit,
                (state.selectedTarget as FiatAccount).fiatCurrency
            )
            AssetAction.Withdraw -> resources.getString(
                R.string.tx_title_withdraw,
                (state.sendingAccount as FiatAccount).fiatCurrency
            )
            AssetAction.InterestWithdraw -> resources.getString(
                R.string.tx_title_withdraw, state.sendingAsset.displayTicker
            )
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }
    }

    override fun enterAmountMaxButton(state: TransactionState): String =
        when (state.action) {
            AssetAction.Send -> resources.getString(R.string.send_enter_amount_max)
            AssetAction.InterestDeposit -> resources.getString(R.string.send_enter_amount_deposit_max)
            AssetAction.Swap -> resources.getString(R.string.swap_enter_amount_max)
            AssetAction.Sell -> resources.getString(R.string.sell_enter_amount_max)
            AssetAction.InterestWithdraw -> resources.getString(R.string.withdraw_enter_amount_max)
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }

    override fun enterAmountSourceLabel(state: TransactionState): String =
        when (state.action) {
            AssetAction.Swap -> resources.getString(
                R.string.swap_enter_amount_source,
                state.amount.toStringWithSymbol()
            )
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

    override fun enterAmountSourceIcon(state: TransactionState): Int =
        when (state.action) {
            AssetAction.FiatDeposit,
            AssetAction.Withdraw -> {
                when ((state.selectedTarget as FiatAccount).fiatCurrency) {
                    "GBP" -> R.drawable.ic_funds_gbp
                    "EUR" -> R.drawable.ic_funds_euro
                    else -> R.drawable.ic_funds_usd
                }
            }
            else -> assetResources.drawableResFilled(state.sendingAsset)
        }

    override fun shouldShowMaxLimit(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.FiatDeposit -> false
            else -> true
        }

    override fun enterAmountLimitsViewTitle(state: TransactionState): String =
        when (state.action) {
            AssetAction.FiatDeposit -> resources.getString(R.string.deposit_enter_amount_limit_title)
            AssetAction.Withdraw -> state.sendingAccount.label
            else -> throw java.lang.IllegalStateException("Limits title view not configured for ${state.action}")
        }

    override fun enterAmountLimitsViewInfo(state: TransactionState): String =
        when (state.action) {
            AssetAction.FiatDeposit ->
                resources.getString(
                    R.string.deposit_enter_amount_limit_label,
                    state.pendingTx?.maxLimit?.toStringWithSymbol() ?: ""
                )
            AssetAction.Withdraw -> state.availableBalance.toStringWithSymbol()
            else -> throw java.lang.IllegalStateException("Limits info view not configured for ${state.action}")
        }

    override fun enterAmountMaxNetworkFeeLabel(state: TransactionState): String =
        when (state.action) {
            AssetAction.InterestDeposit,
            AssetAction.InterestWithdraw,
            AssetAction.Sell,
            AssetAction.Swap,
            AssetAction.Send -> resources.getString(R.string.send_enter_amount_max_fee)
            else -> throw java.lang.IllegalStateException("Max network fee label not configured for ${state.action}")
        }

    override fun shouldNotDisplayNetworkFee(state: TransactionState): Boolean =
        state.action == AssetAction.Swap &&
            state.sendingAccount is NonCustodialAccount && state.selectedTarget is NonCustodialAccount

    override fun confirmTitle(state: TransactionState): String =
        when (state.action) {
            AssetAction.Send -> resources.getString(R.string.send_confirmation_title)
            AssetAction.Swap -> resources.getString(R.string.common_confirm)
            AssetAction.InterestDeposit,
            AssetAction.InterestWithdraw -> resources.getString(R.string.common_confirm)
            AssetAction.Sell -> resources.getString(R.string.checkout)
            AssetAction.FiatDeposit -> resources.getString(R.string.common_deposit)
            AssetAction.Withdraw -> resources.getString(R.string.common_withdraw)
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }

    override fun confirmCtaText(state: TransactionState): String {
        val amount = state.pendingTx?.amount?.toStringWithSymbol().orEmpty()

        return when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.send_confirmation_cta_button, amount
            )
            AssetAction.Swap -> resources.getString(
                R.string.swap_confirmation_cta_button,
                state.sendingAsset.displayTicker,
                (state.selectedTarget as CryptoAccount).asset.displayTicker
            )
            AssetAction.Sell -> resources.getString(
                R.string.sell_confirmation_cta_button, amount
            )
            AssetAction.InterestDeposit -> resources.getString(
                R.string.send_confirmation_deposit_cta_button
            )
            AssetAction.FiatDeposit -> resources.getString(
                R.string.deposit_confirmation_cta_button, amount
            )
            AssetAction.Withdraw,
            AssetAction.InterestWithdraw -> resources.getString(
                R.string.withdraw_confirmation_cta_button, amount
            )
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }
    }

    override fun confirmListItemTitle(assetAction: AssetAction): String {
        return when (assetAction) {
            AssetAction.Send -> resources.getString(R.string.common_send)
            AssetAction.InterestDeposit -> resources.getString(R.string.common_transfer)
            AssetAction.Sell -> resources.getString(R.string.common_sell)
            AssetAction.FiatDeposit -> resources.getString(R.string.common_deposit)
            AssetAction.Withdraw -> resources.getString(R.string.common_withdraw)
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }
    }

    override fun confirmDisclaimerBlurb(state: TransactionState, context: Context): CharSequence =
        when (state.action) {
            AssetAction.Swap -> {
                val map = mapOf("refund_policy" to Uri.parse(CHECKOUT_REFUND_POLICY))
                stringUtils.getStringWithMappedAnnotations(
                    R.string.swap_confirmation_disclaimer_1,
                    map,
                    context
                )
            }
            AssetAction.InterestWithdraw -> resources.getString(
                R.string.checkout_interest_confirmation_disclaimer, state.sendingAsset.displayTicker,
                state.selectedTarget.label
            )
            else -> throw IllegalStateException("Disclaimer not set for asset action ${state.action}")
        }

    override fun confirmDisclaimerVisibility(assetAction: AssetAction): Boolean =
        when (assetAction) {
            AssetAction.Swap,
            AssetAction.InterestWithdraw -> true
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
                resources.getString(
                    R.string.swap_progress_title,
                    state.amount.toStringWithSymbol(), receivingAmount.toStringWithSymbol()
                )
            }
            AssetAction.InterestDeposit -> resources.getString(
                R.string.send_confirmation_progress_title,
                amount
            )
            AssetAction.Sell -> resources.getString(
                R.string.sell_confirmation_progress_title,
                amount
            )
            AssetAction.FiatDeposit -> resources.getString(
                R.string.deposit_confirmation_progress_title,
                amount
            )
            AssetAction.Withdraw,
            AssetAction.InterestWithdraw -> resources.getString(
                R.string.withdraw_confirmation_progress_title,
                amount
            )
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }
    }

    override fun transactionProgressMessage(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Send -> resources.getString(R.string.send_progress_sending_subtitle)
            AssetAction.InterestDeposit -> resources.getString(
                R.string.send_confirmation_progress_message,
                state.sendingAsset.displayTicker
            )
            AssetAction.Sell -> resources.getString(R.string.sell_confirmation_progress_message)
            AssetAction.Swap -> resources.getString(R.string.swap_confirmation_progress_message)
            AssetAction.FiatDeposit -> resources.getString(R.string.deposit_confirmation_progress_message)
            AssetAction.Withdraw,
            AssetAction.InterestWithdraw -> resources.getString(R.string.withdraw_confirmation_progress_message)
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
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
            AssetAction.InterestDeposit -> resources.getString(
                R.string.send_confirmation_success_title,
                amount
            )
            AssetAction.FiatDeposit -> resources.getString(
                R.string.deposit_confirmation_success_title,
                amount
            )
            AssetAction.Withdraw,
            AssetAction.InterestWithdraw -> resources.getString(R.string.withdraw_confirmation_success_title, amount)
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }
    }

    override fun transactionCompleteMessage(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Send -> resources.getString(
                R.string.send_progress_complete_subtitle, state.sendingAsset.displayTicker
            )
            AssetAction.InterestDeposit -> resources.getString(
                R.string.send_confirmation_success_message,
                state.sendingAsset.displayTicker
            )
            AssetAction.Sell -> resources.getString(
                R.string.sell_confirmation_success_message,
                (state.selectedTarget as? FiatAccount)?.fiatCurrency
            )
            AssetAction.Swap -> resources.getString(
                R.string.swap_confirmation_success_message,
                (state.selectedTarget as CryptoAccount).asset.displayTicker
            )
            AssetAction.FiatDeposit -> resources.getString(
                R.string.deposit_confirmation_success_message,
                state.pendingTx?.amount?.toStringWithSymbol() ?: "",
                state.pendingTx?.selectedFiat ?: "",
                getEstimatedTransactionCompletionTime()
            )
            AssetAction.Withdraw -> resources.getString(
                R.string.withdraw_confirmation_success_message,
                getEstimatedTransactionCompletionTime()
            )
            AssetAction.InterestWithdraw -> resources.getString(
                R.string.withdraw_interest_confirmation_success_message,
                state.sendingAsset.displayTicker,
                state.selectedTarget.label
            )
            else -> throw IllegalArgumentException("Action not supported by Transaction Flow")
        }
    }

    override fun selectTargetAccountTitle(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Swap,
            AssetAction.Send -> resources.getString(R.string.common_receive)
            AssetAction.Sell -> resources.getString(R.string.sell)
            AssetAction.FiatDeposit -> resources.getString(R.string.common_deposit)
            AssetAction.Withdraw,
            AssetAction.InterestWithdraw -> resources.getString(R.string.withdraw_target_select_title)
            else -> ""
        }
    }

    override fun selectSourceAccountTitle(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Swap -> resources.getString(R.string.common_swap)
            AssetAction.FiatDeposit -> resources.getString(R.string.deposit_source_select_title)
            AssetAction.InterestDeposit -> resources.getString(R.string.select_deposit_source_title)
            else -> ""
        }
    }

    override fun selectSourceAccountSubtitle(state: TransactionState): String {
        return when (state.action) {
            AssetAction.Swap -> resources.getString(R.string.swap_account_select_subtitle)
            else -> ""
        }
    }

    override fun selectSourceShouldShowAddNew(state: TransactionState): Boolean =
        when (state.action) {
            AssetAction.FiatDeposit -> true
            else -> false
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
                    MAX_ACCOUNTS_FOR_SHEET
                ).map { it as BlockchainAccount })
            }
        } else {
            TargetAddressSheetState.TargetAccountSelected(state.selectedTarget)
        }
    }

    override fun issueFlashMessage(state: TransactionState, input: CurrencyType?): String? {
        if (state.pendingTx?.amount?.toBigInteger() == BigInteger.ZERO && (
                state.errorState == TransactionErrorState.INVALID_AMOUNT ||
                    state.errorState == TransactionErrorState.BELOW_MIN_LIMIT
                )
        ) return null
        return when (state.errorState) {
            TransactionErrorState.NONE -> null
            TransactionErrorState.INSUFFICIENT_FUNDS -> resources.getString(
                R.string.send_enter_amount_error_insufficient_funds,
                state.sendingAccount.uiCurrency()
            )
            TransactionErrorState.INVALID_AMOUNT -> resources.getString(
                R.string.send_enter_amount_error_invalid_amount_1,
                state.pendingTx?.minLimit?.formatOrSymbolForZero() ?: throw IllegalStateException("Missing limit")
            )
            TransactionErrorState.INVALID_ADDRESS -> resources.getString(
                R.string.send_error_not_valid_asset_address,
                state.sendingAccount.uiCurrency()
            )
            TransactionErrorState.ADDRESS_IS_CONTRACT -> resources.getString(
                R.string.send_error_address_is_eth_contract
            )
            TransactionErrorState.INVALID_PASSWORD -> resources.getString(
                R.string.send_enter_invalid_password
            )
            TransactionErrorState.NOT_ENOUGH_GAS -> resources.getString(
                R.string.send_enter_insufficient_gas
            )
            TransactionErrorState.UNEXPECTED_ERROR -> resources.getString(
                R.string.send_enter_unexpected_error
            )
            TransactionErrorState.BELOW_MIN_LIMIT -> composeBelowLimitErrorMessage(state, input)
            TransactionErrorState.ABOVE_MAX_LIMIT -> {
                val exchangeRate = state.fiatRate ?: return ""
                val amount =
                    input?.let {
                        state.pendingTx?.maxLimit?.toEnteredCurrency(
                            it, exchangeRate, RoundingMode.FLOOR
                        )
                    } ?: state.pendingTx?.maxLimit?.toStringWithSymbol()

                resources.getString(R.string.sell_enter_amount_max_error, amount)
            }
            TransactionErrorState.OVER_SILVER_TIER_LIMIT -> resources.getString(R.string.swap_enter_amount_silver_limit)
            TransactionErrorState.OVER_GOLD_TIER_LIMIT -> {
                val exchangeRate = state.fiatRate ?: return ""
                val amount =
                    input?.let {
                        state.pendingTx?.maxLimit?.toEnteredCurrency(
                            it, exchangeRate, RoundingMode.FLOOR
                        )
                    } ?: state.pendingTx?.maxLimit?.toStringWithSymbol()

                resources.getString(R.string.swap_enter_amount_over_limit, amount)
            }
            TransactionErrorState.TRANSACTION_IN_FLIGHT -> resources.getString(R.string.send_error_tx_in_flight)
            TransactionErrorState.TX_OPTION_INVALID -> resources.getString(R.string.send_error_tx_option_invalid)
            TransactionErrorState.UNKNOWN_ERROR -> resources.getString(R.string.send_error_tx_option_invalid)
            TransactionErrorState.PENDING_ORDERS_LIMIT_REACHED ->
                resources.getString(R.string.too_many_pending_orders_error_message, state.sendingAsset.displayTicker)
        }
    }

    override fun issueFeesTooHighMessage(state: TransactionState): String? {
        return when (state.action) {
            AssetAction.Send ->
                resources.getString(
                    R.string.send_enter_amount_error_insufficient_funds_for_fees,
                    state.sendingAsset.displayTicker
                )
            AssetAction.Swap ->
                resources.getString(
                    R.string.swap_enter_amount_error_insufficient_funds_for_fees,
                    state.sendingAsset.displayTicker
                )
            AssetAction.Sell ->
                resources.getString(
                    R.string.sell_enter_amount_error_insufficient_funds_for_fees,
                    state.sendingAsset.displayTicker
                )
            AssetAction.InterestDeposit ->
                resources.getString(
                    R.string.interest_enter_amount_error_insufficient_funds_for_fees,
                    state.sendingAsset.displayTicker
                )
            else -> null
        }
    }

    override fun installEnterAmountLowerSlotView(
        ctx: Context,
        frame: FrameLayout,
        state: TransactionState
    ): EnterAmountWidget =
        when (state.action) {
            AssetAction.ViewActivity,
            AssetAction.Summary -> throw IllegalStateException()
            AssetAction.Send,
            AssetAction.InterestDeposit,
            AssetAction.InterestWithdraw,
            AssetAction.Sell,
            AssetAction.Swap -> BalanceAndFeeView(ctx).also { frame.addView(it) }
            AssetAction.Receive -> SmallBalanceView(ctx).also { frame.addView(it) }
            AssetAction.Withdraw,
            AssetAction.FiatDeposit -> AccountInfoBank(ctx).also { frame.addView(it) }
        }

    override fun installEnterAmountUpperSlotView(
        ctx: Context,
        frame: FrameLayout,
        state: TransactionState
    ): EnterAmountWidget =
        when (state.action) {
            AssetAction.Withdraw,
            AssetAction.FiatDeposit -> AccountLimitsView(ctx).also {
                frame.addView(it)
            }
            else -> FromAndToView(ctx).also {
                frame.addView(it)
            }
        }

    override fun installAddressSheetSource(
        ctx: Context,
        frame: FrameLayout,
        state: TransactionState
    ): TxFlowWidget =
        when (state.action) {
            AssetAction.Withdraw -> AccountInfoFiat(ctx).also {
                frame.addView(it)
            }
            else -> AccountInfoCrypto(ctx).also {
                frame.addView(it)
            }
        }

    private fun composeBelowLimitErrorMessage(state: TransactionState, input: CurrencyType?): String {
        val exchangeRate = state.fiatRate ?: return ""
        val amount =
            input?.let {
                state.pendingTx?.minLimit?.toEnteredCurrency(
                    it, exchangeRate, RoundingMode.CEILING
                )
            } ?: state.pendingTx?.minLimit?.toStringWithSymbol()

        return when (state.action) {
            AssetAction.InterestDeposit -> resources.getString(
                R.string.send_enter_amount_min_deposit,
                amount
            )
            AssetAction.Sell -> resources.getString(
                R.string.sell_enter_amount_min_error,
                amount
            )
            AssetAction.Send -> resources.getString(
                R.string.send_enter_amount_min_send,
                amount
            )
            AssetAction.Swap -> resources.getString(
                R.string.swap_enter_amount_min_swap,
                amount
            )
            AssetAction.FiatDeposit -> resources.getString(
                R.string.swap_enter_amount_min_swap,
                amount
            )
            AssetAction.Withdraw,
            AssetAction.InterestWithdraw -> resources.getString(
                R.string.withdraw_enter_amount_min,
                amount
            )
            else -> throw IllegalArgumentException(
                "Action not supported by Transaction Flow ${state.action}"
            )
        }
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
            AssetAction.Withdraw,
            AssetAction.FiatDeposit -> {
                when (state.pendingTx?.selectedFiat) {
                    "GBP" -> R.drawable.ic_funds_gbp_masked
                    "EUR" -> R.drawable.ic_funds_euro_masked
                    else -> R.drawable.ic_funds_usd_masked
                }
            }
            else -> assetResources.maskedAsset(state.sendingAsset)
        }

    override fun transactionProgressExceptionMessage(state: TransactionState): String {
        require(state.executionStatus is TxExecutionStatus.Error)
        return (state.executionStatus.exception as? TransactionError)?.let {
            when (it) {
                TransactionError.OrderLimitReached -> resources.getString(
                    R.string.trading_order_limit, getActionStringResource(state.action)
                )
                TransactionError.OrderNotCancelable -> resources.getString(
                    R.string.trading_order_not_cancelable, getActionStringResource(state.action)
                )
                TransactionError.WithdrawalAlreadyPending -> resources.getString(
                    R.string.trading_pending_withdrawal
                )
                TransactionError.WithdrawalBalanceLocked -> resources.getString(
                    R.string.trading_withdrawal_balance_locked
                )
                TransactionError.WithdrawalInsufficientFunds -> resources.getString(
                    R.string.trading_withdrawal_insufficient_funds
                )
                TransactionError.InternalServerError -> resources.getString(R.string.trading_internal_server_error)
                TransactionError.AlbertExecutionError -> resources.getString(R.string.trading_albert_error)
                TransactionError.TradingTemporarilyDisabled -> resources.getString(
                    R.string.trading_service_temp_disabled
                )
                TransactionError.InsufficientBalance -> {
                    resources.getString(
                        R.string.trading_insufficient_balance, getActionStringResource(state.action)
                    )
                }
                TransactionError.OrderBelowMin -> resources.getString(
                    R.string.trading_amount_below_min, getActionStringResource(state.action)
                )
                TransactionError.OrderAboveMax -> resources.getString(
                    R.string.trading_amount_above_max, getActionStringResource(state.action)
                )
                TransactionError.SwapDailyLimitExceeded -> resources.getString(
                    R.string.trading_daily_limit_exceeded, getActionStringResource(state.action)
                )
                TransactionError.SwapWeeklyLimitExceeded -> resources.getString(
                    R.string.trading_weekly_limit_exceeded, getActionStringResource(state.action)
                )
                TransactionError.SwapYearlyLimitExceeded -> resources.getString(
                    R.string.trading_yearly_limit_exceeded, getActionStringResource(state.action)
                )
                TransactionError.InvalidCryptoAddress -> resources.getString(R.string.trading_invalid_address)
                TransactionError.InvalidCryptoCurrency -> resources.getString(R.string.trading_invalid_currency)
                TransactionError.InvalidFiatCurrency -> resources.getString(R.string.trading_invalid_fiat)
                TransactionError.OrderDirectionDisabled -> resources.getString(R.string.trading_direction_disabled)
                TransactionError.InvalidOrExpiredQuote -> resources.getString(
                    R.string.trading_quote_invalid_or_expired
                )
                TransactionError.IneligibleForSwap -> resources.getString(R.string.trading_ineligible_for_swap)
                TransactionError.InvalidDestinationAmount -> resources.getString(
                    R.string.trading_invalid_destination_amount
                )
                is TransactionError.ExecutionFailed -> resources.getString(
                    R.string.executing_transaction_error, state.sendingAsset.displayTicker
                )
                TransactionError.UnexpectedError -> resources.getString(R.string.send_progress_error_title)
                else -> resources.getString(R.string.send_progress_error_title)
            }
        } ?: resources.getString(R.string.send_progress_error_title)
    }

    private fun getActionStringResource(action: AssetAction): String =
        resources.getString(
            when (action) {
                AssetAction.Send -> R.string.common_send
                AssetAction.Withdraw,
                AssetAction.InterestWithdraw -> R.string.common_withdraw
                AssetAction.Swap -> R.string.common_swap
                AssetAction.Sell -> R.string.common_sell
                AssetAction.InterestDeposit,
                AssetAction.FiatDeposit -> R.string.common_deposit
                AssetAction.ViewActivity -> R.string.common_activity
                AssetAction.Receive -> R.string.common_receive
                AssetAction.Summary -> R.string.common_summary
            }
        )

    override fun amountHeaderConfirmationVisible(state: TransactionState): Boolean =
        state.action != AssetAction.Swap

    override fun confirmInstallHeaderView(
        ctx: Context,
        frame: FrameLayout,
        state: TransactionState
    ): ConfirmSheetWidget =
        when (state.action) {
            AssetAction.Swap -> SwapInfoHeaderView(ctx).also { frame.addView(it) }
            AssetAction.FiatDeposit, AssetAction.Withdraw ->
                SimpleInfoHeaderView(ctx, false).also { frame.addView(it) }
            else -> SimpleInfoHeaderView(ctx).also { frame.addView(it) }
        }

    override fun defInputType(state: TransactionState, fiatCurrency: String): CurrencyType =
        when (state.action) {
            AssetAction.Swap,
            AssetAction.Sell,
            AssetAction.Withdraw,
            AssetAction.FiatDeposit -> CurrencyType.Fiat(fiatCurrency)
            else -> CurrencyType.Crypto(state.sendingAsset)
        }

    override fun sourceAccountSelectionStatusDecorator(state: TransactionState): StatusDecorator =
        when (state.action) {
            AssetAction.Swap -> {
                {
                    SwapAccountSelectSheetFeeDecorator(it)
                }
            }
            AssetAction.InterestDeposit,
            AssetAction.Withdraw,
            AssetAction.FiatDeposit -> {
                {
                    DefaultCellDecorator()
                }
            }
            else -> throw IllegalStateException("Action is not supported")
        }

    override fun getLinkingSourceForAction(state: TransactionState): BankAuthSource =
        when (state.action) {
            AssetAction.FiatDeposit -> {
                BankAuthSource.DEPOSIT
            }
            AssetAction.Withdraw -> {
                BankAuthSource.WITHDRAW
            }
            else -> {
                throw IllegalStateException("Attempting to link from an unsupported action")
            }
        }

    companion object {
        const val MAX_ACCOUNTS_FOR_SHEET = 3
        private const val FIVE_DAYS = 5

        fun getEstimatedTransactionCompletionTime(daysInFuture: Int = FIVE_DAYS): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, daysInFuture)
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return sdf.format(cal.time)
        }
    }

    private fun Money.toEnteredCurrency(
        input: CurrencyType,
        exchangeRate: ExchangeRate,
        roundingMode: RoundingMode
    ): String {
        if (input.isSameType(this)) {
            return toStringWithSymbol()
        }
        if (input.isFiat() && this is CryptoValue) {
            val cryptoToFiatRate = exchangeRate as ExchangeRate.CryptoToFiat
            return FiatValue.fromMajor(
                cryptoToFiatRate.to,
                cryptoToFiatRate.convert(this, round = false).toBigDecimal().setScale(
                    Currency.getInstance(exchangeRate.to).defaultFractionDigits, roundingMode
                )
            ).toStringWithSymbol()
        }
        if (input.isCrypto() && this is FiatValue) {
            return exchangeRate.inverse().convert(this).toStringWithSymbol()
        }
        throw IllegalStateException("Not valid currency")
    }
}

private fun BlockchainAccount.uiCurrency(): String {
    require(this is CryptoAccount || this is FiatAccount)
    return when (this) {
        is CryptoAccount -> asset.displayTicker
        is FiatAccount -> fiatCurrency
        else -> throw IllegalStateException("Unsupported account ttype")
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