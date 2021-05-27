package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.extensions.withoutNullValues
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BankAccount
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.InterestAccount
import piuk.blockchain.android.coincore.NonCustodialAccount
import piuk.blockchain.android.coincore.NullAddress
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.TradingAccount
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.android.ui.customviews.CurrencyType
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionStep
import java.io.Serializable

const val WALLET_TYPE_NON_CUSTODIAL = "non_custodial"
const val WALLET_TYPE_CUSTODIAL = "custodial"
const val WALLET_TYPE_FIAT = "fiat"
const val WALLET_TYPE_INTEREST = "interest"
const val WALLET_TYPE_BANK = "bank"
const val WALLET_TYPE_EXTERNAL = "external"
const val WALLET_TYPE_UNKNOWN = "unknown"

class TxFlowAnalytics(
    private val analytics: Analytics
) {
    // General
    fun onFlowCanceled(state: TransactionState) {
        when (state.action) {
            AssetAction.Send ->
                if (state.currentStep == TransactionStep.CONFIRM_DETAIL)
                    analytics.logEvent(SendAnalyticsEvent.CancelTransaction)
            AssetAction.Sell ->
                if (state.currentStep == TransactionStep.CONFIRM_DETAIL)
                    analytics.logEvent(
                        SellAnalyticsEvent(
                            event = SellAnalytics.CancelTransaction,
                            asset = state.sendingAsset,
                            source = state.sendingAccount.toCategory()
                        )
                    )
            AssetAction.Swap ->
                if (state.currentStep == TransactionStep.CONFIRM_DETAIL)
                    analytics.logEvent(SwapAnalyticsEvents.CancelTransaction)
            AssetAction.InterestDeposit ->
                if (state.currentStep == TransactionStep.CONFIRM_DETAIL)
                    analytics.logEvent(InterestDepositAnalyticsEvent.CancelTransaction)
            AssetAction.Withdraw ->
                if (state.currentStep == TransactionStep.CONFIRM_DETAIL) {
                    analytics.logEvent(
                        withdrawEvent(
                            WithdrawAnalytics.WITHDRAW_CHECKOUT_CANCEL,
                            (state.sendingAccount as FiatAccount).fiatCurrency
                        )
                    )
                }
            else -> {
            }
        }
    }

    fun onStepChanged(state: TransactionState) {
        when (state.action) {
            AssetAction.Send -> triggerSendScreenEvent(state.currentStep)
            AssetAction.Sell -> triggerSellScreenEvent(state)
            AssetAction.Swap -> triggerSwapScreenEvent(state.currentStep)
            AssetAction.InterestDeposit -> triggerDepositScreenEvent(state.currentStep)
            AssetAction.Withdraw -> triggerWithdrawScreenEvent(
                state.currentStep, (state.sendingAccount as FiatAccount).fiatCurrency
            )
            else -> {
            }
        }
    }

    fun onTargetAccountSelected(account: BlockchainAccount, state: TransactionState) {
        when (state.action) {
            AssetAction.Swap -> analytics.logEvent(
                SwapAnalyticsEvents.SwapTargetAccountSelected(
                    (account as CryptoAccount).asset.networkTicker,
                    TxFlowAnalyticsAccountType.fromAccount(account)
                )
            )
            else -> {
            }
        }
    }

    private fun triggerWithdrawScreenEvent(step: TransactionStep, currency: String) {
        when (step) {
            TransactionStep.SELECT_SOURCE,
            TransactionStep.SELECT_TARGET_ACCOUNT -> analytics.logEvent(
                withdrawEvent(WithdrawAnalytics.WITHDRAW_SHOWN, currency)
            )
            TransactionStep.CONFIRM_DETAIL -> analytics.logEvent(
                withdrawEvent(WithdrawAnalytics.WITHDRAW_CHECKOUT_SHOWN, currency)
            )
            else -> {
            }
        }
    }

    private fun triggerSwapScreenEvent(step: TransactionStep) {
        when (step) {
            TransactionStep.SELECT_SOURCE -> analytics.logEvent(SwapAnalyticsEvents.FromPickerSeen)
            TransactionStep.SELECT_TARGET_ACCOUNT -> analytics.logEvent(SwapAnalyticsEvents.ToPickerSeen)
            TransactionStep.ENTER_ADDRESS -> analytics.logEvent(SwapAnalyticsEvents.SwapTargetAddressSheet)
            TransactionStep.ENTER_AMOUNT -> analytics.logEvent(SwapAnalyticsEvents.SwapEnterAmount)
            TransactionStep.CONFIRM_DETAIL -> analytics.logEvent(SwapAnalyticsEvents.SwapConfirmSeen)
            else -> {
            }
        }
    }

    private fun triggerSendScreenEvent(step: TransactionStep) {
        when (step) {
            TransactionStep.ENTER_ADDRESS -> analytics.logEvent(SendAnalyticsEvent.EnterAddressDisplayed)
            TransactionStep.ENTER_AMOUNT -> analytics.logEvent(SendAnalyticsEvent.EnterAmountDisplayed)
            TransactionStep.CONFIRM_DETAIL -> analytics.logEvent(SendAnalyticsEvent.ConfirmationsDisplayed)
            else -> {
            }
        }
    }

    private fun triggerDepositScreenEvent(step: TransactionStep) {
        when (step) {
            TransactionStep.ENTER_AMOUNT -> analytics.logEvent(InterestDepositAnalyticsEvent.EnterAmountSeen)
            TransactionStep.CONFIRM_DETAIL -> analytics.logEvent(InterestDepositAnalyticsEvent.ConfirmationsSeen)
            else -> {
            }
        }
    }

    private fun triggerSellScreenEvent(state: TransactionState) {
        when (state.currentStep) {
            TransactionStep.CONFIRM_DETAIL -> analytics.logEvent(
                SellAnalyticsEvent(
                    event = SellAnalytics.ConfirmationsDisplayed,
                    asset = state.sendingAsset,
                    source = state.sendingAccount.toCategory()
                )
            )
            else -> {
            }
        }
    }

    fun onStepBackClicked(state: TransactionState) {}

    // Enter address sheet
    fun onManualAddressEntered(state: TransactionState) {}

    fun onScanQrClicked(state: TransactionState) {
        when (state.action) {
            AssetAction.Send -> analytics.logEvent(SendAnalyticsEvent.QrCodeScanned)
            else -> {
            }
        }
    }

    fun onAccountSelected(account: SingleAccount, state: TransactionState) {
        when (state.action) {
            AssetAction.Send -> analytics.logEvent(SendAnalyticsEvent.EnterAddressCtaClick)
            AssetAction.Swap -> {
                require(account is CryptoAccount)
                analytics.logEvent(SwapAnalyticsEvents.FromAccountSelected)
            }
            else -> {
            }
        }
    }

    fun onSourceAccountSelected(account: BlockchainAccount, state: TransactionState) {
        when (state.action) {
            AssetAction.Swap -> {
                require(account is CryptoAccount)
                analytics.logEvent(
                    SwapAnalyticsEvents.SwapFromSelected(
                        currency = account.asset.networkTicker,
                        accountType = TxFlowAnalyticsAccountType.fromAccount(account)
                    )
                )
            }
            AssetAction.Send -> {
                require(account is CryptoAccount)
                analytics.logEvent(
                    SendAnalyticsEvent.SendSourceAccountSelected(
                        currency = account.asset.networkTicker,
                        fromAccountType = TxFlowAnalyticsAccountType.fromAccount(account)
                    )
                )
            }
            else -> {
            }
        }
    }

    fun onEnterAddressCtaClick(state: TransactionState) {
        when (state.action) {
            AssetAction.Swap -> {
                analytics.logEvent(
                    SwapAnalyticsEvents.SwapConfirmPair(
                        asset = state.sendingAsset,
                        target = state.selectedTarget.toCategory()
                    )
                )
                analytics.logEvent(
                    SwapAnalyticsEvents.SwapAccountsSelected(
                        inputCurrency = state.sendingAsset.networkTicker,
                        outputCurrency = (state.selectedTarget as CryptoAccount).asset.networkTicker,
                        sourceAccountType = TxFlowAnalyticsAccountType.fromAccount(state.sendingAccount),
                        targetAccountType = TxFlowAnalyticsAccountType.fromAccount(state.selectedTarget),
                        werePreselected = false
                    )
                )
            }
            else -> {
            }
        }
    }

    // Enter amount sheet
    fun onMaxClicked(state: TransactionState) {
        when (state.action) {
            AssetAction.Send -> {
                analytics.logEvent(SendAnalyticsEvent.SendMaxClicked)
                analytics.logEvent(
                    SendAnalyticsEvent.SendAmountMaxClicked(
                        currency = state.sendingAsset.networkTicker,
                        toAccountType = TxFlowAnalyticsAccountType.fromAccount(state.sendingAccount),
                        fromAccountType = TxFlowAnalyticsAccountType.fromTransactionTarget(state.selectedTarget)
                    )
                )
            }
            AssetAction.Swap -> {
                check(state.selectedTarget is CryptoAccount)
                analytics.logEvent(
                    SwapAnalyticsEvents.SwapMaxAmountClicked(
                        sourceCurrency = state.sendingAsset.toString(),
                        sourceAccountType = TxFlowAnalyticsAccountType.fromAccount(state.sendingAccount),
                        targetAccountType = TxFlowAnalyticsAccountType.fromTransactionTarget(state.selectedTarget),
                        targetCurrency = state.selectedTarget.asset.toString()
                    )
                )
            }
            else -> {
            }
        }
    }

    fun onCryptoToggle(inputType: CurrencyType, state: TransactionState) {
        analytics.logEvent(
            AmountSwitched(
                action = state.action,
                newInput = inputType
            )
        )
    }

    fun onEnterAmountCtaClick(state: TransactionState) {
        when (state.action) {
            AssetAction.Send ->
                analytics.logEvent(SendAnalyticsEvent.EnterAmountCtaClick)
            AssetAction.Sell ->
                analytics.logEvent(
                    SellAnalyticsEvent(
                        event = SellAnalytics.EnterAmountCtaClick,
                        asset = state.sendingAsset,
                        source = state.sendingAccount.toCategory()
                    )
                )
            AssetAction.InterestDeposit ->
                analytics.logEvent(InterestDepositAnalyticsEvent.EnterAmountCtaClick(state.sendingAsset))
            AssetAction.Swap -> {
                analytics.logEvent(
                    SwapAnalyticsEvents.EnterAmountCtaClick(
                        source = state.sendingAsset,
                        target = state.selectedTarget.toCategory()
                    )
                )
                analytics.logEvent(
                    SwapAnalyticsEvents.SwapAmountEntered(
                        amount = state.amount,
                        inputAccountType = TxFlowAnalyticsAccountType.fromAccount(state.sendingAccount),
                        outputCurrency = (state.selectedTarget as CryptoAccount).asset.networkTicker,
                        outputAccountType = TxFlowAnalyticsAccountType.fromAccount(state.selectedTarget)
                    )
                )
            }
            AssetAction.Withdraw ->
                analytics.logEvent(
                    withdrawEvent(
                        WithdrawAnalytics.WITHDRAW_CONFIRM, (state.sendingAccount as FiatAccount).fiatCurrency
                    )
                )
            else -> {
            }
        }
    }

    // Confirm sheet
    fun onConfirmationCtaClick(state: TransactionState) {
        when (state.action) {
            AssetAction.Send -> {
                analytics.logEvent(
                    SendAnalyticsEvent.ConfirmTransaction(
                        asset = state.sendingAsset,
                        source = state.sendingAccount.toCategory(),
                        target = state.selectedTarget.toCategory(),
                        feeLevel = state.pendingTx?.feeSelection?.selectedLevel.toString()
                    )
                )
                analytics.logEvent(
                    SendAnalyticsEvent.SendSubmitted(
                        currency = state.sendingAsset.networkTicker,
                        feeType = state.pendingTx?.feeSelection?.selectedLevel?.toAnalyticsFee()
                            ?: AnalyticsFeeType.NONE,
                        fromAccountType = TxFlowAnalyticsAccountType.fromAccount(state.sendingAccount),
                        toAccountType = TxFlowAnalyticsAccountType.fromTransactionTarget(state.selectedTarget)
                    )
                )
            }
            AssetAction.InterestDeposit ->
                analytics.logEvent(
                    InterestDepositAnalyticsEvent.ConfirmationsCtaClick(
                        state.sendingAsset
                    )
                )
            AssetAction.Sell ->
                analytics.logEvent(
                    SellAnalyticsEvent(
                        event = SellAnalytics.ConfirmTransaction,
                        asset = state.sendingAsset,
                        source = state.sendingAccount.toCategory()
                    )
                )
            AssetAction.Swap ->
                analytics.logEvent(
                    SwapAnalyticsEvents.SwapConfirmCta(
                        source = state.sendingAsset,
                        target = state.selectedTarget.toCategory()
                    )
                )
            AssetAction.Withdraw ->
                analytics.logEvent(
                    withdrawEvent(
                        WithdrawAnalytics.WITHDRAW_CHECKOUT_CONFIRM, (state.sendingAccount as FiatAccount).fiatCurrency
                    )
                )
            else -> {
            }
        }
    }

    // Progress sheet
    fun onTransactionSuccess(state: TransactionState) {
        when (state.action) {
            AssetAction.Send ->
                analytics.logEvent(
                    SendAnalyticsEvent.TransactionSuccess(
                        asset = state.sendingAsset,
                        target = state.selectedTarget.toCategory(),
                        source = state.sendingAccount.toCategory()
                    )
                )
            AssetAction.Sell -> analytics.logEvent(
                SellAnalyticsEvent(
                    event = SellAnalytics.TransactionSuccess,
                    asset = state.sendingAsset,
                    source = state.sendingAccount.toCategory()
                )
            )
            AssetAction.InterestDeposit -> analytics.logEvent(
                InterestDepositAnalyticsEvent.TransactionSuccess(state.sendingAsset)
            )
            AssetAction.Swap -> {
                analytics.logEvent(
                    SwapAnalyticsEvents.TransactionSuccess(
                        asset = state.sendingAsset,
                        target = state.selectedTarget.toCategory(),
                        source = state.sendingAccount.toCategory()
                    )
                )
                require(state.pendingTx != null)
                if (
                    state.sendingAccount is CryptoNonCustodialAccount &&
                    state.selectedTarget is CryptoNonCustodialAccount
                )
                    analytics.logEvent(
                        SwapAnalyticsEvents.OnChainSwapRequested(
                            exchangeRate = state.targetRate?.rate ?: throw IllegalStateException(
                                "Target rate is missing"
                            ),
                            amount = state.pendingTx.amount,
                            inputNetworkFee = state.pendingTx.feeAmount,
                            outputNetworkFee = state.pendingTx.confirmations
                                .filterIsInstance<TxConfirmationValue.NetworkFee>()[0].txFee.fee,
                            outputAmount = state.pendingTx.confirmations
                                .filterIsInstance<TxConfirmationValue.SwapReceiveValue>()[0].receiveAmount
                        )
                    )
            }
            AssetAction.Withdraw ->
                analytics.logEvent(
                    withdrawEvent(
                        WithdrawAnalytics.WITHDRAW_SUCCESS, (state.sendingAccount as FiatAccount).fiatCurrency
                    )
                )
            else -> {
            }
        }
    }

    fun onTransactionFailure(state: TransactionState, error: String) {
        when (state.action) {
            AssetAction.Send -> analytics.logEvent(
                SendAnalyticsEvent.TransactionFailure(
                    asset = state.sendingAsset,
                    target = state.selectedTarget.takeIf { it != NullAddress }?.toCategory(),
                    source = state.sendingAccount.takeIf { it !is NullCryptoAccount }?.toCategory(),
                    error = error
                )
            )
            AssetAction.Sell -> analytics.logEvent(
                SellAnalyticsEvent(
                    event = SellAnalytics.TransactionFailed,
                    asset = state.sendingAsset,
                    source = state.sendingAccount.toCategory()
                )
            )
            AssetAction.InterestDeposit -> analytics.logEvent(
                InterestDepositAnalyticsEvent.TransactionFailed(state.sendingAsset)
            )
            AssetAction.Swap -> analytics.logEvent(
                SwapAnalyticsEvents.TransactionFailed(
                    asset = state.sendingAsset,
                    target = state.selectedTarget.takeIf { it != NullAddress }?.toCategory(),
                    source = state.sendingAccount.takeIf { it !is NullCryptoAccount }?.toCategory(),
                    error = error
                )
            )
            AssetAction.Withdraw ->
                analytics.logEvent(
                    withdrawEvent(
                        WithdrawAnalytics.WITHDRAW_ERROR, (state.sendingAccount as FiatAccount).fiatCurrency
                    )
                )
            else -> {
            }
        }
    }

    fun onFeeLevelChanged(oldLevel: FeeLevel, newLevel: FeeLevel) {
        if (oldLevel != newLevel) {
            analytics.logEvent(SendAnalyticsEvent.FeeChanged(oldLevel, newLevel))
        }
    }

    companion object {
        internal const val PARAM_ASSET = "asset"
        internal const val PARAM_SOURCE = "source"
        internal const val PARAM_TARGET = "target"
        internal const val PARAM_ERROR = "error"
        internal const val PARAM_OLD_FEE = "old_fee"
        internal const val PARAM_NEW_FEE = "new_fee"
        internal const val FEE_SCHEDULE = "fee_level"

        internal fun constructMap(
            asset: CryptoCurrency,
            target: String?,
            error: String? = null,
            source: String? = null
        ): Map<String, String> =
            mapOf(
                PARAM_ASSET to asset.networkTicker,
                PARAM_TARGET to target,
                PARAM_SOURCE to source,
                PARAM_ERROR to error
            ).withoutNullValues()
    }
}

fun BlockchainAccount.toCategory() =
    when (this) {
        is InterestAccount -> WALLET_TYPE_INTEREST
        is TradingAccount -> WALLET_TYPE_CUSTODIAL
        is NonCustodialAccount -> WALLET_TYPE_NON_CUSTODIAL
        is BankAccount -> WALLET_TYPE_BANK
        else -> WALLET_TYPE_UNKNOWN
    }

fun TransactionTarget.toCategory(): String =
    when (this) {
        is CryptoAddress -> WALLET_TYPE_EXTERNAL
        is InterestAccount -> WALLET_TYPE_INTEREST
        is TradingAccount -> WALLET_TYPE_CUSTODIAL
        is NonCustodialAccount -> WALLET_TYPE_NON_CUSTODIAL
        is BankAccount -> WALLET_TYPE_BANK
        else -> WALLET_TYPE_UNKNOWN
    }

enum class TxFlowAnalyticsAccountType {
    TRADING, USERKEY, SAVINGS, EXTERNAL;

    companion object {
        fun fromAccount(account: BlockchainAccount): TxFlowAnalyticsAccountType =
            when (account) {
                is TradingAccount,
                is BankAccount -> TRADING
                is InterestAccount -> SAVINGS
                else -> USERKEY
            }

        fun fromTransactionTarget(transactionTarget: TransactionTarget): TxFlowAnalyticsAccountType {
            (transactionTarget as? BlockchainAccount)?.let {
                return fromAccount(it)
            } ?: return EXTERNAL
        }
    }
}

private fun FeeLevel.toAnalyticsFee(): AnalyticsFeeType =
    when (this) {
        FeeLevel.Custom -> AnalyticsFeeType.CUSTOM
        FeeLevel.Regular -> AnalyticsFeeType.NORMAL
        FeeLevel.None -> AnalyticsFeeType.NONE
        FeeLevel.Priority -> AnalyticsFeeType.PRIORITY
    }

enum class AnalyticsFeeType {
    CUSTOM, NORMAL, PRIORITY, NONE
}

class AmountSwitched(private val action: AssetAction, private val newInput: CurrencyType) : AnalyticsEvent {
    override val event: String
        get() = AnalyticsNames.AMOUNT_SWITCHED.eventName
    override val params: Map<String, Serializable>
        get() = mapOf(
            "product" to action.toAnalyticsProduct(),
            "switch_to" to if (newInput is CurrencyType.Fiat) "FIAT" else "Crypto"
        )
}

private fun AssetAction.toAnalyticsProduct(): String =
    when (this) {
        AssetAction.InterestDeposit -> "Savings"
        else -> this.name
    }
