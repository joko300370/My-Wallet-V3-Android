package piuk.blockchain.android.ui.transfer.send

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.NullAddress
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.SendTarget
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import timber.log.Timber
import java.util.Stack

enum class SendStep(val addToBackStack: Boolean = false) {
    ZERO,
    ENTER_PASSWORD,
    ENTER_ADDRESS(true),
    ENTER_AMOUNT(true),
    CONFIRM_DETAIL,
    IN_PROGRESS
}

enum class SendErrorState {
    NONE,
    INVALID_PASSWORD,
    INVALID_ADDRESS,
    ADDRESS_IS_CONTRACT,
    INSUFFICIENT_FUNDS,
    INVALID_AMOUNT,
    BELOW_MIN_LIMIT,
    ABOVE_MAX_LIMIT,
    NOT_ENOUGH_GAS,
    UNEXPECTED_ERROR
}

enum class TxExecutionStatus {
    NOT_STARTED,
    IN_PROGRESS,
    ERROR,
    COMPLETED
}

data class SendState(
    val action: AssetAction = AssetAction.NewSend,
    val currentStep: SendStep = SendStep.ZERO,
    val sendingAccount: CryptoAccount = NullCryptoAccount(),
    val sendTarget: SendTarget = NullAddress,
    val fiatRate: ExchangeRate.CryptoToFiat? = null,
    val targetRate: ExchangeRate? = null,
    val passwordRequired: Boolean = false,
    val secondPassword: String = "",
    val nextEnabled: Boolean = false,
    val errorState: SendErrorState = SendErrorState.NONE,
    val pendingTx: PendingTx? = null,
    val allowFiatInput: Boolean = false,
    val executionStatus: TxExecutionStatus = TxExecutionStatus.NOT_STARTED,
    val stepsBackStack: Stack<SendStep> = Stack()
) : MviState {

    val asset: CryptoCurrency = sendingAccount.asset

    val sendAmount: Money
        get() = pendingTx?.amount ?: CryptoValue.zero(asset) // TODO: BEtter default required

    val availableBalance: Money
        get() = pendingTx?.available ?: CryptoValue.zero(sendingAccount.asset) // TODO: BEtter default required

    val feeAmount: Money
        get() = pendingTx?.fees ?: throw IllegalStateException("No pending tx, fees unavailable")

    val canGoBack: Boolean
        get() = stepsBackStack.isNotEmpty()
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
            is SendIntent.InitialiseWithSourceAccount -> null
            is SendIntent.ValidatePassword -> processPasswordValidation(intent.password)
            is SendIntent.UpdatePasswordIsValidated -> null
            is SendIntent.UpdatePasswordNotValidated -> null
            is SendIntent.PrepareTransaction -> null
            is SendIntent.ExecuteTransaction -> processExecuteTransaction()
            is SendIntent.ValidateInputTargetAddress ->
                processValidateAddress(intent.targetAddress, intent.expectedCrypto)
            is SendIntent.TargetAddressValidated -> null
            is SendIntent.TargetAddressInvalid -> null
            is SendIntent.InitialiseWithSourceAndTargetAccount -> {
                process(SendIntent.TargetSelectionConfirmed(intent.toAccount))
                null
            }
            is SendIntent.TargetSelectionConfirmed ->
                processTargetSelectionConfirmed(
                    previousState.sendingAccount,
                    previousState.sendAmount,
                    intent.sendTarget
                )
            is SendIntent.PendingTransactionStarted -> null
            is SendIntent.FatalTransactionError -> null
            is SendIntent.SendAmountChanged -> processAmountChanged(intent.amount)
            is SendIntent.ModifyTxOption -> processModifyTxOptionRequest(intent.option)
            is SendIntent.PendingTxUpdated -> null
            is SendIntent.UpdateTransactionComplete -> null
            is SendIntent.ReturnToPreviousStep -> null
            is SendIntent.FetchFiatRates -> processGetFiatRate()
            is SendIntent.FetchTargetRates -> processGetTargetRate()
            is SendIntent.FiatRateUpdated -> null
            is SendIntent.CryptoRateUpdated -> null
            is SendIntent.ValidateTransaction -> processValidateTransaction()
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
                        is TxValidationFailure -> process(
                            SendIntent.TargetAddressInvalid(it))
                        else -> process(SendIntent.FatalTransactionError(it))
                    }
                }
            )

    // At this point we can build a transactor object from coincore and configure
    // the state object a bit more; depending on whether it's an internal, external,
    // bitpay or BTC Url address we can set things like note, amount, fee schedule
    // and hook up the correct processor to execute the transaction.
    private fun processTargetSelectionConfirmed(
        sourceAccount: SingleAccount,
        amount: Money,
        sendTarget: SendTarget
    ): Disposable =
        interactor.initialiseTransaction(sourceAccount, sendTarget)
            .doOnFirst { onFirstUpdate(amount) }
            .subscribeBy(
                onNext = {
                    process(SendIntent.PendingTxUpdated(it))
                },
                onError = {
                    Timber.e("!SEND!> Processor failed: $it")
                    process(SendIntent.FatalTransactionError(it))
                }
            )

    private fun onFirstUpdate(
        amount: Money
    ) {
        process(SendIntent.PendingTransactionStarted(interactor.canTransactFiat))
        process(SendIntent.FetchFiatRates)
        process(SendIntent.FetchTargetRates)
        process(SendIntent.SendAmountChanged(amount))
    }

    private fun processAmountChanged(amount: Money): Disposable =
        interactor.updateTransactionAmount(amount)
            .subscribeBy(
                onError = {
                    Timber.e("!SEND!> Unable to get update available balance")
                    process(SendIntent.FatalTransactionError(it))
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
            onError = {
                Timber.e("Failed updating Tx options")
            }
        )

    private fun processGetFiatRate(): Disposable =
        interactor.startFiatRateFetch()
            .subscribeBy(
                onNext = { process(SendIntent.FiatRateUpdated(it)) },
                onComplete = { Timber.d("Fiat exchange Rate completed") },
                onError = { Timber.e("Failed getting exchange rate") }
            )

    private fun processGetTargetRate(): Disposable =
        interactor.startTargetRateFetch()
            .subscribeBy(
                onNext = { process(SendIntent.CryptoRateUpdated(it)) },
                onComplete = { Timber.d("Target exchange Rate completed") },
                onError = { Timber.e("Failed getting target exchange rate") }
            )

    override fun distinctIntentFilter(
        previousIntent: SendIntent,
        nextIntent: SendIntent
    ): Boolean {
        return when (previousIntent) {
            // Allow consecutive ReturnToPreviousStep intents
            is SendIntent.ReturnToPreviousStep -> {
                if (nextIntent is SendIntent.ReturnToPreviousStep) {
                    false
                } else {
                    super.distinctIntentFilter(previousIntent, nextIntent)
                }
            }
            else -> super.distinctIntentFilter(previousIntent, nextIntent)
        }
    }

    private fun processValidateTransaction(): Disposable? =
        interactor.validateTransaction()
            .subscribeBy(
                onError = {
                    Timber.e("!SEND!> Unable to validate transaction: $it")
                    process(SendIntent.FatalTransactionError(it))
                }
            )
}

fun <T> Observable<T>.doOnFirst(onAction: (T) -> Unit): Observable<T> =
    take(1)
        .doOnNext { onAction.invoke(it) }
        .concatWith(skip(1))