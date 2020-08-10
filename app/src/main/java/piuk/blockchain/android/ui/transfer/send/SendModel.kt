package piuk.blockchain.android.ui.transfer.send

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.coincore.NullAddress
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.SendTarget
import piuk.blockchain.android.coincore.TransactionValidationError
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import timber.log.Timber
import java.lang.IllegalStateException

enum class SendStep {
    ZERO,
    ENTER_PASSWORD,
    ENTER_ADDRESS,
    ENTER_AMOUNT,
    CONFIRM_DETAIL,
    IN_PROGRESS
}

enum class SendErrorState {
    NONE,
    INVALID_PASSWORD,
    INVALID_ADDRESS,
    ADDRESS_IS_CONTRACT,
//    FEE_REQUEST_FAILED,
    MAX_EXCEEDED,
    MIN_REQUIRED,
    NOT_ENOUGH_GAS,
    UNEXPECTED_ERROR
}

enum class TransactionInFlightState {
    IN_PROGRESS,
    ERROR,
    COMPLETED,
    NOT_STARTED
}

data class SendState(
    val action: AssetAction = AssetAction.NewSend,
    val currentStep: SendStep = SendStep.ZERO,
    val sendingAccount: CryptoAccount = NullCryptoAccount,
    val sendTarget: SendTarget = NullAddress,
    val passwordRequired: Boolean = false,
    val secondPassword: String = "",
    val nextEnabled: Boolean = false,
    val errorState: SendErrorState = SendErrorState.NONE,
    val pendingTx: PendingTx? = null,
    val transactionInFlight: TransactionInFlightState = TransactionInFlightState.NOT_STARTED
) : MviState {

    val asset: CryptoCurrency = sendingAccount.asset

    val sendAmount: CryptoValue
        get() = pendingTx?.amount ?: CryptoValue.zero(asset)

    val availableBalance: CryptoValue
        get() = pendingTx?.available ?: CryptoValue.zero(sendingAccount.asset)

    val feeAmount: CryptoValue
        get() = pendingTx?.fees ?: throw IllegalStateException("No pending tx, fees unavailable")
}

class SendModel(
    initialState: SendState,
    mainScheduler: Scheduler,
    private val interactor: SendInteractor
) : MviModel<SendState, SendIntent>(
    initialState,
    mainScheduler
) {
    override fun performAction(previousState: SendState, intent: SendIntent): Disposable? {
        Timber.v("!SEND!> Send Model: performAction: ${intent.javaClass.simpleName}")

        return when (intent) {
            is SendIntent.Initialise -> null
            is SendIntent.ValidatePassword -> processPasswordValidation(intent.password)
            is SendIntent.UpdatePasswordIsValidated -> null
            is SendIntent.UpdatePasswordNotValidated -> null
            is SendIntent.PrepareTransaction -> null
            is SendIntent.ExecuteTransaction -> processExecuteTransaction()
            is SendIntent.ValidateInputTargetAddress ->
                processValidateAddress(intent.targetAddress, intent.expectedCrypto)
            is SendIntent.TargetAddressValidated -> null
            is SendIntent.TargetAddressInvalid -> null
            is SendIntent.TargetSelectionConfirmed ->
                processAddressConfirmation(
                    previousState.sendingAccount,
                    previousState.sendAmount,
                    intent.sendTarget
                )
            is SendIntent.SendAmountChanged -> processAmountChanged(intent.amount)
            is SendIntent.ModifyTxOption -> processModifyTxOptionRequest(intent.option)
            is SendIntent.PendingTxUpdated -> null
            is SendIntent.UpdateTransactionComplete -> null
            is SendIntent.InputValidationError -> null
            is SendIntent.FatalTransactionError -> null
            is SendIntent.ReturnToPreviousStep -> null
        }
    }

    override fun onScanLoopError(t: Throwable) {
        Timber.e("!SEND!> Send Model: loop error -> $t")
    }

    override fun onStateUpdate(s: SendState) {
        Timber.v("!SEND!> Send Model: state update -> $s")
    }

    private fun processPasswordValidation(password: String) =
        interactor.validatePassword(password)
            .subscribeBy(
                onSuccess = {
                    process(
                        if (it) {
                            SendIntent.UpdatePasswordIsValidated(password)
                        } else {
                            SendIntent.UpdatePasswordNotValidated
                        }
                    )
                },
                onError = { /* Error! What to do? Abort? Or... */ }
            )

    private fun processValidateAddress(
        address: String,
        expectedAsset: CryptoCurrency
    ): Disposable =
        interactor.validateTargetAddress(address, expectedAsset)
            .subscribeBy(
                onSuccess = {
                    process(SendIntent.TargetAddressValidated(it))
                },
                onError = {
                    when (it) {
                        is TransactionValidationError -> process(SendIntent.TargetAddressInvalid(it))
                        else -> process(SendIntent.FatalTransactionError(it))
                    }
                }
            )

    private fun processAddressConfirmation(
        sourceAccount: SingleAccount,
        amount: CryptoValue,
        sendTarget: SendTarget
    ): Disposable =
        // At this point we can build a transactor object from coincore and configure
        // the state object a bit more; depending on whether it's an internal, external,
        // bitpay or BTC Url address we can set things like note, amount, fee schedule
        // and hook up the correct processor to execute the transaction.
        interactor.initialiseTransaction(sourceAccount, sendTarget)
            .subscribeBy(
                onSuccess = {
                    process(SendIntent.TargetAddressValidated(sendTarget))
                    process(SendIntent.PendingTxUpdated(it))
                    process(SendIntent.SendAmountChanged(amount))
                },
                onError = {
                    Timber.e("!SEND!> Unable to get transaction processor: $it")
                    process(SendIntent.FatalTransactionError(it))
                }
            )

    private fun processAmountChanged(amount: CryptoValue): Disposable =
        interactor.updateTransactionAmount(amount)
            .subscribeBy(
                onSuccess = { pendingTx ->
                    process(SendIntent.PendingTxUpdated(pendingTx))
                },
                onError = {
                    if (it is TransactionValidationError) {
                        process(SendIntent.InputValidationError(it))
                    } else {
                        Timber.e("!SEND!> Unable to get update available balance")
                        process(SendIntent.FatalTransactionError(it))
                    }
            }
        )

    private fun processExecuteTransaction(): Disposable? =
        interactor.verifyAndExecute()
            .subscribeBy(
                onComplete = {
                    process(SendIntent.UpdateTransactionComplete)
                },
                onError = {
                    Timber.e("!SEND!> Unable to execute transaction: $it")
                    process(SendIntent.FatalTransactionError(it))
                }
            )

    private fun processModifyTxOptionRequest(newOption: TxOptionValue): Disposable? =
        interactor.modifyOptionValue(
            newOption
        ).subscribeBy(
            onSuccess = { pendingTx ->
                process(SendIntent.PendingTxUpdated(pendingTx))
            },
            onError = {
                // TODO: Report problem
            }
        )
}