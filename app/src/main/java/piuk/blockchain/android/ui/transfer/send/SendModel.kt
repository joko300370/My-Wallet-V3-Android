package piuk.blockchain.android.ui.transfer.send

import info.blockchain.balance.CryptoValue
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.NullAccount
import piuk.blockchain.android.coincore.NullAddress
import piuk.blockchain.android.coincore.PendingSendTx
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SendProcessor
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import timber.log.Timber

enum class SendStep {
    ZERO,
    ENTER_PASSWORD,
    ENTER_ADDRESS,
    ENTER_AMOUNT,
    CONFIRM_DETAIL,
    IN_PROGRESS,
    SEND_ERROR,
    SEND_COMPLETE
}

data class SendState(
    val currentStep: SendStep = SendStep.ZERO,
    val sendingAccount: CryptoSingleAccount = NullAccount,
    val targetAddress: ReceiveAddress = NullAddress,
    val sendProcessor: SendProcessor? = null, // MOve this to interactor
    val sendAmount: CryptoValue? = null,
    val availableBalance: CryptoValue? = null,
    val passwordRequired: Boolean = false,
    val secondPassword: String = "",
    val nextEnabled: Boolean = false
) : MviState {
    // Placeholders - these will make more sense when BitPay and/or URL based sends are in place
    // Question: If we scan a bitpay invoice, do we show the amount screen?
    val initialAmount: Single<CryptoValue> = Single.just(CryptoValue.zero(sendingAccount.asset))
    val canEditAmount: Boolean = true // Will be false for URL or BitPay txs
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
        Timber.d("!SEND!> Send Model: performAction: ${intent.javaClass.simpleName}")

        return when (intent) {
            is SendIntent.Initialise -> null
            is SendIntent.ValidatePassword -> processPasswordValidation(intent.password)
            is SendIntent.UpdatePasswordIsValidated -> null
            is SendIntent.UpdatePasswordNotValidated -> null
            is SendIntent.AddressSelected -> null
            is SendIntent.PrepareTransaction -> null
            is SendIntent.ExecuteTransaction -> null
            is SendIntent.AddressSelectionConfirmed -> processAddressConfirmation(previousState)
            is SendIntent.UpdateSendProcessor -> null
            is SendIntent.FatalTransactionError -> null
            is SendIntent.SendAmountChanged -> processAmountChanged(intent.amount, previousState)
            is SendIntent.UpdateTransactionAmounts -> null
        }
    }

    override fun onScanLoopError(t: Throwable) {
        Timber.d("!SEND!> Send Model: loop error -> $t")
    }

    override fun onStateUpdate(s: SendState) {
        Timber.d("!SEND!> Send Model: state update -> $s")
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

        interactor.fetchSendTransaction(state.sendingAccount, state.targetAddress)
            .subscribeBy(
                onSuccess = {
                    process(SendIntent.UpdateSendProcessor(it))
                },
                onError = {
                    // If we get here, then something has gone badly wrong!
                    Timber.e("Unable to get transaction processor")
                    process(SendIntent.FatalTransactionError)
                }
            )

        private fun processAmountChanged(amount: CryptoValue, state: SendState): Disposable =
            state.sendProcessor?.let { tx ->
                tx.availableBalance(
                    PendingSendTx(amount)
                )
                .subscribeBy(
                    onSuccess = {
                        process(SendIntent.UpdateTransactionAmounts(amount, it))
                    },
                    onError = {
                        // If we get here, then something has gone badly wrong!
                        Timber.e("Unable to get update available balance")
                        process(SendIntent.FatalTransactionError)
                    }
                )
            } ?: throw IllegalStateException("No send processor found!")
}