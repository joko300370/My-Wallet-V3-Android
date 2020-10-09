package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.notifications.analytics.Analytics
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.InterestAccount
import piuk.blockchain.android.coincore.NonCustodialAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.TradingAccount
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.ui.customviews.CurrencyType
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionStep

const val WALLET_TYPE_NON_CUSTODIAL = "non_custodial"
const val WALLET_TYPE_CUSTODIAL = "custodial"
const val WALLET_TYPE_INTEREST = "interest"
const val WALLET_TYPE_EXTERNAL = "external"
const val WALLET_TYPE_UNKNOWN = "unknown"

class TxFlowAnalytics(
    private val analytics: Analytics
) {
    // General
    fun onFlowCanceled(state: TransactionState) {
        when (state.action) {
            AssetAction.NewSend ->
                if (state.currentStep == TransactionStep.CONFIRM_DETAIL)
                    analytics.logEvent(SendAnalyticsEvent.CancelTransaction)
            AssetAction.Sell ->
                if (state.currentStep == TransactionStep.CONFIRM_DETAIL)
                    analytics.logEvent(SellAnalyticsEvent.CancelTransaction)
            else -> { }
        }
    }

    fun onStepChanged(state: TransactionState) {
        when (state.action) {
            AssetAction.NewSend -> triggerSendScreenEvent(state.currentStep)
            AssetAction.Sell -> triggerSellScreenEvent(state.currentStep)
            else -> { }
        }
    }

    private fun triggerSendScreenEvent(step: TransactionStep) {
        when (step) {
            TransactionStep.ENTER_ADDRESS -> analytics.logEvent(SendAnalyticsEvent.EnterAddressDisplayed)
            TransactionStep.ENTER_AMOUNT -> analytics.logEvent(SendAnalyticsEvent.EnterAmountDisplayed)
            TransactionStep.CONFIRM_DETAIL -> analytics.logEvent(SendAnalyticsEvent.ConfirmationsDisplayed)
            else -> { }
        }
    }

    private fun triggerSellScreenEvent(step: TransactionStep) {
        when (step) {
            TransactionStep.CONFIRM_DETAIL -> analytics.logEvent(SellAnalyticsEvent.ConfirmationsDisplayed)
            else -> { }
        }
    }

    fun onStepBackClicked(state: TransactionState) { }

    // Enter address sheet
    fun onManualAddressEntered(state: TransactionState) { }

    fun onScanQrClicked(state: TransactionState) {
        when (state.action) {
            AssetAction.NewSend -> analytics.logEvent(SendAnalyticsEvent.QrCodeScanned)
            else -> {}
        }
    }

    fun onAccountSelected(account: SingleAccount, state: TransactionState) {
        when (state.action) {
            AssetAction.NewSend -> analytics.logEvent(SendAnalyticsEvent.EnterAddressCtaClick)
            else -> {}
        }
    }

    // Enter amount sheet
    fun onMaxClicked(state: TransactionState) {
        when (state.action) {
            AssetAction.NewSend -> analytics.logEvent(SendAnalyticsEvent.SendMaxClicked)
            else -> {}
        }
    }

    fun onCryptoToggle(inputType: CurrencyType, state: TransactionState) { }

    fun onEnterAmountCtaClick(state: TransactionState) {
        when (state.action) {
            AssetAction.NewSend ->
                analytics.logEvent(SendAnalyticsEvent.EnterAmountCtaClick)
            AssetAction.Sell ->
                analytics.logEvent(SellAnalyticsEvent.EnterAmountCtaClick(state.asset))
            else -> {}
        }
    }

    // Confirm sheet
    fun onConfirmationCtaClick(state: TransactionState) {
        when (state.action) {
            AssetAction.NewSend ->
                analytics.logEvent(
                    SendAnalyticsEvent.ConfirmTransaction(
                        asset = state.asset,
                        source = state.sendingAccount.toCategory(),
                        target = state.selectedTarget.toCategory(),
                        feeLevel = state.pendingTx?.feeLevel.toString()
                    )
                )
            AssetAction.Deposit ->
                analytics.logEvent(DepositAnalyticsEvent.ConfirmTransaction)
            AssetAction.Sell ->
                analytics.logEvent(SellAnalyticsEvent.ConfirmTransaction)
            else -> {}
        }
    }

    // Progress sheet
    fun onTransactionSuccess(state: TransactionState) {
        when (state.action) {
            AssetAction.NewSend ->
                analytics.logEvent(
                    SendAnalyticsEvent.TransactionSuccess(
                        asset = state.asset
                    )
                )
            AssetAction.Sell -> analytics.logEvent(SellAnalyticsEvent.TransactionSuccess)
            else -> {}
        }
    }

    fun onTransactionFailure(state: TransactionState) {
        when (state.action) {
            AssetAction.NewSend -> analytics.logEvent(SendAnalyticsEvent.TransactionFailed)
            AssetAction.Sell -> analytics.logEvent(SellAnalyticsEvent.TransactionFailed)
            else -> {}
        }
    }
}

fun SingleAccount.toCategory() =
    when (this) {
        is InterestAccount -> WALLET_TYPE_INTEREST
        is TradingAccount -> WALLET_TYPE_CUSTODIAL
        is NonCustodialAccount -> WALLET_TYPE_NON_CUSTODIAL
        else -> WALLET_TYPE_UNKNOWN
    }

fun TransactionTarget.toCategory(): String =
    when (this) {
        is CryptoAddress -> WALLET_TYPE_EXTERNAL
        is InterestAccount -> WALLET_TYPE_INTEREST
        is TradingAccount -> WALLET_TYPE_CUSTODIAL
        is NonCustodialAccount -> WALLET_TYPE_NON_CUSTODIAL
        else -> WALLET_TYPE_UNKNOWN
    }
