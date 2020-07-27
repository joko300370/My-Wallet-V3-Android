package piuk.blockchain.android.ui.transfer.send

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.coincore.NullAddress
import piuk.blockchain.android.coincore.PendingSendTx
import piuk.blockchain.android.coincore.SendTarget
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.androidcore.utils.extensions.thenSingle
import timber.log.Timber

enum class SendStep {
    ZERO,
    ENTER_PASSWORD,
    ENTER_ADDRESS,
    ENTER_AMOUNT,
    CONFIRM_DETAIL,
    IN_PROGRESS
}

enum class SendErrorState {
    FEE_REQUEST_FAILED,
    MAX_EXCEEDED,
    MIN_REQUIRED,
    NONE
}

enum class NoteState {
    NOT_SET,
    UPDATE_SUCCESS,
    UPDATE_ERROR
}

enum class TransactionInFlightState {
    IN_PROGRESS,
    ERROR,
    COMPLETED,
    NOT_STARTED
}

data class SendState(
    val currentStep: SendStep = SendStep.ZERO,
    val sendingAccount: CryptoAccount = NullCryptoAccount,
    val sendTarget: SendTarget = NullAddress,
    val sendAmount: Money = CryptoValue.zero(sendingAccount.asset),
    val availableBalance: Money = CryptoValue.zero(sendingAccount.asset),
    val passwordRequired: Boolean = false,
    val secondPassword: String = "",
    val nextEnabled: Boolean = false,
    val errorState: SendErrorState = SendErrorState.NONE,
    val feeAmount: Money = CryptoValue.zero(sendingAccount.feeAsset ?: sendingAccount.asset),
    val transactionNoteSupported: Boolean? = null,
    val noteState: NoteState = NoteState.NOT_SET,
    val note: String = "",
    val transactionInFlight: TransactionInFlightState = TransactionInFlightState.NOT_STARTED
) : MviState {
    // Placeholders - these will make more sense when BitPay and/or URL based sends are in place
    // Question: If we scan a bitpay invoice, do we show the amount screen?
    val initialAmount: Single<CryptoValue> = Single.just(CryptoValue.zero(sendingAccount.asset))
    val canEditAmount: Boolean = true // Will be false for URL or BitPay txs

    val asset: CryptoCurrency = sendingAccount.asset
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
            is SendIntent.TargetSelected -> null
            is SendIntent.PrepareTransaction -> null
            is SendIntent.ExecuteTransaction -> processExecuteTransaction(previousState)
            is SendIntent.TargetSelectionConfirmed -> processAddressConfirmation(previousState)
            is SendIntent.FatalTransactionError -> null
            is SendIntent.SendAmountChanged -> processAmountChanged(intent.amount, previousState)
            is SendIntent.UpdateTransactionAmounts -> null
            is SendIntent.UpdateTransactionComplete -> null
            is SendIntent.ReturnToPreviousStep -> null
            is SendIntent.MaxAmountExceeded -> null
            is SendIntent.MinRequired -> null
            is SendIntent.RequestFee -> processFeeRequest(previousState.sendAmount)
            is SendIntent.FeeRequestError -> null
            is SendIntent.FeeUpdate -> null
            is SendIntent.RequestTransactionNoteSupport -> processTransactionNoteSupport()
            is SendIntent.TransactionNoteSupported -> null
            is SendIntent.NoteAdded -> null
        }
    }

    private fun processTransactionNoteSupport(): Disposable =
        interactor.checkIfNoteSupported()
            .subscribeBy(
                onSuccess = {
                    process(SendIntent.TransactionNoteSupported)
                },
                onError = {
                    Timber.e("Error when getting note supported $it")
                }
            )

    private fun processFeeRequest(amount: Money): Disposable =
        interactor.getFeeForTransaction(
            PendingSendTx(amount)
        ).subscribeBy(
            onSuccess = {
                process(SendIntent.FeeUpdate(it))
            },
            onError = {
                process(SendIntent.FeeRequestError)
            }
        )

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

    private fun processAddressConfirmation(state: SendState): Disposable =
    // At this point we can build a transactor object from coincore and configure
    // the state object a bit more; depending on whether it's an internal, external,
    // bitpay or BTC Url address we can set things like note, amount, fee schedule
        // and hook up the correct processor to execute the transaction.
        interactor.initialiseTransaction(state.sendingAccount, state.sendTarget)
            .thenSingle {
                interactor.getAvailableBalance(
                    PendingSendTx(
                        amount = state.sendAmount
                    )
                )
            }
            .subscribeBy(
                onSuccess = {
                    process(SendIntent.UpdateTransactionAmounts(state.sendAmount, it))
                },
                onError = {
                    Timber.e("!SEND!> Unable to get transaction processor: $it")
                    process(SendIntent.FatalTransactionError(it))
                }
            )

    private fun processAmountChanged(amount: CryptoValue, state: SendState): Disposable =
        interactor.getAvailableBalance(
            PendingSendTx(amount)
        )
            .subscribeBy(
                onSuccess = {
                    if (amount > it) {
                        process(SendIntent.MaxAmountExceeded)
                    } else if (!amount.isPositive && !amount.isZero) {
                        process(SendIntent.MinRequired)
                    } else {
                        process(SendIntent.UpdateTransactionAmounts(amount, it))
                    }
                },
                onError = {
                    Timber.e("!SEND!> Unable to get update available balance")
                    process(SendIntent.FatalTransactionError(it))
                }
            )

    private fun processExecuteTransaction(state: SendState): Disposable =
        interactor.verifyAndExecute(
            PendingSendTx(state.sendAmount, notes = state.note)
        ).subscribeBy(
            onComplete = {
                process(SendIntent.UpdateTransactionComplete)
            },
            onError = {
                Timber.e("!SEND!> Unable to execute transaction: $it")
                process(SendIntent.FatalTransactionError(it))
            }
        )
}