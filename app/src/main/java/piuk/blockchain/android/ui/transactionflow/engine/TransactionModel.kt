package piuk.blockchain.android.ui.transactionflow.engine

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
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import timber.log.Timber
import java.util.Stack

enum class TransactionStep(val addToBackStack: Boolean = false) {
    ZERO,
    ENTER_PASSWORD,
    ENTER_ADDRESS(true),
    ENTER_AMOUNT(true),
    CONFIRM_DETAIL,
    IN_PROGRESS
}

enum class TransactionErrorState {
    NONE,
    INVALID_PASSWORD,
    INVALID_ADDRESS,
    ADDRESS_IS_CONTRACT,
    INSUFFICIENT_FUNDS,
    INVALID_AMOUNT,
    BELOW_MIN_LIMIT,
    ABOVE_MAX_LIMIT,
    NOT_ENOUGH_GAS,
    UNEXPECTED_ERROR,
    TRANSACTION_IN_FLIGHT,
    TX_OPTION_INVALID
}

enum class TxExecutionStatus {
    NOT_STARTED,
    IN_PROGRESS,
    ERROR,
    COMPLETED
}

data class TransactionState(
    val action: AssetAction = AssetAction.NewSend,
    val currentStep: TransactionStep = TransactionStep.ZERO,
    val sendingAccount: CryptoAccount = NullCryptoAccount(),
    val selectedTarget: TransactionTarget = NullAddress,
    val fiatRate: ExchangeRate.CryptoToFiat? = null,
    val targetRate: ExchangeRate? = null,
    val passwordRequired: Boolean = false,
    val secondPassword: String = "",
    val nextEnabled: Boolean = false,
    val errorState: TransactionErrorState = TransactionErrorState.NONE,
    val pendingTx: PendingTx? = null,
    val allowFiatInput: Boolean = false,
    val executionStatus: TxExecutionStatus = TxExecutionStatus.NOT_STARTED,
    val stepsBackStack: Stack<TransactionStep> = Stack(),
    val availableTargets: List<TransactionTarget> = emptyList()
) : MviState {

    val asset: CryptoCurrency = sendingAccount.asset

    val amount: Money
        get() = pendingTx?.amount ?: CryptoValue.zero(asset) // TODO: BEtter default required

    val availableBalance: Money
        get() = pendingTx?.available ?: CryptoValue.zero(sendingAccount.asset) // TODO: BEtter default required

    val feeAmount: Money
        get() = pendingTx?.fees ?: throw IllegalStateException("No pending tx, fees unavailable")

    val canGoBack: Boolean
        get() = stepsBackStack.isNotEmpty()
}

class TransactionModel(
    initialState: TransactionState,
    mainScheduler: Scheduler,
    private val interactor: TransactionInteractor
) : MviModel<TransactionState, TransactionIntent>(
    initialState,
    mainScheduler
) {
    override fun performAction(previousState: TransactionState, intent: TransactionIntent): Disposable? {
        Timber.v("!SEND!> Send Model: performAction: ${intent.javaClass.simpleName}")

        return when (intent) {
            is TransactionIntent.InitialiseWithSourceAccount -> null
            is TransactionIntent.ValidatePassword -> processPasswordValidation(intent.password)
            is TransactionIntent.UpdatePasswordIsValidated -> null
            is TransactionIntent.UpdatePasswordNotValidated -> null
            is TransactionIntent.PrepareTransaction -> null
            is TransactionIntent.ExecuteTransaction -> processExecuteTransaction()
            is TransactionIntent.ValidateInputTargetAddress ->
                processValidateAddress(intent.targetAddress, intent.expectedCrypto)
            is TransactionIntent.TargetAddressValidated -> null
            is TransactionIntent.TargetAddressInvalid -> null
            is TransactionIntent.InitialiseWithSourceAndTargetAccount -> {
                process(
                    TransactionIntent.TargetSelectionConfirmed(
                        intent.toAccount
                    )
                )
                null
            }
            is TransactionIntent.TargetSelectionConfirmed ->
                processTargetSelectionConfirmed(
                    previousState.sendingAccount,
                    previousState.amount,
                    intent.transactionTarget,
                    previousState.action
                )
            is TransactionIntent.PendingTransactionStarted -> null
            is TransactionIntent.FatalTransactionError -> null
            is TransactionIntent.AmountChanged -> processAmountChanged(intent.amount)
            is TransactionIntent.ModifyTxOption -> processModifyTxOptionRequest(intent.option)
            is TransactionIntent.PendingTxUpdated -> null
            is TransactionIntent.UpdateTransactionComplete -> null
            is TransactionIntent.ReturnToPreviousStep -> null
            is TransactionIntent.FetchFiatRates -> processGetFiatRate()
            is TransactionIntent.FetchTargetRates -> processGetTargetRate()
            is TransactionIntent.FiatRateUpdated -> null
            is TransactionIntent.CryptoRateUpdated -> null
            is TransactionIntent.ValidateTransaction -> processValidateTransaction()
            is TransactionIntent.EnteredAddressReset -> null
            is TransactionIntent.InvalidateTransaction -> processInvalidateTransaction()
        }
    }

    override fun onScanLoopError(t: Throwable) {
        Timber.e("!SEND!> Send Model: loop error -> $t")
    }

    override fun onStateUpdate(s: TransactionState) {
        Timber.v("!SEND!> Send Model: state update -> $s")
    }

    private fun processInvalidateTransaction(): Disposable? =
        interactor.invalidateTransaction()
            .subscribeBy(
                onComplete = {
                    process(TransactionIntent.ReturnToPreviousStep)
                },
                onError = {
                    process(
                        TransactionIntent.FatalTransactionError(
                            it
                        )
                    )
                }
            )

    private fun processPasswordValidation(password: String) =
        interactor.validatePassword(password)
            .subscribeBy(
                onSuccess = {
                    process(
                        if (it) {
                            TransactionIntent.UpdatePasswordIsValidated(
                                password
                            )
                        } else {
                            TransactionIntent.UpdatePasswordNotValidated
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
                    process(
                        TransactionIntent.TargetAddressValidated(
                            it
                        )
                    )
                },
                onError = {
                    when (it) {
                        is TxValidationFailure -> process(
                            TransactionIntent.TargetAddressInvalid(
                                it
                            )
                        )
                        else -> process(
                            TransactionIntent.FatalTransactionError(
                                it
                            )
                        )
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
        transactionTarget: TransactionTarget,
        action: AssetAction
    ): Disposable =
        interactor.initialiseTransaction(sourceAccount, transactionTarget, action)
            .doOnFirst { onFirstUpdate(amount) }
            .subscribeBy(
                onNext = {
                    process(
                        TransactionIntent.PendingTxUpdated(
                            it
                        )
                    )
                },
                onError = {
                    Timber.e("!SEND!> Processor failed: $it")
                    process(
                        TransactionIntent.FatalTransactionError(
                            it
                        )
                    )
                }
            )

    private fun onFirstUpdate(
        amount: Money
    ) {
        process(
            TransactionIntent.PendingTransactionStarted(
                interactor.canTransactFiat
            )
        )
        process(TransactionIntent.FetchFiatRates)
        process(TransactionIntent.FetchTargetRates)
        process(
            TransactionIntent.AmountChanged(
                amount
            )
        )
    }

    private fun processAmountChanged(amount: Money): Disposable =
        interactor.updateTransactionAmount(amount)
            .subscribeBy(
                onError = {
                    Timber.e("!SEND!> Unable to get update available balance")
                    process(
                        TransactionIntent.FatalTransactionError(
                            it
                        )
                    )
                }
            )

    private fun processExecuteTransaction(): Disposable? =
        interactor.verifyAndExecute()
            .subscribeBy(
                onComplete = {
                    process(TransactionIntent.UpdateTransactionComplete)
                },
                onError = {
                    Timber.e("!SEND!> Unable to execute transaction: $it")
                    process(
                        TransactionIntent.FatalTransactionError(
                            it
                        )
                    )
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
                onNext = { process(
                    TransactionIntent.FiatRateUpdated(
                        it
                    )
                ) },
                onComplete = { Timber.d("Fiat exchange Rate completed") },
                onError = { Timber.e("Failed getting exchange rate") }
            )

    private fun processGetTargetRate(): Disposable =
        interactor.startTargetRateFetch()
            .subscribeBy(
                onNext = { process(
                    TransactionIntent.CryptoRateUpdated(
                        it
                    )
                ) },
                onComplete = { Timber.d("Target exchange Rate completed") },
                onError = { Timber.e("Failed getting target exchange rate") }
            )

    override fun distinctIntentFilter(
        previousIntent: TransactionIntent,
        nextIntent: TransactionIntent
    ): Boolean {
        return when (previousIntent) {
            // Allow consecutive ReturnToPreviousStep intents
            is TransactionIntent.ReturnToPreviousStep -> {
                if (nextIntent is TransactionIntent.ReturnToPreviousStep) {
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
                    process(
                        TransactionIntent.FatalTransactionError(
                            it
                        )
                    )
                }
            )
}

private var firstCall = true
fun <T> Observable<T>.doOnFirst(onAction: (T) -> Unit): Observable<T> {
    firstCall = true
    return this.doOnNext {
        if (firstCall) {
            onAction.invoke(it)
            firstCall = false
        }
    }
}
